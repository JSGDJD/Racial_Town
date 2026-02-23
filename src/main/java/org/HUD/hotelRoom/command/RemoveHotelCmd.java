package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemoveHotelCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "用法：/removehotel <领地名称>");
            return true;
        }

        String hotelName = args[0];
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName); // ① 先拿到旧索引对象
        if (info == null) {
            sender.sendMessage(ChatColor.RED + "领地 '" + hotelName + "' 不存在。");
            return true;
        }

        SQLiteStorage.removeHotel(hotelName);   // ② 删库（内部已反注册索引）
        SelectionMgr.HOTELS.remove(hotelName);  // ③ 删内存
        SelectionMgr.getInst().removeIndex(info); // ④ 反注册空间索引（兜底）

        sender.sendMessage(ChatColor.GREEN + "领地 '" + hotelName + "' 已被撤销。");
        return true;
    }
}
