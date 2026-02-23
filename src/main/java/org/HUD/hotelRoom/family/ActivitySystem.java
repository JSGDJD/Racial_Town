package org.HUD.hotelRoom.family;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivitySystem implements Listener {
    private final Plugin plugin;
    private final Map<String, ActivityConfig> activityConfigs;
    private final Map<UUID, Long> lastLoginTimes;
    private final double weeklyDecayRate;

    public ActivitySystem(Plugin plugin) {
        this.plugin = plugin;
        this.activityConfigs = new HashMap<>();
        this.lastLoginTimes = new HashMap<>();
        
        // 从配置文件加载活跃值配置
        loadConfig();
        
        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // 启动每周衰减任务
        startWeeklyDecay();
        
        // 启动每日重置任务
        startDailyReset();
        
        this.weeklyDecayRate = plugin.getConfig().getDouble("activity.weekly-decay-rate", 0.9);
    }

    private void loadConfig() {
        ConfigurationSection actionsSection = plugin.getConfig().getConfigurationSection("activity.actions");
        if (actionsSection != null) {
            for (String action : actionsSection.getKeys(false)) {
                ConfigurationSection actionSection = actionsSection.getConfigurationSection(action);
                if (actionSection != null) {
                    double amount = actionSection.getDouble("amount", 0.0);
                    double amountPerCoin = actionSection.getDouble("amount-per-coin", 0.0);
                    double dailyLimit = actionSection.getDouble("daily-limit", -1);
                    
                    ActivityConfig config = new ActivityConfig(amount, amountPerCoin, dailyLimit);
                    activityConfigs.put(action, config);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 检查是否是当天首次登录
        long currentTime = System.currentTimeMillis();
        long lastLogin = lastLoginTimes.getOrDefault(playerId, 0L);
        
        if (!isSameDay(currentTime, lastLogin)) {
            // 给予首次登录活跃值
            addActivity(player, "first-login", 1.0);
            lastLoginTimes.put(playerId, currentTime);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            addActivity(player, "kill-monster", 1.0);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();
        
        // 检查是否是矿石
        if (isOre(blockType)) {
            addActivity(player, "mine-ore", 1.0);
        }
    }

    public void addActivity(Player player, String action, double multiplier) {
        FamilyManager manager = FamilyManager.getInstance();
        FamilyMember member = manager.getMember(player);
        
        if (member == null) {
            return;
        }
        
        ActivityConfig config = activityConfigs.get(action);
        if (config == null) {
            return;
        }
        
        double amount = config.getAmount() * multiplier;
        if (action.equals("donate-money")) {
            amount = config.getAmountPerCoin() * multiplier;
        }
        
        // 检查每日限制
        double dailyLimit = config.getDailyLimit();
        if (dailyLimit > 0) {
            double current = member.getDailyActivity(action);
            if (current + amount > dailyLimit) {
                amount = dailyLimit - current;
            }
        }
        
        if (amount > 0) {
            // 更新成员的每日活跃值
            member.addDailyActivity(action, amount);
            
            // 更新家族的总活跃值
            Family family = manager.getFamily(member.getFamilyId());
            if (family != null) {
                family.addActivity(amount);
                manager.getStorage().saveFamily(family);
            }
            
            // 保存成员数据
            manager.getStorage().saveMember(member);
        }
    }

    public void resetDailyActivity() {
        FamilyManager manager = FamilyManager.getInstance();
        for (FamilyMember member : manager.getAllMembers()) {
            member.resetDailyActivity();
            manager.getStorage().saveMember(member);
        }
    }

    public void applyWeeklyDecay() {
        FamilyManager manager = FamilyManager.getInstance();
        for (Family family : manager.getAllFamilies()) {
            double currentActivity = family.getActivity();
            double newActivity = currentActivity * weeklyDecayRate;
            family.setActivity(newActivity);
            manager.getStorage().saveFamily(family);
        }
    }

    private boolean isSameDay(long time1, long time2) {
        return (time1 / 86400000) == (time2 / 86400000);
    }

    private boolean isOre(String blockType) {
        return blockType.endsWith("_ORE") || 
               blockType.endsWith("_ORE_DEEPSLATE") || 
               blockType.equals("NETHER_GOLD_ORE") || 
               blockType.equals("ANCIENT_DEBRIS") || 
               blockType.equals("NETHER_QUARTZ_ORE");
    }

    private void startWeeklyDecay() {
        // 每周日凌晨2点执行衰减
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTimeInMillis(now);
            
            if (calendar.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY && 
                calendar.get(java.util.Calendar.HOUR_OF_DAY) == 2 && 
                calendar.get(java.util.Calendar.MINUTE) == 0) {
                
                applyWeeklyDecay();
            }
        }, 0L, 6000L); // 每5分钟检查一次
    }

    private void startDailyReset() {
        // 每天凌晨0点重置每日活跃值
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTimeInMillis(now);
            
            if (calendar.get(java.util.Calendar.HOUR_OF_DAY) == 0 && 
                calendar.get(java.util.Calendar.MINUTE) == 0) {
                
                resetDailyActivity();
            }
        }, 0, 6000); // 每5分钟检查一次
    }

    private static class ActivityConfig {
        private final double amount;
        private final double amountPerCoin;
        private final double dailyLimit;

        public ActivityConfig(double amount, double amountPerCoin, double dailyLimit) {
            this.amount = amount;
            this.amountPerCoin = amountPerCoin;
            this.dailyLimit = dailyLimit;
        }

        public double getAmount() {
            return amount;
        }

        public double getAmountPerCoin() {
            return amountPerCoin;
        }

        public double getDailyLimit() {
            return dailyLimit;
        }
    }
}
