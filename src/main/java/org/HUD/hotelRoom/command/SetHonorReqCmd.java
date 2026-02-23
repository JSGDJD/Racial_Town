package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetHonorReqCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/hotelroom sethonorreq <领地> <数值>");
            return true;
        }
        String hotel = args[0];
        int req;
        try { req = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "数值必须是整数。");
            return true;
        }
        if (!SelectionMgr.HOTELS.containsKey(hotel)) {
            sender.sendMessage(ChatColor.RED + "领地 '" + hotel + "' 不存在。");
            return true;
        }
        SQLiteStorage.setHonorReq(hotel, req);
        sender.sendMessage(ChatColor.GREEN + "领地 '" + hotel + "' 荣誉门槛已设为 " + req + "。");
        return true;
    }
}
