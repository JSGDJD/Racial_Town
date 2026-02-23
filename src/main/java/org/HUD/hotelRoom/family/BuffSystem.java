package org.HUD.hotelRoom.family;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class BuffSystem {
    private final Plugin plugin;
    private final Map<String, BuffConfig> buffConfigs;
    private final Map<UUID, Map<String, Long>> familyBuffCooldowns;
    private final Map<UUID, Map<String, Long>> activeBuffs;

    public BuffSystem(Plugin plugin) {
        this.plugin = plugin;
        this.buffConfigs = new HashMap<>();
        this.familyBuffCooldowns = new HashMap<>();
        this.activeBuffs = new HashMap<>();
        
        // 从配置文件加载Buff配置
        loadConfig();
        
        // 启动Buff效果更新任务
        startBuffUpdateTask();
    }

    private void loadConfig() {
        ConfigurationSection buffsSection = plugin.getConfig().getConfigurationSection("buffs");
        if (buffsSection != null) {
            for (String buffName : buffsSection.getKeys(false)) {
                ConfigurationSection buffSection = buffsSection.getConfigurationSection(buffName);
                
                if (buffSection != null) {
                    BuffConfig config = new BuffConfig(buffName);
                    
                    // 加载基本信息
                    config.setName(buffSection.getString("name", buffName));
                    config.setDescription(buffSection.getString("description", ""));
                    config.setEffect(buffSection.getString("effect", "STRENGTH"));
                    config.setAmplifier(buffSection.getInt("amplifier", 1));
                    config.setDuration(buffSection.getInt("duration", 3600));
                    
                    // 加载消耗
                    ConfigurationSection costSection = buffSection.getConfigurationSection("cost");
                    if (costSection != null) {
                        config.setMoneyCost(costSection.getDouble("money", 0));
                        config.setHonorCost(costSection.getDouble("honor", 0));
                        config.setActivityCost(costSection.getDouble("activity", 0));
                        config.setItemsCost(costSection.getStringList("items"));
                    }
                    
                    // 加载冷却
                    config.setCooldown(buffSection.getInt("cooldown", 86400));
                    
                    buffConfigs.put(buffName, config);
                }
            }
        }
    }

    public boolean canActivateBuff(Family family, String buffName) {
        BuffConfig config = buffConfigs.get(buffName);
        if (config == null) {
            return false;
        }
        
        // 检查冷却
        Map<String, Long> cooldowns = familyBuffCooldowns.getOrDefault(family.getId(), new HashMap<>());
        long lastActivated = cooldowns.getOrDefault(buffName, 0L);
        long now = System.currentTimeMillis();
        
        if (now - lastActivated < config.getCooldown() * 1000) {
            return false;
        }
        
        // 检查消耗
        if (family.getHonor() < config.getHonorCost()) {
            return false;
        }
        
        if (family.getActivity() < config.getActivityCost()) {
            return false;
        }
        
        // 其他消耗检查（游戏币、物品）将在激活时进行
        
        return true;
    }

    public boolean activateBuff(Family family, Player leader, String buffName) {
        BuffConfig config = buffConfigs.get(buffName);
        if (config == null) {
            return false;
        }
        
        // 检查冷却
        Map<String, Long> cooldowns = familyBuffCooldowns.computeIfAbsent(family.getId(), k -> new HashMap<>());
        long lastActivated = cooldowns.getOrDefault(buffName, 0L);
        long now = System.currentTimeMillis();
        
        if (now - lastActivated < config.getCooldown() * 1000) {
            return false;
        }
        
        // 检查并扣除消耗
        if (!checkAndDeductCosts(family, leader, config)) {
            return false;
        }
        
        // 应用Buff
        applyBuff(family, config);
        
        // 设置冷却
        cooldowns.put(buffName, now);
        
        // 记录活跃Buff
        Map<String, Long> familyActiveBuffs = activeBuffs.computeIfAbsent(family.getId(), k -> new HashMap<>());
        familyActiveBuffs.put(buffName, now + config.getDuration() * 1000);
        
        return true;
    }

    private boolean checkAndDeductCosts(Family family, Player leader, BuffConfig config) {
        // 检查并扣除荣誉值
        if (family.getHonor() < config.getHonorCost()) {
            return false;
        }
        family.removeHonor(config.getHonorCost());
        
        // 检查并扣除活跃值
        if (family.getActivity() < config.getActivityCost()) {
            return false;
        }
        family.removeActivity(config.getActivityCost());
        
        // 检查并扣除游戏币
        if (config.getMoneyCost() > 0) {
            // 这里需要Vault经济插件的支持
            // 暂时省略，实际实现时需要添加Vault依赖
        }
        
        // 检查并扣除物品
        if (!config.getItemsCost().isEmpty()) {
            // 这里需要检查玩家背包中的物品
            // 暂时省略，实际实现时需要添加物品检查逻辑
        }
        
        return true;
    }

    private void applyBuff(Family family, BuffConfig config) {
        PotionEffectType effectType = PotionEffectType.getByName(config.getEffect());
        if (effectType == null) {
            return;
        }
        
        PotionEffect effect = new PotionEffect(
                effectType,
                config.getDuration() * 20, // 转换为ticks
                config.getAmplifier() - 1, // Bukkit的amplifier从0开始
                true,
                true,
                true
        );
        
        // 为家族所有在线成员应用效果
        for (UUID memberId : family.getMemberIds()) {
            Player player = plugin.getServer().getPlayer(memberId);
            if (player != null && player.isOnline()) {
                // 移除旧效果
                player.removePotionEffect(effectType);
                // 应用新效果
                player.addPotionEffect(effect);
            }
        }
    }

    private void startBuffUpdateTask() {
        // 每10秒检查一次活跃Buff
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            
            // 检查所有活跃Buff
            Iterator<Map.Entry<UUID, Map<String, Long>>> familyIterator = activeBuffs.entrySet().iterator();
            while (familyIterator.hasNext()) {
                Map.Entry<UUID, Map<String, Long>> familyEntry = familyIterator.next();
                UUID familyId = familyEntry.getKey();
                Map<String, Long> familyBuffs = familyEntry.getValue();
                
                Iterator<Map.Entry<String, Long>> buffIterator = familyBuffs.entrySet().iterator();
                while (buffIterator.hasNext()) {
                    Map.Entry<String, Long> buffEntry = buffIterator.next();
                    String buffName = buffEntry.getKey();
                    long expireTime = buffEntry.getValue();
                    
                    // 如果Buff已过期，移除它
                    if (now >= expireTime) {
                        buffIterator.remove();
                        
                        // 如果该家族没有活跃Buff了，移除家族记录
                        if (familyBuffs.isEmpty()) {
                            familyIterator.remove();
                        }
                    }
                }
            }
        }, 0, 200L); // 200 ticks = 10秒
    }

    public Map<String, BuffConfig> getBuffConfigs() {
        return buffConfigs;
    }

    public BuffConfig getBuffConfig(String buffName) {
        return buffConfigs.get(buffName);
    }

    public long getBuffCooldown(UUID familyId, String buffName) {
        Map<String, Long> cooldowns = familyBuffCooldowns.getOrDefault(familyId, new HashMap<>());
        long lastActivated = cooldowns.getOrDefault(buffName, 0L);
        long now = System.currentTimeMillis();
        BuffConfig config = buffConfigs.get(buffName);
        
        if (config == null) {
            return 0;
        }
        
        long remaining = (lastActivated + config.getCooldown() * 1000) - now;
        return Math.max(0, remaining / 1000);
    }

    public long getBuffRemainingTime(UUID familyId, String buffName) {
        Map<String, Long> familyBuffs = activeBuffs.getOrDefault(familyId, new HashMap<>());
        long expireTime = familyBuffs.getOrDefault(buffName, 0L);
        long now = System.currentTimeMillis();
        
        return Math.max(0, expireTime - now);
    }

    public boolean isBuffActive(UUID familyId, String buffName) {
        return getBuffRemainingTime(familyId, buffName) > 0;
    }

    public class BuffConfig {
        private final String id;
        private String name;
        private String description;
        private String effect;
        private int amplifier;
        private int duration;
        private double moneyCost;
        private double honorCost;
        private double activityCost;
        private List<String> itemsCost;
        private int cooldown;

        public BuffConfig(String id) {
            this.id = id;
            this.name = id;
            this.description = "";
            this.effect = "STRENGTH";
            this.amplifier = 1;
            this.duration = 3600;
            this.moneyCost = 0;
            this.honorCost = 0;
            this.activityCost = 0;
            this.itemsCost = new ArrayList<>();
            this.cooldown = 86400;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getEffect() {
            return effect;
        }

        public void setEffect(String effect) {
            this.effect = effect;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public void setAmplifier(int amplifier) {
            this.amplifier = amplifier;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public double getMoneyCost() {
            return moneyCost;
        }

        public void setMoneyCost(double moneyCost) {
            this.moneyCost = moneyCost;
        }

        public double getHonorCost() {
            return honorCost;
        }

        public void setHonorCost(double honorCost) {
            this.honorCost = honorCost;
        }

        public double getActivityCost() {
            return activityCost;
        }

        public void setActivityCost(double activityCost) {
            this.activityCost = activityCost;
        }

        public List<String> getItemsCost() {
            return itemsCost;
        }

        public void setItemsCost(List<String> itemsCost) {
            this.itemsCost = itemsCost;
        }

        public int getCooldown() {
            return cooldown;
        }

        public void setCooldown(int cooldown) {
            this.cooldown = cooldown;
        }
    }
}
