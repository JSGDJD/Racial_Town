package org.HUD.hotelRoom.attribute.listener;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.attribute.CustomAttributeManager;
import org.HUD.hotelRoom.attribute.FormulaEvaluator;
import org.HUD.hotelRoom.attribute.ItemAttributeParser;
import org.HUD.hotelRoom.attribute.MythicMobsAttributeManager;
import org.HUD.hotelRoom.attribute.PlayerAttribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class CombatAttributeListener implements Listener {
    
    private final Random random = new Random();
    
    private void logDamage(String message) {
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(message);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = event.getDamager() instanceof Player ? (Player) event.getDamager() : null;
        LivingEntity victim = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        
        if (victim == null) return;
        
        AttributeManager manager = AttributeManager.getInstance();
        if (manager == null || !manager.isEnabled()) return;
        
        double finalDamage = event.getDamage();
        boolean isPlayerAttacker = attacker != null;
        
        if (AttributeManager.isDamageLogEnabled()) {
            String attackerName = isPlayerAttacker ? attacker.getName() : victim.getType().name();
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 开始计算伤害: 攻击者: " + attackerName + 
                " 目标: " + victim.getType().name() + 
                " 基础伤害: " + event.getDamage() + 
                " 攻击类型: " + event.getCause().name()
            );
        }
        
        double weaponMagicDamage = 0;
        double totalMagicDamage = 0;
        double totalPhysicalDamage = 0;
        double totalCritRate = 0;
        double totalCritDamage = 0;
        double totalArmorPenetration = 0;
        double totalMagicPenetration = 0;
        double critRate = 0;
        
        if (isPlayerAttacker) {
            PlayerAttribute attackerAttr = manager.getPlayerAttribute(attacker.getUniqueId());
            Map<String, Double> equipmentAttributes = getAttackerEquipmentAttributes(attacker);
            if (!equipmentAttributes.isEmpty() && AttributeManager.isDamageLogEnabled()) {
                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                    "[CombatAttribute] 攻击者装备属性: " + equipmentAttributes);
            }
            
            totalPhysicalDamage = attackerAttr.getAttribute("physical_damage") + equipmentAttributes.getOrDefault("physical_damage", 0.0);
            double playerBaseMagicDamage = attackerAttr.getAttribute("magic_damage");
            double weaponMagicBonus = equipmentAttributes.getOrDefault("magic_damage", 0.0);
            totalMagicDamage = playerBaseMagicDamage;
            totalCritRate = attackerAttr.getAttribute("crit_rate") + equipmentAttributes.getOrDefault("crit_rate", 0.0);
            totalCritDamage = attackerAttr.getAttribute("crit_damage") + equipmentAttributes.getOrDefault("crit_damage", 0.0);
            totalArmorPenetration = attackerAttr.getAttribute("armor_penetration") + equipmentAttributes.getOrDefault("armor_penetration", 0.0);
            totalMagicPenetration = attackerAttr.getAttribute("magic_penetration") + equipmentAttributes.getOrDefault("magic_penetration", 0.0);
            
            if (AttributeManager.isDamageLogEnabled()) {
                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                    "[CombatAttribute] 攻击者属性: 物理伤害: " + totalPhysicalDamage + 
                    " 魔法伤害: " + totalMagicDamage + 
                    " 暴击率: " + totalCritRate + 
                    " 暴击伤害: " + totalCritDamage + 
                    " 护甲穿透: " + totalArmorPenetration + 
                    " 魔法穿透: " + totalMagicPenetration
                );
            }
            
            weaponMagicDamage = weaponMagicBonus;
            boolean forceMagicAttack = weaponMagicDamage > 0;
            
            if (forceMagicAttack || totalMagicDamage > totalPhysicalDamage + 1.0) {
                finalDamage = totalMagicDamage;
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 魔法攻击: " + finalDamage
                    );
                }
            } else {
                if (totalPhysicalDamage > 0) {
                    finalDamage = event.getDamage() + totalPhysicalDamage;
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 物理攻击: " + event.getDamage() + " + " + totalPhysicalDamage + " = " + finalDamage
                        );
                    }
                } else {
                    finalDamage = event.getDamage();
                }
            }
            
            if (victim instanceof Player) {
                double pvpDamage = attackerAttr.getAttribute("pvp_damage") + equipmentAttributes.getOrDefault("pvp_damage", 0.0);
                if (pvpDamage > 0) finalDamage += pvpDamage;
            } else {
                double pveDamage = attackerAttr.getAttribute("pve_damage") + equipmentAttributes.getOrDefault("pve_damage", 0.0);
                if (pveDamage > 0) finalDamage += pveDamage;
            }
            
            critRate = manager.applyCritRateCap(totalCritRate);
            if (totalCritRate > 0 && random.nextDouble() * 100 < totalCritRate) {
                double critMultiplier = totalCritDamage / 100.0;
                finalDamage *= critMultiplier;
                attacker.sendMessage("§e§l✨暴击！§r §6" + String.format("%.1f%%", totalCritDamage) + " §e伤害");
            }
            
            if (victim instanceof Player && totalArmorPenetration > 0) {
                Player victimPlayer = (Player) victim;
                PlayerAttribute victimAttr = manager.getPlayerAttribute(victimPlayer.getUniqueId());
                double victimArmor = victimAttr.getAttribute("armor");
                double effectiveArmor = Math.max(0, victimArmor - totalArmorPenetration);
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 护甲穿透: " + totalArmorPenetration + " 目标护甲: " + victimArmor + " 有效护甲: " + effectiveArmor
                    );
                }
            }
            
            double lifesteal = attackerAttr.getAttribute("lifesteal") + equipmentAttributes.getOrDefault("lifesteal", 0.0);
            if (lifesteal > 0 && finalDamage > 0) {
                double healAmount = finalDamage * (lifesteal / 100.0);
                double currentHealth = attacker.getHealth();
                double maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(currentHealth + healAmount, maxHealth);
                    attacker.setHealth(newHealth);
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 生命偷取: " + attacker.getName() + " 偷取: " + healAmount
                        );
                    }
                }
            }
        } else {
            boolean isMythicMob = false;
            double mobPhysicalDamage = 0;
            double mobMagicDamage = 0;
            double mobCritRate = 0;
            double mobCritDamage = 0;
            double mobArmorPenetration = 0;
            double mobMagicPenetration = 0;
            
            try {
                // 尝试通过MythicMobs API 获取怪物
                try {
                    // 尝试获取 MythicMobs 插件实例
                    Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                    java.lang.reflect.Method getPluginMethod = mythicBukkitClass.getMethod("getPlugin", Class.class);
                    Object mythicBukkit = getPluginMethod.invoke(null, mythicBukkitClass);
                    
                    if (mythicBukkit != null) {
                        // 尝试获取怪物管理器
                        java.lang.reflect.Method getMobManagerMethod = mythicBukkit.getClass().getMethod("getMobManager");
                        Object mobManager = getMobManagerMethod.invoke(mythicBukkit);
                        
                        if (mobManager != null) {
                            // 尝试获取怪物
                            Object activeMob = null;
                            try {
                                // 尝试方法1: 使用 Entity 参数
                                try {
                                    java.lang.reflect.Method getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", org.bukkit.entity.Entity.class);
                                    Object result = getActiveMobMethod.invoke(mobManager, attacker);
                                    if (result != null) {
                                        // 检查是否返回 Optional 对象
                                        if (result.getClass().getName().equals("java.util.Optional")) {
                                            // 调用 Optional.get() 方法
                                            java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                                            activeMob = getMethod.invoke(result);
                                        } else {
                                            activeMob = result;
                                        }
                                    }
                                } catch (Exception e1) {
                                    // 尝试方法2: 使用 UUID 参数
                                    try {
                                        java.lang.reflect.Method getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", java.util.UUID.class);
                                        Object result = getActiveMobMethod.invoke(mobManager, attacker.getUniqueId());
                                        if (result != null && result.getClass().getName().equals("java.util.Optional")) {
                                            java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                                            activeMob = getMethod.invoke(result);
                                        } else {
                                            activeMob = result;
                                        }
                                    } catch (Exception e2) {
                                        // 尝试方法3: 使用 String 参数
                                        try {
                                            java.lang.reflect.Method getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", String.class);
                                            Object result = getActiveMobMethod.invoke(mobManager, attacker.getUniqueId().toString());
                                            if (result != null && result.getClass().getName().equals("java.util.Optional")) {
                                                java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                                                activeMob = getMethod.invoke(result);
                                            } else {
                                                activeMob = result;
                                            }
                                        } catch (Exception e3) {
                                            // 无法获取怪物，继续
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // 无法获取怪物，继续
                            }
                            
                            if (activeMob != null) {
                                isMythicMob = true;
                                
                                // 尝试获取属性
                                try {
                                    // 尝试方法1: 通过 AttributeHandler 获取属性
                                    try {
                                        java.lang.reflect.Method getAttributeHandlerMethod = activeMob.getClass().getMethod("getAttributeHandler");
                                        Object attributeHandler = getAttributeHandlerMethod.invoke(activeMob);
                                        
                                        if (attributeHandler != null) {
                                            // 尝试获取物理伤害
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                                Object damageValue = getAttributeMethod.invoke(attributeHandler, "physical_damage");
                                                if (damageValue != null && damageValue instanceof Number) {
                                                    mobPhysicalDamage = ((Number) damageValue).doubleValue();
                                                }
                                            } catch (Exception e) {
                                                // 尝试其他方法
                                            }
                                            
                                            // 尝试获取魔法伤害
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                                Object damageValue = getAttributeMethod.invoke(attributeHandler, "magic_damage");
                                                if (damageValue != null && damageValue instanceof Number) {
                                                    mobMagicDamage = ((Number) damageValue).doubleValue();
                                                }
                                            } catch (Exception e) {
                                                // 尝试其他方法
                                            }
                                            
                                            // 尝试获取暴击率
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                                Object critValue = getAttributeMethod.invoke(attributeHandler, "crit_rate");
                                                if (critValue != null && critValue instanceof Number) {
                                                    mobCritRate = ((Number) critValue).doubleValue();
                                                }
                                            } catch (Exception e) {
                                                // 尝试其他方法
                                            }
                                            
                                            // 尝试获取暴击伤害
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                                Object critValue = getAttributeMethod.invoke(attributeHandler, "crit_damage");
                                                if (critValue != null && critValue instanceof Number) {
                                                    mobCritDamage = ((Number) critValue).doubleValue();
                                                }
                                            } catch (Exception e) {
                                                // 尝试其他方法
                                            }
                                        }
                                    } catch (Exception e) {
                                        // 尝试方法2: 直接从 activeMob 获取属性
                                        try {
                                            // 尝试获取物理伤害
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                                Object damageValue = getAttributeMethod.invoke(activeMob, "physical_damage");
                                                if (damageValue != null && damageValue instanceof Number) {
                                                    mobPhysicalDamage = ((Number) damageValue).doubleValue();
                                                }
                                            } catch (Exception ex) {
                                                // 尝试其他方法
                                            }
                                            
                                            // 尝试获取魔法伤害
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                                Object damageValue = getAttributeMethod.invoke(activeMob, "magic_damage");
                                                if (damageValue != null && damageValue instanceof Number) {
                                                    mobMagicDamage = ((Number) damageValue).doubleValue();
                                                }
                                            } catch (Exception ex) {
                                                // 尝试其他方法
                                            }
                                            
                                            // 尝试获取暴击率
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                                Object critValue = getAttributeMethod.invoke(activeMob, "crit_rate");
                                                if (critValue != null && critValue instanceof Number) {
                                                    mobCritRate = ((Number) critValue).doubleValue();
                                                }
                                            } catch (Exception ex) {
                                                // 尝试其他方法
                                            }
                                            
                                            // 尝试获取暴击伤害
                                            try {
                                                java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                                Object critValue = getAttributeMethod.invoke(activeMob, "crit_damage");
                                                if (critValue != null && critValue instanceof Number) {
                                                    mobCritDamage = ((Number) critValue).doubleValue();
                                                }
                                            } catch (Exception ex) {
                                                // 尝试其他方法
                                            }
                                        } catch (Exception ex) {
                                            // 无法获取属性，继续
                                        }
                                    }
                                } catch (Exception e) {
                                    // 无法获取属性，继续
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 无法获取 MythicMobs 插件，继续
                }
                
                // 尝试从配置文件读取怪物属性
                try {
                    MythicMobsAttributeManager mmAttrManager = MythicMobsAttributeManager.getInstance();
                    if (mmAttrManager != null && mmAttrManager.isEnabled()) {
                        // 通过实体类型名称获取
                        String mobId = attacker.getType().name();
                        Map<String, Double> configAttributes = mmAttrManager.getMobAttributes(mobId);
                        if (configAttributes != null && !configAttributes.isEmpty()) {
                            // 从配置文件获取属性
                            mobPhysicalDamage = configAttributes.getOrDefault("physical_damage", mobPhysicalDamage);
                            mobMagicDamage = configAttributes.getOrDefault("magic_damage", mobMagicDamage);
                            mobCritRate = configAttributes.getOrDefault("crit_rate", mobCritRate);
                            mobCritDamage = configAttributes.getOrDefault("crit_damage", mobCritDamage);
                            mobArmorPenetration = configAttributes.getOrDefault("armor_penetration", mobArmorPenetration);
                            mobMagicPenetration = configAttributes.getOrDefault("magic_penetration", mobMagicPenetration);
                            isMythicMob = true;
                        }
                    }
                } catch (Exception e) {
                    // 无法从配置文件读取属性，继续
                }
                
                // 应用怪物属性
                if (isMythicMob) {
                    // 记录怪物属性
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 怪物属性: " + attacker.getType().name() + 
                            " 物理伤害: " + mobPhysicalDamage + 
                            " 魔法伤害: " + mobMagicDamage + 
                            " 暴击率: " + mobCritRate + 
                            " 暴击伤害: " + mobCritDamage
                        );
                    }
                    
                    // 应用怪物伤害
                    if (mobPhysicalDamage > 0) {
                        finalDamage += mobPhysicalDamage;
                    }
                    
                    // 应用怪物暴击
                    if (mobCritRate > 0 && random.nextDouble() * 100 < mobCritRate) {
                        // 暴击！
                        double critMultiplier = (mobCritDamage > 0) ? mobCritDamage / 100.0 : 1.5; // 默认150%暴击伤害
                        finalDamage *= critMultiplier;
                        
                        if (AttributeManager.isDamageLogEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                "[CombatAttribute] 怪物暴击！" + attacker.getType().name() + " 暴击伤害: " + String.format("%.1f%%", mobCritDamage)
                            );
                        }
                    }
                }
            } catch (Exception e) {
                // 发生异常，继续处理
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 处理怪物属性时发生异常: " + e.getMessage()
                    );
                }
            }
        }
        
        // 记录攻击者属性应用后的伤害
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 应用攻击者属性后: 伤害: " + finalDamage
            );
        }
        
        // 应用自定义属性
        if (isPlayerAttacker) {
            // 重新获取攻击者属性，因为之前的变量作用域问题
            PlayerAttribute attackerAttr = manager.getPlayerAttribute(attacker.getUniqueId());
            Map<String, Double> equipmentAttributes = getAttackerEquipmentAttributes(attacker);
            
            // 真实伤害
            double trueDamage = attackerAttr.getAttribute("true_damage") + equipmentAttributes.getOrDefault("true_damage", 0.0);
            if (trueDamage > 0) {
                // 真实伤害直接加到最终伤害中，无视防御
                finalDamage += trueDamage;
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 应用真实伤害: " + trueDamage + " 最终伤害: " + finalDamage
                    );
                }
            }
            
            // 吸血
            double vampire = attackerAttr.getAttribute("vampire") + equipmentAttributes.getOrDefault("vampire", 0.0);
            if (vampire > 0 && finalDamage > 0) {
                double healAmount = finalDamage * (vampire / 100.0);
                double currentHealth = attacker.getHealth();
                double maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                
                // 防止超过最大血量
                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(currentHealth + healAmount, maxHealth);
                    attacker.setHealth(newHealth);
                    
                    // 记录吸血效果
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 应用吸血: " + attacker.getName() + " 吸血: " + healAmount + " 血量 " + currentHealth + " -> " + newHealth
                        );
                    }
                }
            }
            
            // 反伤
            // 反伤效果在被攻击者受到伤害时触发，这里暂不处理
            
            // 狂暴
            double berserk = attackerAttr.getAttribute("berserk") + equipmentAttributes.getOrDefault("berserk", 0.0);
            if (berserk > 0) {
                // 狂暴增加伤害
                double berserkMultiplier = berserk / 100.0;
                finalDamage *= (1 + berserkMultiplier);
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 应用狂暴: " + berserk + "% 伤害增加到: " + finalDamage
                    );
                }
            }
            
            // 斩杀
            double execute = attackerAttr.getAttribute("execute") + equipmentAttributes.getOrDefault("execute", 0.0);
            if (execute > 0 && victim instanceof LivingEntity) {
                LivingEntity livingVictim = (LivingEntity) victim;
                double victimHealth = livingVictim.getHealth();
                double victimMaxHealth = livingVictim.getMaxHealth();
                double healthPercent = (victimHealth / victimMaxHealth) * 100;
                
                // 如果目标生命值低于斩杀阈值，增加伤害
                if (healthPercent < execute) {
                    double executeMultiplier = (execute - healthPercent) / 100.0;
                    finalDamage *= (1 + executeMultiplier);
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 应用斩杀: 目标生命值: " + healthPercent + "% 斩杀阈值: " + execute + "% 伤害增加到: " + finalDamage
                        );
                    }
                }
            }
        }
        
        // 记录自定义属性应用后的伤害
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 应用自定义属性后: 伤害: " + finalDamage
            );
        }
        
        // 检查是否使用魔法伤害
        boolean useMagicDamage = weaponMagicDamage > 0 || totalMagicDamage > totalPhysicalDamage;
        
        // ===== 被攻击者防御计算 =====
        if (victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            PlayerAttribute victimAttr = manager.getPlayerAttribute(victimPlayer.getUniqueId());
            
            // 1. 闪避判定（考虑攻击者的命中几率）
            double dodgeRate = victimAttr.getAttribute("dodge_rate");
            dodgeRate = manager.applyDodgeRateCap(dodgeRate); // 应用闪避率上限
            
            // 如果攻击者是玩家，应用命中几率抵消闪避率
            if (isPlayerAttacker) {
                // 重新获取攻击者属性，因为之前的变量作用域问题
                PlayerAttribute attackerAttr = manager.getPlayerAttribute(attacker.getUniqueId());
                Map<String, Double> equipmentAttributes = getAttackerEquipmentAttributes(attacker);
                
                double hitRate = attackerAttr.getAttribute("hit_rate") + equipmentAttributes.getOrDefault("hit_rate", 0.0);
                dodgeRate = Math.max(0, dodgeRate - hitRate); // 命中几率抵消闪避率
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 应用命中几率: " + hitRate + " 目标闪避率: " + victimAttr.getAttribute("dodge_rate") + " 有效闪避率: " + dodgeRate
                    );
                }
            }
            
            if (dodgeRate > 0 && random.nextDouble() * 100 < dodgeRate) {
                // 闪避成功！
                event.setCancelled(true);
                victimPlayer.sendMessage("§f✨闪避成功！");
                if (attacker != null) {
                    attacker.sendMessage("§7对方闪避了你的攻击！");
                }
                return;
            }
            
            // 2. 格挡判定
            double blockRate = victimAttr.getAttribute("block_rate");
            if (blockRate > 0 && random.nextDouble() * 100 < blockRate) {
                // 格挡！减少50%伤害
                finalDamage *= 0.5;
                victimPlayer.sendMessage("§7🛡️格挡！§f伤害减半");
            }
            
            // 3. 应用防御
            // 获取防御者装备属性
            Map<String, Double> victimEquipmentAttributes = getVictimEquipmentAttributes(victimPlayer);
            if (!victimEquipmentAttributes.isEmpty() && AttributeManager.isDamageLogEnabled()) {
                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                    "[CombatAttribute] 防御者装备属性: " + victimEquipmentAttributes
                );
            }
            
            if (useMagicDamage) {
                // 魔法攻击：应用魔法防御
                double baseMagicDefense = victimAttr.getAttribute("magic_defense");
                double equipmentMagicDefense = victimEquipmentAttributes.getOrDefault("magic_defense", 0.0);
                double totalMagicDefense = baseMagicDefense + equipmentMagicDefense;
                
                if (totalMagicDefense > 0) {
                    // 应用魔法穿透
                    double effectiveMagicDefense = Math.max(0, totalMagicDefense - totalMagicPenetration);
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 魔法防御计算: 基础魔法防御=" + baseMagicDefense + " 装备魔法防御=" + equipmentMagicDefense + " 总魔法防御=" + totalMagicDefense + " 魔法穿透=" + totalMagicPenetration + " 有效魔法防御=" + effectiveMagicDefense
                        );
                    }
                    double originalDamage = finalDamage;
                    double damageReduction = manager.calculatePhysicalDamage(finalDamage, effectiveMagicDefense);
                    finalDamage = damageReduction;
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 应用魔法防御: 伤害从 " + originalDamage + " 减少到 " + damageReduction
                        );
                    }
                }
            } else {
                // 物理攻击：应用物理防御和护甲
                double basePhysicalDefense = victimAttr.getAttribute("physical_defense");
                double baseArmor = victimAttr.getAttribute("armor");
                double equipmentPhysicalDefense = victimEquipmentAttributes.getOrDefault("physical_defense", 0.0);
                double equipmentArmor = victimEquipmentAttributes.getOrDefault("armor", 0.0);
                
                double totalPhysicalDefense = basePhysicalDefense + equipmentPhysicalDefense;
                double totalArmor = baseArmor + equipmentArmor;
                // 应用盔甲穿透
                double effectiveArmor = Math.max(0, totalArmor - totalArmorPenetration);
                double totalDefense = totalPhysicalDefense + effectiveArmor;
                
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 物理防御计算: 基础物理防御=" + basePhysicalDefense + " 装备物理防御=" + equipmentPhysicalDefense + " 总物理防御=" + totalPhysicalDefense + " 基础护甲=" + baseArmor + " 装备护甲=" + equipmentArmor + " 总护甲=" + totalArmor + " 盔甲穿透=" + totalArmorPenetration + " 有效护甲=" + effectiveArmor + " 总防御=" + totalDefense
                    );
                }
                
                if (totalDefense > 0) {
                    double originalDamage = finalDamage;
                    double damageReduction = manager.calculatePhysicalDamage(finalDamage, totalDefense);
                    double reducedAmount = originalDamage - damageReduction;
                    finalDamage = damageReduction;
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 应用物理防御: 伤害从 " + originalDamage + " 减少到 " + damageReduction + " 减少量: " + reducedAmount
                        );
                    }
                }
            }
        } else {
            // 处理怪物防御属性
            boolean isMythicMob = false;
            double physicalDefense = 0;
            double magicDefense = 0;
            double armor = 0;
            double dodgeRate = 0;
            double blockRate = 0;
            double damageReduction = 0;
            String mobId = null;
            
            try {
                // 尝试方法1: 直接通过MythicMobs API 获取怪物
                try {
                    // 尝试获取 MythicMobs 插件实例
                    Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                    java.lang.reflect.Method getPluginMethod = mythicBukkitClass.getMethod("getPlugin", Class.class);
                    Object mythicBukkit = getPluginMethod.invoke(null, mythicBukkitClass);
                    
                    if (mythicBukkit != null) {
                        // 尝试获取怪物管理器
                        java.lang.reflect.Method getMobManagerMethod = mythicBukkit.getClass().getMethod("getMobManager");
                        Object mobManager = getMobManagerMethod.invoke(mythicBukkit);
                        
                        if (mobManager != null) {
                            // 调试：查看 mobManager 类型
                            if (AttributeManager.isDamageLogEnabled()) {
                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info("[CombatAttribute] MobManager 类型: " + mobManager.getClass().getName());
                            }
                            
                            // 尝试获取怪物
                                Object activeMob = null;
                                try {
                                    // 尝试方法1: 使用 Entity 参数
                                    try {
                                        java.lang.reflect.Method getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", org.bukkit.entity.Entity.class);
                                        Object result = getActiveMobMethod.invoke(mobManager, victim);
                                        if (result != null) {
                                            if (AttributeManager.isDamageLogEnabled()) {
                                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                    "[CombatAttribute] getActiveMob() 返回类型: " + result.getClass().getName()
                                                );
                                            }
                                            // 检查是否返回 Optional 对象
                                            if (result.getClass().getName().equals("java.util.Optional")) {
                                                // 调用 Optional.get() 方法
                                                java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                                                activeMob = getMethod.invoke(result);
                                                if (AttributeManager.isDamageLogEnabled()) {
                                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                        "[CombatAttribute] 从 Optional 获取到 ActiveMob: " + (activeMob != null ? activeMob.getClass().getName() : "null")
                                                    );
                                                }
                                            } else {
                                                activeMob = result;
                                            }
                                        }
                                    } catch (Exception e1) {
                                        // 尝试方法2: 使用 UUID 参数
                                        try {
                                            java.lang.reflect.Method getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", java.util.UUID.class);
                                            Object result = getActiveMobMethod.invoke(mobManager, victim.getUniqueId());
                                            if (result != null && result.getClass().getName().equals("java.util.Optional")) {
                                                java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                                                activeMob = getMethod.invoke(result);
                                            } else {
                                                activeMob = result;
                                            }
                                        } catch (Exception e2) {
                                            // 尝试方法3: 使用 String 参数
                                            try {
                                                java.lang.reflect.Method getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", String.class);
                                                Object result = getActiveMobMethod.invoke(mobManager, victim.getUniqueId().toString());
                                                if (result != null && result.getClass().getName().equals("java.util.Optional")) {
                                                    java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                                                    activeMob = getMethod.invoke(result);
                                                } else {
                                                    activeMob = result;
                                                }
                                            } catch (Exception e3) {
                                                if (AttributeManager.isDamageLogEnabled()) {
                                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                        "[CombatAttribute] 获取 ActiveMob 失败: Entity=" + e1.getMessage() + 
                                                        ", UUID=" + e2.getMessage() + 
                                                        ", String=" + e3.getMessage()
                                                    );
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    if (AttributeManager.isDamageLogEnabled()) {
                                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                            "[CombatAttribute] 获取 ActiveMob 失败: " + e.getMessage()
                                        );
                                    }
                                }
                                
                                if (activeMob != null) {
                                isMythicMob = true;
                                
                                // 尝试获取属性
                                // 尝试方法1: 通过 AttributeHandler 获取属性
                                try {
                                    java.lang.reflect.Method getAttributeHandlerMethod = activeMob.getClass().getMethod("getAttributeHandler");
                                    Object attributeHandler = getAttributeHandlerMethod.invoke(activeMob);
                                    
                                    if (attributeHandler != null) {
                                        // 尝试获取物理防御
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(attributeHandler, "physical_defense");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                physicalDefense = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception e) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取魔法防御
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(attributeHandler, "magic_defense");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                magicDefense = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception e) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取护甲
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(attributeHandler, "armor");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                armor = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception e) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取闪避率
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(attributeHandler, "dodge_rate");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                dodgeRate = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception e) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取格挡率
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(attributeHandler, "block_rate");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                blockRate = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception e) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取削弱伤害
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = attributeHandler.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(attributeHandler, "damage_reduction");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                damageReduction = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception e) {
                                            // 尝试其他方法
                                        }
                                    }
                                } catch (Exception e) {
                                    // 尝试方法2: 直接从 activeMob 获取属性
                                    try {
                                        // 尝试获取物理防御
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(activeMob, "physical_defense");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                physicalDefense = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception ex) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取魔法防御
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(activeMob, "magic_defense");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                magicDefense = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception ex) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取护甲
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(activeMob, "armor");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                armor = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception ex) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取闪避率
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(activeMob, "dodge_rate");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                dodgeRate = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception ex) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取格挡率
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(activeMob, "block_rate");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                blockRate = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception ex) {
                                            // 尝试其他方法
                                        }
                                        
                                        // 尝试获取削弱伤害
                                        try {
                                            java.lang.reflect.Method getAttributeMethod = activeMob.getClass().getMethod("getAttribute", String.class);
                                            Object defenseValue = getAttributeMethod.invoke(activeMob, "damage_reduction");
                                            if (defenseValue != null && defenseValue instanceof Number) {
                                                damageReduction = ((Number) defenseValue).doubleValue();
                                            }
                                        } catch (Exception ex) {
                                            // 尝试其他方法
                                        }
                                    } catch (Exception ex) {
                                        // 无法获取属性，继续
                                    }
                                }
                                
                                // 尝试获取怪物ID
                                    try {
                                        Object mobType = null;
                                        try {
                                            java.lang.reflect.Method getTypeMethod = activeMob.getClass().getMethod("getType");
                                            Object typeResult = getTypeMethod.invoke(activeMob);
                                            if (AttributeManager.isDamageLogEnabled()) {
                                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                    "[CombatAttribute] getType() 返回类型: " + typeResult.getClass().getName()
                                                );
                                            }
                                            
                                            // 检查是否返回 Optional 对象
                                            if (typeResult != null && typeResult.getClass().getName().equals("java.util.Optional")) {
                                                // 调用 Optional.get() 方法获取实际值
                                                java.lang.reflect.Method getMethod = typeResult.getClass().getMethod("get");
                                                mobType = getMethod.invoke(typeResult);
                                                if (AttributeManager.isDamageLogEnabled()) {
                                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                        "[CombatAttribute] 从 Optional 获取到 mobType: " + (mobType != null ? mobType.getClass().getName() : "null")
                                                    );
                                                }
                                            } else {
                                                // 直接使用返回值
                                                mobType = typeResult;
                                            }
                                        } catch (Exception e) {
                                            if (AttributeManager.isDamageLogEnabled()) {
                                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                    "[CombatAttribute] 获取 mobType 失败: " + e.getMessage()
                                                );
                                            }
                                        }
                                        
                                        if (mobType != null) {
                                            try {
                                                java.lang.reflect.Method getInternalNameMethod = mobType.getClass().getMethod("getInternalName");
                                                mobId = (String) getInternalNameMethod.invoke(mobType);
                                                if (AttributeManager.isDamageLogEnabled()) {
                                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                        "[CombatAttribute] 获取到 mobId: " + mobId
                                                    );
                                                }
                                            } catch (Exception e) {
                                                if (AttributeManager.isDamageLogEnabled()) {
                                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                        "[CombatAttribute] 获取 internalName 失败: " + e.getMessage()
                                                    );
                                                }
                                                // 尝试其他方法获取怪物ID
                                                try {
                                                    // 尝试 getName() 方法
                                                    java.lang.reflect.Method getNameMethod = mobType.getClass().getMethod("getName");
                                                    mobId = (String) getNameMethod.invoke(mobType);
                                                    if (AttributeManager.isDamageLogEnabled()) {
                                                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                            "[CombatAttribute] 使用 getName() 获取到 mobId: " + mobId
                                                        );
                                                    }
                                                } catch (Exception e2) {
                                                    if (AttributeManager.isDamageLogEnabled()) {
                                                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                            "[CombatAttribute] 获取 getName() 失败: " + e2.getMessage()
                                                        );
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        if (AttributeManager.isDamageLogEnabled()) {
                                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                "[CombatAttribute] 获取怪物ID失败: " + e.getMessage()
                                            );
                                        }
                                    }
                            } else {
                                if (AttributeManager.isDamageLogEnabled()) {
                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info("[CombatAttribute] ActiveMob 为null");
                                }
                            }
                        } else {
                            if (AttributeManager.isDamageLogEnabled()) {
                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info("[CombatAttribute] MobManager 为null");
                            }
                        }
                    } else {
                        if (AttributeManager.isDamageLogEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("[CombatAttribute] MythicBukkit 实例为null");
                        }
                    }
                } catch (Exception e) {
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 通过 MythicBukkit 获取属性失败: " + e.getMessage()
                        );
                    }
                }
                
                // 尝试从配置文件读取属性
                try {
                    MythicMobsAttributeManager mmAttrManager = MythicMobsAttributeManager.getInstance();
                    if (mmAttrManager != null && mmAttrManager.isEnabled()) {
                        // 如果之前没有获取到怪物ID，尝试通过其他方法获取
                        if (mobId == null) {
                            // 尝试通过实体类型名称获取
                            mobId = victim.getType().name();
                        }
                        
                        // 如果获取到了怪物ID，从配置文件读取属性
                        if (mobId != null) {
                            Map<String, Double> configAttributes = mmAttrManager.getMobAttributes(mobId);
                            if (configAttributes != null && !configAttributes.isEmpty()) {
                                // 从配置文件获取属性
                                physicalDefense = configAttributes.getOrDefault("physical_defense", 0.0);
                                magicDefense = configAttributes.getOrDefault("magic_defense", 0.0);
                                armor = configAttributes.getOrDefault("armor", 0.0);
                                dodgeRate = configAttributes.getOrDefault("dodge_rate", 0.0);
                                blockRate = configAttributes.getOrDefault("block_rate", 0.0);
                                damageReduction = configAttributes.getOrDefault("damage_reduction", 0.0);
                                isMythicMob = true;
                                
                                // 记录从配置文件读取的属性
                                if (AttributeManager.isDamageLogEnabled()) {
                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                        "[CombatAttribute] 从配置文件读取怪物属性: " + mobId + 
                                        " 物理防御: " + physicalDefense + 
                                        " 魔法防御: " + magicDefense + 
                                        " 护甲: " + armor + 
                                        " 闪避率: " + dodgeRate + 
                                        " 格挡率: " + blockRate + 
                                        " 削弱伤害: " + damageReduction
                                    );
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 无法从配置文件读取属性，继续
                }
                
                // 获取怪物装备属性
                double equipmentArmor = 0;
                Map<String, Double> monsterEquipmentAttributes = new java.util.HashMap<>();
                
                if (victim instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) victim;
                    // 检查实体是否有装备
                    if (livingEntity.getEquipment() != null) {
                        // 获取所有装备槽位的装备
                        org.bukkit.inventory.ItemStack[] armorContents = livingEntity.getEquipment().getArmorContents();
                        if (armorContents != null) {
                            for (int i = 0; i < armorContents.length; i++) {
                                org.bukkit.inventory.ItemStack item = armorContents[i];
                                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                                    // 获取装备的盔甲值
                                    equipmentArmor += getArmorValue(item);
                                    
                                    // 解析装备属性
                                    Map<String, Double> itemAttrs = ItemAttributeParser.parseAttributes(item);
                                    if (!itemAttrs.isEmpty()) {
                                        mergeAttributes(monsterEquipmentAttributes, itemAttrs);
                                        
                                        // 记录装备属性
                                        if (AttributeManager.isDamageLogEnabled()) {
                                            String slotName = getArmorSlotName(i);
                                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                                "[CombatAttribute] 怪物" + slotName + "属性: " + item.getType() + " -> " + itemAttrs
                                            );
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 检查主手物品
                        org.bukkit.inventory.ItemStack mainHand = livingEntity.getEquipment().getItemInMainHand();
                        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
                            Map<String, Double> mainHandAttrs = ItemAttributeParser.parseAttributes(mainHand);
                            if (!mainHandAttrs.isEmpty()) {
                                mergeAttributes(monsterEquipmentAttributes, mainHandAttrs);
                                if (AttributeManager.isDamageLogEnabled()) {
                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                        "[CombatAttribute] 怪物主手属性: " + mainHand.getType() + " -> " + mainHandAttrs
                                    );
                                }
                            }
                        }
                        
                        // 检查副手物品
                        org.bukkit.inventory.ItemStack offHand = livingEntity.getEquipment().getItemInOffHand();
                        if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
                            Map<String, Double> offHandAttrs = ItemAttributeParser.parseAttributes(offHand);
                            if (!offHandAttrs.isEmpty()) {
                                mergeAttributes(monsterEquipmentAttributes, offHandAttrs);
                                if (AttributeManager.isDamageLogEnabled()) {
                                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                        "[CombatAttribute] 怪物副手属性: " + offHand.getType() + " -> " + offHandAttrs
                                    );
                                }
                            }
                        }
                    }
                    
                    // 记录装备盔甲值和属性
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 怪物装备盔甲值: " + equipmentArmor
                        );
                        if (!monsterEquipmentAttributes.isEmpty()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                "[CombatAttribute] 怪物装备总属性: " + monsterEquipmentAttributes
                            );
                        }
                    }
                }
                
                // 如果是MythicMobs 怪物，应用各种属性效果
                if (isMythicMob) {
                    // 将装备属性合并到怪物属性中
                    physicalDefense += monsterEquipmentAttributes.getOrDefault("physical_defense", 0.0);
                    magicDefense += monsterEquipmentAttributes.getOrDefault("magic_defense", 0.0);
                    armor += monsterEquipmentAttributes.getOrDefault("armor", 0.0);
                    dodgeRate += monsterEquipmentAttributes.getOrDefault("dodge_rate", 0.0);
                    blockRate += monsterEquipmentAttributes.getOrDefault("block_rate", 0.0);
                    damageReduction += monsterEquipmentAttributes.getOrDefault("damage_reduction", 0.0);
                    
                    // 记录获取到的属性（包括装备属性）
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 获取到怪物属性: " + victim.getType().name() + 
                            " 物理防御: " + physicalDefense + 
                            " 魔法防御: " + magicDefense + 
                            " 护甲: " + armor + 
                            " 装备盔甲: " + equipmentArmor + 
                            " 闪避率: " + dodgeRate + 
                            " 格挡率: " + blockRate + 
                            " 削弱伤害: " + damageReduction
                        );
                    }
                    
                    // 1. 应用闪避率
                    if (dodgeRate > 0 && random.nextDouble() * 100 < dodgeRate) {
                        // 闪避成功！
                        event.setCancelled(true);
                        if (attacker != null) {
                            attacker.sendMessage("§7目标闪避了你的攻击！");
                        }
                        if (AttributeManager.isDamageLogEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                "[CombatAttribute] 怪物闪避生效: " + victim.getType().name() + " 闪避率: " + dodgeRate + "%"
                            );
                        }
                        return;
                    }
                    
                    // 2. 应用格挡率
                    if (blockRate > 0 && random.nextDouble() * 100 < blockRate) {
                        // 格挡！减少50%伤害
                        finalDamage *= 0.5;
                        if (AttributeManager.isDamageLogEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                "[CombatAttribute] 怪物格挡生效: " + victim.getType().name() + " 格挡率: " + blockRate + "% 伤害减半"
                            );
                        }
                    }
                    
                    // 3. 应用削弱伤害
                    if (damageReduction > 0) {
                        double reducedDamage = finalDamage * (1 - damageReduction / 100);
                        finalDamage = reducedDamage;
                        if (AttributeManager.isDamageLogEnabled()) {
                            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                "[CombatAttribute] 怪物削弱伤害生效: " + victim.getType().name() + " 削弱伤害: " + damageReduction + "% 伤害从" + event.getDamage() + " 减少到" + finalDamage
                            );
                        }
                    }
                    
                    // 4. 应用防御（根据攻击类型）
                    if (!useMagicDamage) {
                        // 物理攻击：应用物理防御和护甲
                        double totalDefense = physicalDefense + armor + equipmentArmor;
                        if (totalDefense > 0) {
                            double originalDamage = finalDamage;
                            double damageReductionValue = manager.calculatePhysicalDamage(finalDamage, totalDefense);
                            finalDamage = damageReductionValue;
                            // 记录防御效果
                            if (AttributeManager.isDamageLogEnabled()) {
                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                    "[CombatAttribute] 怪物物理防御生效: " + victim.getType().name() + " 物理防御: " + physicalDefense + " 护甲: " + armor + " 装备盔甲: " + equipmentArmor + " 总防御: " + totalDefense + " 伤害从" + originalDamage + " 减少到" + finalDamage
                                );
                            }
                        } else {
                            if (AttributeManager.isDamageLogEnabled()) {
                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                    "[CombatAttribute] 怪物物理防御为0: " + victim.getType().name() + " 物理防御: " + physicalDefense + " 护甲: " + armor + " 装备盔甲: " + equipmentArmor
                                );
                            }
                        }
                    } else {
                        // 魔法攻击：应用魔法防御
                        if (magicDefense > 0) {
                            // 应用魔法穿透
                            double effectiveMagicDefense = magicDefense;
                            double mobTotalMagicPenetration = 0;
                            
                            // 计算攻击者的魔法穿透
                            if (isPlayerAttacker && attacker instanceof Player) {
                                // 玩家攻击者使用之前计算的总魔法穿透
                                mobTotalMagicPenetration = totalMagicPenetration;
                            } else if (attacker instanceof Player) {
                                // 重新计算魔法穿透（怪物攻击的情况）
                                Player attackerPlayer = (Player) attacker;
                                PlayerAttribute attackerPlayerAttr = manager.getPlayerAttribute(attackerPlayer.getUniqueId());
                                mobTotalMagicPenetration = attackerPlayerAttr.getAttribute("magic_penetration");
                                // 检查装备的魔法穿透
                                if (attackerPlayer.getEquipment() != null) {
                                    // 主手
                                    org.bukkit.inventory.ItemStack mainHand = attackerPlayer.getEquipment().getItemInMainHand();
                                    if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
                                        Map<String, Double> mainHandAttrs = ItemAttributeParser.parseAttributes(mainHand);
                                        mobTotalMagicPenetration += mainHandAttrs.getOrDefault("magic_penetration", 0.0);
                                    }
                                    // 副手
                                    org.bukkit.inventory.ItemStack offHand = attackerPlayer.getEquipment().getItemInOffHand();
                                    if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
                                        Map<String, Double> offHandAttrs = ItemAttributeParser.parseAttributes(offHand);
                                        mobTotalMagicPenetration += offHandAttrs.getOrDefault("magic_penetration", 0.0);
                                    }
                                    // 盔甲
                                    org.bukkit.inventory.ItemStack[] armorContents = attackerPlayer.getEquipment().getArmorContents();
                                    if (armorContents != null) {
                                        for (org.bukkit.inventory.ItemStack item : armorContents) {
                                            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                                                Map<String, Double> itemAttrs = ItemAttributeParser.parseAttributes(item);
                                                mobTotalMagicPenetration += itemAttrs.getOrDefault("magic_penetration", 0.0);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (mobTotalMagicPenetration > 0) {
                                effectiveMagicDefense = Math.max(0, magicDefense - mobTotalMagicPenetration);
                            }
                            
                            double originalDamage = finalDamage;
                            double damageReductionValue = manager.calculatePhysicalDamage(finalDamage, effectiveMagicDefense);
                            finalDamage = damageReductionValue;
                            // 记录防御效果
                            if (AttributeManager.isDamageLogEnabled()) {
                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                    "[CombatAttribute] 怪物魔法防御生效: " + victim.getType().name() + " 魔法防御: " + magicDefense + " 魔法穿透: " + mobTotalMagicPenetration + " 有效防御: " + effectiveMagicDefense + " 伤害从" + originalDamage + " 减少到" + finalDamage
                                );
                            }
                        } else {
                            // 怪物没有魔法防御
                            if (AttributeManager.isDamageLogEnabled()) {
                                org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                                    "[CombatAttribute] 怪物无魔法防御: " + victim.getType().name() + " 魔法防御: 0，魔法攻击不受减伤"
                                );
                            }
                        }
                    }
                } else {
                    // 记录不是 MythicMobs 怪物的情况
                    if (AttributeManager.isDamageLogEnabled()) {
                        org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                            "[CombatAttribute] 不是 MythicMobs 怪物: " + victim.getType().name() + " 类名: " + victim.getClass().getName()
                        );
                    }
                }
            } catch (Exception e) {
                // 发生异常，继续处理
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                        "[CombatAttribute] 处理怪物防御属性时发生异常: " + e.getMessage()
                    );
                }
            }
        }
        
        // 记录防御者属性应用后的伤害
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 应用防御者属性后: 最终伤害: " + finalDamage
            );
        }
        
        // 应用最终伤害
        // 采用终极方案：完全控制伤害值，使用我们已经计算好的最终伤害
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("=== 开始应用最终伤害 ===");
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("目标玩家: " + (victim instanceof Player ? ((Player) victim).getName() : "未知"));
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("计算后的最终伤害值: " + finalDamage);
            
            // 显示应用前的所有DamageModifier状态
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("应用前的DamageModifier状态:");
            for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
                if (event.isApplicable(modifier)) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info("  " + modifier + ": " + event.getDamage(modifier));
                }
            }
        }
        
        // 清除所有DamageModifier，完全控制伤害计算
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (event.isApplicable(modifier)) {
                if (AttributeManager.isDamageLogEnabled()) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info("清除修饰符: " + modifier + " 原值: " + event.getDamage(modifier));
                }
                event.setDamage(modifier, 0.0);
            }
        }
        
        // 设置最终伤害为我们计算的值（已包含所有防御减伤）
        event.setDamage(finalDamage);
        
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("设置最终伤害为: " + finalDamage);
            
            // 显示应用后的所有DamageModifier状态
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("应用后的DamageModifier状态:");
            for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
                if (event.isApplicable(modifier)) {
                    org.HUD.hotelRoom.HotelRoom.get().getLogger().info("  " + modifier + ": " + event.getDamage(modifier));
                }
            }
            
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info("=== 最终伤害应用完成 ===");
        }
        
        // 记录最终设置的伤害
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 最终设置伤害: " + finalDamage
            );
        }
        
        // 记录玩家实际攻击伤害值
        if (AttributeManager.isDamageLogEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 玩家实际攻击伤害值: " + finalDamage
            );
        }
        
        // 记录玩家受到伤害的信息
        if (AttributeManager.isDamageLogEnabled() && victim instanceof Player) {
            String attackerName = isPlayerAttacker ? attacker.getName() : victim.getType().name();
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[CombatAttribute] 玩家受到伤害: 来源: " + attackerName + 
                " 玩家: " + ((Player) victim).getName() + 
                " 伤害值: " + finalDamage
            );
        }
        
        // ===== 处理自定义属性效果 =====
        processCustomAttributes(event, attacker, victim, finalDamage);
    }
    
    /**
     * 获取攻击者的装备属性（包括武器）
     */
    private Map<String, Double> getAttackerEquipmentAttributes(Player attacker) {
        Map<String, Double> equipmentAttrs = new java.util.HashMap<>();
        
        // 获取主手物品属性
        org.bukkit.inventory.ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> mainHandAttrs = ItemAttributeParser.parseAttributes(mainHand);
            if (!mainHandAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, mainHandAttrs);
            }
        }
        
        // 获取副手物品属性
        org.bukkit.inventory.ItemStack offHand = attacker.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> offHandAttrs = ItemAttributeParser.parseAttributes(offHand);
            if (!offHandAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, offHandAttrs);
            }
        }
        
        // 获取装备属性
        org.bukkit.inventory.PlayerInventory inventory = attacker.getInventory();
        
        // 头盔
        org.bukkit.inventory.ItemStack helmet = inventory.getHelmet();
        if (helmet != null && helmet.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> helmetAttrs = ItemAttributeParser.parseAttributes(helmet);
            if (!helmetAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, helmetAttrs);
            }
        }
        
        // 胸甲
        org.bukkit.inventory.ItemStack chestplate = inventory.getChestplate();
        if (chestplate != null && chestplate.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> chestAttrs = ItemAttributeParser.parseAttributes(chestplate);
            if (!chestAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, chestAttrs);
            }
        }
        
        // 护腿
        org.bukkit.inventory.ItemStack leggings = inventory.getLeggings();
        if (leggings != null && leggings.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> legAttrs = ItemAttributeParser.parseAttributes(leggings);
            if (!legAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, legAttrs);
            }
        }
        
        // 靴子
        org.bukkit.inventory.ItemStack boots = inventory.getBoots();
        if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> bootAttrs = ItemAttributeParser.parseAttributes(boots);
            if (!bootAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, bootAttrs);
            }
        }
        
        return equipmentAttrs;
    }
    
    /**
     * 获取防御者的装备属性
     */
    private Map<String, Double> getVictimEquipmentAttributes(Player victim) {
        Map<String, Double> equipmentAttrs = new java.util.HashMap<>();
        
        // 获取主手物品属性
        org.bukkit.inventory.ItemStack mainHand = victim.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> mainHandAttrs = ItemAttributeParser.parseAttributes(mainHand);
            if (!mainHandAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, mainHandAttrs);
            }
        }
        
        // 获取副手物品属性
        org.bukkit.inventory.ItemStack offHand = victim.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> offHandAttrs = ItemAttributeParser.parseAttributes(offHand);
            if (!offHandAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, offHandAttrs);
            }
        }
        
        // 获取装备属性
        org.bukkit.inventory.PlayerInventory inventory = victim.getInventory();
        
        // 头盔
        org.bukkit.inventory.ItemStack helmet = inventory.getHelmet();
        if (helmet != null && helmet.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> helmetAttrs = ItemAttributeParser.parseAttributes(helmet);
            if (!helmetAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, helmetAttrs);
            }
        }
        
        // 胸甲
        org.bukkit.inventory.ItemStack chestplate = inventory.getChestplate();
        if (chestplate != null && chestplate.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> chestAttrs = ItemAttributeParser.parseAttributes(chestplate);
            if (!chestAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, chestAttrs);
            }
        }
        
        // 护腿
        org.bukkit.inventory.ItemStack leggings = inventory.getLeggings();
        if (leggings != null && leggings.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> legAttrs = ItemAttributeParser.parseAttributes(leggings);
            if (!legAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, legAttrs);
            }
        }
        
        // 靴子
        org.bukkit.inventory.ItemStack boots = inventory.getBoots();
        if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> bootAttrs = ItemAttributeParser.parseAttributes(boots);
            if (!bootAttrs.isEmpty()) {
                mergeAttributes(equipmentAttrs, bootAttrs);
            }
        }
        
        return equipmentAttrs;
    }
    
    /**
     * 合并属性
     */
    private void mergeAttributes(Map<String, Double> target, Map<String, Double> source) {
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }
    
    /**
     * 获取装备槽位名称
     */
    private String getArmorSlotName(int slotIndex) {
        switch (slotIndex) {
            case 0: return "头盔";
            case 1: return "胸甲";
            case 2: return "护腿";
            case 3: return "靴子";
            default: return "装备";
        }
    }
    
    /**
     * 获取装备的盔甲值
     */
    private double getArmorValue(org.bukkit.inventory.ItemStack item) {
        if (item == null) return 0;
        
        // 获取装备的类型
        org.bukkit.Material material = item.getType();
        
        // 根据装备类型返回对应的盔甲值
        switch (material) {
            // 头盔
            case LEATHER_HELMET: return 1;
            case CHAINMAIL_HELMET: return 2;
            case IRON_HELMET: return 2;
            case GOLDEN_HELMET: return 2;
            case DIAMOND_HELMET: return 3;
            case NETHERITE_HELMET: return 3;
            
            // 胸甲
            case LEATHER_CHESTPLATE: return 3;
            case CHAINMAIL_CHESTPLATE: return 4;
            case IRON_CHESTPLATE: return 5;
            case GOLDEN_CHESTPLATE: return 3;
            case DIAMOND_CHESTPLATE: return 6;
            case NETHERITE_CHESTPLATE: return 8;
            
            // 护腿
            case LEATHER_LEGGINGS: return 2;
            case CHAINMAIL_LEGGINGS: return 3;
            case IRON_LEGGINGS: return 4;
            case GOLDEN_LEGGINGS: return 2;
            case DIAMOND_LEGGINGS: return 5;
            case NETHERITE_LEGGINGS: return 6;
            
            // 靴子
            case LEATHER_BOOTS: return 1;
            case CHAINMAIL_BOOTS: return 1;
            case IRON_BOOTS: return 2;
            case GOLDEN_BOOTS: return 1;
            case DIAMOND_BOOTS: return 3;
            case NETHERITE_BOOTS: return 3;
            
            default: return 0;
        }
    }
    
    /**
     * 处理自定义属性效果
     */
    private void processCustomAttributes(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim, double finalDamage) {
        CustomAttributeManager customManager = CustomAttributeManager.getInstance();
        if (customManager == null) return;
        
        AttributeManager attrManager = AttributeManager.getInstance();
        if (attrManager == null) return;
        
        // 处理攻击者的 on_attack 效果
        if (attacker != null) {
            PlayerAttribute attackerAttr = attrManager.getPlayerAttribute(attacker.getUniqueId());
            List<CustomAttributeManager.CustomAttribute> attackEffects = 
                customManager.getAttributesByTrigger("on_attack");
            
            for (CustomAttributeManager.CustomAttribute customAttr : attackEffects) {
                double attrValue = attackerAttr.getAttribute(customAttr.key);
                if (attrValue <= 0) continue;
                
                // 创建变量映射
                double distance = attacker.getLocation().distance(victim.getLocation());
                double targetHealth = victim.getHealth();
                double targetMaxHealth = victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                double targetArmor = 0.0;
                
                if (victim instanceof Player) {
                    PlayerAttribute victimAttr = attrManager.getPlayerAttribute(((Player) victim).getUniqueId());
                    targetArmor = victimAttr.getAttribute("armor");
                }
                
                Map<String, Double> variables = FormulaEvaluator.createVariableMap(
                    finalDamage,
                    attacker.getHealth(),
                    attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),
                    targetHealth,
                    targetMaxHealth,
                    targetArmor,
                    attacker.getLevel(),
                    distance,
                    attackerAttr.getAllAttributes()
                );
                
                // 检查是否触发
                if (!customAttr.shouldTrigger(variables)) {
                    continue;
                }
                
                // 计算效果值
                double effectValue = customAttr.calculateEffect(variables);
                
                // 应用效果
                applyCustomEffect(customAttr, attacker, victim, effectValue);
            }
        }
        
        // 处理防御者的 on_defend 效果
        if (victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            PlayerAttribute victimAttr = attrManager.getPlayerAttribute(victimPlayer.getUniqueId());
            List<CustomAttributeManager.CustomAttribute> defendEffects = 
                customManager.getAttributesByTrigger("on_defend");
            
            for (CustomAttributeManager.CustomAttribute customAttr : defendEffects) {
                double attrValue = victimAttr.getAttribute(customAttr.key);
                if (attrValue <= 0) continue;
                
                // 创建变量映射
                double distance = attacker != null ? attacker.getLocation().distance(victimPlayer.getLocation()) : 0;
                double attackerHealth = attacker != null ? attacker.getHealth() : 0;
                double attackerMaxHealth = attacker != null ? 
                    attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 0;
                
                Map<String, Double> variables = FormulaEvaluator.createVariableMap(
                    finalDamage,
                    victimPlayer.getHealth(),
                    victimPlayer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),
                    attackerHealth,
                    attackerMaxHealth,
                    0.0,
                    victimPlayer.getLevel(),
                    distance,
                    victimAttr.getAllAttributes()
                );
                
                // 检查是否触发
                if (!customAttr.shouldTrigger(variables)) {
                    continue;
                }
                
                // 计算效果值
                double effectValue = customAttr.calculateEffect(variables);
                
                // 应用效果（目标可能是攻击者）
                if (customAttr.target.equals("attacker") && attacker != null) {
                    applyCustomEffect(customAttr, victimPlayer, attacker, effectValue);
                } else {
                    applyCustomEffect(customAttr, victimPlayer, victimPlayer, effectValue);
                }
            }
        }
    }
    
    /**
     * 应用自定义效果
     */
    private void applyCustomEffect(CustomAttributeManager.CustomAttribute customAttr, 
                                   Player source, LivingEntity target, double value) {
        if (value == 0) return;
        
        switch (customAttr.effectType) {
            case "heal":
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    double maxHealth = targetPlayer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    double newHealth = Math.min(targetPlayer.getHealth() + value, maxHealth);
                    targetPlayer.setHealth(newHealth);
                    
                    if (customAttr.showMessage) {
                        String msg = customAttr.message.replace("{value}", String.format("%.1f", value));
                        targetPlayer.sendMessage(msg);
                    }
                }
                break;
                
            case "damage":
                target.damage(value);
                
                if (customAttr.showMessage && target instanceof Player) {
                    String msg = customAttr.message.replace("{value}", String.format("%.1f", value));
                    ((Player) target).sendMessage(msg);
                }
                
                if (customAttr.showMessage && source != null) {
                    String msg = customAttr.message.replace("{value}", String.format("%.1f", value));
                    source.sendMessage(msg);
                }
                break;
                
            case "buff":
            case "debuff":
                // TODO: 实现 buff/debuff 效果
                break;
        }
        
        // 检查是否启用详细日志
        AttributeManager manager = AttributeManager.getInstance();
        if (manager != null && manager.isLoggingEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[自定义属性] " + source.getName() + " 触发: " + 
                customAttr.displayName + " -> " + value + " (" + customAttr.effectType + ")"
            );
        }
    }
    
    /**
     * 记录实际造成的伤害（在所有伤害计算完成后）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageFinal(EntityDamageByEntityEvent event) {
        // 只记录玩家攻击的伤害
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        
        // 检查是否启用伤害信息显示
        AttributeManager manager = AttributeManager.getInstance();
        if (manager != null && !manager.isDamageChatEnabled()) {
            return; // 如果配置中关闭了伤害显示，则不发送消息
        }
        
        // 记录最终实际造成的伤害（包含所有修正）
        double actualDamage = event.getFinalDamage();
        String targetName = victim instanceof Player ? 
            ((Player) victim).getName() : victim.getType().name();
        
        // 发送到攻击者聊天框
        attacker.sendMessage("§7[伤害] §e" + attacker.getName() + " §7对§c" + targetName + " §7造成 §c" + String.format("%.1f", actualDamage) + " §7点伤害");
    }
}
