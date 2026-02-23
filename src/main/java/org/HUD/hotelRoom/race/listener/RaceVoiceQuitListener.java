package org.HUD.hotelRoom.race.listener;

import org.HUD.hotelRoom.race.RaceVoiceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 种族语音退出监听器
 * 处理玩家离线时的清理工作
 */
public class RaceVoiceQuitListener implements Listener {
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        RaceVoiceManager voiceManager = RaceVoiceManager.getInstance();
        if (voiceManager != null) {
            // 玩家离线时自动离开语音频道
            voiceManager.leaveChannel(event.getPlayer().getUniqueId());
        }
    }
}
