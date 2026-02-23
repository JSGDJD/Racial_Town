package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.gui.AbandonConfirmGUI;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class AbandonCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以放弃领地。");
            return true;
        }

        UUID playerUuid = p.getUniqueId();
        
        if (args.length > 0) {
            // 如果提供了参数，直接放弃指定的房屋
            String hotelName = args[0];
            HotelInfo hotelInfo = SelectionMgr.HOTELS.get(hotelName);
            
            if (hotelInfo == null) {
                p.sendMessage(ChatColor.RED + "房屋 '" + hotelName + "' 不存在。");
                return true;
            }
            
            // 检查玩家是否拥有这个私有房屋
            if (hotelInfo.owner.equals(playerUuid)) {
                // 打开二次确认 GUI
                AbandonConfirmGUI.open(p, hotelInfo);
                return true;
            }
            
            // 检查玩家是否在公共房屋中
            if (hotelInfo.isPublic && hotelInfo.members.contains(playerUuid)) {
                // 从公共房屋的成员列表中移除玩家
                hotelInfo.members.remove(playerUuid);
                // 从数据库中移除成员信息
                SQLiteStorage.removeMember(hotelName, playerUuid);
                p.sendMessage(ChatColor.GREEN + "你已成功放弃公共房屋 '" + hotelInfo.name + "'。");
                return true;
            }
            
            p.sendMessage(ChatColor.RED + "你没有权限放弃房屋 '" + hotelName + "'。");
            return true;
        }

        HotelInfo owned = null;
        HotelInfo publicHotel = null;
        
        // 查找玩家拥有的私有房屋
        for (HotelInfo hi : SelectionMgr.HOTELS.values()) {
            if (hi.owner.equals(playerUuid)) {
                owned = hi;
                break;
            }
        }
        
        // 查找玩家领取的公共房屋
        for (HotelInfo hi : SelectionMgr.HOTELS.values()) {
            if (hi.isPublic && hi.members.contains(playerUuid)) {
                publicHotel = hi;
                break;
            }
        }

        // 如果玩家既没有私有房屋也没有公共房屋
        if (owned == null && publicHotel == null) {
            p.sendMessage(ChatColor.RED + "你当前没有领地可放弃！");
            return true;
        }

        // 如果玩家既有私有房屋也有公共房屋，提示选择
        if (owned != null && publicHotel != null) {
            p.sendMessage(ChatColor.YELLOW + "你同时拥有私有房屋和领取了公共房屋：");
            p.sendMessage(ChatColor.YELLOW + "- 私有房屋: " + owned.name);
            p.sendMessage(ChatColor.YELLOW + "- 公共房屋: " + publicHotel.name);
            p.sendMessage(ChatColor.YELLOW + "请使用 /abandon <房屋名称> 来指定放弃哪个房屋。");
            return true;
        }

        // 如果只有私有房屋
        if (owned != null) {
            // 打开二次确认 GUI
            AbandonConfirmGUI.open(p, owned);
            return true;
        }

        // 如果只有公共房屋
        if (publicHotel != null) {
            // 从公共房屋的成员列表中移除玩家
            publicHotel.members.remove(playerUuid);
            // 从数据库中移除成员信息
            SQLiteStorage.removeMember(publicHotel.name, playerUuid);
            p.sendMessage(ChatColor.GREEN + "你已成功放弃公共房屋 '" + publicHotel.name + "'。");
            return true;
        }

        return true;
    }
}