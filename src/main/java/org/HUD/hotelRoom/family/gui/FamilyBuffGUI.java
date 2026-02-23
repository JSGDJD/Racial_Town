package org.HUD.hotelRoom.family.gui;

import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyMember;
import org.HUD.hotelRoom.family.FamilyManager;
import org.HUD.hotelRoom.family.BuffSystem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FamilyBuffGUI implements Listener {
    private static final String GUI_NAME = ChatColor.GOLD + "家族加成管理";
    private static final int GUI_SIZE = 54;
    
    private static FamilyBuffGUI instance;
    
    private final JavaPlugin plugin;
    private final FamilyManager familyManager;
    private final BuffSystem buffSystem;
    
    private FamilyBuffGUI() {
        this.plugin = org.HUD.hotelRoom.HotelRoom.get();
        this.familyManager = FamilyManager.getInstance();
        this.buffSystem = familyManager.getBuffSystem();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public static FamilyBuffGUI getInstance() {
        if (instance == null) {
            instance = new FamilyBuffGUI();
        }
        return instance;
    }
    
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_NAME);
        
        // 获取玩家的家族信息
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不属于任何家族！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        // 设置GUI背景
        setBackground(inv);
        
        // 添加加成信息
        addBuffs(inv, family, player);
        
        // 添加返回按钮
        addBackButton(inv);
        
        player.openInventory(inv);
    }
    
    /** 设置GUI背景 */
    private void setBackground(Inventory inv) {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        meta.setDisplayName(" ");
        background.setItemMeta(meta);
        
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, background);
        }
    }
    
    /** 添加加成信息 */
    private void addBuffs(Inventory inv, Family family, Player player) {
        // 获取所有加成配置
        ConfigurationSection buffsSection = familyManager.getConfig().getConfigurationSection("buffs");
        if (buffsSection == null) {
            player.sendMessage(ChatColor.RED + "未找到加成配置！");
            return;
        }
        
        Set<String> buffKeys = buffsSection.getKeys(false);
        
        int slot = 10;
        for (String buffName : buffKeys) {
            ConfigurationSection configSection = buffsSection.getConfigurationSection(buffName);
            if (configSection == null) {
                continue;
            }
            
            if (slot >= 44) break; // 限制显示数量
            
            ItemStack buffItem = createBuffItem(buffName, configSection, family, player);
            inv.setItem(slot, buffItem);
            
            // 布局：每行6个，跳过边缘
            if ((slot + 1) % 9 == 0) {
                slot += 3;
            } else {
                slot++;
            }
        }
    }
    
    /** 创建加成物品 */
    private ItemStack createBuffItem(String buffName, ConfigurationSection config, Family family, Player player) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String displayName = config.getString("name", buffName);
        meta.setDisplayName(ChatColor.GOLD + displayName);
        
        // 设置Lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        // 描述
        if (config.contains("description")) {
            lore.add(ChatColor.GRAY + config.getString("description"));
        }
        lore.add("");
        
        // 效果信息
        if (config.contains("effect")) {
            String effect = config.getString("effect");
            int amplifier = config.getInt("amplifier", 1);
            int duration = config.getInt("duration", 3600);
            
            lore.add(ChatColor.YELLOW + "效果：" + getEffectName(effect) + " " + amplifier + "级");
            lore.add(ChatColor.YELLOW + "持续时间：" + formatDuration(duration));
        }
        lore.add("");
        
        // 消耗信息
        ConfigurationSection costSection = config.getConfigurationSection("cost");
        if (costSection != null) {
            lore.add(ChatColor.GREEN + "消耗：");
            
            if (costSection.contains("money") && costSection.getDouble("money") > 0) {
                lore.add(ChatColor.GRAY + "- 金币：" + costSection.getDouble("money"));
            }
            if (costSection.contains("honor") && costSection.getDouble("honor") > 0) {
                lore.add(ChatColor.GRAY + "- 荣誉值：" + costSection.getDouble("honor"));
            }
            if (costSection.contains("activity") && costSection.getDouble("activity") > 0) {
                lore.add(ChatColor.GRAY + "- 活跃值：" + costSection.getDouble("activity"));
            }
        }
        lore.add("");
        
        // 冷却信息
        int cooldown = config.getInt("cooldown", 86400);
        lore.add(ChatColor.RED + "冷却时间：" + formatDuration(cooldown));
        
        // 检查是否可以激活
        boolean canActivate = buffSystem.canActivateBuff(family, buffName);
        if (canActivate) {
            lore.add(ChatColor.GREEN + "左键点击激活");
        } else {
            lore.add(ChatColor.RED + "无法激活：冷却中或资源不足");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /** 获取效果名称 */
    private String getEffectName(String effectType) {
        switch (effectType.toUpperCase()) {
            case "STRENGTH": return "力量";
            case "SPEED": return "速度";
            case "REGENERATION": return "生命恢复";
            case "RESISTANCE": return "抗性提升";
            case "JUMP_BOOST": return "跳跃提升";
            case "HASTE": return "挖掘效率";
            case "NIGHT_VISION": return "夜视";
            case "INVISIBILITY": return "隐身";
            case "WATER_BREATHING": return "水下呼吸";
            case "FIRE_RESISTANCE": return "火焰抗性";
            case "HEALTH_BOOST": return "生命提升";
            case "ABSORPTION": return "伤害吸收";
            default: return effectType;
        }
    }
    
    /** 格式化时间 */
    private String formatDuration(int seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分钟");
        if (secs > 0) sb.append(secs).append("秒");
        
        return sb.toString();
    }
    
    /** 添加返回按钮 */
    private void addBackButton(Inventory inv) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "返回");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "点击返回家族主菜单");
        meta.setLore(lore);
        
        backButton.setItemMeta(meta);
        inv.setItem(49, backButton);
    }
    
    /** 处理GUI点击事件 */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        if (slot == 49) {
            // 返回按钮
            FamilyMainGUI mainGUI = FamilyMainGUI.getInstance();
            mainGUI.open(player);
            return;
        }
        
        // 处理加成激活
        if (clickedItem.getType() == Material.ENCHANTED_BOOK) {
            handleActivateBuff(player, clickedItem);
        }
    }
    
    /** 处理激活加成 */
    private void handleActivateBuff(Player player, ItemStack clickedItem) {
        // 获取玩家的家族信息
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不属于任何家族！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        // 获取加成名称
        String displayName = clickedItem.getItemMeta().getDisplayName();
        String buffName = displayName.substring(2); // 去掉颜色代码
        
        // 获取加成配置
        ConfigurationSection buffsSection = plugin.getConfig().getConfigurationSection("buffs");
        String actualBuffName = null;
        
        if (buffsSection != null) {
            for (String key : buffsSection.getKeys(false)) {
                ConfigurationSection configSection = buffsSection.getConfigurationSection(key);
                if (configSection != null) {
                    String configName = configSection.getString("name", key);
                    if (configName.equals(buffName)) {
                        actualBuffName = key;
                        break;
                    }
                }
            }
        }
        
        if (actualBuffName != null) {
            // 激活加成
            boolean activated = buffSystem.activateBuff(family, player, actualBuffName);
            
            if (activated) {
                player.sendMessage(ChatColor.GOLD + "[家族] " + ChatColor.YELLOW + "家族加成已激活！");
                // 重新打开GUI以更新状态
                open(player);
            } else {
                player.sendMessage(ChatColor.GOLD + "[家族] " + ChatColor.RED + "激活失败，可能是资源不足或冷却中！");
            }
        }
    }
}