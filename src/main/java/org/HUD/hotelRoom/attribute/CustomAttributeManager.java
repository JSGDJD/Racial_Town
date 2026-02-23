package org.HUD.hotelRoom.attribute;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 自定义属性管理器
 * 管理所有自定义属性及其效果计算
 */
public class CustomAttributeManager {
    
    private static CustomAttributeManager instance;
    // 使用 ConcurrentHashMap 保证并发安全
    private final Map<String, CustomAttribute> customAttributes = new java.util.concurrent.ConcurrentHashMap<>();
    private final org.HUD.hotelRoom.HotelRoom plugin;
    
    private CustomAttributeManager(org.HUD.hotelRoom.HotelRoom plugin) {
        this.plugin = plugin;
    }
    
    public static void initialize(org.HUD.hotelRoom.HotelRoom plugin) {
        instance = new CustomAttributeManager(plugin);
        instance.loadCustomAttributes();
    }
    
    public static CustomAttributeManager getInstance() {
        return instance;
    }
    
    /**
     * 从配置文件加载自定义属性
     */
    public void loadCustomAttributes() {
        customAttributes.clear();
        
        // 路径改为 attributes/attributes.yml
        File configFile = new File(plugin.getDataFolder(), "attributes/attributes.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("attributes/attributes.yml 不存在");
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection customSection = config.getConfigurationSection("custom-attributes");
        
        if (customSection == null) {
            plugin.getLogger().info("未找到自定义属性配置");
            return;
        }
        
        for (String key : customSection.getKeys(false)) {
            try {
                ConfigurationSection attrSection = customSection.getConfigurationSection(key);
                if (attrSection == null) continue;
                
                CustomAttribute attr = new CustomAttribute();
                attr.key = key;
                attr.displayName = attrSection.getString("display-name", key);
                attr.defaultValue = attrSection.getDouble("default-value", 0.0);
                attr.minValue = attrSection.getDouble("min-value", 0.0);
                attr.maxValue = attrSection.getDouble("max-value", 10000.0);
                attr.type = attrSection.getString("type", "passive");
                attr.trigger = attrSection.getString("trigger", "passive");
                attr.formula = attrSection.getString("formula", "0");
                attr.ignoreDefense = attrSection.getBoolean("ignore-defense", false);
                attr.effectType = attrSection.getString("effect-type", "damage");
                attr.target = attrSection.getString("target", "victim");
                attr.chance = attrSection.getString("chance", null);
                attr.showMessage = attrSection.getBoolean("show-message", false);
                attr.message = attrSection.getString("message", "");
                attr.description = attrSection.getString("description", "");
                
                customAttributes.put(key, attr);
                
                // 注册到默认属性中
                AttributeManager attrManager = AttributeManager.getInstance();
                if (attrManager != null) {
                    attrManager.registerCustomAttribute(key, attr.displayName, attr.defaultValue);
                }
                
                plugin.getLogger().info("[自定义属性] 已加载: " + attr.displayName + " (" + key + ")");
            } catch (Exception e) {
                plugin.getLogger().severe("[自定义属性] 加载失败: " + key);
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("[自定义属性] 共加载 " + customAttributes.size() + " 个自定义属性");
    }
    
    /**
     * 获取指定触发时机的所有自定义属性
     */
    public List<CustomAttribute> getAttributesByTrigger(String trigger) {
        List<CustomAttribute> result = new ArrayList<>();
        for (CustomAttribute attr : customAttributes.values()) {
            if (attr.trigger.equals(trigger)) {
                result.add(attr);
            }
        }
        return result;
    }
    
    /**
     * 获取自定义属性
     */
    public CustomAttribute getCustomAttribute(String key) {
        return customAttributes.get(key);
    }
    
    /**
     * 获取所有自定义属性
     */
    public Collection<CustomAttribute> getAllCustomAttributes() {
        return customAttributes.values();
    }
    
    /**
     * 重新加载
     */
    public void reload() {
        loadCustomAttributes();
    }
    
    /**
     * 自定义属性数据类
     */
    public static class CustomAttribute {
        public String key;                  // 属性键
        public String displayName;          // 显示名称
        public double defaultValue;         // 默认值
        public double minValue;             // 最小值
        public double maxValue;             // 最大值
        public String type;                 // 类型：offensive, defensive, utility, passive
        public String trigger;              // 触发时机：on_attack, on_defend, on_kill, passive
        public String formula;              // 计算公式
        public boolean ignoreDefense;       // 是否忽略防御
        public String effectType;           // 效果类型：heal, damage, buff, debuff
        public String target;               // 目标：attacker, victim, self
        public String chance;               // 触发概率（可以是公式）
        public boolean showMessage;         // 是否显示消息
        public String message;              // 消息内容
        public String description;          // 描述
        
        /**
         * 检查是否触发（基于概率）
         */
        public boolean shouldTrigger(Map<String, Double> variables) {
            if (chance == null || chance.isEmpty()) {
                return true; // 没有概率限制，总是触发
            }
            
            try {
                double chanceValue = FormulaEvaluator.evaluate(chance, variables);
                return Math.random() * 100 < chanceValue;
            } catch (Exception e) {
                org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                    "[自定义属性] 概率计算失败: " + chance);
                return false;
            }
        }
        
        /**
         * 计算效果值
         */
        public double calculateEffect(Map<String, Double> variables) {
            try {
                return FormulaEvaluator.evaluate(formula, variables);
            } catch (Exception e) {
                org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                    "[自定义属性] 公式计算失败: " + formula);
                return 0.0;
            }
        }
    }
}
