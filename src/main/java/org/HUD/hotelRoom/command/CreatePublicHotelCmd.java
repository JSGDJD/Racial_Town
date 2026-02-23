package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CreatePublicHotelCmd implements CommandExecutor {
    private final SelectionMgr selectionMgr;

    public CreatePublicHotelCmd(SelectionMgr selectionMgr) {
        this.selectionMgr = selectionMgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以创建公共酒店。");
            return true;
        }

        if (!player.hasPermission("hotelroom.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /createpublichotel <酒店名称>");
            return true;
        }

        String hotelName = args[0];

        // 检查酒店名称是否已存在
        if (SelectionMgr.HOTELS.containsKey(hotelName)) {
            player.sendMessage(ChatColor.RED + "酒店名称已存在，请使用其他名称。");
            return true;
        }

        // 检查玩家是否已选择两个点
        if (!selectionMgr.hasBothPoints(player)) {
            player.sendMessage(ChatColor.RED + "请先使用铁斧选择两个对角点。");
            return true;
        }

        // 获取选区坐标
        Location[] corners = selectionMgr.getSelection(player);
        if (corners == null || corners[0] == null || corners[1] == null) {
            player.sendMessage(ChatColor.RED + "选区无效，请重新选择。");
            return true;
        }

        // 创建公共酒店
        HotelInfo hotelInfo = new HotelInfo(hotelName, new UUID(0, 0), corners, true); // isPublic = true
        SelectionMgr.HOTELS.put(hotelName, hotelInfo);
        selectionMgr.addIndex(hotelInfo); // 添加到空间索引

        // 生成外观快照
        hotelInfo.facade.addAll(org.HUD.hotelRoom.protection.FacadeProtection.snapshot(corners));
        SQLiteStorage.saveFacade(hotelName, hotelInfo.facade);

        // 保存到数据库
        SQLiteStorage.saveHotel(hotelName, new UUID(0, 0), corners, true);

        player.sendMessage(ChatColor.GREEN + "公共酒店 '" + hotelName + "' 创建成功！");

        return true;
    }
}