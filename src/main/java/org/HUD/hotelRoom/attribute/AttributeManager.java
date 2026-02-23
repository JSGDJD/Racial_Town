package org.HUD.hotelRoom.attribute;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性管理器
 * 负责管理所有玩家的属性数据
 */
public class AttributeManager {
    
    private static AttributeManager instance;
    private final HotelRoom plugin;
    
    // 内存缓存
    private final Map<UUID, PlayerAttribute> attributeCache = new ConcurrentHashMap<>();
    
    // 配置
    private YamlConfiguration config;
    private boolean enabled;
    private boolean autoRefreshOnJoin;
    private boolean damageChatEnabled;  // 是否在聊天框显示伤害信息
    private boolean loggingEnabled;     // 是否启用详细日志
    private static boolean damageLogEnabled = false;  // 是否启用伤害计算日志（从主配置读取）
    private Map<String, Double> defaultAttributes;
    private Map<String, String> attributeDisplayNames;
    private Map<String, Double> calculationLimits;
    
    private AttributeManager(HotelRoom plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }
    
    public static void initialize(HotelRoom plugin) {
        instance = new AttributeManager(plugin);
        AttributeStorage.initialize();
    }
    
    public static AttributeManager getInstance() {
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfiguration() {
        loadDamageLogConfig();
        
        File attributesFolder = new File(plugin.getDataFolder(), "attributes");
        if (!attributesFolder.exists()) {
            attributesFolder.mkdirs();
            plugin.getLogger().info("已创建属性配置文件夹: attributes/");
        }
        
        File configFile = new File(attributesFolder, "attributes.yml");
        if (!configFile.exists()) {
            plugin.saveResource("attributes/attributes.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        autoRefreshOnJoin = config.getBoolean("auto-refresh-on-join", true);
        damageChatEnabled = config.getBoolean("logging.show-damage-in-chat", true);
        loggingEnabled = config.getBoolean("logging.enabled", true);
        
        defaultAttributes = new HashMap<>();
        ConfigurationSection defaultSection = config.getConfigurationSection("default-attributes");
        if (defaultSection != null) {
            for (String key : defaultSection.getKeys(false)) {
                defaultAttributes.put(key, defaultSection.getDouble(key));
            }
        }
        
        attributeDisplayNames = new HashMap<>();
        ConfigurationSection displaySection = config.getConfigurationSection("display.names");
        if (displaySection != null) {
            for (String key : displaySection.getKeys(false)) {
                attributeDisplayNames.put(key, displaySection.getString(key));
            }
        }
        
        calculationLimits = new HashMap<>();
        ConfigurationSection calcSection = config.getConfigurationSection("calculation");
        if (calcSection != null) {
            calculationLimits.put("armor-effectiveness", calcSection.getDouble("armor-effectiveness", 100.0));
            calculationLimits.put("max-crit-rate", calcSection.getDouble("max-crit-rate", 100.0));
            calculationLimits.put("max-dodge-rate", calcSection.getDouble("max-dodge-rate", 75.0));
            calculationLimits.put("max-block-rate", calcSection.getDouble("max-block-rate", 75.0));
            calculationLimits.put("max-cooldown-reduction", calcSection.getDouble("max-cooldown-reduction", 80.0));
        }
        
        plugin.getLogger().info("属性系统配置已加载 (启用: " + enabled + ", 伤害显示: " + damageChatEnabled + ", 详细日志: " + loggingEnabled + ")");
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfiguration();
        
        // 重新应用所有在线玩家的属性
        if (enabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                refreshPlayerAttributes(player);
            }
        }
    }
    
    /**
     * 从主配置加载伤害日志开关
     */
    public static void loadDamageLogConfig() {
        HotelRoom plugin = HotelRoom.get();
        if (plugin != null) {
            damageLogEnabled = plugin.getConfig().getBoolean("logging.damage-log-enabled", false);
        }
    }
    
    /**
     * 检查是否启用伤害计算日志
     */
    public static boolean isDamageLogEnabled() {
        return damageLogEnabled;
    }
    
    /**
     * 检查系统是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取玩家属性
     * 注意：返回的是缓存中的引用，修改后需要调用 refreshPlayerAttributes() 应用
     */
    public PlayerAttribute getPlayerAttribute(UUID playerUUID) {
        // 先从缓存获取
        PlayerAttribute cached = attributeCache.get(playerUUID);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库加载
        PlayerAttribute attr = new PlayerAttribute(playerUUID);
        Map<String, Double> loadedAttrs = AttributeStorage.loadPlayerAttributes(playerUUID);
        
        if (loadedAttrs.isEmpty()) {
            // 使用默认属性
            attr.setAllAttributes(new HashMap<>(defaultAttributes));
        } else {
            // 合并默认属性和加载的属性（确保所有属性都存在）
            Map<String, Double> mergedAttrs = new HashMap<>(defaultAttributes);
            mergedAttrs.putAll(loadedAttrs);
            attr.setAllAttributes(mergedAttrs);
        }
        
        attributeCache.put(playerUUID, attr);
        return attr;
    }
    
    /**
     * 获取玩家指定属性值
     */
    public double getPlayerAttributeValue(UUID playerUUID, String key) {
        return getPlayerAttribute(playerUUID).getAttribute(key);
    }
    
    /**
     * 设置玩家属性值
     */
    public void setPlayerAttributeValue(UUID playerUUID, String key, double value) {
        PlayerAttribute attr = getPlayerAttribute(playerUUID);
        attr.setAttribute(key, value);
        
        // 异步保存到数据库
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Double> attrs = new HashMap<>();
            attrs.put(key, value);
            AttributeStorage.savePlayerAttributes(playerUUID, attrs);
        });
    }
    
    /**
     * 保存玩家属性
     */
    public void savePlayerAttributes(UUID playerUUID) {
        PlayerAttribute attr = attributeCache.get(playerUUID);
        if (attr != null) {
            AttributeStorage.savePlayerAttributes(playerUUID, attr.getAllAttributes());
        }
    }
    
    /**
     * 刷新玩家属性（应用到游戏中）
     */
    public void refreshPlayerAttributes(Player player) {
        if (!enabled) return;
        
        UUID uuid = player.getUniqueId();
        PlayerAttribute attr = getPlayerAttribute(uuid);
        
        // 应用生命值
        double health = attr.getAttribute("health");
        if (health > 0) {
            // 获取当前真实的最大血量（baseValue 不受药水影响）
            double oldMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            double currentHealth = player.getHealth();
            
            // 防止除零错误：检查 oldMaxHealth 是否有效
            if (oldMaxHealth <= 0) {
                oldMaxHealth = 20.0; // 使用原版默认值
                plugin.getLogger().warning("[血量调整] " + player.getName() + " 的旧最大血量异常，使用默认值20.0");
            }
            
            // 如果最大血量没有变化，直接返回（避免不必要的血量调整）
            if (Math.abs(oldMaxHealth - health) < 0.01) {
                return;
            }
            
            // 计算血量百分比（确保不超过100%）
            double healthPercent = Math.min(currentHealth / oldMaxHealth, 1.0);
            
            // 设置新的最大血量
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
            
            // 按百分比恢复血量
            double newHealth = health * healthPercent;
            player.setHealth(Math.max(1.0, Math.min(newHealth, health)));
            
            // 同步更新缓存中的生命值
            attr.setAttribute("health", player.getHealth());
        }
        
        // 应用移动速度
        double movementSpeed = attr.getAttribute("movement_speed");
        double movementSpeedPercent = attr.getAttribute("movement_speed_percent");
        if (movementSpeed > 0 || movementSpeedPercent > 0) {
            double finalSpeed = movementSpeed * (1.0 + movementSpeedPercent / 100.0);
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(finalSpeed);
        }
        
        // 应用攻击速度
        double attackSpeed = attr.getAttribute("attack_speed");
        if (attackSpeed > 0) {
            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(attackSpeed * 4.0); // Minecraft基准是4
        }
        
        plugin.getLogger().fine("已刷新玩家 " + player.getName() + " 的属性");
    }
    
    /**
     * 重置玩家属性为默认值
     */
    public void resetPlayerAttributes(UUID playerUUID) {
        PlayerAttribute attr = new PlayerAttribute(playerUUID);
        attr.setAllAttributes(new HashMap<>(defaultAttributes));
        attributeCache.put(playerUUID, attr);
        
        // 异步保存
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AttributeStorage.deletePlayerAttributes(playerUUID);
            AttributeStorage.savePlayerAttributes(playerUUID, attr.getAllAttributes());
        });
    }
    
    /**
     * 移除玩家缓存
     */
    public void removePlayerCache(UUID playerUUID) {
        attributeCache.remove(playerUUID);
    }
    
    /**
     * 获取属性显示名称
     */
    public String getAttributeDisplayName(String key) {
        return attributeDisplayNames.getOrDefault(key, key);
    }
    
    /**
     * 根据显示名称获取属性key
     */
    public String getAttributeKeyByDisplayName(String displayName) {
        // 遍历显示名称映射
        for (Map.Entry<String, String> entry : attributeDisplayNames.entrySet()) {
            String key = entry.getKey();
            String display = entry.getValue();
            
            // 去除颜色代码和符号后比较
            String cleanDisplay = display.replaceAll("§[0-9a-fk-or]", "").replaceAll("[^\\u4e00-\\u9fa5a-zA-Z%]", "").trim();
            String cleanInput = displayName.replaceAll("§[0-9a-fk-or]", "").replaceAll("[^\\u4e00-\\u9fa5a-zA-Z%]", "").trim();
            
            if (cleanDisplay.equals(cleanInput)) {
                return key;
            }
        }
        
        // 直接匹配内部key
        if (defaultAttributes.containsKey(displayName)) {
            return displayName;
        }
        
        return null;
    }
    
    /**
     * 获取所有默认属性
     */
    public Map<String, Double> getDefaultAttributes() {
        return new HashMap<>(defaultAttributes);
    }
    
    /**
     * 是否在加入时自动刷新
     */
    public boolean isAutoRefreshOnJoin() {
        return autoRefreshOnJoin;
    }
    
    /**
     * 检查是否启用伤害聊天显示
     */
    public boolean isDamageChatEnabled() {
        return damageChatEnabled;
    }
    
    /**
     * 检查是否启用详细日志
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
    
    /**
     * 计算实际伤害（考虑护甲）
     */
    public double calculatePhysicalDamage(double baseDamage, double armor) {
        double effectiveness = calculationLimits.getOrDefault("armor-effectiveness", 100.0);
        double reduction = armor / (armor + effectiveness);
        return baseDamage * (1.0 - reduction);
    }
    
    /**
     * 应用暴击率上限
     */
    public double applyCritRateCap(double critRate) {
        double maxCritRate = calculationLimits.getOrDefault("max-crit-rate", 100.0);
        return Math.min(critRate, maxCritRate);
    }
    
    /**
     * 应用闪避率上限
     */
    public double applyDodgeRateCap(double dodgeRate) {
        double maxDodgeRate = calculationLimits.getOrDefault("max-dodge-rate", 75.0);
        return Math.min(dodgeRate, maxDodgeRate);
    }
    
    /**
     * 注册自定义属性
     */
    public void registerCustomAttribute(String key, String displayName, double defaultValue) {
        if (!defaultAttributes.containsKey(key)) {
            defaultAttributes.put(key, defaultValue);
            attributeDisplayNames.put(key, displayName);
            plugin.getLogger().info("[属性系统] 注册自定义属性: " + displayName + " (" + key + ")");
        }
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        // 保存所有缓存
        for (Map.Entry<UUID, PlayerAttribute> entry : attributeCache.entrySet()) {
            AttributeStorage.savePlayerAttributes(entry.getKey(), entry.getValue().getAllAttributes());
        }
        attributeCache.clear();
        AttributeStorage.close();
    }
}
