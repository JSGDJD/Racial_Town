package org.HUD.hotelRoom.house;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.protection.ProtectionListener;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.Color;


import java.util.*;

public final class HouseListGUI implements Listener {
    /* 全息文字导航 */
    private static final Map<UUID, TextDisplay> NAV_TEXT = new HashMap<>();

    /* ================= 物品导航 ================= */
    private static final double ITEM_FOLLOW_DIST = 6.8;      // 物品悬停距离（格）
    private static final double ITEM_Y_OFFSET    = 2.0;      // 高度偏移
    private static final Map<UUID, ArmorStand> NAV_STAND = new HashMap<>();


    private static final int PAGE_SIZE = 45;
    private static final String TITLE = "§8§l房屋列表";
    /* 导航缓存：玩家 -> 目标房屋 + 粒子任务 */
    private static final Map<UUID, NavSession> NAV = new HashMap<>();

    private static class NavSession {
        final HotelInfo house;
        final BukkitTask task;
        List<Location> path;   // 简化为直线路径点
        int index = 0;

        Location lastLoc;

        NavSession(HotelInfo h, BukkitTask t, List<Location> path, Location lastLoc) {
            this.house = h;
            this.task = t;
            this.path = path;
            this.lastLoc = lastLoc;
        }
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        List<HotelInfo> list = sortedHotels();
        int total = list.size();
        int maxPage = (total + PAGE_SIZE - 1) / PAGE_SIZE - 1;

        // 防御：防止负页码或越界
        if (page < 0) page = 0;
        if (maxPage < 0) maxPage = 0;
        if (page > maxPage) page = maxPage;

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        // 再次防御：防止 from > to（空列表时）
        if (from > to || from < 0 || to < 0) {
            player.sendMessage("§c当前没有可展示的房屋。");
            return;
        }

        List<HotelInfo> sub = list.subList(from, to);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE + " - 第" + (page + 1) + "页");
        for (int i = 0; i < sub.size(); i++) {
            inv.setItem(i, buildIcon(sub.get(i)));
        }

        // 导航按钮
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName("§a← 上一页");
            prev.setItemMeta(meta);
            inv.setItem(48, prev);
        }

        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName("§a下一页 →");
            next.setItemMeta(meta);
            inv.setItem(50, next);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c关闭");
        close.setItemMeta(closeMeta);
        inv.setItem(49, close);

        player.openInventory(inv);
    }



    /* 构建房屋图标 */
    private static ItemStack buildIcon(HotelInfo h) {
        boolean free = h.owner.equals(new UUID(0, 0));
        int req = SQLiteStorage.getHonorReq(h.name);
        Location loc = h.corners[0];
        ItemStack item = new ItemStack(free ? Material.LIME_BED : Material.RED_BED);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName((free ? "§a§n" : "§c§n") + h.name);
        List<String> lore = new ArrayList<>();
        lore.add("§7主人: " + (free ? "§o空闲" : Bukkit.getOfflinePlayer(h.owner).getName()));
        
        // 添加官方房屋类型信息
        if (h.isOfficial) {
            lore.add("§7类型: §e" + (h.hotelType != null && !h.hotelType.isEmpty() ? h.hotelType : "默认"));
        }
        
        lore.add("§7所需荣誉: §e" + req);
        lore.add("§7世界: §b" + loc.getWorld().getName());
        lore.add("§7坐标: §f" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        
        if (h.isOfficial) {
            lore.add("");
            lore.add("§c§l官方房屋");
        }
        
        lore.add("");
        lore.add("§6点击导航 →");
        
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    /* GUI 点击事件 */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(TITLE)) return;
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        Player p = (Player) e.getWhoClicked();
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (name.equals("上一页")) {
            int page = Integer.parseInt(title.replaceAll(".*第(\\d+)页", "$1")) - 1;
            open(p, page - 1);
            return;
        }

        if (name.equals("下一页")) {
            int page = Integer.parseInt(title.replaceAll(".*第(\\d+)页", "$1")) - 1;
            open(p, page + 1);
            return;
        }

        if (name.equals("关闭")) {
            p.closeInventory();
            return;
        }

        // 正常点击房屋图标 → 导航
        HotelInfo house = SelectionMgr.HOTELS.get(name);
        if (house != null) {
            p.closeInventory();
            startNavigation(p, house);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.5f);
            p.sendMessage("§a开始导航 → " + name);
        }
    }


    private static void startNavigation(Player p, HotelInfo h) {
        stopNav(p);
        Location target = centerOf(h);
        if (target.getWorld() == null) return;

        HouseConfig conf = HotelRoom.get().getHouseConfig();

        // 1. 生成 TextDisplay 当全息名牌
        TextDisplay text = p.getWorld().spawn(p.getLocation(), TextDisplay.class);
        text.setTeleportDuration(1);   // 1 = 1 tick 插值，平滑且不掉帧

        text.setBillboard(Display.Billboard.CENTER);          // 始终面朝玩家
        text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));  // 完全透明
        text.setShadowed(false);                               // 无阴影
        text.setSeeThrough(true);                              // 可穿墙
        text.setCustomNameVisible(false);
        text.setVisibleByDefault(false);   // 默认隐藏
        p.showEntity(HotelRoom.get(), text); // 仅该玩家可见
        // 仅该玩家可见
        NAV_TEXT.put(p.getUniqueId(), text);

        // 2. 计时器：每 tick 更新坐标 & 文字
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(HotelRoom.get(), () -> {
            if (!p.isOnline() || text.isDead()) {
                stopNav(p);
                return;
            }
            Location loc = p.getLocation();
            if (loc.getWorld().equals(target.getWorld()) && loc.distanceSquared(target) <= 9.0) {
                stopNav(p);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                p.sendMessage("§a已抵达 §6" + h.name);
                return;
            }
            // 计算面前坐标
            Vector dir = target.toVector().subtract(loc.toVector()).setY(0).normalize();
            Location front = loc.clone().add(dir.multiply(ITEM_FOLLOW_DIST)).add(0, ITEM_Y_OFFSET, 0);
            text.teleport(front);

            // 文字内容实时刷新
            String newText = ChatColor.translateAlternateColorCodes('&',
                    conf.getNavDisplay()
                            .replace("{house}", h.name)
                            .replace("{distance}", String.format("%.1f", loc.distance(target))));
            text.setText(newText);
        }, 0L, 1L);

        NAV.put(p.getUniqueId(), new NavSession(h, task, new ArrayList<>(), p.getLocation()));
    }



    private static float getYawToward(Location from, Location to) {
        Vector dir = to.clone().subtract(from).toVector().setY(0).normalize();
        return (float) Math.toDegrees(Math.atan2(dir.getX(), -dir.getZ()));
    }



    /* 到达边界即停止（额外保险） */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        NavSession nav = NAV.get(p.getUniqueId());
        if (nav == null) return;
        if (ProtectionListener.getHotelAt(p.getLocation()) == nav.house) {
            stopNav(p);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendMessage("§a已抵达 §6" + nav.house.name);
        }
    }



    /* 房屋中心点 */
    private static Location centerOf(HotelInfo h) {
        Location a = h.corners[0];
        Location b = h.corners[1];
        return new Location(a.getWorld(),
                (a.getX() + b.getX()) / 2,
                Math.min(a.getY(), b.getY()) + 1, // 地面+1
                (a.getZ() + b.getZ()) / 2);
    }

    private static List<HotelInfo> sortedHotels() {
        return SelectionMgr.HOTELS.values().stream()
                .sorted(Comparator
                        .<HotelInfo, Boolean>comparing(h -> !h.owner.equals(new UUID(0, 0))) // 空闲优先（false < true）
                        .thenComparingInt(h -> SQLiteStorage.getHonorReq(h.name))           // 荣誉升序
                )
                .toList();
    }

    /* 粒子贴地：确保 Y = 最高固体方块 + 1 */
    private static Location surfaceLoc(Location loc) {
        World w = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int minY = w.getMinHeight();
        int maxY = w.getMaxHeight();
        int y = w.getHighestBlockYAt(x, z);
        while (y > minY && w.getBlockAt(x, y, z).isPassable()) y--;
        while (y < maxY - 1 && !w.getBlockAt(x, y + 1, z).isPassable()) y++;
        y = Math.max(minY + 1, Math.min(maxY - 1, y));   // 钳位
        return new Location(w, x + 0.5, y + 1.0, z + 0.5);
    }


    /* 判断方块是否可行走（含楼梯上半砖） */
    private static boolean isWalkable(Block b) {
        Material type = b.getType();
        if (type.isAir()) return false;
        if (type.createBlockData() instanceof org.bukkit.block.data.type.Stairs) return true;
        if (type.createBlockData() instanceof org.bukkit.block.data.type.Slab slab
                && slab.getType() == org.bukkit.block.data.type.Slab.Type.TOP) return true;
        return !b.isPassable(); // 实心
    }


    /* 东南西北四个水平方向 */
    private static final List<Vector> CARDINAL = List.of(
            new Vector(1, 0, 0),
            new Vector(-1, 0, 0),
            new Vector(0, 0, 1),
            new Vector(0, 0, -1)
    );


    /* ===== 服务器关闭时强制清理所有导航实体 ===== */
    public static void onDisable()
    {
        // 清 ArmorStand
        NAV_STAND.values().forEach(stand -> {
            if (stand != null && !stand.isDead()) stand.remove();
        });
        NAV_STAND.clear();
        // 清任务
        NAV.values().forEach(nav -> {
            if (nav.task != null) nav.task.cancel();
        });
        NAV.clear();
    }


    /* 停止导航 */
    public static void stopNav(Player p) {
        TextDisplay text = NAV_TEXT.remove(p.getUniqueId());
        if (text != null && !text.isDead()) text.remove();

        NavSession nav = NAV.remove(p.getUniqueId());
        if (nav != null && !nav.task.isCancelled()) nav.task.cancel();
    }
    /* ===== 自动清理导航 ===== */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stopNav(e.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        stopNav(e.getPlayer());
    }
}