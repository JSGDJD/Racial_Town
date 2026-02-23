package org.HUD.hotelRoom.attribute;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MythicMobs 属性管理器
 * 负责将自定义属性应用到 MythicMobs 怪物上
 */
public class MythicMobsAttributeManager {

    private static MythicMobsAttributeManager instance;
    private final HotelRoom plugin;
    private final Map<String, Map<String, Double>> mobAttributes = new ConcurrentHashMap<>();
    private boolean mythicMobsEnabled;

    private MythicMobsAttributeManager(HotelRoom plugin) {
        this.plugin = plugin;
        checkMythicMobs();
        loadConfiguration();
    }

    public static void initialize(HotelRoom plugin) {
        instance = new MythicMobsAttributeManager(plugin);
    }

    public static MythicMobsAttributeManager getInstance() {
        return instance;
    }

    /**
     * 检查 MythicMobs 是否安装并启用
     */
    private void checkMythicMobs() {
        try {
            Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            mythicMobsEnabled = true;
            plugin.getLogger().info("MythicMobs 已检测到，将启用 MythicMobs 自定义属性支持");
        } catch (ClassNotFoundException e) {
            mythicMobsEnabled = false;
            plugin.getLogger().info("MythicMobs 未安装，将禁用 MythicMobs 自定义属性支持");
        }
    }

    /**
     * 加载配置文件
     */
    public void loadConfiguration() {
        mobAttributes.clear();

        // 创建 attributes 文件夹
        File attributesFolder = new File(plugin.getDataFolder(), "attributes");
        if (!attributesFolder.exists()) {
            attributesFolder.mkdirs();
        }

        // 加载 MythicMobs 怪物属性配置
        File configFile = new File(attributesFolder, "mythicmobs_attributes.yml");
        if (!configFile.exists()) {
            plugin.saveResource("attributes/mythicmobs_attributes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");

        if (mobsSection != null) {
            for (String mobId : mobsSection.getKeys(false)) {
                ConfigurationSection attributesSection = mobsSection.getConfigurationSection(mobId);
                if (attributesSection != null) {
                    Map<String, Double> attributes = new HashMap<>();
                    for (String attrKey : attributesSection.getKeys(false)) {
                        attributes.put(attrKey, attributesSection.getDouble(attrKey));
                    }
                    mobAttributes.put(mobId, attributes);
                    plugin.getLogger().info("[MythicMobs 属性] 已加载怪物: " + mobId + " 的自定义属性");
                }
            }
        }

        plugin.getLogger().info("[MythicMobs 属性] 共加载 " + mobAttributes.size() + " 个怪物的自定义属性");
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        checkMythicMobs();
        loadConfiguration();
    }

    /**
     * 检查系统是否启用
     */
    public boolean isEnabled() {
        return mythicMobsEnabled;
    }

    /**
     * 获取指定怪物的自定义属性
     */
    public Map<String, Double> getMobAttributes(String mobId) {
        return mobAttributes.getOrDefault(mobId, Collections.emptyMap());
    }
}
