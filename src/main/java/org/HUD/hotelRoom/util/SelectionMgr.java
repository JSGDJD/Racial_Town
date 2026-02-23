package org.HUD.hotelRoom.util;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.protection.FacadeProtection;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SelectionMgr implements Listener {
    public void addIndex(HotelInfo info) { SpatialIndex.getInstance().add(info); }
    public void removeIndex(HotelInfo info) { SpatialIndex.getInstance().remove(info); }
    private static SelectionMgr INSTANCE;
    public static final Map<String, HotelInfo> HOTELS = new HashMap<>();
    private final HotelRoom plugin;
    private final Map<UUID, Location[]> selections = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();

    public SelectionMgr(HotelRoom plugin) {
        this.plugin = plugin;
        INSTANCE = this;                    // ← 新增
    }

    public static SelectionMgr getInst() {  // ← 新增
        return INSTANCE;
    }

    /* ========================= 选区事件 ========================= */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = e.getItem();
        if (hand == null || hand.getType() != Material.IRON_AXE) return;

        Action a = e.getAction();
        if (a != Action.LEFT_CLICK_BLOCK && a != Action.RIGHT_CLICK_BLOCK) return;
        
        // 检查权限：只有管理员可以使用铁斧圈地
        if (!p.hasPermission("hotelroom.admin")) {
            return; // 没有权限，不取消事件，让铁斧正常工作
        }
        
        e.setCancelled(true);

        Location loc = Objects.requireNonNull(e.getClickedBlock()).getLocation();
        UUID uuid = p.getUniqueId();
        selections.putIfAbsent(uuid, new Location[2]);

        int index = a == Action.LEFT_CLICK_BLOCK ? 0 : 1;
        selections.get(uuid)[index] = loc;
        p.sendMessage(ChatColor.YELLOW + "点" + (index + 1) + " 已设置 " +
                loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        if (hasBothPoints(p)) showParticles(p);
    }

    /* ========================= 粒子边框 ========================= */
    private void showParticles(Player p) {
        stopParticles(p);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !hasBothPoints(p)) {
                stopParticles(p);
                return;
            }
            drawOutline(p);
        }, 0L, 10L);
        particleTasks.put(p.getUniqueId(), task);
    }

    private void drawOutline(Player p) {
        Location[] arr = selections.get(p.getUniqueId());
        World w = arr[0].getWorld();
        int x1 = arr[0].getBlockX(), y1 = arr[0].getBlockY(), z1 = arr[0].getBlockZ();
        int x2 = arr[1].getBlockX(), y2 = arr[1].getBlockY(), z2 = arr[1].getBlockZ();

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                w.spawnParticle(Particle.FLAME, x + 0.5, y + 0.5, minZ + 0.5, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLAME, x + 0.5, y + 0.5, maxZ + 0.5, 1, 0, 0, 0, 0);
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                w.spawnParticle(Particle.FLAME, minX + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLAME, maxX + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    public void stopParticles(Player p) {
        BukkitTask t = particleTasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    /* ========================= 创建领地 ========================= */
    public boolean createHotel(Player p, String name) {
        /* 每人只能拥有 1 个领地 */
        for (HotelInfo ex : HOTELS.values()) {
            if (ex.owner.equals(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "你已拥有领地，无法再次创建！");
                return false;
            }
        }
        if (HOTELS.containsKey(name)) return false;
        Location[] arr = selections.get(p.getUniqueId());
        if (arr == null || arr[0] == null || arr[1] == null) return false;

        for (HotelInfo info : HOTELS.values()) {
            if (isOverlap(arr, info.corners)) {
                p.sendActionBar("§c当前选区与已有领地重叠，请远离后再试！");
                return false;
            }
        }

        HotelInfo info = new HotelInfo(name, p.getUniqueId(), arr.clone());
        HOTELS.put(name, info);
        addIndex(info);  // 添加到空间索引
        SQLiteStorage.saveHotel(name, info.owner, info.corners);
        stopParticles(p);
        selections.remove(p.getUniqueId());
        //
        info.facade.addAll(FacadeProtection.snapshot(arr));
        SQLiteStorage.saveFacade(name, info.facade);
        //
        return true;
    }

    /* 新增：允许指定主人（管理员可传入系统占位表示“无主人”）*/
    public boolean createHotel(Player p, String name, UUID owner) {
        // ① 单领主限制（仅对“自己要成为主人”的情况生效）
        if (!owner.equals(new UUID(0, 0))) {   // 如果本次要设成自己
            for (HotelInfo ex : HOTELS.values()) {
                if (ex.owner.equals(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "你已拥有领地，无法再次创建！");
                    return false;
                }
            }
        }
        // ② 以下原有逻辑不变
        if (HOTELS.containsKey(name)) return false;
        Location[] arr = selections.get(p.getUniqueId());
        if (arr == null || arr[0] == null || arr[1] == null) return false;
        for (HotelInfo info : HOTELS.values()) {
            if (isOverlap(arr, info.corners)) {
                p.sendActionBar("§c当前选区与已有领地重叠，请远离后再试！");
                return false;
            }
        }
        HotelInfo info = new HotelInfo(name, owner, arr.clone());
        HOTELS.put(name, info);
        SQLiteStorage.saveHotel(name, owner, arr);
        stopParticles(p);
        selections.remove(p.getUniqueId());
// 改为异步执行
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 1. 异步生成快照（注意：Bukkit的Block操作需在主线程，此处需调整）
            // 修正：方块数据获取必须在主线程，因此拆分步骤
            Set<Location> facade = new HashSet<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                facade.addAll(FacadeProtection.snapshot(arr)); // 主线程获取方块数据
                // 2. 再次异步保存到数据库
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    info.facade.addAll(facade);
                    SQLiteStorage.saveFacade(name, info.facade);
                });
            });
        });
        addIndex(info);

        return true;
    }


    /* ========================= 工具 ========================= */
    public boolean hasBothPoints(Player p) {
        Location[] arr = selections.get(p.getUniqueId());
        return arr != null && arr[0] != null && arr[1] != null;
    }
    
    public Location[] getSelection(Player p) {
        return selections.get(p.getUniqueId());
    }

    public void shutdown() {
        particleTasks.values().forEach(BukkitTask::cancel);
        particleTasks.clear();
    }

    /**
     * 判断两个立方体是否相交
     * @param a 选区1 [0]=min [1]=max
     * @param b 选区2 [0]=min [1]=max
     */
    private static boolean isOverlap(Location[] a, Location[] b) {
        if (!a[0].getWorld().equals(b[0].getWorld())) return false;
        int minAX = Math.min(a[0].getBlockX(), a[1].getBlockX());
        int maxAX = Math.max(a[0].getBlockX(), a[1].getBlockX());
        int minAY = Math.min(a[0].getBlockY(), a[1].getBlockY());
        int maxAY = Math.max(a[0].getBlockY(), a[1].getBlockY());
        int minAZ = Math.min(a[0].getBlockZ(), a[1].getBlockZ());
        int maxAZ = Math.max(a[0].getBlockZ(), a[1].getBlockZ());

        int minBX = Math.min(b[0].getBlockX(), b[1].getBlockX());
        int maxBX = Math.max(b[0].getBlockX(), b[1].getBlockX());
        int minBY = Math.min(b[0].getBlockY(), b[1].getBlockY());
        int maxBY = Math.max(b[0].getBlockY(), b[1].getBlockY());
        int minBZ = Math.min(b[0].getBlockZ(), b[1].getBlockZ());
        int maxBZ = Math.max(b[0].getBlockZ(), b[1].getBlockZ());

        // AABB 相交判断
        return maxAX >= minBX && maxBX >= minAX &&
                maxAY >= minBY && maxBY >= minAY &&
                maxAZ >= minBZ && maxBZ >= minAZ;
    }


}
