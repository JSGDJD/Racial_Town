package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CreateCmd implements CommandExecutor {
    private final SelectionMgr sm;

    public CreateCmd(SelectionMgr sm) { this.sm = sm; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "仅玩家可用");
            return true;
        }
        
        // 检查权限：只有管理员可以创建领地
        if (!p.hasPermission("hotelroom.admin")) {
            p.sendMessage(ChatColor.RED + "你没有权限创建领地！");
            return true;
        }
        
        if (!sm.hasBothPoints(p)) {
            p.sendMessage(ChatColor.RED + "请先使用铁斧选择两个点！");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(ChatColor.RED + "用法：/createhotel <领地名称> [system]");
            return true;
        }
        String name = args[0];
        // 第2参数：system = 无主人（系统占位）
        UUID owner = (args.length >= 2 && args[1].equalsIgnoreCase("system"))
                ? new UUID(0, 0)          // 无主人
                : p.getUniqueId();        // 默认自己
        if (sm.createHotel(p, name, owner)) {
            p.sendMessage(ChatColor.GREEN + "酒店领地 '" + name + "' 创建成功！"
                    + (owner.equals(new UUID(0, 0)) ? "（无主人，可领取）" : ""));
        } else {
            p.sendMessage(ChatColor.RED + "创建失败，名称已存在或选区无效！");
        }
        return true;
    }
}
