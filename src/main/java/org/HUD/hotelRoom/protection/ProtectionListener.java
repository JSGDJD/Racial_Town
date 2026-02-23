package org.HUD.hotelRoom.protection;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.util.SpatialIndex;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

import static org.HUD.hotelRoom.util.SelectionMgr.*;

/**
 * 领地保护监听器
 */
public class ProtectionListener implements Listener {

    private final HotelRoom plugin;
    private Set<Material> allowedPublicBlocks = new HashSet<>();

    public ProtectionListener(HotelRoom plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        List<String> blockList = config.getStringList("public-hotel-blocks.allowed-blocks");
        Set<Material> newAllowedBlocks = new HashSet<>();
        for (String blockName : blockList) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                newAllowedBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in public-hotel-blocks config: " + blockName);
            }
        }
        this.allowedPublicBlocks = newAllowedBlocks;
    }
    
    public void reloadConfig() {
        loadConfig();
    }


    /* ========== 破坏 ========== */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();
        HotelInfo hi = getHotelAt(loc);
        if (hi == null) return;          // 不在 HR 领地，不管

        /* HR 领地内最终裁决 */
        if (p.hasPermission("hotelroom.admin")) {
            SQLiteStorage.untrackPlace(e.getBlock());
            e.setCancelled(false);       // 管理员强制允许
            return;
        }
        
        // 检查是否为官方房屋
        if (hi.isOfficial) {
            // 官方房屋不允许普通玩家破坏
            e.setCancelled(true);
            warn(p);
            return;
        }
        
        // 检查是否为公共房屋
        if (hi.isPublic) {
            // 检查玩家是否是公共房屋的成员
            if (!hi.members.contains(p.getUniqueId())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            
            // 检查方块是否在允许列表中
            if (!allowedPublicBlocks.contains(e.getBlock().getType())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            
            // 在公共房屋中，玩家只能破坏自己放置的方块
            UUID placer = SQLiteStorage.getPlacer(e.getBlock());
            if (placer != null && !placer.equals(p.getUniqueId())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            // 如果方块没有记录（即原始方块或外观），则不允许破坏
            if (placer == null && !FacadeProtection.isFacade(hi, loc)) {
                // 允许破坏非外观的原始方块
                e.setCancelled(false);
                SQLiteStorage.untrackPlace(e.getBlock());
                return;
            }
            if (FacadeProtection.isFacade(hi, loc)) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            e.setCancelled(false);
            SQLiteStorage.untrackPlace(e.getBlock());
            return;
        }
        
        if (FacadeProtection.isFacade(hi, loc) || isInOthersHotel(p, loc)) {
            e.setCancelled(true);        // HR 拒绝
            warn(p);
            return;
        }
        UUID placer = SQLiteStorage.getPlacer(e.getBlock());
        if (placer != null && !placer.equals(p.getUniqueId())) {
            e.setCancelled(true);
            warn(p);
            return;
        }
        /* 允许 */
        e.setCancelled(false);
        SQLiteStorage.untrackPlace(e.getBlock());
    }



    /* ========== 放置 ========== */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();
        HotelInfo hi = getHotelAt(loc);
        if (hi == null) return;

        // 检查是否为官方房屋
        if (hi.isOfficial) {
            // 官方房屋不允许普通玩家放置
            e.setCancelled(true);
            warn(p);
            return;
        }
        
        // 检查是否为公共房屋
        if (hi.isPublic) {
            // 检查玩家是否是公共房屋的成员
            if (!hi.members.contains(p.getUniqueId())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            
            // 检查方块是否在允许列表中
            if (!allowedPublicBlocks.contains(e.getBlock().getType())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            
            // 在公共房屋中，成员可以放置方块
            e.setCancelled(false);               // 允许
            SQLiteStorage.trackPlace(e.getBlock(), p.getUniqueId());
            return;
        }
        
        if (isInOthersHotel(p, loc)) {
            e.setCancelled(true);
            warn(p);
            return;
        }
        e.setCancelled(false);               // 允许
        SQLiteStorage.trackPlace(e.getBlock(), p.getUniqueId());
    }


    /* ========== 交互（容器/功能方块） ========== */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        Location loc = e.getClickedBlock().getLocation();
        HotelInfo hi = getHotelAt(loc);
        if (hi == null) return;

        Material type = e.getClickedBlock().getType();
        
        // 检查是否为官方房屋
        if (hi.isOfficial) {
            // 官方房屋不允许普通玩家交互
            e.setCancelled(true);
            warn(p);
            return;
        }
        
        // 检查是否为公共房屋
        if (hi.isPublic) {
            // 在公共房屋中，玩家必须是成员才能交互
            if (!hi.members.contains(p.getUniqueId())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            
            // 在公共房屋中，玩家只能与自己放置的容器/功能方块交互
            UUID placer = SQLiteStorage.getPlacer(e.getClickedBlock());
            if (isContainerOrUtility(type) && placer != null && !placer.equals(p.getUniqueId())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            e.setCancelled(false);               // 允许
            return;
        }
        
        if (isContainerOrUtility(type) && isInOthersHotel(p, loc)) {
            e.setCancelled(true);
            warn(p);
            return;
        }
        e.setCancelled(false);               // 允许
    }

    /* ========== 打开容器兜底 ========== */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOpenInv(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        Location loc = null;
        try { loc = e.getInventory().getLocation(); } catch (Exception ignored) {}
        if (loc == null) return;
        HotelInfo hi = getHotelAt(loc);
        if (hi == null) return;

        // 检查是否为官方房屋
        if (hi.isOfficial) {
            // 官方房屋不允许普通玩家打开容器
            e.setCancelled(true);
            warn(p);
            return;
        }
        
        // 检查是否为公共房屋
        if (hi.isPublic) {
            // 在公共房屋中，玩家必须是成员才能打开容器
            if (!hi.members.contains(p.getUniqueId())) {
                e.setCancelled(true);
                warn(p);
                return;
            }
            
            // 在公共房屋中，玩家只能打开自己放置的容器
            if (e.getInventory().getLocation() != null) {
                Block block = e.getInventory().getLocation().getBlock();
                UUID placer = SQLiteStorage.getPlacer(block);
                if (placer != null && !placer.equals(p.getUniqueId())) {
                    e.setCancelled(true);
                    warn(p);
                    return;
                }
            }
            e.setCancelled(false);               // 允许
            return;
        }
        
        if (isInOthersHotel(p, loc)) {
            e.setCancelled(true);
            warn(p);
            return;
        }
        e.setCancelled(false);               // 允许
    }

    /* ========================= 工具 ========================= */
    private boolean isInOthersHotel(Player p, Location loc) {
        HotelInfo info = getHotelAt(loc);
        if (info == null) return false;

        /* 荣誉门槛检测（仅对"非主人"生效） */
        if (info != null) {
            int req = SQLiteStorage.getHonorReq(info.name);
            int playerHonor = SQLiteStorage.getHonor(p.getUniqueId());
            if (req > 0 && playerHonor < req) {
                p.sendActionBar("§c你的荣誉值不足 " + req + "，无法操作此领地！");
                return true;   // 直接视为"无权限"
            }
        }

        UUID uid = p.getUniqueId();
        
        // 对于官方房屋，普通玩家无法操作
        if (info.isOfficial) {
            return true; // 官方房屋对普通玩家视为在他人酒店
        }
        
        // 对于公共房屋，玩家必须是成员才能操作
        if (info.isPublic) {
            return !info.members.contains(uid); // 如果不是成员，则视为在他人酒店
        }
        
        // 对于私有房屋，必须是主人或成员
        return !info.owner.equals(uid) && !info.members.contains(uid);
    }



    /* 公共工具：供外部查询坐标所在酒店 */
    public static HotelInfo getHotelAt(Location loc) {
        return SpatialIndex.getInstance().get(loc);
    }





    /** 快速判断点是否在立方体选区 */
    public static boolean isInside(Location[] diagonals, Location loc) {
        if (!diagonals[0].getWorld().equals(loc.getWorld())) return false;
        int x1 = diagonals[0].getBlockX(), x2 = diagonals[1].getBlockX();
        int y1 = diagonals[0].getBlockY(), y2 = diagonals[1].getBlockY();
        int z1 = diagonals[0].getBlockZ(), z2 = diagonals[1].getBlockZ();
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    /** 容器+功能性方块枚举 */
    private boolean isContainerOrUtility(Material m) {
        return m.name().endsWith("CHEST") || m.name().endsWith("SHULKER_BOX") ||
                m == Material.BARREL ||
                m == Material.FURNACE ||
                m == Material.BLAST_FURNACE ||
                m == Material.SMOKER ||
                m == Material.CRAFTING_TABLE ||   // ← 只用这一个
                m == Material.BREWING_STAND ||
                m == Material.ANVIL ||
                m == Material.ENCHANTING_TABLE ||
                m == Material.GRINDSTONE ||
                m == Material.STONECUTTER ||
                m == Material.LOOM ||
                m == Material.CARTOGRAPHY_TABLE ||
                m == Material.SMITHING_TABLE;
    }


    private void warn(Player p) {
        p.sendActionBar("§c你没有权限在此领地操作！");
    }}