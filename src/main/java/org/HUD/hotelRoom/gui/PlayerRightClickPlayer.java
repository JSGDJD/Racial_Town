package org.HUD.hotelRoom.gui;

import org.HUD.hotelRoom.gui.HonorGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public final class PlayerRightClickPlayer implements Listener {

    @EventHandler
    public void onRightClickPlayer(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;

        Player operator = e.getPlayer();
        //if (!operator.hasPermission("hotelroom.admin")) return; // 仅管理员可操作

        UUID targetUUID = target.getUniqueId();
        HonorGUI.record(operator, targetUUID);
        HonorGUI.open(operator, targetUUID, target.getName());
    }
}
