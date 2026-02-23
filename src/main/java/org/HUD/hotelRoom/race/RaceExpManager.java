package org.HUD.hotelRoom.race;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * 种族经验管理器
 * 管理种族经验获取方式和等级系统
 */
public class RaceExpManager {
    
    private static RaceExpManager instance;
    private final HotelRoom plugin;
    
    // 经验获取方式配置 <方法名, <属性名, 值>>
    private Map<String, Map<String, Object>> expMethods = new HashMap<>();
    
    // 种族允许的经验获取方式 <种族名, List<方法名>>
    private Map<String, List<String>> raceAllowedMethods = new HashMap<>();
    
    // 等级所需经验配置
    private Map<Integer, Integer> levelExpRequirements = new HashMap<>();
    
    private RaceExpManager(HotelRoom plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public static void initialize(HotelRoom plugin) {
        if (instance == null) {
            instance = new RaceExpManager(plugin);
        }
    }
    
    public static RaceExpManager getInstance() {
        return instance;
    }
    
    /**
     * 从race.yml加载配置
     */
    public void loadConfig() {
        File raceConfigFile = new File(plugin.getDataFolder(), "race.yml");
        if (!raceConfigFile.exists()) {
            plugin.saveResource("race.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(raceConfigFile);
        
        // 加载经验获取方式配置
        expMethods.clear();
        ConfigurationSection methodsSection = config.getConfigurationSection("experience-methods");
        if (methodsSection != null) {
            for (String methodName : methodsSection.getKeys(false)) {
                Map<String, Object> methodConfig = new HashMap<>();
                ConfigurationSection methodSection = methodsSection.getConfigurationSection(methodName);
                if (methodSection != null) {
                    for (String key : methodSection.getKeys(false)) {
                        methodConfig.put(key, methodSection.get(key));
                    }
                }
                expMethods.put(methodName, methodConfig);
            }
        }
        
        // 加载种族允许的经验获取方式
        raceAllowedMethods.clear();
        ConfigurationSection raceMethodsSection = config.getConfigurationSection("race-experience-methods");
        if (raceMethodsSection != null) {
            for (String raceName : raceMethodsSection.getKeys(false)) {
                List<String> methods = raceMethodsSection.getStringList(raceName);
                raceAllowedMethods.put(raceName, methods);
            }
        }
        
        // 加载等级经验需求
        levelExpRequirements.clear();
        ConfigurationSection levelSection = config.getConfigurationSection("level-requirements");
        if (levelSection != null) {
            for (String levelStr : levelSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelStr);
                    int expRequired = levelSection.getInt(levelStr);
                    levelExpRequirements.put(level, expRequired);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        // 如果没有配置，使用默认值
        if (levelExpRequirements.isEmpty()) {
            for (int i = 1; i <= 100; i++) {
                levelExpRequirements.put(i, i * 100);
            }
        }
        
        plugin.getLogger().info("种族经验系统配置已加载: " + expMethods.size() + " 个经验获取方式");
    }
    
    /**
     * 重载配置
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * 检查种族是否允许使用某个经验获取方式
     */
    public boolean isMethodAllowedForRace(String race, String method) {
        if (race == null || method == null) {
            return false;
        }
        
        if (raceAllowedMethods.isEmpty()) {
            return true;
        }
        
        List<String> allowedMethods = raceAllowedMethods.get(race);
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            return false;
        }
        
        return allowedMethods.contains(method);
    }
    
    /**
     * 检查经验获取方式是否启用
     */
    public boolean isMethodEnabled(String method) {
        Map<String, Object> methodConfig = expMethods.get(method);
        if (methodConfig == null) {
            return false;
        }
        
        Object enabled = methodConfig.get("enabled");
        return enabled instanceof Boolean && (Boolean) enabled;
    }
    
    /**
     * 获取经验获取方式的经验值
     */
    public int getExpValue(String method, String key) {
        Map<String, Object> methodConfig = expMethods.get(method);
        if (methodConfig == null) {
            return 0;
        }
        
        Object value = methodConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        return 0;
    }
    
    /**
     * 给玩家添加种族经验
     */
    public void addExperience(Player player, int amount, String source) {
        UUID uuid = player.getUniqueId();
        String race = RaceDataStorage.getPlayerRace(uuid);
        
        if (race == null) {
            return;
        }
        
        // 检查是否允许该种族使用此经验获取方式
        if (source != null && !source.equals("command") && !isMethodAllowedForRace(race, source)) {
            return;
        }
        
        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        int currentExp = RaceDataStorage.getPlayerRaceExperience(uuid);
        int newExp = currentExp + amount;
        
        // 获取该种族的最大等级
        int maxLevel = 100;
        RaceDataStorage.RaceConfig raceConfig = RaceDataStorage.getRaceConfig(race);
        if (raceConfig != null) {
            maxLevel = raceConfig.maxLevel;
        }
        
        // 检查是否升级
        int newLevel = currentLevel;
        boolean leveledUp = false;
        while (newLevel < maxLevel && newExp >= getExpRequired(newLevel + 1)) {
            newExp -= getExpRequired(newLevel + 1);
            newLevel++;
            leveledUp = true;
            
            player.sendMessage("§a§l[种族系统] §e你的 §6" + race + " §e种族等级提升到了 §b" + newLevel + " §e级！");
            
            if (newLevel >= maxLevel) {
                player.sendMessage("§6§l[种族系统] §e恭喜你已达到种族最高等级 " + maxLevel + " 级！");
                break;
            }
        }
        
        // 保存数据
        RaceDataStorage.setPlayerRace(uuid, race, newLevel, newExp);
        
        // 如果升级了，重新应用种族属性
        if (leveledUp) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                RaceAttributeManager attrManager = RaceAttributeManager.getInstance();
                if (attrManager != null) {
                    attrManager.applyRaceAttributes(player);
                }
            });
        }
        
        // 发送经验获得提示
        if (amount > 0) {
            player.sendMessage("§a§l[种族系统] §7+" + amount + " 经验 §8(来源: " + (source != null ? source : "未知") + ")");
        }
    }
    
    /**
     * 设置玩家种族经验
     */
    public void setExperience(UUID uuid, int amount) {
        String race = RaceDataStorage.getPlayerRace(uuid);
        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        RaceDataStorage.setPlayerRace(uuid, race, currentLevel, amount);
    }
    
    /**
     * 获取指定等级所需经验
     */
    public int getExpRequired(int level) {
        return levelExpRequirements.getOrDefault(level, level * 100);
    }
    
    /**
     * 获取所有经验获取方式
     */
    public Set<String> getAllMethods() {
        return expMethods.keySet();
    }
    
    /**
     * 获取种族允许的经验获取方式
     */
    public List<String> getRaceAllowedMethods(String race) {
        return raceAllowedMethods.getOrDefault(race, new ArrayList<>(expMethods.keySet()));
    }
}
