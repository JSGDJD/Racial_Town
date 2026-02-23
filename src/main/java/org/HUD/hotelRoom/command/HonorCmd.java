package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.DailyHonorManager;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HonorCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用荣誉命令。");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法：/hotelroom honor <add|set|remove> <玩家> <数值>");
            return true;
        }
        String op = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家不在线或不存在。");
            return true;
        }
        int val;
        try { val = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "数值必须是整数。");
            return true;
        }
        UUID uid = target.getUniqueId();
        switch (op) {
            case "add":
                if (DailyHonorManager.isEnabled()) {
                    int actualAdded = DailyHonorManager.addHonorWithLimit(uid, val);
                    sender.sendMessage(ChatColor.GREEN + "已为 " + target.getName() + " 尝试增加 " + val + " 荣誉值，实际增加 " + actualAdded + " 荣誉值。");
                } else {
                    SQLiteStorage.addHonor(uid, val);
                    sender.sendMessage(ChatColor.GREEN + "已为 " + target.getName() + " 增加 " + val + " 荣誉值。");
                }
                break;
            case "set":
                SQLiteStorage.setHonor(uid, val);
                // 重置该玩家的当日荣誉值记录
                DailyHonorManager.resetDailyHonor(uid);
                DailyHonorManager.resetDailyReturn(uid);
                sender.sendMessage(ChatColor.GREEN + "已将 " + target.getName() + " 荣誉值设为 " + val + "。");
                break;
            case "remove":
                SQLiteStorage.addHonor(uid, -val);
                sender.sendMessage(ChatColor.GREEN + "已为 " + target.getName() + " 扣除 " + val + " 荣誉值。");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知操作，可用：add / set / remove");
        }
        return true;
    }
}