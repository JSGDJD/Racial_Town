package org.HUD.hotelRoom.family;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpgradeSystem {
    private final Plugin plugin;
    private final Map<Integer, LevelConfig> levelConfigs;
    private final int maxLevel;

    public UpgradeSystem(Plugin plugin) {
        this.plugin = plugin;
        this.levelConfigs = new HashMap<>();
        
        // 从配置文件加载升级配置
        loadConfig();
        
        this.maxLevel = plugin.getConfig().getInt("upgrade-table.max-level", 10);
    }

    private void loadConfig() {
        ConfigurationSection levelsSection = plugin.getConfig().getConfigurationSection("upgrade-table.levels");
        if (levelsSection != null) {
            for (String levelStr : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelStr);
                    ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelStr);
                    
                    if (levelSection != null) {
                        LevelConfig config = new LevelConfig(level);
                        
                        // 加载升级需求
                        ConfigurationSection reqSection = levelSection.getConfigurationSection("requirements");
                        if (reqSection != null) {
                            config.setMoneyReq(reqSection.getDouble("money", 0));
                            config.setHonorReq(reqSection.getDouble("honor", 0));
                            config.setActivityReq(reqSection.getDouble("activity", 0));
                            config.setItemsReq(reqSection.getStringList("items"));
                        }
                        
                        // 加载成员上限
                        config.setMemberLimit(levelSection.getInt("member-limit", 10));
                        
                        // 加载职位上限
                        ConfigurationSection positionsSection = levelSection.getConfigurationSection("positions");
                        if (positionsSection != null) {
                            for (String position : positionsSection.getKeys(false)) {
                                config.setPositionLimit(position, positionsSection.getInt(position, 0));
                            }
                        }
                        
                        // 加载属性加成
                        ConfigurationSection attributesSection = levelSection.getConfigurationSection("attributes");
                        if (attributesSection != null) {
                            for (String attribute : attributesSection.getKeys(false)) {
                                config.addAttributeBonus(attribute, attributesSection.getDouble(attribute, 0));
                            }
                        }
                        
                        // 加载经验倍率
                        ConfigurationSection expSection = levelSection.getConfigurationSection("experience-multipliers");
                        if (expSection != null) {
                            for (String type : expSection.getKeys(false)) {
                                config.addExpMultiplier(type, expSection.getDouble(type, 1.0));
                            }
                        }
                        
                        levelConfigs.put(level, config);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level format: " + levelStr);
                }
            }
        }
    }

    public boolean canUpgrade(Family family) {
        int currentLevel = family.getLevel();
        if (currentLevel >= maxLevel) {
            return false;
        }
        
        LevelConfig nextLevelConfig = levelConfigs.get(currentLevel + 1);
        if (nextLevelConfig == null) {
            return false;
        }
        
        // 检查升级需求
        if (family.getHonor() < nextLevelConfig.getHonorReq()) {
            return false;
        }
        
        if (family.getActivity() < nextLevelConfig.getActivityReq()) {
            return false;
        }
        
        // 检查成员上限
        if (family.getMemberCount() > nextLevelConfig.getMemberLimit()) {
            return false;
        }
        
        return true;
    }

    public boolean upgradeFamily(Family family, Player leader) {
        int currentLevel = family.getLevel();
        if (currentLevel >= maxLevel) {
            return false;
        }
        
        LevelConfig nextLevelConfig = levelConfigs.get(currentLevel + 1);
        if (nextLevelConfig == null) {
            return false;
        }
        
        // 检查并扣除升级需求
        if (!checkAndDeductRequirements(family, leader, nextLevelConfig)) {
            return false;
        }
        
        // 升级家族
        family.setLevel(currentLevel + 1);
        
        // 应用属性加成
        applyAttributeBonuses(family);
        
        // 保存家族数据
        FamilyManager.getInstance().getStorage().saveFamily(family);
        
        return true;
    }

    private boolean checkAndDeductRequirements(Family family, Player leader, LevelConfig config) {
        // 检查荣誉值
        if (family.getHonor() < config.getHonorReq()) {
            return false;
        }
        
        // 检查活跃值
        if (family.getActivity() < config.getActivityReq()) {
            return false;
        }
        
        // 扣除荣誉值
        family.removeHonor(config.getHonorReq());
        
        // 扣除活跃值
        family.removeActivity(config.getActivityReq());
        
        // 检查并扣除游戏币
        if (config.getMoneyReq() > 0) {
            // 这里需要Vault经济插件的支持
            // 暂时省略，实际实现时需要添加Vault依赖
        }
        
        // 检查并扣除物品
        if (!config.getItemsReq().isEmpty()) {
            // 这里需要检查玩家背包中的物品
            // 暂时省略，实际实现时需要添加物品检查逻辑
        }
        
        return true;
    }

    private void applyAttributeBonuses(Family family) {
        int level = family.getLevel();
        LevelConfig config = levelConfigs.get(level);
        
        if (config != null) {
            // 为家族所有成员应用属性加成
            for (UUID memberId : family.getMemberIds()) {
                Player player = plugin.getServer().getPlayer(memberId);
                if (player != null && player.isOnline()) {
                    // 这里需要与属性系统集成
                    // 暂时省略，实际实现时需要添加属性加成逻辑
                }
            }
        }
    }

    public LevelConfig getLevelConfig(int level) {
        return levelConfigs.get(level);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Map<String, Double> getAttributeBonuses(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.getAttributeBonuses() : new HashMap<>();
    }

    public Map<String, Double> getExpMultipliers(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.getExpMultipliers() : new HashMap<>();
    }

    public class LevelConfig {
        private final int level;
        private double moneyReq;
        private double honorReq;
        private double activityReq;
        private java.util.List<String> itemsReq;
        private int memberLimit;
        private final Map<String, Integer> positionLimits;
        private final Map<String, Double> attributeBonuses;
        private final Map<String, Double> expMultipliers;

        public LevelConfig(int level) {
            this.level = level;
            this.moneyReq = 0;
            this.honorReq = 0;
            this.activityReq = 0;
            this.itemsReq = new java.util.ArrayList<>();
            this.memberLimit = 10;
            this.positionLimits = new HashMap<>();
            this.attributeBonuses = new HashMap<>();
            this.expMultipliers = new HashMap<>();
        }

        public int getLevel() {
            return level;
        }

        public double getMoneyReq() {
            return moneyReq;
        }

        public void setMoneyReq(double moneyReq) {
            this.moneyReq = moneyReq;
        }

        public double getHonorReq() {
            return honorReq;
        }

        public void setHonorReq(double honorReq) {
            this.honorReq = honorReq;
        }

        public double getActivityReq() {
            return activityReq;
        }

        public void setActivityReq(double activityReq) {
            this.activityReq = activityReq;
        }

        public java.util.List<String> getItemsReq() {
            return itemsReq;
        }

        public void setItemsReq(java.util.List<String> itemsReq) {
            this.itemsReq = itemsReq;
        }

        public int getMemberLimit() {
            return memberLimit;
        }

        public void setMemberLimit(int memberLimit) {
            this.memberLimit = memberLimit;
        }

        public Map<String, Integer> getPositionLimits() {
            return positionLimits;
        }

        public int getPositionLimit(String position) {
            return positionLimits.getOrDefault(position, -1);
        }

        public void setPositionLimit(String position, int limit) {
            this.positionLimits.put(position, limit);
        }

        public Map<String, Double> getAttributeBonuses() {
            return attributeBonuses;
        }

        public void addAttributeBonus(String attribute, double bonus) {
            this.attributeBonuses.put(attribute, bonus);
        }

        public Map<String, Double> getExpMultipliers() {
            return expMultipliers;
        }

        public void addExpMultiplier(String type, double multiplier) {
            this.expMultipliers.put(type, multiplier);
        }
    }
}
