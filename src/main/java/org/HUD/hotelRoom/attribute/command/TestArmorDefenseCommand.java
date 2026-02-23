package org.HUD.hotelRoom.attribute.command;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.attribute.AttributeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * 测试盔甲防御命令
 * 用于诊断MM怪物伤害是否正确应用原版盔甲防御
 */
public class TestArmorDefenseCommand implements CommandExecutor, Listener {
    
    private final HotelRoom plugin;
    private boolean testMode = false;
    private Player testPlayer = null;
    
    public TestArmorDefenseCommand(HotelRoom plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以执行此命令");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                startTest(player);
                break;
            case "stop":
                stopTest();
                player.sendMessage(ChatColor.GREEN + "测试模式已停止");
                break;
            case "setup":
                setupTestEquipment(player);
                break;
            case "info":
                showPlayerInfo(player);
                break;
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 盔甲防御测试命令 ===");
        player.sendMessage(ChatColor.YELLOW + "/testarmor start" + ChatColor.WHITE + " - 开始测试模式");
        player.sendMessage(ChatColor.YELLOW + "/testarmor stop" + ChatColor.WHITE + " - 停止测试模式");
        player.sendMessage(ChatColor.YELLOW + "/testarmor setup" + ChatColor.WHITE + " - 装备满级钻石盔甲");
        player.sendMessage(ChatColor.YELLOW + "/testarmor info" + ChatColor.WHITE + " - 显示玩家信息");
    }
    
    private void startTest(Player player) {
        if (testMode) {
            player.sendMessage(ChatColor.RED + "测试模式已在运行中");
            return;
        }
        
        testMode = true;
        testPlayer = player;
        
        player.sendMessage(ChatColor.GREEN + "盔甲防御测试模式已启动！");
        player.sendMessage(ChatColor.YELLOW + "请让MM怪物攻击你来测试盔甲防御效果");
        player.sendMessage(ChatColor.YELLOW + "服务器控制台将显示详细的伤害计算日志");
    }
    
    private void stopTest() {
        testMode = false;
        testPlayer = null;
    }
    
    private void setupTestEquipment(Player player) {
        // 给玩家装备满级钻石盔甲
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        
        // 添加保护附魔
        addProtectionEnchant(helmet, 4);
        addProtectionEnchant(chestplate, 4);
        addProtectionEnchant(leggings, 4);
        addProtectionEnchant(boots, 4);
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        player.sendMessage(ChatColor.GREEN + "已为你装备满级钻石盔甲（保护IV）");
        showPlayerInfo(player);
    }
    
    private void addProtectionEnchant(ItemStack item, int level) {
        // 这里简化处理，实际应该添加附魔
        // 在实际实现中需要使用ItemMeta和Enchantment
    }
    
    private void showPlayerInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 玩家信息 ===");
        player.sendMessage(ChatColor.YELLOW + "名称: " + player.getName());
        player.sendMessage(ChatColor.YELLOW + "生命值: " + player.getHealth() + "/" + player.getMaxHealth());
        
        // 显示盔甲值
        double armorPoints = 0;
        if (player.getInventory().getHelmet() != null) armorPoints += 3;
        if (player.getInventory().getChestplate() != null) armorPoints += 8;
        if (player.getInventory().getLeggings() != null) armorPoints += 6;
        if (player.getInventory().getBoots() != null) armorPoints += 3;
        
        player.sendMessage(ChatColor.YELLOW + "盔甲点数: " + armorPoints);
        
        // 显示属性
        if (player.getAttribute(Attribute.ARMOR) != null) {
            player.sendMessage(ChatColor.YELLOW + "ARMOR属性: " + player.getAttribute(Attribute.ARMOR).getValue());
        }
        if (player.getAttribute(Attribute.ARMOR_TOUGHNESS) != null) {
            player.sendMessage(ChatColor.YELLOW + "ARMOR_TOUGHNESS属性: " + player.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue());
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!testMode) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (!player.equals(testPlayer)) return;
        
        // 记录伤害事件的详细信息
        plugin.getLogger().info("=== 盔甲防御测试伤害事件 ===");
        plugin.getLogger().info("受害者: " + player.getName());
        plugin.getLogger().info("攻击者: " + event.getDamager().getType());
        plugin.getLogger().info("伤害原因: " + event.getCause());
        plugin.getLogger().info("原始伤害: " + event.getDamage());
        
        // 显示所有DamageModifier
        plugin.getLogger().info("DamageModifier状态:");
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (event.isApplicable(modifier)) {
                plugin.getLogger().info("  " + modifier + ": " + event.getDamage(modifier));
            }
        }
        
        plugin.getLogger().info("================================");
    }
}