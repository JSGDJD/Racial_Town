package org.HUD.hotelRoom.race;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.attribute.ItemAttributeParser;
import org.HUD.hotelRoom.attribute.PlayerAttribute;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 种族属性管理器
 * 负责将种族属性应用到新的属性系统中
 */
public class RaceAttributeManager {
    
    private static RaceAttributeManager instance;
    private final HotelRoom plugin;
    private final Map<String, RaceAttributeData> raceAttributeCache = new HashMap<>();
    
    private RaceAttributeManager(HotelRoom plugin) {
        this.plugin = plugin;
        loadAllRaceAttributes();
    }
    
    public static void initialize(HotelRoom plugin) {
        instance = new RaceAttributeManager(plugin);
    }
    
    public static RaceAttributeManager getInstance() {
        return instance;
    }
    
    /**
     * 加载所有种族的属性配置
     */
    private void loadAllRaceAttributes() {
        File racesFolder = new File(plugin.getDataFolder(), "races");
        if (!racesFolder.exists()) {
            plugin.getLogger().warning("种族配置文件夹不存在");
            return;
        }
        
        List<File> raceFiles = new ArrayList<>();
        scanYamlFiles(racesFolder, raceFiles);
        
        plugin.getLogger().info("找到 " + raceFiles.size() + " 个种族配置文件");
        
        for (File file : raceFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String raceName = config.getString("race-name");
                if (raceName != null) {
                    RaceAttributeData data = loadRaceAttributeData(config);
                    raceAttributeCache.put(raceName, data);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("加载种族属性失败: " + file.getName());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 递归扫描文件夹中的所有yml文件
     */
    private void scanYamlFiles(File folder, List<File> result) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanYamlFiles(file, result);
                } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                    result.add(file);
                }
            }
        }
    }
    
    /**
     * 从配置文件加载种族属性数据
     */
    private RaceAttributeData loadRaceAttributeData(YamlConfiguration config) {
        RaceAttributeData data = new RaceAttributeData();
        
        ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
        if (attributesSection == null) {
            plugin.getLogger().warning("种族配置没有 attributes 节：" + config.getString("race-name"));
            return data;
        }
        
        // 加载基础属性
        ConfigurationSection baseSection = attributesSection.getConfigurationSection("base");
        if (baseSection != null) {
            for (String key : baseSection.getKeys(false)) {
                double value = baseSection.getDouble(key);
                data.baseAttributes.put(key, value);
            }
        }
        
        // 加载每级成长
        ConfigurationSection perLevelSection = attributesSection.getConfigurationSection("per-level");
        if (perLevelSection != null) {
            for (String key : perLevelSection.getKeys(false)) {
                double value = perLevelSection.getDouble(key);
                data.perLevelAttributes.put(key, value);
            }
        }
        
        // 加载特殊加成
        ConfigurationSection bonusSection = attributesSection.getConfigurationSection("bonus");
        if (bonusSection != null) {
            for (String key : bonusSection.getKeys(false)) {
                double value = bonusSection.getDouble(key);
                data.bonusAttributes.put(key, value);
            }
        }
        
        return data;
    }
    
    /**
     * 重新加载所有种族属性
     */
    public void reload() {
        raceAttributeCache.clear();
        loadAllRaceAttributes();
        
        // 重新应用所有在线玩家的属性
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyRaceAttributes(player);
        }
    }
    
    /**
     * 应用种族属性到玩家
     */
    public void applyRaceAttributes(Player player) {
        if (player == null) {
            return;
        }
        
        AttributeManager attrManager = AttributeManager.getInstance();
        if (attrManager == null || !attrManager.isEnabled()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        String race = RaceDataStorage.getPlayerRace(uuid);
        int level = RaceDataStorage.getPlayerRaceLevel(uuid);
        
        if (attrManager.isLoggingEnabled()) {
            plugin.getLogger().info("[属性] " + player.getName() + 
                " 种族=" + race + ", 等级=" + level);
        }
        
        if (race == null) {
            plugin.getLogger().warning("玩家 " + player.getName() + " 没有种族数据");
            return;
        }
        
        RaceAttributeData raceData = raceAttributeCache.get(race);
        if (raceData == null) {
            plugin.getLogger().warning("种族 " + race + " 没有属性配置");
            return;
        }
        
        PlayerAttribute playerAttr = attrManager.getPlayerAttribute(uuid);
        if (playerAttr == null) {
            return;
        }
        
        Map<String, Double> defaultAttrs = attrManager.getDefaultAttributes();
        
        Map<String, Double> finalAttributes = new HashMap<>(defaultAttrs);
        
        if (attrManager.isLoggingEnabled()) {
            plugin.getLogger().info("[属性计算] " + player.getName() + " 基础属性: health=" + 
                defaultAttrs.getOrDefault("health", 0.0) + " (这是替代原版的基础值，不是叠加)");
        }
        
        for (Map.Entry<String, Double> entry : raceData.baseAttributes.entrySet()) {
            String attrKey = entry.getKey();
            double baseValue = entry.getValue();
            double perLevelValue = raceData.perLevelAttributes.getOrDefault(attrKey, 0.0);
            
            double raceValue = baseValue + (level > 0 ? (level - 1) * perLevelValue : 0);
            
            if (attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[属性计算] " + player.getName() + " 种族 " + race + " " + attrKey + ": base=" + 
                    baseValue + " + (lv" + level + "-1)*" + perLevelValue + " = " + raceValue);
            }
            
            String bonusKey = attrKey + "_percent";
            if (raceData.bonusAttributes.containsKey(bonusKey)) {
                double bonusPercent = raceData.bonusAttributes.get(bonusKey);
                double beforeBonus = raceValue;
                raceValue *= (1.0 + bonusPercent / 100.0);
                if (attrManager.isLoggingEnabled()) {
                    plugin.getLogger().info("[属性计算] " + player.getName() + " 百分比加成: " + 
                        beforeBonus + " * (1 + " + bonusPercent + "%) = " + raceValue);
                }
            }
            
            double beforeMerge = finalAttributes.getOrDefault(attrKey, 0.0);
            finalAttributes.merge(attrKey, raceValue, Double::sum);
            if (attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[属性计算] " + player.getName() + " 种族加成后 " + attrKey + ": " + 
                    beforeMerge + " + " + raceValue + " = " + finalAttributes.get(attrKey));
            }
        }
        
        for (Map.Entry<String, Double> entry : raceData.bonusAttributes.entrySet()) {
            String bonusKey = entry.getKey();
            if (!bonusKey.endsWith("_percent")) {
                finalAttributes.merge(bonusKey, entry.getValue(), Double::sum);
                if (attrManager.isLoggingEnabled()) {
                    plugin.getLogger().info("[属性计算] " + player.getName() + " 特殊加成: " + 
                        bonusKey + " +" + entry.getValue());
                }
            }
        }
        
        // 获取装备属性
        Map<String, Double> equipmentAttrs = parseEquipmentAttributes(player);
        if (!equipmentAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
            plugin.getLogger().info("[属性计算] " + player.getName() + " 当前装备属性: " + equipmentAttrs);
        }
        
        // 将装备属性加到最终属性中
        for (Map.Entry<String, Double> entry : equipmentAttrs.entrySet()) {
            double beforeValue = finalAttributes.getOrDefault(entry.getKey(), 0.0);
            finalAttributes.merge(entry.getKey(), entry.getValue(), Double::sum);
            if (attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[属性计算] " + player.getName() + " 装备加成 " + entry.getKey() + ": " + 
                    beforeValue + " + " + entry.getValue() + " = " + finalAttributes.get(entry.getKey()));
            }
        }
        
        playerAttr.setAllAttributes(finalAttributes);
        
        if (player.isOnline()) {
            attrManager.refreshPlayerAttributes(player);
        }
        
        if (attrManager.isLoggingEnabled()) {
            plugin.getLogger().info("[属性完成] " + player.getName() + 
                " health=" + finalAttributes.getOrDefault("health", 0.0) + 
                ", physical_damage=" + finalAttributes.getOrDefault("physical_damage", 0.0));
        }
    }
    
    /**
     * 获取玩家的完整属性（包含装备属性）
     * 这是战斗时应该调用的方法
     */
    public Map<String, Double> getPlayerAttributesWithEquipment(Player player) {
        AttributeManager attrManager = AttributeManager.getInstance();
        if (attrManager == null) {
            return new HashMap<>();
        }
        
        // 获取基础属性（种族+手动设置）
        PlayerAttribute playerAttr = attrManager.getPlayerAttribute(player.getUniqueId());
        Map<String, Double> baseAttributes = playerAttr.getAllAttributes();
        
        // 获取装备属性
        Map<String, Double> equipmentAttributes = parseEquipmentAttributes(player);
        
        // 合并属性：基础属性 + 装备属性
        Map<String, Double> finalAttributes = new HashMap<>(baseAttributes);
        for (Map.Entry<String, Double> entry : equipmentAttributes.entrySet()) {
            finalAttributes.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        
        if (attrManager.isLoggingEnabled()) {
            plugin.getLogger().info("[战斗属性] " + player.getName() + " 基础属性: " + baseAttributes);
            plugin.getLogger().info("[战斗属性] " + player.getName() + " 装备属性: " + equipmentAttributes);
            plugin.getLogger().info("[战斗属性] " + player.getName() + " 最终属性: " + finalAttributes);
        }
        
        return finalAttributes;
    }
    
    /**
     * 解析玩家所有装备的属性
     */
    private Map<String, Double> parseEquipmentAttributes(Player player) {
        AttributeManager attrManager = AttributeManager.getInstance();
        Map<String, Double> attributes = new HashMap<>();
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        
        // 主手物品 - 排除头盔、胸甲、护腿、靴子类型的物品
        org.bukkit.inventory.ItemStack mainHand = inv.getItemInMainHand();
        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
            if (!isArmorItem(mainHand)) {
                Map<String, Double> mainHandAttrs = ItemAttributeParser.parseAttributes(mainHand);
                if (!mainHandAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
                    plugin.getLogger().info("[装备属性] " + player.getName() + " 主手: " + mainHand.getType() + " -> " + mainHandAttrs);
                }
                mergeAttributes(attributes, mainHandAttrs);
            }
        }
        
        // 副手物品 - 排除头盔、胸甲、护腿、靴子类型的物品
        org.bukkit.inventory.ItemStack offHand = inv.getItemInOffHand();
        if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
            if (!isArmorItem(offHand)) {
                Map<String, Double> offHandAttrs = ItemAttributeParser.parseAttributes(offHand);
                if (!offHandAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
                    plugin.getLogger().info("[装备属性] " + player.getName() + " 副手: " + offHand.getType() + " -> " + offHandAttrs);
                }
                mergeAttributes(attributes, offHandAttrs);
            }
        }
        
        // 头盔
        org.bukkit.inventory.ItemStack helmet = inv.getHelmet();
        if (helmet != null && helmet.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> helmetAttrs = ItemAttributeParser.parseAttributes(helmet);
            if (!helmetAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[装备属性] " + player.getName() + " 头盔: " + helmet.getType() + " -> " + helmetAttrs);
            }
            mergeAttributes(attributes, helmetAttrs);
        }
        
        // 胸甲
        org.bukkit.inventory.ItemStack chestplate = inv.getChestplate();
        if (chestplate != null && chestplate.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> chestAttrs = ItemAttributeParser.parseAttributes(chestplate);
            if (!chestAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[装备属性] " + player.getName() + " 胸甲: " + chestplate.getType() + " -> " + chestAttrs);
            }
            mergeAttributes(attributes, chestAttrs);
        }
        
        // 护腿
        org.bukkit.inventory.ItemStack leggings = inv.getLeggings();
        if (leggings != null && leggings.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> legAttrs = ItemAttributeParser.parseAttributes(leggings);
            if (!legAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[装备属性] " + player.getName() + " 护腿: " + leggings.getType() + " -> " + legAttrs);
            }
            mergeAttributes(attributes, legAttrs);
        }
        
        // 靴子
        org.bukkit.inventory.ItemStack boots = inv.getBoots();
        if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
            Map<String, Double> bootAttrs = ItemAttributeParser.parseAttributes(boots);
            if (!bootAttrs.isEmpty() && attrManager.isLoggingEnabled()) {
                plugin.getLogger().info("[装备属性] " + player.getName() + " 靴子: " + boots.getType() + " -> " + bootAttrs);
            }
            mergeAttributes(attributes, bootAttrs);
        }
        
        if (!attributes.isEmpty() && attrManager.isLoggingEnabled()) {
            plugin.getLogger().info("[装备属性] " + player.getName() + " 总装备属性: " + attributes);
        }
        
        return attributes;
    }
    
    /**
     * 判断物品是否为头盔、胸甲、护腿或靴子
     */
    private boolean isArmorItem(org.bukkit.inventory.ItemStack item) {
        if (item == null) {
            return false;
        }
        
        org.bukkit.Material type = item.getType();
        String typeName = type.name().toLowerCase();
        
        return typeName.endsWith("_helmet") || 
               typeName.endsWith("_chestplate") || 
               typeName.endsWith("_leggings") || 
               typeName.endsWith("_boots");
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
     * 种族属性数据类
     */
    private static class RaceAttributeData {
        Map<String, Double> baseAttributes = new HashMap<>();
        Map<String, Double> perLevelAttributes = new HashMap<>();
        Map<String, Double> bonusAttributes = new HashMap<>();
    }
}
