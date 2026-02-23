package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HomeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "仅玩家可使用此命令。");
            return true;
        }
        HotelInfo owned = null;
        for (HotelInfo hi : SelectionMgr.HOTELS.values()) {
            if (hi.owner.equals(p.getUniqueId())) { owned = hi; break; }
        }
        if (owned == null) {
            p.sendMessage(ChatColor.RED + "你还没有领取任何领地！");
            return true;
        }
        Location c = owned.corners[0].clone().add(owned.corners[1]).multiply(0.5);
        c.setY(c.getWorld().getHighestBlockYAt(c.getBlockX(), c.getBlockZ()) + 1);
        p.teleport(c);
        p.sendMessage(ChatColor.GREEN + "已传送回你的领地 '" + owned.name + "'！");
        return true;
    }
}
