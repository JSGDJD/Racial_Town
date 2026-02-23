package org.HUD.hotelRoom.attribute;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * MythicMobs 变量提供者
 * 为 MythicMobs 技能提供 HotelRoom 属性系统的变量
 */
public class MythicMobsVariableProvider {
    
    private final HotelRoom plugin;
    
    public MythicMobsVariableProvider(HotelRoom plugin) {
        this.plugin = plugin;
        registerVariables();
    }
    
    /**
     * 注册变量
     */
    public void registerVariables() {
        try {
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 开始注册变量提供者");
            }
            
            // 使用反射获取 MythicMobs 相关类
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 找到 MythicBukkit 类: " + mythicBukkitClass.getName());
            }
            
            // 获取 getPlugin 方法
            java.lang.reflect.Method getPluginMethod = mythicBukkitClass.getMethod("getPlugin", Class.class);
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 找到 getPlugin 方法");
            }
            
            // 调用 getPlugin 方法
            Object mythicBukkit = getPluginMethod.invoke(null, mythicBukkitClass);
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 获取 MythicBukkit 实例成功: " + (mythicBukkit != null ? mythicBukkit.getClass().getName() : "null"));
            }
            
            if (mythicBukkit == null) {
                if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                    plugin.getLogger().warning("[MythicMobs 集成] MythicBukkit 实例为 null");
                }
                return;
            }
            
            // 获取 getVariableManager 方法
            java.lang.reflect.Method getVariableManagerMethod = mythicBukkit.getClass().getMethod("getVariableManager");
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 找到 getVariableManager 方法");
            }
            
            // 调用 getVariableManager 方法
            Object variableManager = getVariableManagerMethod.invoke(mythicBukkit);
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 获取变量管理器成功: " + (variableManager != null ? variableManager.getClass().getName() : "null"));
            }
            
            if (variableManager == null) {
                if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                    plugin.getLogger().warning("[MythicMobs 集成] 变量管理器为 null");
                }
                return;
            }
            
            // 打印变量管理器的所有方法
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().info("[MythicMobs 集成] 变量管理器所有方法:");
                for (java.lang.reflect.Method method : variableManager.getClass().getMethods()) {
                    plugin.getLogger().info("[MythicMobs 集成] - " + method.getName() + "(" + java.util.Arrays.toString(method.getParameterTypes()) + ")");
                }
            }
            
            // 尝试直接在变量管理器上注册变量提供者
            boolean registered = false;
            for (java.lang.reflect.Method method : variableManager.getClass().getMethods()) {
                if (method.getName().equals("registerProvider") && method.getParameterCount() == 2) {
                    if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 找到 registerProvider 方法: " + method.toString());
                    }
                    
                    // 创建变量提供者实例（使用匿名类）
                    Object provider = createVariableProvider();
                    if (provider == null) {
                        if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                            plugin.getLogger().warning("[MythicMobs 集成] 创建变量提供者失败");
                        }
                        return;
                    }
                    if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 创建变量提供者成功: " + provider.getClass().getName());
                    }
                    
                    // 注册变量提供者
                    if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 准备注册变量提供者: hotelroom_attr");
                    }
                    method.invoke(variableManager, "hotelroom_attr", provider);
                    
                    if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                        plugin.getLogger().info("[MythicMobs 集成] 已成功注册变量提供者: hotelroom_attr");
                    }
                    registered = true;
                    break;
                }
            }
            
            if (!registered && org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 未找到合适的 registerProvider 方法");
            }
        } catch (Exception e) {
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 注册变量提供者失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 创建变量提供者实例
     */
    private Object createVariableProvider() {
        try {
            // 获取 VariableProvider 接口
            Class<?> variableProviderClass = Class.forName("io.lumine.mythic.core.skills.variables.VariableProvider");
            
            // 使用反射创建匿名类实例
            return java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{variableProviderClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("getVariable")) {
                        return getVariable((String) args[0], (Object[]) args[1]);
                    } else if (method.getName().equals("getType")) {
                        return getType();
                    } else if (method.getName().equals("getScope")) {
                        return getScope();
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 创建变量提供者失败: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * 获取变量值
     */
    private Optional<Object> getVariable(String key, Object... context) {
        try {
            // 检查上下文是否包含玩家
            Player player = null;
            for (Object obj : context) {
                if (obj instanceof Player) {
                    player = (Player) obj;
                    break;
                }
            }
            
            if (player == null) {
                return Optional.empty();
            }
            
            // 获取属性管理器
            AttributeManager manager = AttributeManager.getInstance();
            if (manager == null || !manager.isEnabled()) {
                return Optional.empty();
            }
            
            // 获取玩家属性
            PlayerAttribute attr = manager.getPlayerAttribute(player.getUniqueId());
            
            // 处理属性变量
            double value = attr.getAttribute(key);
            return Optional.of(value);
        } catch (Exception e) {
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 获取变量失败: " + e.getMessage());
            }
            return Optional.empty();
        }
    }
    
    /**
     * 获取变量类型
     */
    private Object getType() {
        try {
            // 使用正确的包路径
            String[] possiblePaths = {
                "io.lumine.mythic.core.skills.variables.VariableType"
            };
            
            for (String path : possiblePaths) {
                try {
                    Class<?> variableTypeClass = Class.forName(path);
                    return variableTypeClass.getField("NUMBER").get(null);
                } catch (Exception e) {
                    // 尝试下一个路径
                    continue;
                }
            }
            
            // 如果所有路径都失败，返回null
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 找不到 VariableType 类");
            }
            return null;
        } catch (Exception e) {
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 获取变量类型失败: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * 获取变量作用域
     */
    private Object getScope() {
        try {
            // 使用正确的包路径
            String[] possiblePaths = {
                "io.lumine.mythic.core.skills.variables.VariableScope"
            };
            
            for (String path : possiblePaths) {
                try {
                    Class<?> variableScopeClass = Class.forName(path);
                    return variableScopeClass.getField("GLOBAL").get(null);
                } catch (Exception e) {
                    // 尝试下一个路径
                    continue;
                }
            }
            
            // 如果所有路径都失败，返回null
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 找不到 VariableScope 类");
            }
            return null;
        } catch (Exception e) {
            if (org.HUD.hotelRoom.attribute.AttributeManager.isDamageLogEnabled()) {
                plugin.getLogger().warning("[MythicMobs 集成] 获取变量作用域失败: " + e.getMessage());
            }
            return null;
        }
    }
}
