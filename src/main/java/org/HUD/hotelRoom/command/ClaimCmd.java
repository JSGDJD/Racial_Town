package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.protection.FacadeProtection;
import org.HUD.hotelRoom.protection.ProtectionListener;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Set;

import java.util.UUID;

public class ClaimCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "仅玩家可领取领地。");
            return true;
        }

        // 检查参数，如果提供了酒店名称，则是领取公共房屋
        if (args.length >= 1) {
            String hotelName = args[0];
            return claimPublicHotel(p, hotelName);
        }

        // 如果没有参数，则是原来的领取私有房屋逻辑
        return claimPrivateHotel(p);
    }

    // 领取私有房屋的逻辑（原来的逻辑）
    private boolean claimPrivateHotel(Player p) {
        /* 0. 官方房直接拒绝 */
        HotelInfo info = ProtectionListener.getHotelAt(p.getLocation());
        if (info != null && info.isOfficial) {
            p.sendMessage(ChatColor.RED + "这是官方房屋，永远无法被玩家领取！");
            return true;
        }

        /* 1. 检测是否已拥有领地 */
        for (HotelInfo ex : SelectionMgr.HOTELS.values()) {
            if (ex.owner.equals(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "你已拥有领地，无法再次领取！");
                return true;
            }
        }

        /* 2. 获取当前所在酒店 */
        if (info == null) {
            p.sendMessage(ChatColor.RED + "你必须站在一块酒店领地内才能领取。");
            return true;
        }

        /* 3. 检测是否为官方房屋 */
        if (info.isOfficial) {
            p.sendMessage(ChatColor.RED + "这是官方房屋，无法领取！");
            return true;
        }

        /* 4. 检测是否有主人（无主人才能领取） */
        if (!info.owner.equals(new UUID(0, 0))) { // 系统占位UUID表示"无主人"
            p.sendMessage(ChatColor.RED + "该领地已有主人，无法领取！");
            return true;
        }

        /* 5. 检测是否为公共房屋 */
        if (info.isPublic) {
            p.sendMessage(ChatColor.RED + "这是公共房屋，请使用 /hotelroom claim <酒店名称> 领取。");
            return true;
        }

        /* 6. 荣誉门槛检测 */
        int req = SQLiteStorage.getHonorReq(info.name);
        int honor = SQLiteStorage.getHonor(p.getUniqueId());
        if (honor < req) {
            p.sendMessage(ChatColor.RED + "你的荣誉值不足 " + req + "，无法领取此领地！");
            return true;
        }

        /* 7. 更改主人 & 数据库，保留原有属性 */
        HotelInfo newInfo = new HotelInfo(info.name, p.getUniqueId(), info.corners, 
                                          info.isPublic, info.isOfficial, info.hotelType);
        newInfo.members.addAll(info.members); // 保留成员列表
        
        SelectionMgr.HOTELS.put(info.name, newInfo);
        SQLiteStorage.saveHotel(info.name, p.getUniqueId(), info.corners, 
                                info.isPublic, info.isOfficial, info.hotelType);
        // 刷新空间索引
        SelectionMgr.getInst().removeIndex(info);
        SelectionMgr.getInst().addIndex(newInfo);

        /* 8. 只读回最初外观，不重新生成（玩家建筑永不保护） */
        newInfo.facade.clear();
        newInfo.facade.addAll(SQLiteStorage.loadFacade(info.name));
        SQLiteStorage.saveFacade(info.name, newInfo.facade);


        /* 9. 一次性清空本酒店范围内所有 placed_blocks，
             前任任何方块不再受"归属保护"限制 */
        SQLiteStorage.untrackPlaceInHotel(info.name);


        p.sendMessage(ChatColor.GREEN + "恭喜你成功领取领地 '" + info.name + "'！");
        Bukkit.broadcastMessage(ChatColor.YELLOW + p.getName() + " 通过荣誉门槛领取了领地 '" + info.name + "'！");
        return true;
    }

    // 领取公共房屋的逻辑
    private boolean claimPublicHotel(Player p, String hotelName) {
        // 官方房永远不可领取
        if (SelectionMgr.HOTELS.containsKey(hotelName)) {
            HotelInfo hotelInfo = SelectionMgr.HOTELS.get(hotelName);
            if (hotelInfo.isOfficial) {
                p.sendMessage(ChatColor.RED + "这是官方房屋，永远无法被玩家领取！");
                return true;
            }
        }

        UUID playerUuid = p.getUniqueId();

        // 检查玩家是否已经领取了其他公共房屋
        if (isPlayerInAnyPublicHotel(playerUuid)) {
            p.sendMessage(ChatColor.RED + "你已经领取了其他公共房屋，无法再领取新的房屋。");
            return true;
        }

        // 检查玩家是否已经是某个私有房屋的主人
        for (HotelInfo hotel : SelectionMgr.HOTELS.values()) {
            if (hotel.owner.equals(playerUuid)) {
                p.sendMessage(ChatColor.RED + "你已经是私有房屋的主人，无法再领取公共房屋。");
                return true;
            }
        }

        // 检查酒店是否存在
        HotelInfo hotelInfo = SelectionMgr.HOTELS.get(hotelName);
        if (hotelInfo == null) {
            p.sendMessage(ChatColor.RED + "酒店 '" + hotelName + "' 不存在。");
            return true;
        }

        // 检查是否为官方房屋
        if (hotelInfo.isOfficial) {
            p.sendMessage(ChatColor.RED + "酒店 '" + hotelName + "' 是官方房屋，无法领取。");
            return true;
        }

        // 检查是否为公共房屋
        if (!hotelInfo.isPublic) {
            p.sendMessage(ChatColor.RED + "酒店 '" + hotelName + "' 不是公共房屋，无法领取。");
            return true;
        }

        // 检查玩家是否已经在该酒店的成员列表中
        if (hotelInfo.members.contains(playerUuid)) {
            p.sendMessage(ChatColor.YELLOW + "你已经可以访问该公共房屋了。");
            return true;
        }

        // 将玩家添加到酒店成员列表
        hotelInfo.members.add(playerUuid);
        
        // 保存到数据库
        SQLiteStorage.addMember(hotelName, playerUuid);

        // 发送成功消息
        p.sendMessage(ChatColor.GREEN + "你已成功领取公共房屋 '" + hotelName + "'，现在可以在其中放置和破坏自己的方块。");

        return true;
    }

    // 检查玩家是否已经领取了任何公共房屋
    private boolean isPlayerInAnyPublicHotel(UUID playerUuid) {
        for (HotelInfo hotel : SelectionMgr.HOTELS.values()) {
            if (hotel.isPublic && hotel.members.contains(playerUuid)) {
                return true;
            }
        }
        return false;
    }
}