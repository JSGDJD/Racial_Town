package org.HUD.hotelRoom.gui;

import org.HUD.hotelRoom.race.RaceDataStorage;
import org.HUD.hotelRoom.race.RaceEvolutionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

/**
 * 种族进化GUI界面
 * 显示当前种族和可选的进化路线
 */
public class RaceEvolutionGUI implements Listener {
    
    private static final String TITLE = "种族进化系统";
    private static final int SIZE = 54; // 6行9列
    
    // 从配置文件加载的槽位布局
    private static int[] MAIN_RACE_SLOTS = {22}; // 主种族显示槽位
    private static int[] EVOLUTION_PATH_SLOTS = {13, 21, 23, 31}; // 进化路径槽位
    private static int[] PAGING_SLOTS = {48, 50}; // 翻页按钮槽位
    private static int[] CLOSE_BUTTON_SLOTS = {53}; // 关闭按钮槽位
    
    // 从配置文件加载的物品配置
    private static Map<String, Object> MAIN_RACE_ITEM_CONFIG = new HashMap<>();
    private static Map<String, Object> EVOLUTION_PATH_ITEM_CONFIG = new HashMap<>();
    

    
    private static boolean configLoaded = false;
    
    /**
     * 从配置文件加载GUI布局和物品配置（只加载一次）
     */
    public static void loadConfig() {
        if (configLoaded) {
            return; // 避免重复加载
        }
        
        // 获取race.yml配置文件
        File dataFolder = new File(org.bukkit.Bukkit.getWorldContainer(), "plugins/HotelRoom");
        File configFile = new File(dataFolder, "race.yml");
        if (!configFile.exists()) {
            MAIN_RACE_SLOTS = new int[]{22};
            EVOLUTION_PATH_SLOTS = new int[]{13, 21, 23, 31};
            PAGING_SLOTS = new int[]{48, 50};
            CLOSE_BUTTON_SLOTS = new int[]{53};
            configLoaded = true;
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        if (config.contains("gui.main-race-slots")) {
            List<Integer> slots = config.getIntegerList("gui.main-race-slots");
            MAIN_RACE_SLOTS = slots.stream().mapToInt(Integer::intValue).toArray();
        } else {
            MAIN_RACE_SLOTS = new int[]{22};
        }
        
        if (config.contains("gui.evolution-path-slots")) {
            List<Integer> slots = config.getIntegerList("gui.evolution-path-slots");
            EVOLUTION_PATH_SLOTS = slots.stream().mapToInt(Integer::intValue).toArray();
        } else {
            EVOLUTION_PATH_SLOTS = new int[]{13, 21, 23, 31};
        }
        
        if (config.contains("gui.paging-slots")) {
            List<Integer> slots = config.getIntegerList("gui.paging-slots");
            PAGING_SLOTS = slots.stream().mapToInt(Integer::intValue).toArray();
        } else {
            PAGING_SLOTS = new int[]{48, 50};
        }
        
        if (config.contains("gui.close-button-slots")) {
            List<Integer> slots = config.getIntegerList("gui.close-button-slots");
            CLOSE_BUTTON_SLOTS = slots.stream().mapToInt(Integer::intValue).toArray();
        } else {
            CLOSE_BUTTON_SLOTS = new int[]{53};
        }
        
        if (config.contains("gui.main-race-item")) {
            MAIN_RACE_ITEM_CONFIG = config.getConfigurationSection("gui.main-race-item").getValues(false);
        }
        
        if (config.contains("gui.evolution-path-item")) {
            EVOLUTION_PATH_ITEM_CONFIG = config.getConfigurationSection("gui.evolution-path-item").getValues(false);
        }
        
        configLoaded = true;
    }
    
    /**
     * 强制重新加载配置（用于重载命令）
     */
    public static void reloadConfig() {
        configLoaded = false;
        if (MAIN_RACE_ITEM_CONFIG != null) {
            MAIN_RACE_ITEM_CONFIG.clear();
        } else {
            MAIN_RACE_ITEM_CONFIG = new HashMap<>();
        }
        if (EVOLUTION_PATH_ITEM_CONFIG != null) {
            EVOLUTION_PATH_ITEM_CONFIG.clear();
        } else {
            EVOLUTION_PATH_ITEM_CONFIG = new HashMap<>();
        }
        loadConfig();
    }
    
    /**
     * 打开种族进化GUI
     */
    public static void open(Player player) {
        loadConfig();
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        refresh(inv, player);
        player.openInventory(inv);
    }
    
    /**
     * 刷新界面内容
     */
    private static void refresh(Inventory inv, Player player) {
        inv.clear();
        
        UUID uuid = player.getUniqueId();
        String currentRace = RaceDataStorage.getPlayerRace(uuid);
        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        
        RaceDataStorage.RaceConfig currentConfig = RaceDataStorage.getRaceConfig(currentRace);
        String currentDisplayName = currentConfig != null ? currentConfig.displayName : currentRace;
        
        // 当前种族信息展示（放在主槽位）
        ItemStack currentRaceItem = createMainRaceItem(player, currentRace, currentDisplayName, currentLevel, currentConfig);
        for (int slot : MAIN_RACE_SLOTS) {
            if (slot >= 0 && slot < SIZE) {  // 添加边界检查
                inv.setItem(slot, currentRaceItem);
            }
        }
        
        // 检查是否可以进化
        RaceEvolutionManager evolutionManager = RaceEvolutionManager.getInstance();
        
        // 获取当前种族的可进化目标
        List<String> availableTargets = evolutionManager.getAvailableEvolutionTargets(player);
        
        // 对于所有种族，确保显示所有可进化的种族预览（无论等级是否满足）
        RaceDataStorage.RaceConfig currentConfigForTargets = RaceDataStorage.getRaceConfig(currentRace);
        if (currentConfigForTargets != null && !currentConfigForTargets.evolutionTargets.isEmpty()) {
            for (String target : currentConfigForTargets.evolutionTargets) {
                if (target != null && !availableTargets.contains(target)) {
                    availableTargets.add(target);
                }
            }
        }
        
        boolean canEvolve = !availableTargets.isEmpty();
        int levelsUntilEvolution = evolutionManager.getLevelsUntilEvolution(player);
        
        // 只显示当前种族的可进化目标
        List<String> allTargets = new ArrayList<>(availableTargets);
        
        // 显示所有可进化目标，不使用翻页
        int itemsPlaced = 0;
        
        for (int i = 0; i < Math.min(allTargets.size(), EVOLUTION_PATH_SLOTS.length); i++) {
            String targetRace = allTargets.get(i);
            RaceDataStorage.RaceConfig targetConfig = RaceDataStorage.getRaceConfig(targetRace);
            
            if (targetConfig != null && itemsPlaced < EVOLUTION_PATH_SLOTS.length) {
                int targetSlot = EVOLUTION_PATH_SLOTS[itemsPlaced];
                
                // 获取当前种族进化到目标种族所需的等级
                RaceDataStorage.RaceConfig currentRaceConfig = RaceDataStorage.getRaceConfig(currentRace);
                int requiredLevel = currentRaceConfig != null ? currentRaceConfig.evolutionLevel : -1;
                
                // 确保目标种族配置不为null
                if (targetConfig == null) {
                    continue; // 跳过无效的种族配置
                }
                
                // 放置可进化的种族
                ItemStack targetItem = createEvolutionPathItem(player, targetRace, targetConfig, requiredLevel);
                if (targetSlot >= 0 && targetSlot < SIZE) {  // 添加边界检查
                    inv.setItem(targetSlot, targetItem);
                }
                
                itemsPlaced++;
            }
        }
        
        // 添加关闭按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "关闭");
        backButton.setItemMeta(backMeta);
        if (CLOSE_BUTTON_SLOTS.length > 0 && CLOSE_BUTTON_SLOTS[0] >= 0 && CLOSE_BUTTON_SLOTS[0] < SIZE) {
            inv.setItem(CLOSE_BUTTON_SLOTS[0], backButton);
        } else if (SIZE > 53) {
            inv.setItem(53, backButton); // 默认位置
        } else {
            // 如果GUI大小小于54格，将关闭按钮放在最后一个槽位
            int lastSlot = SIZE - 1;
            if (lastSlot >= 0 && lastSlot < SIZE) {
                inv.setItem(lastSlot, backButton);
            }
        }
    }
    
    /**
     * 创建主种族显示物品
     */
    private static ItemStack createMainRaceItem(Player player, String raceName, String displayName, int level, RaceDataStorage.RaceConfig config) {
        // 从配置中获取物品类型
        String materialName = (String) MAIN_RACE_ITEM_CONFIG.getOrDefault("material", "PAPER");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.PAPER; // 默认材料
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // 从配置中获取模型数据
        int modelData = 0;
        if (MAIN_RACE_ITEM_CONFIG.containsKey("model-data")) {
            Object modelDataObj = MAIN_RACE_ITEM_CONFIG.get("model-data");
            if (modelDataObj instanceof Number) {
                modelData = ((Number) modelDataObj).intValue();
            }
        }
        
        // 设置模型数据
        if (meta != null) {
            try {
                meta.setCustomModelData(modelData);
            } catch (Exception e) {
                // 忽略不支持模型数据的物品类型
            }
        }
        
        // 从配置中获取名称并替换PAPI变量
        String name = (String) MAIN_RACE_ITEM_CONFIG.getOrDefault("name", ChatColor.AQUA + "[当前] " + displayName + ChatColor.GRAY + " (Lv." + level + ")");
        name = replacePapiVariables(player, name, displayName, level);
        meta.setDisplayName(name);
        
        // 从配置中获取lore描述
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) MAIN_RACE_ITEM_CONFIG.getOrDefault("lore", Arrays.asList(
            "",
            ChatColor.GRAY + "种族: " + ChatColor.WHITE + raceName,
            ChatColor.GRAY + "等级: " + ChatColor.WHITE + level,
            ""
        ));
        
        // 如果配置中没有自定义lore，使用默认lore
        if (lore.isEmpty() || lore.size() <= 1) {
            lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "种族: " + ChatColor.WHITE + raceName);
            lore.add(ChatColor.GRAY + "等级: " + ChatColor.WHITE + level);
            
            if (config != null) {
                // 添加种族描述
                if (config.description != null) {
                    lore.add("");
                    lore.add(ChatColor.GRAY + config.description);
                }
            }
            
            lore.add("");
        } else {
            // 替换lore中的PAPI变量
            List<String> processedLore = new ArrayList<>();
            for (String loreLine : lore) {
                processedLore.add(replacePapiVariables(player, loreLine, displayName, level));
            }
            lore = processedLore;
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * 创建进化路径物品
     */
    private static ItemStack createEvolutionPathItem(Player player, String raceName, RaceDataStorage.RaceConfig config, int requiredLevel) {
        // 从配置中获取物品类型
        String materialName = (String) EVOLUTION_PATH_ITEM_CONFIG.getOrDefault("material", "DIAMOND");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.DIAMOND; // 默认材料
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // 从配置中获取模型数据
        int modelData = 0;
        if (EVOLUTION_PATH_ITEM_CONFIG.containsKey("model-data")) {
            Object modelDataObj = EVOLUTION_PATH_ITEM_CONFIG.get("model-data");
            if (modelDataObj instanceof Number) {
                modelData = ((Number) modelDataObj).intValue();
            }
        }
        
        // 设置模型数据
        if (meta != null) {
            try {
                meta.setCustomModelData(modelData);
            } catch (Exception e) {
                // 忽略不支持模型数据的物品类型
            }
        }
        
        // 从配置中获取名称并替换PAPI变量
        String name = (String) EVOLUTION_PATH_ITEM_CONFIG.getOrDefault("name", ChatColor.YELLOW + "[进化] " + config.displayName + ChatColor.GRAY + " (Lv." + requiredLevel + ")");
        name = replacePapiVariables(player, name, config.displayName, requiredLevel);
        meta.setDisplayName(name);
        
        // 从配置中获取lore描述
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) EVOLUTION_PATH_ITEM_CONFIG.getOrDefault("lore", null);
        
        // 存储原始lore配置状态
        boolean isCustomLore = lore != null && !lore.isEmpty();
        
        // 如果没有自定义lore，使用默认lore
        if (!isCustomLore) {
            lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "种族: " + ChatColor.WHITE + raceName);
            lore.add(ChatColor.GRAY + "等级: " + ChatColor.WHITE + requiredLevel);
            
            if (config != null) {
                // 添加种族描述
                if (config.description != null) {
                    lore.add("");
                    lore.add(ChatColor.GRAY + config.description);
                }
                
                // 显示进化条件
                lore.add("");
                lore.add(ChatColor.GOLD + "进化条件:");
                lore.add(ChatColor.YELLOW + "• 进化到 " + config.displayName);
                lore.add(ChatColor.YELLOW + "• 需要等级: " + requiredLevel);
            }
            
            lore.add("");
        } else {
            // 替换lore中的PAPI变量
            List<String> processedLore = new ArrayList<>();
            for (String loreLine : lore) {
                processedLore.add(replacePapiVariables(player, loreLine, config.displayName, requiredLevel));
            }
            lore = processedLore;
        }
        
        // 使用PersistentDataContainer存储种族ID，不显示在lore中
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            // 创建NamespacedKey
            NamespacedKey raceIdKey = new NamespacedKey(org.HUD.hotelRoom.HotelRoom.get(), "race_id");
            pdc.set(raceIdKey, org.bukkit.persistence.PersistentDataType.STRING, raceName);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * 替换PAPI变量并处理颜色代码
     */
    private static String replacePapiVariables(Player player, String text, String displayName, int level) {
        if (text == null) return "";
        
        String result = text;
        
        // 替换占位符
        result = result.replace("%display_name%", displayName != null ? displayName : "未知种族");
        result = result.replace("%level%", String.valueOf(level));
        
        if (player != null) {
            UUID uuid = player.getUniqueId();
            result = result.replace("%race%", RaceDataStorage.getPlayerRace(uuid) != null ? RaceDataStorage.getPlayerRace(uuid) : "unknown");
            result = result.replace("%current_level%", String.valueOf(RaceDataStorage.getPlayerRaceLevel(uuid)));
        }
        
        // 如果安装了PlaceholderAPI，使用它来处理其他变量
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            // 使用PAPI处理其他变量
            result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
        } catch (ClassNotFoundException e) {
            // 如果没有安装PlaceholderAPI，跳过
        }
        
        // 处理颜色代码
        result = ChatColor.translateAlternateColorCodes('&', result);
        
        return result;
    }
    
    /**
     * 事件处理
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        
        e.setCancelled(true); // 阻止拿取物品
        
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        UUID uuid = player.getUniqueId();
        
        // 获取点击的槽位
        int clickedSlot = e.getRawSlot();
        
        // 检查是否是进化目标种族槽位
        boolean isEvolutionSlot = false;
        for (int slot : EVOLUTION_PATH_SLOTS) {
            if (slot == clickedSlot) {
                isEvolutionSlot = true;
                break;
            }
        }
        
        if (isEvolutionSlot) {
            // 从PersistentDataContainer中提取种族ID
            String raceName = null;
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                // 创建NamespacedKey
                NamespacedKey raceIdKey = new NamespacedKey(org.HUD.hotelRoom.HotelRoom.get(), "race_id");
                if (pdc.has(raceIdKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    raceName = pdc.get(raceIdKey, org.bukkit.persistence.PersistentDataType.STRING);
                }
            }
            
            if (raceName != null) {
                // 执行进化
                RaceEvolutionManager evolutionManager = RaceEvolutionManager.getInstance();
                boolean success = evolutionManager.evolve(player, raceName);
                
                if (success) {
                    // 刷新界面
                    refresh(e.getInventory(), player);
                }
            }
        } 

        // 检查是否是关闭按钮
        else if (displayName.equals(ChatColor.RED + "关闭")) {
            player.closeInventory();
        }
    }
    
    /**
     * 从显示名称中提取种族名称
     */
    private static String extractRaceNameFromDisplayName(String displayName) {
        // 从显示名称中提取种族名称
        String raceName = null;
        
        // 如果显示名称包含" (Lv.", 先去掉等级部分
        if (displayName.contains(" (Lv.")) {
            displayName = displayName.substring(0, displayName.indexOf(" (Lv."));
        }
        
        // 如果显示名称包含"[进化] ", 去掉这个前缀
        if (displayName.contains("[进化] ")) {
            displayName = displayName.replace("[进化] ", "");
        }
        
        // 去掉颜色代码
        displayName = ChatColor.stripColor(displayName);
        
        // 如果显示名称不为空，转换为小写
        if (!displayName.isEmpty()) {
            raceName = displayName.toLowerCase();
        }
        
        return raceName;
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals(TITLE)) {
            e.setCancelled(true);
        }
    }
}