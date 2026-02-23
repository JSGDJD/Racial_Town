package org.HUD.hotelRoom.race.listener;

import org.HUD.hotelRoom.race.RaceExpManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

/**
 * 种族经验监听器
 * 监听玩家行为并给予种族经验
 */
public class RaceExpListener implements Listener {
    
    /**
     * 狩猎经验 - 击杀生物
     */
    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        RaceExpManager expManager = RaceExpManager.getInstance();
        if (expManager == null) return;
        
        if (!expManager.isMethodEnabled("hunting")) return;
        
        int expValue = expManager.getExpValue("hunting", "exp-per-kill");
        if (expValue > 0) {
            expManager.addExperience(killer, expValue, "hunting");
        }
    }
    
    /**
     * 挖矿经验 - 破坏方块
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        RaceExpManager expManager = RaceExpManager.getInstance();
        if (expManager == null) return;
        
        if (!expManager.isMethodEnabled("mining")) return;
        
        // 只有矿石类方块才给经验
        if (isOreBlock(block.getType())) {
            int expValue = expManager.getExpValue("mining", "exp-per-block");
            if (expValue > 0) {
                expManager.addExperience(player, expValue, "mining");
            }
        }
    }
    
    /**
     * 种田经验 - 收获作物
     */
    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        
        RaceExpManager expManager = RaceExpManager.getInstance();
        if (expManager == null) return;
        
        if (!expManager.isMethodEnabled("farming")) return;
        
        int expValue = expManager.getExpValue("farming", "exp-per-harvest");
        if (expValue > 0) {
            expManager.addExperience(player, expValue, "farming");
        }
    }
    
    /**
     * 制作经验 - 合成物品
     */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.isCancelled()) return;
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        RaceExpManager expManager = RaceExpManager.getInstance();
        if (expManager == null) return;
        
        if (!expManager.isMethodEnabled("crafting")) return;
        
        int expValue = expManager.getExpValue("crafting", "exp-per-craft");
        if (expValue > 0) {
            expManager.addExperience(player, expValue, "crafting");
        }
    }
    
    /**
     * 检查是否为矿石方块
     */
    private boolean isOreBlock(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE,
                 ANCIENT_DEBRIS -> true;
            default -> false;
        };
    }
}
