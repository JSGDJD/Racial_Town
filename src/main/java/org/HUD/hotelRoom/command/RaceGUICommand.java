package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.gui.RaceEvolutionGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RaceGUICommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查权限
        if (!player.hasPermission("hotelroom.race.gui")) {
            player.sendMessage("§c你没有权限使用此命令！");
            return true;
        }
        
        // 打开种族进化GUI
        RaceEvolutionGUI.open(player);
        
        return true;
    }
}