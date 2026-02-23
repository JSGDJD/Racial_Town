package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GiveHotelCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/givehotel <领地名称> <玩家名>");
            return true;
        }

        String hotelName = args[0];
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + targetName + " 不在线或不存在。");
            return true;
        }

        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        if (info == null) {
            sender.sendMessage(ChatColor.RED + "领地 '" + hotelName + "' 不存在。");
            return true;
        }

        /* 每人只能拥有 1 个领地 */
        for (HotelInfo ex : SelectionMgr.HOTELS.values()) {
            if (ex.owner.equals(target.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "该玩家已拥有其他领地，无法赠送！");
                return true;
            }
        }

        // 更新内存，保留原有属性
        HotelInfo newInfo = new HotelInfo(info.name, target.getUniqueId(), info.corners, 
                                          info.isPublic, info.isOfficial, info.hotelType);
        newInfo.members.addAll(info.members); // 保留成员列表
        newInfo.facade.addAll(info.facade);   // 保留外观快照
        
        SelectionMgr.HOTELS.put(hotelName, newInfo);
        
        // 更新空间索引
        SelectionMgr.getInst().removeIndex(info);
        SelectionMgr.getInst().addIndex(newInfo);

        // 更新数据库
        SQLiteStorage.saveHotel(hotelName, target.getUniqueId(), info.corners, 
                                info.isPublic, info.isOfficial, info.hotelType);

        sender.sendMessage(ChatColor.GREEN + "领地 '" + hotelName + "' 已赠送给 " + target.getName() + "。");
        target.sendMessage(ChatColor.GREEN + "你获得了领地 '" + hotelName + "' 的所有权！");
        return true;
    }
}