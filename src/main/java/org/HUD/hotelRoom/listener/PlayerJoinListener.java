package org.HUD.hotelRoom.listener;

import org.HUD.hotelRoom.util.DailyHonorManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        // 处理暂存的荣誉值
        DailyHonorManager.processStoredHonorOnLogin(playerUuid);
    }
}