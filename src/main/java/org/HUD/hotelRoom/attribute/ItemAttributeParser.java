package org.HUD.hotelRoom.attribute;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物品属性解析器
 * 从物品 Lore 中解析属性
 */
public class ItemAttributeParser {
    
    // 匹配格式: "§f属性名: §f+数值" 或 "属性名: §f+数值" 或 "属性名 +数值" 或 "属性名+数值" 或 "属性名: 数值"
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(?:§[0-9a-fk-or]*)?(.*?)[:：]?\\s*(?:§[0-9a-fk-or]*)?([+\\-]?\\d+(?:\\.\\d+)?)");
    
    // 属性映射表 (显示名称 -> 内部key)
    private static final Map<String, String> ATTRIBUTE_NAME_MAP = new HashMap<>();
    
    static {
        // 基础属性
        ATTRIBUTE_NAME_MAP.put("生命值", "health");
        ATTRIBUTE_NAME_MAP.put("最大生命值", "health");
        ATTRIBUTE_NAME_MAP.put("❤ 生命值", "health");
        ATTRIBUTE_NAME_MAP.put("❤ 最大生命值", "health");
        
        // 攻击属性
        ATTRIBUTE_NAME_MAP.put("物理伤害", "physical_damage");
        ATTRIBUTE_NAME_MAP.put("魔法伤害", "magic_damage");
        ATTRIBUTE_NAME_MAP.put("攻击速度", "attack_speed");
        ATTRIBUTE_NAME_MAP.put("⚔ 物理伤害", "physical_damage");
        ATTRIBUTE_NAME_MAP.put("✦ 魔法伤害", "magic_damage");
        ATTRIBUTE_NAME_MAP.put("⚡ 攻击速度", "attack_speed");
        
        // 防御属性
        ATTRIBUTE_NAME_MAP.put("物理防御", "physical_defense");
        ATTRIBUTE_NAME_MAP.put("魔法防御", "magic_defense");
        ATTRIBUTE_NAME_MAP.put("护甲值", "armor");
        ATTRIBUTE_NAME_MAP.put("🛡 物理防御", "physical_defense");
        ATTRIBUTE_NAME_MAP.put("🛡 魔法防御", "magic_defense");
        ATTRIBUTE_NAME_MAP.put("⛨ 护甲值", "armor");
        
        // 暴击属性
        ATTRIBUTE_NAME_MAP.put("暴击率", "crit_rate");
        ATTRIBUTE_NAME_MAP.put("暴击伤害", "crit_damage");
        ATTRIBUTE_NAME_MAP.put("☄ 暴击率", "crit_rate");
        ATTRIBUTE_NAME_MAP.put("☄ 暴击伤害", "crit_damage");
        
        // 穿透属性
        ATTRIBUTE_NAME_MAP.put("护甲穿透", "armor_penetration");
        ATTRIBUTE_NAME_MAP.put("魔法穿透", "magic_penetration");
        ATTRIBUTE_NAME_MAP.put("⚔ 护甲穿透", "armor_penetration");
        ATTRIBUTE_NAME_MAP.put("⚔ 魔法穿透", "magic_penetration");
        
        // 生命恢复
        ATTRIBUTE_NAME_MAP.put("生命恢复", "health_regen");
        ATTRIBUTE_NAME_MAP.put("生命恢复%", "health_regen_percent");
        ATTRIBUTE_NAME_MAP.put("♥ 生命恢复", "health_regen");
        ATTRIBUTE_NAME_MAP.put("♥ 生命恢复%", "health_regen_percent");
        
        // 魔法属性
        ATTRIBUTE_NAME_MAP.put("魔法值", "mana");
        ATTRIBUTE_NAME_MAP.put("最大魔法值", "max_mana");
        ATTRIBUTE_NAME_MAP.put("魔法恢复", "mana_regen");
        ATTRIBUTE_NAME_MAP.put("魔法恢复%", "mana_regen_percent");
        ATTRIBUTE_NAME_MAP.put("✦ 魔法값", "mana");
        ATTRIBUTE_NAME_MAP.put("✦ 最大魔法값", "max_mana");
        ATTRIBUTE_NAME_MAP.put("✦ 魔法恢复", "mana_regen");
        ATTRIBUTE_NAME_MAP.put("✦ 魔法恢复%", "mana_regen_percent");
        
        // 移动属性
        ATTRIBUTE_NAME_MAP.put("移动速度", "movement_speed");
        ATTRIBUTE_NAME_MAP.put("移动速度%", "movement_speed_percent");
        ATTRIBUTE_NAME_MAP.put("➤ 移动速度", "movement_speed");
        ATTRIBUTE_NAME_MAP.put("➤ 移动速度%", "movement_speed_percent");
        
        // 其他属性
        ATTRIBUTE_NAME_MAP.put("闪避率", "dodge_rate");
        ATTRIBUTE_NAME_MAP.put("格挡率", "block_rate");
        ATTRIBUTE_NAME_MAP.put("生命偷取", "lifesteal");
        ATTRIBUTE_NAME_MAP.put("冷却缩减", "cooldown_reduction");
        ATTRIBUTE_NAME_MAP.put("经验加成", "exp_bonus");
        ATTRIBUTE_NAME_MAP.put("掉落率加成", "drop_rate_bonus");
        ATTRIBUTE_NAME_MAP.put("◈ 闪避率", "dodge_rate");
        ATTRIBUTE_NAME_MAP.put("◈ 格挡率", "block_rate");
        ATTRIBUTE_NAME_MAP.put("♥ 生命偷取", "lifesteal");
        ATTRIBUTE_NAME_MAP.put("⌚ 冷却缩减", "cooldown_reduction");
        ATTRIBUTE_NAME_MAP.put("★ 经验加成", "exp_bonus");
        ATTRIBUTE_NAME_MAP.put("✦ 掉落率加成", "drop_rate_bonus");
    }
    
    /**
     * 从物品 Lore 中解析属性
     */
    public static Map<String, Double> parseAttributes(ItemStack item) {
        Map<String, Double> attributes = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return attributes;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return attributes;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            return attributes;
        }
        
        for (String line : lore) {
            // 尝试匹配属性格式
            Matcher matcher = ATTRIBUTE_PATTERN.matcher(line);
            if (matcher.find()) {
                String attributeName = matcher.group(1).trim();
                String valueStr = matcher.group(2).trim();
                
                // 清理属性名中的多余字符，保留百分号
                attributeName = attributeName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z%]", "").trim();
                
                try {
                    double value = Double.parseDouble(valueStr);
                    
                    // 查找对应的内部属性key
                    String attributeKey = findAttributeKey(attributeName);
                    if (attributeKey != null) {
                        attributes.merge(attributeKey, value, Double::sum);
                        // 调试日志
                        AttributeManager manager = AttributeManager.getInstance();
                        if (manager != null && manager.isLoggingEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                "[Lore解析] 匹配到: '" + line + "' -> " + attributeKey + " = " + value);
                        }
                    } else {
                        AttributeManager attrManager = AttributeManager.getInstance();
                        if (attrManager != null && attrManager.isLoggingEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                                "[Lore解析] 未知属性: '" + attributeName + "' 在 Lore: '" + line + "'");
                        }
                    }
                } catch (NumberFormatException e) {
                    AttributeManager manager = AttributeManager.getInstance();
                    if (manager != null && manager.isLoggingEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                            "[Lore解析] 无效数值: '" + valueStr + "' 在 Lore: '" + line + "'");
                    }
                }
            } else {
                // 调试：显示未匹配的Lore
                String cleanLine = line.replaceAll("§[0-9a-fk-or]", "");
                if (cleanLine.contains("伤害") || cleanLine.contains("生命") || 
                    cleanLine.contains("防御") || cleanLine.contains("暴击")) {
                    AttributeManager manager = AttributeManager.getInstance();
                    if (manager != null && manager.isLoggingEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                            "[Lore解析] 未匹配: '" + line + "' (清理后: '" + cleanLine + "')");
                    }
                }
            }
        }
        
        return attributes;
    }
    
    /**
     * 查找属性名称对应的内部key
     */
    private static String findAttributeKey(String displayName) {
        // 直接匹配
        if (ATTRIBUTE_NAME_MAP.containsKey(displayName)) {
            return ATTRIBUTE_NAME_MAP.get(displayName);
        }
        
        // 去除表情符号和空格后匹配，保留百分号
        String cleanName = displayName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z%]", "").trim();
        if (ATTRIBUTE_NAME_MAP.containsKey(cleanName)) {
            return ATTRIBUTE_NAME_MAP.get(cleanName);
        }
        
        // 模糊匹配
        for (Map.Entry<String, String> entry : ATTRIBUTE_NAME_MAP.entrySet()) {
            if (entry.getKey().contains(cleanName) || cleanName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 注册自定义属性名称映射
     */
    public static void registerAttributeMapping(String displayName, String internalKey) {
        ATTRIBUTE_NAME_MAP.put(displayName, internalKey);
    }
    
    /**
     * 获取所有属性名称映射
     */
    public static Map<String, String> getAttributeNameMap() {
        return new HashMap<>(ATTRIBUTE_NAME_MAP);
    }
}