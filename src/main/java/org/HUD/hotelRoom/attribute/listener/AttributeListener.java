package org.HUD.hotelRoom.attribute.listener;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 属性系统监听器
 */
public class AttributeListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AttributeManager manager = AttributeManager.getInstance();
        
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        
        // 加载玩家属性
        manager.getPlayerAttribute(player.getUniqueId());
        
        // 应用种族属性（包含装备属性）
        org.HUD.hotelRoom.race.RaceAttributeManager raceManager = 
            org.HUD.hotelRoom.race.RaceAttributeManager.getInstance();
        
        if (raceManager != null) {
            // 延迟1tick应用，确保玩家完全加载
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("HotelRoom"),
                () -> raceManager.applyRaceAttributes(player),
                1L
            );
        } else if (manager.isAutoRefreshOnJoin()) {
            // 如果没有种族系统，则直接刷新
            manager.refreshPlayerAttributes(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        AttributeManager manager = AttributeManager.getInstance();
        
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        
        // 保存玩家属性
        manager.savePlayerAttributes(player.getUniqueId());
        
        // 移除缓存
        manager.removePlayerCache(player.getUniqueId());
    }
}
