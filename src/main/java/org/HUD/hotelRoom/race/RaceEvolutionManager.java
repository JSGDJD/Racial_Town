package org.HUD.hotelRoom.race;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 种族进化管理器
 * 负责处理种族进化逻辑
 */
public class RaceEvolutionManager {
    
    private static RaceEvolutionManager instance;
    private final HotelRoom plugin;
    
    private RaceEvolutionManager(HotelRoom plugin) {
        this.plugin = plugin;
    }
    
    public static void initialize(HotelRoom plugin) {
        if (instance == null) {
            instance = new RaceEvolutionManager(plugin);
        }
    }
    
    public static RaceEvolutionManager getInstance() {
        return instance;
    }
    
    /**
     * 检查玩家的种族是否可以进化
     */
    public boolean canPlayerEvolve(Player player) {
        UUID uuid = player.getUniqueId();
        String currentRace = RaceDataStorage.getPlayerRace(uuid);
        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        
        return RaceDataStorage.canEvolve(currentRace, currentLevel);
    }
    
    /**
     * 获取玩家可进化的种族列表
     */
    public List<String> getAvailableEvolutionTargets(Player player) {
        UUID uuid = player.getUniqueId();
        String currentRace = RaceDataStorage.getPlayerRace(uuid);
        
        return RaceDataStorage.getEvolutionTargets(currentRace);
    }
    
    /**
     * 执行种族进化
     */
    public boolean evolve(Player player, String targetRace) {
        if (player == null || targetRace == null) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        String currentRace = RaceDataStorage.getPlayerRace(uuid);
        
        if (currentRace == null) {
            player.sendMessage(ChatColor.RED + "你还没有选择种族！");
            return false;
        }
        
        // 检查是否是自己
        if (currentRace.equalsIgnoreCase(targetRace)) {
            player.sendMessage(ChatColor.RED + "你已经选择了这个种族！");
            return false;
        }
        
        // 检查目标种族是否存在
        RaceDataStorage.RaceConfig targetConfig = RaceDataStorage.getRaceConfig(targetRace);
        if (targetConfig == null) {
            player.sendMessage(ChatColor.RED + "目标种族 " + targetRace + " 不存在！");
            return false;
        }
        
        // 检查是否可以进化到目标种族
        List<String> availableTargets = RaceDataStorage.getEvolutionTargets(currentRace);
        if (availableTargets == null || !availableTargets.contains(targetRace)) {
            player.sendMessage(ChatColor.RED + "你无法从 " + currentRace + " 进化到 " + targetRace + " 种族！");
            return false;
        }
        
        // 获取当前等级
        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        
        // 检查等级是否满足进化要求
        RaceDataStorage.RaceConfig currentConfig = RaceDataStorage.getRaceConfig(currentRace);
        int requiredLevel = currentConfig != null ? currentConfig.evolutionLevel : 20;
        if (currentLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "你的种族等级不够，需要达到 " + requiredLevel + " 级才能进化！");
            return false;
        }
        
        // 执行进化
        RaceDataStorage.setPlayerRace(uuid, targetRace, currentLevel, RaceDataStorage.getPlayerRaceExperience(uuid));

        // 通知玩家
        currentConfig = RaceDataStorage.getRaceConfig(currentRace);
        String currentDisplayName = currentConfig != null ? currentConfig.displayName : currentRace;
        String targetDisplayName = targetConfig.displayName;
        
        player.sendMessage(ChatColor.GOLD + "=== 种族进化 ===");
        player.sendMessage(ChatColor.YELLOW + "你已经从 " + ChatColor.AQUA + currentDisplayName + 
                          ChatColor.YELLOW + " 进化为 " + ChatColor.GREEN + targetDisplayName + ChatColor.YELLOW + "！");
        player.sendMessage(ChatColor.GOLD + "================");
        
        // 重新应用种族属性
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            RaceAttributeManager attrManager = RaceAttributeManager.getInstance();
            if (attrManager != null) {
                attrManager.applyRaceAttributes(player);
            }
        });
        
        return true;
    }
    
    /**
     * 获取种族进化所需等级
     */
    public int getEvolutionRequiredLevel(String raceName) {
        RaceDataStorage.RaceConfig config = RaceDataStorage.getRaceConfig(raceName);
        if (config != null) {
            return config.evolutionLevel;
        }
        return -1; // -1表示不能进化
    }
    
    /**
     * 获取玩家距离进化还差多少等级
     */
    public int getLevelsUntilEvolution(Player player) {
        UUID uuid = player.getUniqueId();
        String currentRace = RaceDataStorage.getPlayerRace(uuid);
        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        
        int requiredLevel = getEvolutionRequiredLevel(currentRace);
        if (requiredLevel <= 0) {
            return -1; // 无法进化
        }
        
        int diff = requiredLevel - currentLevel;
        return Math.max(0, diff); // 如果已经满足等级，返回0
    }
}