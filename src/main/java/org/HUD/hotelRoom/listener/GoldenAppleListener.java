package org.HUD.hotelRoom.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoldenAppleListener implements Listener {

    private final Map<UUID, Integer> tempHealthTasks = new HashMap<>();
    private final double TEMP_HEALTH_BONUS = 4.0;
    private final int DURATION_TICKS = 2400; // 2分钟 = 2400 ticks

    // 存储玩家的原始最大生命值（即没有临时加成时的最大生命值）
    private final Map<UUID, Double> originalMaxHealthMap = new HashMap<>();

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查是否是金苹果或附魔金苹果
        if (item.getType() == Material.GOLDEN_APPLE || item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            // 取消之前的任务（如果有）
            if (tempHealthTasks.containsKey(player.getUniqueId())) {
                int taskId = tempHealthTasks.get(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(taskId);
                tempHealthTasks.remove(player.getUniqueId());
            }

            // 保存原始最大生命值（如果还没有保存）
            double originalMaxHealth;
            if (!originalMaxHealthMap.containsKey(player.getUniqueId())) {
                originalMaxHealth = player.getMaxHealth();
                originalMaxHealthMap.put(player.getUniqueId(), originalMaxHealth);
            } else {
                originalMaxHealth = originalMaxHealthMap.get(player.getUniqueId());
            }
            
            // 根据苹果类型确定临时生命值加成
            double healthBonus;
            if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                healthBonus = 8.0; // 附魔金苹果是8点血
            } else {
                healthBonus = TEMP_HEALTH_BONUS; // 普通金苹果是4点血
            }
            
            // 保存当前生命值
            double currentHealth = player.getHealth();
            
            // 应用临时生命值加成（基于原始最大生命值）
            double newMaxHealth = originalMaxHealth + healthBonus;
            player.setMaxHealth(newMaxHealth);
            
            // 确保生命值不超过新的最大值
            double newHealth = Math.min(currentHealth + healthBonus, newMaxHealth);
            player.setHealth(newHealth);

            // 安排任务在2分钟后移除临时生命值
            int taskId = Bukkit.getScheduler().runTaskLater(org.HUD.hotelRoom.HotelRoom.get(), () -> {
                // 保存当前生命值
                double tempCurrentHealth = player.getHealth();
                
                // 恢复原始最大生命值
                player.setMaxHealth(originalMaxHealth);
                
                // 确保生命值不超过新的最大值
                double tempNewHealth = Math.min(tempCurrentHealth, originalMaxHealth);
                player.setHealth(tempNewHealth);
                
                // 从任务映射中移除
                tempHealthTasks.remove(player.getUniqueId());
            }, DURATION_TICKS).getTaskId();

            // 存储任务ID，以便后续可能的取消
            tempHealthTasks.put(player.getUniqueId(), taskId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 取消玩家的临时生命值任务（如果有）
        if (tempHealthTasks.containsKey(playerId)) {
            int taskId = tempHealthTasks.get(playerId);
            Bukkit.getScheduler().cancelTask(taskId);
            tempHealthTasks.remove(playerId);
        }
        
        // 从原始最大生命值映射中移除玩家记录
        originalMaxHealthMap.remove(playerId);
    }
}
