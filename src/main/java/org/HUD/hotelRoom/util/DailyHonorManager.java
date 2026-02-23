package org.HUD.hotelRoom.util;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyHonorManager {
    private static boolean isEnabled = false;
    private static int dailyHonorLimit = 10;
    private static int storedHonorLimit = 50;
    private static int honorCoolTime = 24; // 小时
    private static int dailyReturnLimit = 10;
    
    // 缓存玩家当日已获得的荣誉值
    private static final Map<UUID, Integer> dailyHonorCache = new HashMap<>();
    // 缓存玩家当日已返还的荣誉值
    private static final Map<UUID, Integer> dailyReturnCache = new HashMap<>();
    // 缓存玩家上次登录时间，用于重置缓存
    private static final Map<UUID, Long> lastLoginTime = new HashMap<>();

    public static void loadConfig(FileConfiguration config) {
        isEnabled = config.getBoolean("daily-honor-system.enabled", false);
        dailyHonorLimit = config.getInt("daily-honor-system.daily_honor_limit", 10);
        storedHonorLimit = config.getInt("daily-honor-system.stored_honor_limit", 50);
        honorCoolTime = config.getInt("daily-honor-system.honor_cool_time", 24);
        dailyReturnLimit = config.getInt("daily-honor-system.daily_return_limit", 10);
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static int getDailyHonorLimit() {
        return dailyHonorLimit;
    }

    public static int getStoredHonorLimit() {
        return storedHonorLimit;
    }

    public static int getHonorCoolTime() {
        return honorCoolTime;
    }

    public static int getDailyReturnLimit() {
        return dailyReturnLimit;
    }

    // 重置玩家的当日荣誉值缓存（通常在每天开始时或玩家上线时）
    public static void resetDailyHonor(UUID uuid) {
        dailyHonorCache.put(uuid, 0);
    }

    // 重置玩家的当日返还荣誉值缓存
    public static void resetDailyReturn(UUID uuid) {
        dailyReturnCache.put(uuid, 0);
    }

    // 检查玩家是否已达到当日荣誉值上限
    public static boolean hasReachedDailyLimit(UUID uuid) {
        return dailyHonorCache.getOrDefault(uuid, 0) >= dailyHonorLimit;
    }

    // 检查玩家是否已达到当日返还荣誉值上限
    public static boolean hasReachedDailyReturnLimit(UUID uuid) {
        return dailyReturnCache.getOrDefault(uuid, 0) >= dailyReturnLimit;
    }

    // 增加玩家当日获得的荣誉值
    public static void addDailyHonor(UUID uuid, int amount) {
        int current = dailyHonorCache.getOrDefault(uuid, 0);
        dailyHonorCache.put(uuid, current + amount);
    }

    // 增加玩家当日返还的荣誉值
    public static void addDailyReturn(UUID uuid, int amount) {
        int current = dailyReturnCache.getOrDefault(uuid, 0);
        dailyReturnCache.put(uuid, current + amount);
    }

    // 获取玩家当日已获得的荣誉值
    public static int getDailyHonor(UUID uuid) {
        return dailyHonorCache.getOrDefault(uuid, 0);
    }

    // 获取玩家当日已返还的荣誉值
    public static int getDailyReturn(UUID uuid) {
        return dailyReturnCache.getOrDefault(uuid, 0);
    }

    // 检查暂存的荣誉值是否已过冷却时间
    public static boolean isStoredHonorExpired(UUID uuid) {
        long lastUpdate = SQLiteStorage.getLastStoredHonorUpdate(uuid);
        long currentTime = System.currentTimeMillis();
        long timeDiffHours = (currentTime - lastUpdate) / (1000 * 60 * 60); // 转换为小时
        return timeDiffHours >= honorCoolTime;
    }

    // 玩家上线时检查并处理暂存的荣誉值
    public static void processStoredHonorOnLogin(UUID uuid) {
        // 检查是否需要重置缓存（根据上次登录时间）
        Long lastLogin = lastLoginTime.get(uuid);
        if (lastLogin == null || isNextDay(lastLogin, System.currentTimeMillis())) {
            resetDailyHonor(uuid);
            resetDailyReturn(uuid);
        }
        
        lastLoginTime.put(uuid, System.currentTimeMillis());

        // 检查暂存的荣誉值是否已过冷却时间
        if (isStoredHonorExpired(uuid)) {
            int storedHonor = SQLiteStorage.getStoredHonor(uuid);
            if (storedHonor > 0) {
                // 尝试返还暂存的荣誉值（受每日返还限制）
                int canReturn = Math.min(storedHonor, dailyReturnLimit - getDailyReturn(uuid));
                canReturn = Math.max(0, canReturn); // 确保不为负数
                
                if (canReturn > 0) {
                    int currentHonor = SQLiteStorage.getHonor(uuid);
                    int newHonor = currentHonor + canReturn;
                    
                    SQLiteStorage.setHonor(uuid, newHonor);
                    addDailyReturn(uuid, canReturn);
                    
                    // 从暂存中减去已返还的荣誉值
                    SQLiteStorage.setStoredHonor(uuid, storedHonor - canReturn);
                    
                    // 如果还有剩余的暂存荣誉值，更新时间戳以便后续处理
                    if (storedHonor > canReturn) {
                        SQLiteStorage.setStoredHonor(uuid, storedHonor - canReturn);
                    } else {
                        SQLiteStorage.clearStoredHonor(uuid);
                    }
                }
            }
        }
    }

    // 添加荣誉值，处理每日限制
    public static int addHonorWithLimit(UUID uuid, int amount) {
        if (!isEnabled) {
            // 如果未启用系统，直接添加荣誉值
            SQLiteStorage.addHonor(uuid, amount);
            return amount;
        }

        int currentHonor = SQLiteStorage.getHonor(uuid);
        int storedHonor = SQLiteStorage.getStoredHonor(uuid);
        
        // 检查是否达到当日限制
        if (hasReachedDailyLimit(uuid)) {
            // 已达到当日限制，将荣誉值存入暂存池
            int canStore = Math.min(amount, storedHonorLimit - storedHonor);
            canStore = Math.max(0, canStore); // 确保不为负数
            
            if (canStore > 0) {
                SQLiteStorage.setStoredHonor(uuid, storedHonor + canStore);
                return 0; // 没有直接给玩家荣誉值
            }
            return 0; // 暂存池已满，无法添加
        }

        // 未达到当日限制，正常添加
        int canAdd = Math.min(amount, dailyHonorLimit - getDailyHonor(uuid));
        canAdd = Math.max(0, canAdd); // 确保不为负数

        if (canAdd > 0) {
            int newHonor = currentHonor + canAdd;
            SQLiteStorage.setHonor(uuid, newHonor);
            addDailyHonor(uuid, canAdd);

            // 剩余的荣誉值存入暂存池
            int remaining = amount - canAdd;
            if (remaining > 0) {
                int canStore = Math.min(remaining, storedHonorLimit - storedHonor);
                canStore = Math.max(0, canStore);
                
                if (canStore > 0) {
                    SQLiteStorage.setStoredHonor(uuid, storedHonor + canStore);
                }
            }

            return canAdd;
        }

        // 如果无法直接添加，尝试存入暂存池
        int canStore = Math.min(amount, storedHonorLimit - storedHonor);
        canStore = Math.max(0, canStore);
        
        if (canStore > 0) {
            SQLiteStorage.setStoredHonor(uuid, storedHonor + canStore);
        }

        return 0;
    }

    // 检查是否是新的一天（简单的实现，基于小时）
    private static boolean isNextDay(long lastTime, long currentTime) {
        // 简单的实现：如果距离上次登录超过24小时，认为是新的一天
        return (currentTime - lastTime) > (24 * 60 * 60 * 1000); // 24小时
    }
}