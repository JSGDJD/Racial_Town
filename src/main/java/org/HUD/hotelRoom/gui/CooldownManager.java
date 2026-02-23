package org.HUD.hotelRoom.gui;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownManager {

    private static final Map<UUID, ActionRecord> HISTORY = new HashMap<>();
    private static long COOLDOWN_MINUTES = 1440;   // 默认 24 h

    public enum Action { ADD, SUB }
    private record ActionRecord(Action action, Instant time) {}

    /* 读取配置 */
    public static void reload(FileConfiguration cfg) {
        COOLDOWN_MINUTES = cfg.getLong("honor-click-cooldown", 1440L);
        if (COOLDOWN_MINUTES < 0) COOLDOWN_MINUTES = 0;
    }

    /* 检查是否允许操作 */
    public static boolean check(UUID operator, Action want) {
        if (COOLDOWN_MINUTES == 0) return true;
        ActionRecord rec = HISTORY.get(operator);
        if (rec == null) return true;
        if (rec.action != want) return false;
        long left = rec.time.plusSeconds(COOLDOWN_MINUTES * 60).getEpochSecond() - Instant.now().getEpochSecond();
        return left <= 0;
    }

    /* 记录一次操作 */
    public static void record(UUID operator, Action done) {
        HISTORY.put(operator, new ActionRecord(done, Instant.now()));
    }

    /* 给提示用：xx 分钟 xx 秒 */
    public static String getLeftTime(UUID operator) {
        ActionRecord rec = HISTORY.get(operator);
        if (rec == null) return "0";
        long left = rec.time.plusSeconds(COOLDOWN_MINUTES * 60).getEpochSecond() - Instant.now().getEpochSecond();
        if (left <= 0) return "0";
        long m = left / 60;
        long s = left % 60;
        return String.format("%d 分钟 %d 秒", m, s);
    }
}
