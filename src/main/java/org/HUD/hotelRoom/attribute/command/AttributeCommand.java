package org.HUD.hotelRoom.attribute.command;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.attribute.PlayerAttribute;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 属性系统命令
 * /attr <check|set|add|reset|reload|item> [参数...]
 */
public class AttributeCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        AttributeManager manager = AttributeManager.getInstance();
        
        if (manager == null || !manager.isEnabled()) {
            sender.sendMessage("§c属性系统未启用！");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "check":
                return handleCheck(sender, args, manager);
            case "set":
                return handleSet(sender, args, manager);
            case "add":
                return handleAdd(sender, args, manager);
            case "reset":
                return handleReset(sender, args, manager);
            case "reload":
                return handleReload(sender, manager);
            case "item":
                return handleItem(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleCheck(CommandSender sender, String[] args, AttributeManager manager) {
        Player target;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("hotelroom.attr.others")) {
                sender.sendMessage("§c你没有权限查看他人属性！");
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c玩家不在线！");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c控制台必须指定玩家！");
                return true;
            }
            target = (Player) sender;
        }
        
        PlayerAttribute attr = manager.getPlayerAttribute(target.getUniqueId());
        sender.sendMessage("§e========== §6" + target.getName() + " 的属性 §e==========");
        
        Map<String, Double> attrs = attr.getAllAttributes();
        List<String> sortedKeys = new ArrayList<>(attrs.keySet());
        Collections.sort(sortedKeys);
        
        for (String key : sortedKeys) {
            double value = attrs.get(key);
            String displayName = manager.getAttributeDisplayName(key);
            sender.sendMessage(displayName + " §7: §f" + String.format("%.2f", value));
        }
        
        return true;
    }
    
    private boolean handleSet(CommandSender sender, String[] args, AttributeManager manager) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限！");
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage("§c用法: /attr set <玩家> <属性> <值>");
            sender.sendMessage("§e示例: /attr set Player 物理伤害 10");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家不在线！");
            return true;
        }
        
        String attrInput = args[2];
        String attrKey = convertToInternalKey(attrInput, manager);
        if (attrKey == null) {
            sender.sendMessage("§c未知的属性: " + attrInput);
            return true;
        }
        
        double value;
        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的数值！");
            return true;
        }
        
        manager.setPlayerAttributeValue(target.getUniqueId(), attrKey, value);
        manager.refreshPlayerAttributes(target);
        
        sender.sendMessage("§a已设置 " + target.getName() + " 的 " + manager.getAttributeDisplayName(attrKey) + " §a为 §f" + value);
        target.sendMessage("§a你的属性 " + manager.getAttributeDisplayName(attrKey) + " §a已被设置为 §f" + value);
        
        return true;
    }
    
    private boolean handleAdd(CommandSender sender, String[] args, AttributeManager manager) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限！");
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage("§c用法: /attr add <玩家> <属性> <值>");
            sender.sendMessage("§e示例: /attr add Player 最大生命值 20");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家不在线！");
            return true;
        }
        
        String attrInput = args[2];
        String attrKey = convertToInternalKey(attrInput, manager);
        if (attrKey == null) {
            sender.sendMessage("§c未知的属性: " + attrInput);
            return true;
        }
        
        double value;
        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的数值！");
            return true;
        }
        
        PlayerAttribute attr = manager.getPlayerAttribute(target.getUniqueId());
        double oldValue = attr.getAttribute(attrKey);
        double newValue = oldValue + value;
        
        manager.setPlayerAttributeValue(target.getUniqueId(), attrKey, newValue);
        manager.refreshPlayerAttributes(target);
        
        sender.sendMessage("§a已为 " + target.getName() + " 的 " + manager.getAttributeDisplayName(attrKey) + " §a增加 §f" + value + " §7(当前: " + newValue + ")");
        target.sendMessage("§a你的属性 " + manager.getAttributeDisplayName(attrKey) + " §a增加了 §f" + value);
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args, AttributeManager manager) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限！");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /attr reset <玩家>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家不在线！");
            return true;
        }
        
        manager.resetPlayerAttributes(target.getUniqueId());
        manager.refreshPlayerAttributes(target);
        
        sender.sendMessage("§a已重置 " + target.getName() + " 的所有属性！");
        target.sendMessage("§a你的所有属性已被重置为默认值！");
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender, AttributeManager manager) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限！");
            return true;
        }
        
        // 重载属性系统
        manager.reload();
        
        // 重载自定义属性
        org.HUD.hotelRoom.attribute.CustomAttributeManager customManager = 
            org.HUD.hotelRoom.attribute.CustomAttributeManager.getInstance();
        if (customManager != null) {
            customManager.reload();
        }
        
        sender.sendMessage("§a属性系统配置已重载！");
        sender.sendMessage("§a自定义属性配置已重载！");
        
        return true;
    }
    
    /**
     * 处理物品属性子命令
     */
    private boolean handleItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用物品属性命令！");
            return true;
        }
        
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            sendItemHelp(player);
            return true;
        }
        
        String itemCmd = args[1].toLowerCase();
        
        switch (itemCmd) {
            case "add":
                return handleItemAdd(player, args);
            case "remove":
                return handleItemRemove(player, args);
            case "clear":
                return handleItemClear(player);
            case "list":
                return handleItemList(player);
            default:
                sendItemHelp(player);
                return true;
        }
    }
    
    private boolean handleItemAdd(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§c用法: /attr item add <属性> <数值>");
            player.sendMessage("§e示例: /attr item add 物理伤害 5");
            player.sendMessage("§e示例: /attr item add 最大生命值 20");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c请手持一个物品！");
            return true;
        }
        
        String attributeInput = args[2];
        double value;
        
        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数值！");
            return true;
        }
        
        // 获取属性管理器
        AttributeManager manager = AttributeManager.getInstance();
        
        // 尝试将输入转换为内部key（支持中文和英文）
        String internalKey = convertToInternalKey(attributeInput, manager);
        if (internalKey == null) {
            player.sendMessage("§c未知的属性: " + attributeInput);
            player.sendMessage("§e提示: 使用 /attr item add 查看可用属性");
            return true;
        }
        
        // 获取属性显示名称（中文）
        String displayName = manager.getAttributeDisplayName(internalKey);
        
        // 添加 Lore
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        String sign = value >= 0 ? "+" : "";
        String loreLine = "§f" + displayName + ": §f" + sign + value;
        lore.add(loreLine);
        
        // 添加调试日志
        AttributeManager attrManager = AttributeManager.getInstance();
        if (attrManager != null && attrManager.isLoggingEnabled()) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().info(
                "[物品属性] 添加Lore: '" + loreLine + "' 到物品: " + item.getType());
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        player.sendMessage("§a已添加属性: " + loreLine);
        player.sendMessage("§e提示: 切换手持物品以更新属性！");
        
        return true;
    }
    
    private boolean handleItemRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /attr item remove <行号>");
            player.sendMessage("§e使用 /attr item list 查看行号");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c请手持一个物品！");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            player.sendMessage("§c该物品没有 Lore！");
            return true;
        }
        
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的行号！");
            return true;
        }
        
        List<String> lore = meta.getLore();
        if (lineNumber < 0 || lineNumber >= lore.size()) {
            player.sendMessage("§c行号超出范围！");
            return true;
        }
        
        String removed = lore.remove(lineNumber);
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        player.sendMessage("§a已移除 Lore: " + removed);
        player.sendMessage("§e提示: 切换手持物品以更新属性！");
        
        return true;
    }
    
    private boolean handleItemClear(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c请手持一个物品！");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        meta.setLore(new ArrayList<>());
        item.setItemMeta(meta);
        
        player.sendMessage("§a已清除所有 Lore！");
        player.sendMessage("§e提示: 切换手持犉品以更新属性！");
        
        return true;
    }
    
    private boolean handleItemList(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c请手持一个物品！");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            player.sendMessage("§e该物品没有 Lore");
            return true;
        }
        
        List<String> lore = meta.getLore();
        player.sendMessage("§e========== §6物品 Lore 列表 §e==========");
        for (int i = 0; i < lore.size(); i++) {
            player.sendMessage("§7" + (i + 1) + ". §r" + lore.get(i));
        }
        
        return true;
    }
    
    private void sendItemHelp(Player player) {
        player.sendMessage("§e========== §6物品属性命令 §e==========");
        player.sendMessage("§6/attr item add <属性> <数值> §7- 添加属性");
        player.sendMessage("§6/attr item remove <行号> §7- 移除指定行");
        player.sendMessage("§6/attr item clear §7- 清除所有Lore");
        player.sendMessage("§6/attr item list §7- 查看Lore列表");
        player.sendMessage("§e======== §6可用属性（中文）§e ========");
        player.sendMessage("§7攻击: §f物理伤害, 魔法伤害, 攻击速度");
        player.sendMessage("§7防御: §f物理防御, 魔法防御, 护甲值");
        player.sendMessage("§7生命: §f生命值, 最大生命值, 生命恢复");
        player.sendMessage("§7暴击: §f暴击率, 暴击伤害");
        player.sendMessage("§7其他: §f移动速度, 闪避率, 生命偷取");
    }
    
    /**
     * 将中文或英文属性名转换为内部key
     */
    private String convertToInternalKey(String input, AttributeManager manager) {
        // 兜底：创建静态映射表（优先使用，避免配置文件影响）
        Map<String, String> chineseToKey = new HashMap<>();
        chineseToKey.put("生命值", "health");
        chineseToKey.put("最大生命值", "health");
        chineseToKey.put("物理伤害", "physical_damage");
        chineseToKey.put("魔法伤害", "magic_damage");
        chineseToKey.put("攻击速度", "attack_speed");
        chineseToKey.put("物理防御", "physical_defense");
        chineseToKey.put("魔法防御", "magic_defense");
        chineseToKey.put("护甲值", "armor");
        chineseToKey.put("暴击率", "crit_rate");
        chineseToKey.put("暴击伤害", "crit_damage");
        chineseToKey.put("护甲穿透", "armor_penetration");
        chineseToKey.put("魔法穿透", "magic_penetration");
        chineseToKey.put("生命恢复", "health_regen");
        chineseToKey.put("魔法值", "mana");
        chineseToKey.put("最大魔法值", "max_mana");
        chineseToKey.put("魔法恢复", "mana_regen");
        chineseToKey.put("移动速度", "movement_speed");
        chineseToKey.put("闪避率", "dodge_rate");
        chineseToKey.put("格挡率", "block_rate");
        chineseToKey.put("生命偷取", "lifesteal");
        chineseToKey.put("冷却缩减", "cooldown_reduction");
        chineseToKey.put("经验加成", "exp_bonus");
        chineseToKey.put("掉落率加成", "drop_rate_bonus");
        
        // 先尝试静态映射表
        if (chineseToKey.containsKey(input)) {
            return chineseToKey.get(input);
        }
        
        // 先尝试直接作为内部key
        if (manager.getDefaultAttributes().containsKey(input)) {
            return input;
        }
        
        // 使用属性管理器的映射方法（从配置文件读取）
        String key = manager.getAttributeKeyByDisplayName(input);
        if (key != null) {
            return key;
        }
        
        return null;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e========== §6属性系统命令 §e==========");
        sender.sendMessage("§6/attr check [玩家] §7- 查看玩家属性");
        sender.sendMessage("§6/attr set <玩家> <属性> <值> §7- 设置属性");
        sender.sendMessage("§6/attr add <玩家> <属性> <值> §7- 增加属性");
        sender.sendMessage("§6/attr reset <玩家> §7- 重置属性");
        sender.sendMessage("§6/attr reload §7- 重载配置");
        sender.sendMessage("§e========== §6物品属性命令 §e==========");
        sender.sendMessage("§6/attr item add <属性> <数值> §7- 添加属性到手持物品");
        sender.sendMessage("§6/attr item remove <行号> §7- 移除指定行Lore");
        sender.sendMessage("§6/attr item clear §7- 清除所有Lore");
        sender.sendMessage("§6/attr item list §7- 查看物品Lore列表");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "set", "add", "reset", "reload", "item");
        }
        
        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            
            // 玩家属性命令的玩家名补全
            if (subCmd.equals("check") || subCmd.equals("set") || 
                subCmd.equals("add") || subCmd.equals("reset")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            
            // 物品属性子命令补全
            if (subCmd.equals("item")) {
                return Arrays.asList("add", "remove", "clear", "list");
            }
        }
        
        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            
            // 玩家属性命令的属性名补全（中文）
            if (subCmd.equals("set") || subCmd.equals("add")) {
                return Arrays.asList(
                    "物理伤害", "魔法伤害", "攻击速度",
                    "物理防御", "魔法防御", "护甲值",
                    "生命值", "最大生命值", "生命恢复",
                    "暴击率", "暴击伤害",
                    "移动速度", "闪避率", "生命偷取"
                );
            }
            
            // 物品属性 add 的属性名补全（中文）
            if (subCmd.equals("item") && args[1].equalsIgnoreCase("add")) {
                return Arrays.asList(
                    "物理伤害", "魔法伤害", "攻击速度",
                    "物理防御", "魔法防御", "护甲值",
                    "生命值", "最大生命值", "生命恢复",
                    "暴击率", "暴击伤害",
                    "移动速度", "闪避率", "生命偷取"
                );
            }
        }
        
        return Collections.emptyList();
    }
}
