package org.HUD.hotelRoom.attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家属性数据类
 * 存储单个玩家的所有属性值
 */
public class PlayerAttribute {
    
    private final UUID playerUUID;
    private final Map<String, Double> attributes;
    private long lastUpdated;
    
    public PlayerAttribute(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.attributes = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * 获取玩家UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * 获取指定属性值
     */
    public double getAttribute(String key) {
        return attributes.getOrDefault(key, 0.0);
    }
    
    /**
     * 设置指定属性值（带范围验证）
     */
    public synchronized void setAttribute(String key, double value) {
        // 属性值范围验证
        value = validateAttributeValue(key, value);
        attributes.put(key, value);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * 增加指定属性值
     */
    public synchronized void addAttribute(String key, double value) {
        double current = getAttribute(key);
        setAttribute(key, current + value);
    }
    
    /**
     * 获取所有属性
     */
    public Map<String, Double> getAllAttributes() {
        return new HashMap<>(attributes);
    }
    
    /**
     * 设置所有属性
     */
    public synchronized void setAllAttributes(Map<String, Double> attrs) {
        this.attributes.clear();
        // 验证每个属性值
        for (Map.Entry<String, Double> entry : attrs.entrySet()) {
            double validatedValue = validateAttributeValue(entry.getKey(), entry.getValue());
            this.attributes.put(entry.getKey(), validatedValue);
        }
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * 清空所有属性
     */
    public synchronized void clearAttributes() {
        this.attributes.clear();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * 获取最后更新时间
     */
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * 克隆属性数据
     */
    public synchronized PlayerAttribute clone() {
        PlayerAttribute cloned = new PlayerAttribute(this.playerUUID);
        cloned.attributes.putAll(this.attributes);
        cloned.lastUpdated = this.lastUpdated;
        return cloned;
    }
    
    /**
     * 验证属性值范围
     */
    private double validateAttributeValue(String key, double value) {
        switch (key) {
            // 生命值相关（最小1，最大2048）
            case "health":
                return Math.max(1.0, Math.min(2048.0, value));
            
            // 移动速度（0.0-1.0，Minecraft限制）
            case "movement_speed":
                return Math.max(0.0, Math.min(1.0, value));
            
            // 攻击速度（0-24，Minecraft基准4）
            case "attack_speed":
                return Math.max(0.0, Math.min(24.0, value));
            
            // 百分比类属性（0-100%）
            case "crit_rate":
            case "dodge_rate":
            case "block_rate":
            case "lifesteal":
            case "cooldown_reduction":
            case "exp_bonus":
            case "drop_rate_bonus":
            case "health_regen_percent":
            case "mana_regen_percent":
            case "movement_speed_percent":
                return Math.max(0.0, Math.min(100.0, value));
            
            // 暴击伤害（100%-1000%）
            case "crit_damage":
                return Math.max(100.0, Math.min(1000.0, value));
            
            // 魔法值相关（0-10000）
            case "mana":
            case "max_mana":
                return Math.max(0.0, Math.min(10000.0, value));
            
            // 防御属性（0-1000）
            case "physical_defense":
            case "magic_defense":
            case "armor":
            case "armor_penetration":
            case "magic_penetration":
                return Math.max(0.0, Math.min(1000.0, value));
            
            // 伤害属性（0-10000）
            case "physical_damage":
            case "magic_damage":
            case "pvp_damage":
            case "pve_damage":
                return Math.max(0.0, Math.min(10000.0, value));
            
            // 百分比属性（0-100%）
            case "hit_rate":
            case "damage_reduction":
                return Math.max(0.0, Math.min(100.0, value));
            
            // 恢复属性（0-1000）
            case "health_regen":
            case "mana_regen":
                return Math.max(0.0, Math.min(1000.0, value));
            
            // 默认：不允许负数，最大100000
            default:
                return Math.max(0.0, Math.min(100000.0, value));
        }
    }
}
