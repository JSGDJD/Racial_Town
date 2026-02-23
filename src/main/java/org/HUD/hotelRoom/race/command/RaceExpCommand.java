package org.HUD.hotelRoom.race.command;

import org.HUD.hotelRoom.race.RaceDataStorage;
import org.HUD.hotelRoom.race.RaceExpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 种族经验指令
 * 用法: /raceexp <give|set|check> <玩家> <数量>
 */
public class RaceExpCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give" -> {
                return handleGive(sender, args);
            }
            case "set" -> {
                return handleSet(sender, args);
            }
            case "check" -> {
                return handleCheck(sender, args);
            }
            case "reload" -> {
                return handleReload(sender);
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }
    
    /**
     * 给予玩家种族经验
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c用法: /raceexp give <玩家> <经验值>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家 " + args[1] + " 不在线或不存在！");
            return true;
        }
        
        try {
            int amount = Integer.parseInt(args[2]);
            
            RaceExpManager expManager = RaceExpManager.getInstance();
            if (expManager == null) {
                sender.sendMessage("§c种族经验系统未初始化！");
                return true;
            }
            
            expManager.addExperience(target, amount, "command");
            sender.sendMessage("§a成功给予 §e" + target.getName() + " §a种族经验 §6" + amount + " §a点");
            
        } catch (NumberFormatException e) {
            sender.sendMessage("§c经验值必须是整数！");
        }
        
        return true;
    }
    
    /**
     * 设置玩家种族经验
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c用法: /raceexp set <玩家> <经验值>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家 " + args[1] + " 不在线或不存在！");
            return true;
        }
        
        try {
            int amount = Integer.parseInt(args[2]);
            
            RaceExpManager expManager = RaceExpManager.getInstance();
            if (expManager == null) {
                sender.sendMessage("§c种族经验系统未初始化！");
                return true;
            }
            
            expManager.setExperience(target.getUniqueId(), amount);
            sender.sendMessage("§a成功设置 §e" + target.getName() + " §a的种族经验为 §6" + amount + " §a点");
            target.sendMessage("§a§l[种族系统] §7你的种族经验已被设置为 §6" + amount + " §7点");
            
        } catch (NumberFormatException e) {
            sender.sendMessage("§c经验值必须是整数！");
        }
        
        return true;
    }
    
    /**
     * 查询玩家种族信息
     */
    private boolean handleCheck(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length < 2) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§c用法: /raceexp check <玩家>");
                return true;
            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c玩家 " + args[1] + " 不在线或不存在！");
                return true;
            }
        }
        
        String race = RaceDataStorage.getPlayerRace(target.getUniqueId());
        int level = RaceDataStorage.getPlayerRaceLevel(target.getUniqueId());
        int exp = RaceDataStorage.getPlayerRaceExperience(target.getUniqueId());
        
        RaceExpManager expManager = RaceExpManager.getInstance();
        int expRequired = expManager != null ? expManager.getExpRequired(level + 1) : (level + 1) * 100;
        
        sender.sendMessage("§7§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§6§l种族信息 §8- §e" + target.getName());
        sender.sendMessage("");
        sender.sendMessage("  §7种族: §a" + race);
        sender.sendMessage("  §7等级: §b" + level);
        sender.sendMessage("  §7经验: §6" + exp + " §7/ §6" + expRequired);
        sender.sendMessage("");
        
        // 显示允许的经验获取方式
        if (expManager != null) {
            List<String> allowedMethods = expManager.getRaceAllowedMethods(race);
            if (!allowedMethods.isEmpty()) {
                sender.sendMessage("  §7允许的经验获取方式:");
                for (String method : allowedMethods) {
                    if (expManager.isMethodEnabled(method)) {
                        sender.sendMessage("    §a✓ §7" + method);
                    } else {
                        sender.sendMessage("    §c✗ §7" + method + " §8(已禁用)");
                    }
                }
                sender.sendMessage("");
            }
        }
        
        sender.sendMessage("§7§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return true;
    }
    
    /**
     * 重载配置
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }
        
        RaceExpManager expManager = RaceExpManager.getInstance();
        if (expManager == null) {
            sender.sendMessage("§c种族经验系统未初始化！");
            return true;
        }
        
        expManager.reload();
        sender.sendMessage("§a§l[种族系统] §7配置已重载！");
        
        return true;
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§6§l种族经验系统 §7- 指令帮助");
        sender.sendMessage("");
        sender.sendMessage("  §e/raceexp give <玩家> <经验值> §7- 给予玩家种族经验");
        sender.sendMessage("  §e/raceexp set <玩家> <经验值> §7- 设置玩家种族经验");
        sender.sendMessage("  §e/raceexp check [玩家] §7- 查询种族信息");
        sender.sendMessage("  §e/raceexp reload §7- 重载配置文件");
        sender.sendMessage("");
        sender.sendMessage("§7§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("give");
            completions.add("set");
            completions.add("check");
            completions.add("reload");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || 
                                         args[0].equalsIgnoreCase("set") || 
                                         args[0].equalsIgnoreCase("check"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }
}
