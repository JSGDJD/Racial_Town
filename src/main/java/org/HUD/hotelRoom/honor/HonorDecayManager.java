package org.HUD.hotelRoom.honor;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.gui.CooldownManager;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HonorDecayManager {

    /* 缓存玩家最后一次荣誉变化时间 */
    private static final Map<UUID, Instant> LAST_CHANGE = new ConcurrentHashMap<>();

    private static BukkitTask TASK;
    private static long CHECK_PERIOD_MIN   = 30;
    private static int  DEDUCT_VALUE       = 1;
    private static long IDLE_THRESHOLD_MIN = 1440;
    private static long GRACE_MINUTES      = 60;
    private static boolean ENABLED         = true;

    /* 给外部：当荣誉发生任何变化时调用 */
    public static void recordChange(UUID uuid) {
        LAST_CHANGE.put(uuid, Instant.now());
    }

    /* 由 HotelRoom.onEnable 调用 */
    public static void start(HotelRoom plugin) {
        loadConfig(plugin.getConfig());
        if (TASK != null) TASK.cancel();
        if (!ENABLED) return;

        TASK = new BukkitRunnable() {
            @Override
            public void run() {
                decayCycle();
            }
        }.runTaskTimer(plugin, 20L * 60 * CHECK_PERIOD_MIN, 20L * 60 * CHECK_PERIOD_MIN);
    }

    public static void stop() {
        if (TASK != null) TASK.cancel();
    }

    /* 由 HotelRoomCmd 的 reload 调用 */
    public static void reload(FileConfiguration cfg) {
        loadConfig(cfg);   // 只加载配置
        // 加载每日荣誉值限制系统的配置
        org.HUD.hotelRoom.util.DailyHonorManager.loadConfig(cfg);
        // 重启定时器以应用新周期
        HotelRoom plugin = HotelRoom.get();
        if (plugin != null) {
            stop();
            start(plugin);
        }
    }


    /* 仅加载配置，不重启定时器 */
    private static void loadConfig(FileConfiguration cfg) {
        String path = "honor-decay.";
        ENABLED          = cfg.getBoolean(path + "enabled", true);
        CHECK_PERIOD_MIN = cfg.getLong(path + "check-period", 30);
        DEDUCT_VALUE     = cfg.getInt(path + "deduct-value", 1);
        IDLE_THRESHOLD_MIN = cfg.getLong(path + "idle-threshold", 1440);
        GRACE_MINUTES    = cfg.getLong(path + "grace-minutes", 60);
    }


    /* 核心周期 */
    private static void decayCycle() {
        for (Map.Entry<String, HotelInfo> e : SelectionMgr.HOTELS.entrySet()) {
            HotelInfo hi = e.getValue();
            if (hi.owner.equals(new UUID(0, 0))) continue; // 系统房
            UUID owner = hi.owner;
            int current = SQLiteStorage.getHonor(owner);
            int required= SQLiteStorage.getHonorReq(hi.name);

            /* 1. 检测是否 idle */
            Instant last = LAST_CHANGE.getOrDefault(owner,
                    Instant.ofEpochMilli(SQLiteStorage.getLastHonorTime(owner))); // 兜底读库
            long minutesIdle = java.time.Duration.between(last, Instant.now()).toMinutes();
            if (minutesIdle < IDLE_THRESHOLD_MIN) continue;


            /* 2. 扣荣誉 */
            int newHonor = Math.max(0, current - DEDUCT_VALUE);
            SQLiteStorage.setHonor(owner, newHonor);
            CooldownManager.record(owner, CooldownManager.Action.SUB); // 触发变化记录

            /* 3. 低于门槛进入 grace */
            if (newHonor < required) {
                UUID key = owner;
                new BukkitRunnable() {
                    int minutes = (int) GRACE_MINUTES;
                    @Override
                    public void run() {
                        if (minutes <= 0) {
                            if (SQLiteStorage.getHonor(key) < required)
                                abandonHouse(key, hi);
                            this.cancel();
                            return;
                        }
                        Player p = Bukkit.getPlayer(key);
                        if (p != null)
                            p.sendActionBar(ChatColor.RED + "你的荣誉值已低于领地要求，"
                                    + minutes + " 分钟后将被强制放弃！");
                        minutes -= 5;
                    }
                }.runTaskTimer(HotelRoom.get(), 0L, 20L * 60 * 5);
            }
        }
    }

    /* 强制放弃，复用已有逻辑 */
    private static void abandonHouse(UUID owner, HotelInfo hi) {
        // 1. 系统回收，保留原有属性
        HotelInfo sys = new HotelInfo(hi.name, new UUID(0, 0), hi.corners,
                                      hi.isPublic, hi.isOfficial, hi.hotelType);
        sys.members.addAll(hi.members);
        sys.facade.addAll(hi.facade);  // 保留外观快照
        SelectionMgr.HOTELS.put(hi.name, sys);
        SelectionMgr.getInst().removeIndex(hi);   // 反注册旧索引
        SelectionMgr.getInst().addIndex(sys);      // 注册新索引

        SQLiteStorage.saveHotel(hi.name, sys.owner, hi.corners,
                                hi.isPublic, hi.isOfficial, hi.hotelType);
        // 2. 清 placed_blocks
        SQLiteStorage.untrackPlaceInHotel(hi.name);

        Player p = Bukkit.getPlayer(owner);
        if (p != null)
            p.sendMessage(ChatColor.RED + "你的荣誉值已不足，领地 '" + hi.name + "' 被系统回收！");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "玩家 "
                + (p == null ? Bukkit.getOfflinePlayer(owner).getName() : p.getName())
                + " 的领地 '" + hi.name + "' 因荣誉值过低已被回收！");
    }
}
