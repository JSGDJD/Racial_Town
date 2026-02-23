package org.HUD.hotelRoom.command;


import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RemoveHotelPlayerCmd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage(ChatColor.RED + "无权使用。");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /removehotelplayer <酒店名> <玩家>");
            return true;
        }
        String hotel = args[0];
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家不在线。");
            return true;
        }
        if (!SelectionMgr.HOTELS.containsKey(hotel)) {
            sender.sendMessage(ChatColor.RED + "酒店不存在。");
            return true;
        }
        UUID uuid = target.getUniqueId();
        boolean ok = SelectionMgr.HOTELS.get(hotel).members.remove(uuid);
        if (!ok) {
            sender.sendMessage(ChatColor.YELLOW + "该玩家本来就没有权限。");
            return true;
        }
        SQLiteStorage.removeMember(hotel, uuid);
        sender.sendMessage(ChatColor.GREEN + "已将 " + target.getName() + " 踢出酒店 '" + hotel + "' 白名单。");
        target.sendMessage(ChatColor.RED + "你失去了酒店 '" + hotel + "' 的使用权！");
        return true;
    }
}
