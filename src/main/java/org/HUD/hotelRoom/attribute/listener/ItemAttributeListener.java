package org.HUD.hotelRoom.attribute.listener;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * 物品属性监听器
 * 监听玩家手持物品和穿戴装备的变化
 */
public class ItemAttributeListener implements Listener {
    
    /**
     * 玩家切换手持物品
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // 延迟1tick更新，确保物品已切换
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("HotelRoom"),
            () -> updatePlayerItemAttributes(player),
            1L
        );
    }
    
    /**
     * 玩家切换副手物品
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        
        // 延迟1tick更新
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("HotelRoom"),
            () -> updatePlayerItemAttributes(player),
            1L
        );
    }
    
    /**
     * 玩家点击背包（可能更换装备）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // 检查是否是装备栏或手持物品的改变
        int slot = event.getSlot();
        if (slot >= 36 && slot <= 40 || event.getSlotType().name().contains("ARMOR")) {
            // 延迟1tick更新
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("HotelRoom"),
                () -> updatePlayerItemAttributes(player),
                1L
            );
        }
    }
    
    /**
     * 玩家丢弃物品
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        
        // 延迟1tick更新
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("HotelRoom"),
            () -> updatePlayerItemAttributes(player),
            1L
        );
    }
    
    /**
     * 玩家捡起物品
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // 延迟2tick更新
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("HotelRoom"),
            () -> updatePlayerItemAttributes(player),
            2L
        );
    }
    
    /**
     * 玩家关闭背包（确保取下装备后属性立即更新）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // 延迟1tick更新，确保所有物品操作已完成
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("HotelRoom"),
            () -> updatePlayerItemAttributes(player),
            1L
        );
    }
    
    /**
     * 更新玩家的物品属性
     */
    private void updatePlayerItemAttributes(Player player) {
        AttributeManager manager = AttributeManager.getInstance();
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        
        // 重新应用种族属性（会自动包含装备属性）
        org.HUD.hotelRoom.race.RaceAttributeManager raceAttrManager = 
            org.HUD.hotelRoom.race.RaceAttributeManager.getInstance();
        
        if (raceAttrManager != null) {
            raceAttrManager.applyRaceAttributes(player);
        }
    }
}
