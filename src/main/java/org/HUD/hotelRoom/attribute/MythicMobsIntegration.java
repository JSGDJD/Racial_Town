package org.HUD.hotelRoom.attribute;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * MythicMobs 集成类
 * 负责直接使用 MythicMobs API 处理怪物属性
 * 注意：此文件仅在 MythicMobs 安装时才会被加载
 */
public class MythicMobsIntegration implements Listener {

    private final HotelRoom plugin;
    private final MythicMobsAttributeManager attributeManager;

    public MythicMobsIntegration(HotelRoom plugin) {
        this.plugin = plugin;
        this.attributeManager = MythicMobsAttributeManager.getInstance();
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (AttributeManager.isDamageLogEnabled()) {
            plugin.getLogger().info("[MythicMobs 集成] 已注册 MythicMobs 事件监听器");
        }
    }

    /**
     * 处理 MythicMobs 怪物生成事件
     */
    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        ActiveMob activeMob = event.getMob();
        if (activeMob == null) {
            if (AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 怪物实体为null");
            }
            return;
        }

        // 获取怪物的类型名称
        String mobId = activeMob.getType().getInternalName();
        if (mobId == null) {
            if (AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 怪物类型ID为null");
            }
            return;
        }

        if (AttributeManager.isDamageLogEnabled()) {
            plugin.getLogger().info("[MythicMobs 集成] 检测到怪物生成: " + mobId);
        }

        // 检查是否有该怪物的自定义属性配置
        Map<String, Double> attributes = attributeManager.getMobAttributes(mobId);
        if (attributes == null || attributes.isEmpty()) {
            if (AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 怪物 " + mobId + " 没有配置自定义属性");
            }
            return;
        }

        if (AttributeManager.isDamageLogEnabled()) {
            plugin.getLogger().info("[MythicMobs 集成] 怪物 " + mobId + " 有 " + attributes.size() + " 个自定义属性");
        }

        // 应用自定义属性
        applyAttributes(activeMob, attributes);
    }

    /**
     * 应用自定义属性到 MythicMobs 怪物
     */
    private void applyAttributes(ActiveMob activeMob, Map<String, Double> attributes) {
        try {
            // 尝试获取真实的Bukkit实体
            org.bukkit.entity.LivingEntity livingEntity = null;
            Object entity = activeMob.getEntity();
            
            if (entity != null && entity.getClass().getName().equals("io.lumine.mythic.bukkit.adapters.BukkitEntity")) {
                try {
                    // 尝试使用getBukkitEntity方法获取真实的Bukkit实体
                    java.lang.reflect.Method getBukkitEntityMethod = entity.getClass().getMethod("getBukkitEntity");
                    Object bukkitEntity = getBukkitEntityMethod.invoke(entity);
                    
                    if (bukkitEntity != null && bukkitEntity instanceof org.bukkit.entity.LivingEntity) {
                        livingEntity = (org.bukkit.entity.LivingEntity) bukkitEntity;
                    }
                } catch (Exception e) {
                    if (AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 无法从BukkitEntity获取真实实体: " + e.getMessage());
                    }
                }
            } else if (entity != null && entity instanceof org.bukkit.entity.LivingEntity) {
                livingEntity = (org.bukkit.entity.LivingEntity) entity;
            }
            
            // 显示设置属性前的怪物血量
            try {
                if (livingEntity != null && AttributeManager.isDamageLogEnabled()) {
                    double beforeHealth = livingEntity.getHealth();
                    double beforeMaxHealth = livingEntity.getMaxHealth();
                    plugin.getLogger().info("[MythicMobs 集成] 设置属性前的血量: " + beforeHealth + "/" + beforeMaxHealth);
                }
            } catch (Exception e) {
                if (AttributeManager.isDamageLogEnabled()) {
                    plugin.getLogger().info("[MythicMobs 集成] 无法获取设置属性前的血量: " + e.getMessage());
                }
            }
            
            // 使用反射设置属性，以适应不同版本的 MythicMobs API
            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                String attributeKey = entry.getKey();
                double attributeValue = entry.getValue();
                
                // 尝试通过不同的方法设置属性
                boolean attributeSet = false;
                
                // 只对health属性进行血量设置
                if (attributeKey.equals("health")) {
                    // 获取当前的最大生命值和当前生命值
                    double currentMaxHealth = 0;
                    double currentHealth = 0;
                    
                    if (livingEntity != null) {
                        currentMaxHealth = livingEntity.getMaxHealth();
                        currentHealth = livingEntity.getHealth();
                    }
                    
                    // 计算新的生命值（在原血量基础上添加）
                    double newMaxHealth = currentMaxHealth + attributeValue;
                    double newHealth = currentHealth + attributeValue;
                    
                    if (AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 原血量: " + currentHealth + "/" + currentMaxHealth + "，添加 " + attributeValue + " 后: " + newHealth + "/" + newMaxHealth);
                    }
                    
                    // 方法1: 直接从真实的Bukkit实体设置属性
                    if (livingEntity != null) {
                        // 设置最大生命值
                        livingEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(newMaxHealth);
                        // 设置当前生命值
                        livingEntity.setHealth(newHealth);
                        if (AttributeManager.isDamageLogEnabled()) {
                            plugin.getLogger().info("[MythicMobs 集成] 直接从Bukkit实体设置血量: " + newHealth + "/" + newMaxHealth);
                        }
                        attributeSet = true;
                    }
                    
                    // 方法2: 尝试使用Bukkit调度器延迟设置属性
                    if (livingEntity != null) {
                        final org.bukkit.entity.LivingEntity finalLivingEntity = livingEntity;
                        final double finalAttributeValue = attributeValue;
                        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                            @Override
                            public void run() {
                                if (finalLivingEntity.isValid()) {
                                    // 获取MM应用配置后的当前血量和最大血量
                                    double currentMaxHealth = finalLivingEntity.getMaxHealth();
                                    double currentHealth = finalLivingEntity.getHealth();
                                    
                                    // 计算新的生命值（在MM应用配置后的血量基础上添加）
                                    double newMaxHealth = currentMaxHealth + finalAttributeValue;
                                    double newHealth = currentHealth + finalAttributeValue;
                                    
                                    // 对生命值进行四舍五入，避免浮点数精度问题
                                    int roundedMaxHealth = (int) Math.round(newMaxHealth);
                                    int roundedHealth = (int) Math.round(newHealth);
                                    
                                    if (AttributeManager.isDamageLogEnabled()) {
                                        plugin.getLogger().info("[MythicMobs 集成] MM应用配置后的血量: " + currentHealth + "/" + currentMaxHealth + "，添加 " + finalAttributeValue + " 后: " + newMaxHealth + "/" + newMaxHealth + " (四舍五入后: " + roundedHealth + "/" + roundedMaxHealth + ")");
                                    }
                                    
                                    // 延迟设置最大生命值
                                    finalLivingEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(roundedMaxHealth);
                                    // 延迟设置当前生命值
                                    finalLivingEntity.setHealth(roundedHealth);
                                    if (AttributeManager.isDamageLogEnabled()) {
                                        plugin.getLogger().info("[MythicMobs 集成] 延迟从Bukkit实体设置血量: " + roundedHealth + "/" + roundedMaxHealth);
                                    }
                                }
                            }
                        }, 5L);
                    }
                } else {
                    // 对于其他属性，尝试使用MM的API设置
                    try {
                        // 显示尝试设置的属性信息
                        if (AttributeManager.isDamageLogEnabled()) {
                            plugin.getLogger().info("[MythicMobs 集成] 尝试设置非健康属性: " + attributeKey + " = " + attributeValue);
                        }
                        
                        // 尝试方法1: 通过 AttributeHandler 设置属性
                        if (setAttributeByAttributeHandler(activeMob, attributeKey, attributeValue)) {
                            if (AttributeManager.isDamageLogEnabled()) {
                                plugin.getLogger().info("[MythicMobs 集成] 使用 AttributeHandler 设置属性成功: " + attributeKey);
                            }
                            attributeSet = true;
                        } else {
                            // 尝试方法2: 通过配置设置属性
                            if (setAttributeByConfig(activeMob, attributeKey, attributeValue)) {
                                if (AttributeManager.isDamageLogEnabled()) {
                                    plugin.getLogger().info("[MythicMobs 集成] 使用配置设置属性成功: " + attributeKey);
                                }
                                attributeSet = true;
                            } else {
                                // 尝试方法3: 直接通过 activeMob 设置属性
                                try {
                                    // 尝试使用 setAttribute 方法直接在 activeMob 上
                                    try {
                                        java.lang.reflect.Method setAttributeMethod = activeMob.getClass().getMethod("setAttribute", String.class, double.class);
                                        setAttributeMethod.invoke(activeMob, attributeKey, attributeValue);
                                        if (AttributeManager.isDamageLogEnabled()) {
                                            plugin.getLogger().info("[MythicMobs 集成] 直接在 activeMob 上设置属性成功: " + attributeKey + " = " + attributeValue);
                                        }
                                        attributeSet = true;
                                    } catch (Exception e) {
                                        // 尝试使用其他可能的方法名
                                        try {
                                            java.lang.reflect.Method setAttributeMethod = activeMob.getClass().getMethod("setAttribute", String.class, Object.class);
                                            setAttributeMethod.invoke(activeMob, attributeKey, attributeValue);
                                            if (AttributeManager.isDamageLogEnabled()) {
                                                plugin.getLogger().info("[MythicMobs 集成] 直接在 activeMob 上设置属性成功: " + attributeKey + " = " + attributeValue);
                                            }
                                            attributeSet = true;
                                        } catch (Exception ex) {
                                            if (AttributeManager.isDamageLogEnabled()) {
                                                plugin.getLogger().info("[MythicMobs 集成] 无法直接在 activeMob 上设置属性: " + ex.getMessage());
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    if (AttributeManager.isDamageLogEnabled()) {
                                        plugin.getLogger().info("[MythicMobs 集成] 尝试直接设置属性时发生异常: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (AttributeManager.isDamageLogEnabled()) {
                            plugin.getLogger().info("[MythicMobs 集成] 尝试设置属性时发生异常: " + e.getMessage());
                        }
                    }
                }
                
                if (!attributeSet && AttributeManager.isDamageLogEnabled()) {
                    plugin.getLogger().warning("[MythicMobs 集成] 无法为怪物 " + activeMob.getType().getInternalName() + " 设置属性 " + attributeKey);
                }
            }
            
            // 显示设置属性后的怪物血量
            try {
                // 尝试获取怪物的当前血量和最大血量
                double currentHealth = 0;
                double maxHealth = 0;
                
                if (livingEntity != null) {
                    currentHealth = livingEntity.getHealth();
                    maxHealth = livingEntity.getMaxHealth();
                    if (AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 怪物实体类型: " + entity.getClass().getName());
                        plugin.getLogger().info("[MythicMobs 集成] 真实Bukkit实体类型: " + livingEntity.getClass().getName());
                        plugin.getLogger().info("[MythicMobs 集成] 从LivingEntity获取血量: " + currentHealth + "/" + maxHealth);
                    }
                } else if (entity != null) {
                    if (AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 怪物实体类型: " + entity.getClass().getName());
                    }
                }
                
                if (AttributeManager.isDamageLogEnabled()) {
                    plugin.getLogger().info("[MythicMobs 集成] 怪物 " + activeMob.getType().getInternalName() + " 设置属性后的血量: " + currentHealth + "/" + maxHealth);
                }
            } catch (Exception e) {
                if (AttributeManager.isDamageLogEnabled()) {
                    plugin.getLogger().warning("[MythicMobs 集成] 无法获取怪物血量信息: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 应用属性失败: " + e.getMessage());
            }
        }
    }

    /**
     * 尝试通过 AttributeHandler 设置属性
     */
    private boolean setAttributeByAttributeHandler(ActiveMob activeMob, String attributeKey, double attributeValue) {
        try {
            // 尝试获取 AttributeHandler
            Object attributeHandler = null;
            try {
                // 尝试使用 getAttributeHandler 方法
                java.lang.reflect.Method getAttributeHandlerMethod = activeMob.getClass().getMethod("getAttributeHandler");
                attributeHandler = getAttributeHandlerMethod.invoke(activeMob);
            } catch (Exception e) {
                // 尝试使用其他可能的方法名
                try {
                    java.lang.reflect.Method getAttributesMethod = activeMob.getClass().getMethod("getAttributes");
                    attributeHandler = getAttributesMethod.invoke(activeMob);
                } catch (Exception ex) {
                    // 无法获取 AttributeHandler
                    return false;
                }
            }

            if (attributeHandler != null) {
                // 尝试设置属性
                try {
                    // 尝试使用 setAttribute 方法
                    java.lang.reflect.Method setAttributeMethod = attributeHandler.getClass().getMethod("setAttribute", String.class, double.class);
                    setAttributeMethod.invoke(attributeHandler, attributeKey, attributeValue);
                    return true;
                } catch (Exception e) {
                    // 尝试使用其他可能的方法名
                    try {
                        java.lang.reflect.Method setMethod = attributeHandler.getClass().getMethod("set", String.class, Object.class);
                        setMethod.invoke(attributeHandler, attributeKey, attributeValue);
                        return true;
                    } catch (Exception ex) {
                        // 无法设置属性
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            // 发生异常
            return false;
        }
        return false;
    }

    /**
     * 尝试通过配置设置属性
     */
    private boolean setAttributeByConfig(ActiveMob activeMob, String attributeKey, double attributeValue) {
        try {
            // 尝试获取配置
            Object config = null;
            try {
                // 尝试使用 getConfig 方法
                java.lang.reflect.Method getConfigMethod = activeMob.getClass().getMethod("getConfig");
                config = getConfigMethod.invoke(activeMob);
            } catch (Exception e) {
                // 尝试获取类型的配置
                try {
                    Object mobType = activeMob.getType();
                    java.lang.reflect.Method getConfigMethod = mobType.getClass().getMethod("getConfig");
                    config = getConfigMethod.invoke(mobType);
                } catch (Exception ex) {
                    // 无法获取配置
                    return false;
                }
            }

            if (config != null) {
                // 尝试设置配置属性
                try {
                    java.lang.reflect.Method setMethod = config.getClass().getMethod("set", String.class, Object.class);
                    setMethod.invoke(config, attributeKey, attributeValue);
                    return true;
                } catch (Exception e) {
                    // 无法设置配置属性
                    return false;
                }
            }
        } catch (Exception e) {
            // 发生异常
            return false;
        }
        return false;
    }

    /**
     * 检查 MythicMobs 是否安装并启用
     */
    public static boolean isMythicMobsEnabled() {
        return MythicBukkit.getPlugin(MythicBukkit.class) != null && MythicBukkit.getPlugin(MythicBukkit.class).isEnabled();
    }
}
