package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CreateOfficialHotelCmd implements CommandExecutor {
    private final SelectionMgr selectionMgr;

    public CreateOfficialHotelCmd(SelectionMgr selectionMgr) {
        this.selectionMgr = selectionMgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以创建官方酒店。");
            return true;
        }
        if (!player.hasPermission("hotelroom.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /createofficialhotel <酒店类型> <酒店名称>");
            return true;
        }
        String hotelType = args[0];
        String hotelName = args[1];

        if (SelectionMgr.HOTELS.containsKey(hotelName)) {
            player.sendMessage(ChatColor.RED + "酒店名称已存在，请使用其他名称。");
            return true;
        }
        if (!selectionMgr.hasBothPoints(player)) {
            player.sendMessage(ChatColor.RED + "请先使用铁斧选择两个对角点。");
            return true;
        }
        Location[] corners = selectionMgr.getSelection(player);
        if (corners == null || corners[0] == null || corners[1] == null) {
            player.sendMessage(ChatColor.RED + "选区无效，请重新选择。");
            return true;
        }

        // 1. 创建系统 owner 对象
        HotelInfo hotelInfo = new HotelInfo(hotelName, new UUID(0, 0), corners,
                                            false, true, hotelType);
        // 2. 把创建者踢出白名单，防止他通过 public 逻辑混进去
        hotelInfo.members.remove(player.getUniqueId());

        // 3. 写内存 + 空间索引
        SelectionMgr.HOTELS.put(hotelName, hotelInfo);
        selectionMgr.addIndex(hotelInfo);

        // 4. 生成外观快照
        hotelInfo.facade.addAll(org.HUD.hotelRoom.protection.FacadeProtection.snapshot(corners));
        SQLiteStorage.saveFacade(hotelName, hotelInfo.facade);

        // 5. 强制写库，owner 一定是系统 UUID
        SQLiteStorage.saveHotel(hotelName, new UUID(0, 0), corners,
                                false, true, hotelType);

        player.sendMessage(ChatColor.GREEN + "官方酒店 '" + hotelName
                + "' (类型: " + hotelType + ") 创建成功！");
        return true;
    }
}