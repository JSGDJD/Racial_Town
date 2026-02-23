package org.HUD.hotelRoom.family;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.family.storage.FamilyStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class FamilyManager {
    private static FamilyManager instance;
    private final Plugin plugin;
    private final FileConfiguration config;
    private final FileConfiguration messages;
    private final FamilyStorage storage;
    private final Map<UUID, Family> families;
    private final Map<UUID, FamilyMember> players;
    private final ActivitySystem activitySystem;
    private final UpgradeSystem upgradeSystem;
    private final WarehouseSystem warehouseSystem;
    private final BuffSystem buffSystem;
    private final EconomyHelper economyHelper;
    
    // 邀请管理
    private final Map<UUID, Set<UUID>> invitations; // 玩家UUID -> 家族UUID集合
    // 申请管理
    private final Map<UUID, UUID> applications; // 玩家UUID -> 家族UUID
    // 家族名称索引，用于快速查找家族
    private final Map<String, UUID> familyNames; // 家族名称 -> 家族UUID
    // 家族成员索引，用于快速获取家族成员列表
    private final Map<UUID, List<UUID>> familyMembers; // 家族UUID -> 成员UUID列表

    private FamilyManager(Plugin plugin) {
        this.plugin = plugin;
        this.families = new HashMap<>();
        this.players = new HashMap<>();
        this.invitations = new HashMap<>();
        this.applications = new HashMap<>();
        this.familyNames = new HashMap<>();
        this.familyMembers = new HashMap<>();

        // 加载配置文件
        File configFile = new File(plugin.getDataFolder(), "Family.yml");
        if (!configFile.exists()) {
            plugin.saveResource("Family.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // 加载消息文件
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // 初始化存储系统（需要先加载配置）
        this.storage = new FamilyStorage(plugin, config);
        this.activitySystem = new ActivitySystem(plugin);
        this.upgradeSystem = new UpgradeSystem(plugin);
        this.warehouseSystem = new WarehouseSystem(plugin, config, this.storage);
        this.buffSystem = new BuffSystem(plugin);
        this.economyHelper = new EconomyHelper(plugin);

        // 加载数据
        loadData();
    }

    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new FamilyManager(plugin);
        }
    }

    public static FamilyManager getInstance() {
        return instance;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FamilyStorage getStorage() {
        return storage;
    }

    public ActivitySystem getActivitySystem() {
        return activitySystem;
    }

    public UpgradeSystem getUpgradeSystem() {
        return upgradeSystem;
    }

    public WarehouseSystem getWarehouseSystem() {
        return warehouseSystem;
    }

    public BuffSystem getBuffSystem() {
        return buffSystem;
    }

    public synchronized void loadData() {
        // 清空现有数据
        families.clear();
        players.clear();
        familyNames.clear();
        familyMembers.clear();
        
        // 从数据库加载数据
        storage.loadFamilies(families, players);
        
        // 构建家族名称索引和家族成员索引
        for (Family family : families.values()) {
            familyNames.put(family.getName().toLowerCase(), family.getId());
            familyMembers.put(family.getId(), new ArrayList<>(family.getMemberIds()));
        }
    }

    public synchronized void saveData() {
        try {
            long startTime = System.currentTimeMillis();
            storage.saveFamilies(families.values());
            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("Saved " + families.size() + " families in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save families: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void reload() {
        plugin.getLogger().info("Reloading family system configuration and data...");
        
        // 重新加载配置文件
        File configFile = new File(plugin.getDataFolder(), "Family.yml");
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        try {
            config.load(configFile);
            messages.load(messagesFile);
            plugin.getLogger().info("Successfully reloaded configuration files.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to reload configuration files: " + e.getMessage());
            e.printStackTrace();
            return;
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().severe("Invalid configuration format: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 重新加载数据
        try {
            long startTime = System.currentTimeMillis();
            loadData();
            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("Successfully reloaded data in " + (endTime - startTime) + "ms. Loaded " + families.size() + " families and " + players.size() + " members.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload data: " + e.getMessage());
            e.printStackTrace();
        }
        
        plugin.getLogger().info("Family system reload complete.");
    }

    public Family getFamily(UUID familyId) {
        return families.get(familyId);
    }

    public Family getFamilyByName(String name) {
        UUID familyId = familyNames.get(name.toLowerCase());
        return familyId != null ? families.get(familyId) : null;
    }

    public FamilyMember getMember(UUID playerId) {
        return players.get(playerId);
    }

    public FamilyMember getMember(Player player) {
        return getMember(player.getUniqueId());
    }
    
    /**
     * 获取玩家所属的家族
     */
    public Family getPlayerFamily(Player player) {
        return getPlayerFamily(player.getUniqueId());
    }
    
    /**
     * 获取玩家所属的家族
     */
    public Family getPlayerFamily(UUID playerId) {
        FamilyMember member = getMember(playerId);
        if (member == null) {
            return null;
        }
        return getFamily(member.getFamilyId());
    }
    
    /**
     * 发送家族邀请
     */
    public boolean sendInvitation(UUID familyId, UUID playerId) {
        // 参数验证
        if (familyId == null) {
            plugin.getLogger().warning("sendInvitation: familyId is null");
            return false;
        }
        
        if (playerId == null) {
            plugin.getLogger().warning("sendInvitation: playerId is null");
            return false;
        }
        
        Family family = getFamily(familyId);
        if (family == null) {
            plugin.getLogger().warning("sendInvitation: family not found: " + familyId);
            return false;
        }
        
        // 检查玩家是否已经属于一个家族
        if (getPlayerFamily(playerId) != null) {
            plugin.getLogger().warning("sendInvitation: player already in a family: " + playerId);
            return false;
        }
        
        // 检查玩家是否已经被邀请
        Set<UUID> familyInvites = invitations.computeIfAbsent(playerId, k -> new HashSet<>());
        if (familyInvites.contains(familyId)) {
            plugin.getLogger().warning("sendInvitation: player already invited: " + playerId);
            return false;
        }
        
        familyInvites.add(familyId);
        
        return true;
    }
    
    /**
     * 接受家族邀请
     */
    public boolean acceptInvitation(UUID familyId, UUID playerId) {
        Family family = getFamily(familyId);
        if (family == null) {
            return false;
        }
        
        // 检查邀请是否存在
        Set<UUID> familyInvites = invitations.get(playerId);
        if (familyInvites == null || !familyInvites.contains(familyId)) {
            return false;
        }
        
        // 添加玩家到家族
        boolean success = addMember(family, Bukkit.getPlayer(playerId), "member");
        if (success) {
            // 移除邀请
            familyInvites.remove(familyId);
            if (familyInvites.isEmpty()) {
                invitations.remove(playerId);
            }
        }
        
        return success;
    }
    
    /**
     * 检查玩家是否有邀请
     */
    public Set<UUID> getInvitations(UUID playerId) {
        return invitations.getOrDefault(playerId, new HashSet<>());
    }
    
    /**
     * 移除邀请
     */
    public void removeInvitation(UUID playerId, UUID familyId) {
        Set<UUID> familyInvites = invitations.get(playerId);
        if (familyInvites != null) {
            familyInvites.remove(familyId);
            if (familyInvites.isEmpty()) {
                invitations.remove(playerId);
            }
        }
    }
    
    /**
     * 发送加入申请
     */
    public boolean sendApplication(UUID playerId, UUID familyId) {
        Family family = getFamily(familyId);
        if (family == null) {
            return false;
        }
        
        // 检查玩家是否已经属于一个家族
        if (getPlayerFamily(playerId) != null) {
            return false;
        }
        
        // 不管是否已经申请过，都更新为新的家族申请
        applications.put(playerId, familyId);
        return true;
    }
    
    /**
     * 接受加入申请
     */
    public boolean acceptApplication(UUID playerId, UUID familyId) {
        Family family = getFamily(familyId);
        if (family == null) {
            return false;
        }
        
        // 检查申请是否存在
        if (!applications.containsKey(playerId) || !applications.get(playerId).equals(familyId)) {
            return false;
        }
        
        // 检查玩家是否已经属于一个家族
        if (getPlayerFamily(playerId) != null) {
            // 玩家已经加入了其他家族，拒绝所有申请
            applications.remove(playerId);
            return false;
        }
        
        // 添加玩家到家族
        Player player = Bukkit.getPlayer(playerId);
        boolean success = player != null && addMember(family, player, "member");
        
        if (success) {
            // 移除该玩家在其他家族的所有申请
            applications.remove(playerId);
            
            // 同时移除该玩家的所有邀请
            if (invitations.containsKey(playerId)) {
                invitations.remove(playerId);
            }
        }
        
        return success;
    }
    
    /**
     * 拒绝加入申请
     */
    public boolean rejectApplication(UUID playerId) {
        return applications.remove(playerId) != null;
    }
    
    /**
     * 获取玩家的申请家族
     */
    public UUID getPlayerApplication(UUID playerId) {
        return applications.get(playerId);
    }
    
    /**
     * 获取家族收到的所有申请
     */
    public List<UUID> getFamilyApplications(UUID familyId) {
        List<UUID> playerIds = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : applications.entrySet()) {
            if (entry.getValue().equals(familyId)) {
                playerIds.add(entry.getKey());
            }
        }
        return playerIds;
    }
    
    /**
     * 获取玩家的邀请列表
     */
    public List<Family> getPlayerInvitations(UUID playerId) {
        List<Family> invitedFamilies = new ArrayList<>();
        Set<UUID> familyIds = getInvitations(playerId);
        
        for (UUID familyId : familyIds) {
            Family family = getFamily(familyId);
            if (family != null) {
                invitedFamilies.add(family);
            }
        }
        
        return invitedFamilies;
    }

    public synchronized Family createFamily(Player player, String name) {
        // 参数验证
        if (player == null) {
            plugin.getLogger().warning("createFamily: player is null");
            return null;
        }
        
        if (name == null || name.trim().isEmpty()) {
            plugin.getLogger().warning("createFamily: family name is null or empty");
            return null;
        }
        
        // 家族名称长度检查
        name = name.trim();
        if (name.length() < 2 || name.length() > 20) {
            plugin.getLogger().warning("createFamily: family name length must be between 2 and 20 characters");
            return null;
        }
        
        // 检查玩家是否已经有家族
        if (players.containsKey(player.getUniqueId())) {
            player.sendMessage(org.bukkit.ChatColor.RED + "你已经属于一个家族，不能创建新的家族！");
            return null;
        }
        
        // 检查家族名称是否已存在
        if (getFamilyByName(name) != null) {
            plugin.getLogger().warning("createFamily: family name already exists: " + name);
            return null;
        }
        
        // 检查创建条件
        FileConfiguration config = getConfig();
        
        // 检查房屋要求
        if (config.getBoolean("create-requirements.require-house", false)) {
            if (!playerHasHouse(player)) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你必须拥有房屋才能创建家族！");
                return null;
            }
            
            int minArea = config.getInt("create-requirements.house-min-area", 100);
            if (!playerHouseMeetsMinArea(player, minArea)) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你的房屋面积不足，至少需要 " + minArea + " 方块！");
                return null;
            }
        }
        
        // 检查种族等级要求
        int raceLevelReq = config.getInt("create-requirements.race-level", 0);
        if (raceLevelReq > 0) {
            int playerRaceLevel = getPlayerRaceLevel(player);
            if (playerRaceLevel < raceLevelReq) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你的种族等级不足，至少需要达到 " + raceLevelReq + " 级！");
                return null;
            }
        }
        
        // 检查荣誉值要求
        int honorReq = config.getInt("create-requirements.honor", 0);
        if (honorReq > 0) {
            int playerHonor = getPlayerHonor(player);
            if (playerHonor < honorReq) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你的荣誉值不足，至少需要 " + honorReq + " 点！");
                return null;
            }
        }
        
        // 检查经验等级要求
        int expLevelReq = config.getInt("create-requirements.exp-level", 0);
        if (expLevelReq > 0) {
            if (player.getLevel() < expLevelReq) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你的经验值等级不足，至少需要 " + expLevelReq + " 级！");
                return null;
            }
        }
        
        // 检查游戏币（无论是否OP，经济条件必须达标）
        int moneyReq = config.getInt("create-requirements.money", 0);
        if (moneyReq > 0) {
            if (!economyHelper.has(player, "gold", moneyReq)) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你的游戏币不足，至少需要 " + moneyReq + " 金币！");
                return null;
            }
            economyHelper.takeMoney(player, "gold", moneyReq);
        }
        
        // 扣除物品（OP玩家无视物品要求）
        List<String> itemsReq = config.getStringList("create-requirements.items");
        if (!itemsReq.isEmpty() && !player.isOp()) {
            if (!checkPlayerItems(player, itemsReq)) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你缺少创建家族所需的物品！");
                return null;
            }
            deductPlayerItems(player, itemsReq);
        }
        
        // 检查创建冷却
        long cooldownHours = config.getLong("create-requirements.cooldown", 0);
        if (cooldownHours > 0 && !player.isOp()) {
            if (isPlayerOnCooldown(player.getUniqueId(), cooldownHours)) {
                player.sendMessage(org.bukkit.ChatColor.RED + "你还在创建家族的冷却期内，还需等待一段时间！");
                return null;
            }
            recordPlayerCreation(player.getUniqueId());
        }
        
        // 创建家族
        Family family = new Family(UUID.randomUUID(), name, player.getUniqueId(), 1);
        FamilyMember member = new FamilyMember(player.getUniqueId(), family.getId(), "leader");
        
        families.put(family.getId(), family);
        familyNames.put(name.toLowerCase(), family.getId());
        players.put(player.getUniqueId(), member);
        
        storage.saveFamily(family);
        storage.saveMember(member);
        
        // 添加调试日志
        plugin.getLogger().info("Created family " + name + " for player " + player.getName() + ", position: " + member.getPosition());
        plugin.getLogger().info("Player member cached with position: " + players.get(player.getUniqueId()).getPosition());
        
        return family;
    }
    
    // 用于记录玩家创建家族的时间，以支持冷却时间检查
    private final Map<UUID, Long> creationTimestamps = new HashMap<>();
    
    private void deductPlayerItems(Player player, List<String> itemsReq) {
        for (String itemStr : itemsReq) {
            try {
                if (itemStr.startsWith("mm:")) {
                    String[] parts = itemStr.substring(3).split(":");
                    if (parts.length >= 2) {
                        String mmId = parts[0];
                        int amount = Integer.parseInt(parts[1]);
                        // 使用反射调用MythicMobs API扣除玩家物品
                        deductMythicMobsItem(player, mmId, amount);
                    }
                } else {
                    String[] parts = itemStr.split(":");
                    if (parts.length >= 2) {
                        Material material = Material.valueOf(parts[0]);
                        int amount = Integer.parseInt(parts[1]);
                        
                        // 扣除普通物品
                        ItemStack itemToRemove = new ItemStack(material, amount);
                        player.getInventory().removeItem(itemToRemove);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deduct item: " + itemStr);
                continue;
            }
        }
    }
    
    /**
     * 检查玩家是否拥有房屋
     */
    private boolean playerHasHouse(Player player) {
        // 这里需要与房屋系统集成
        // 暂时返回true，实际实现时需要检查玩家是否拥有房屋
        try {
            Class<?> hotelRoomClass = Class.forName("org.HUD.hotelRoom.HotelRoom");
            Object hotelRoomInstance = hotelRoomClass.getMethod("get").invoke(null);
            Object houseList = hotelRoomClass.getMethod("getHouseList").invoke(hotelRoomInstance);
            
            if (houseList != null) {
                // 使用反射调用HouseList的hasHouse方法检查玩家是否有房屋
                java.lang.reflect.Method hasHouseMethod = houseList.getClass().getMethod("hasHouse", java.util.UUID.class);
                return (Boolean) hasHouseMethod.invoke(houseList, player.getUniqueId());
            }
        } catch (Exception e) {
            // 如果无法检查房屋，返回true（默认允许）
            plugin.getLogger().warning("无法检查玩家房屋信息: " + e.getMessage());
        }
        return true; // 默认允许
    }
    
    /**
     * 检查玩家房屋是否达到最小面积
     */
    private boolean playerHouseMeetsMinArea(Player player, int minArea) {
        // 这里需要与房屋系统集成
        // 暂时返回true，实际实现时需要检查房屋面积
        try {
            Class<?> hotelRoomClass = Class.forName("org.HUD.hotelRoom.HotelRoom");
            Object hotelRoomInstance = hotelRoomClass.getMethod("get").invoke(null);
            Object houseList = hotelRoomClass.getMethod("getHouseList").invoke(hotelRoomInstance);
            
            if (houseList != null) {
                // 使用反射调用HouseList的getHouseArea方法检查房屋面积
                java.lang.reflect.Method getHouseAreaMethod = houseList.getClass().getMethod("getHouseArea", java.util.UUID.class);
                int area = (Integer) getHouseAreaMethod.invoke(houseList, player.getUniqueId());
                return area >= minArea;
            }
        } catch (Exception e) {
            // 如果无法检查房屋面积，返回true（默认允许）
            plugin.getLogger().warning("无法检查玩家房屋面积: " + e.getMessage());
        }
        return true; // 默认允许
    }
    
    /**
     * 获取玩家种族等级
     */
    private int getPlayerRaceLevel(Player player) {
        try {
            // 尝试通过PAPI变量获取种族等级
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
            String value = (String) setPlaceholdersMethod.invoke(null, player, "%hr_race_level%");
            return Integer.parseInt(value);
        } catch (Exception e) {
            // 如果PAPI不可用，尝试直接访问种族系统
            try {
                Class<?> raceExpManagerClass = Class.forName("org.HUD.hotelRoom.race.RaceExpManager");
                Object raceExpManager = raceExpManagerClass.getMethod("getInstance").invoke(null);
                if (raceExpManager != null) {
                    java.lang.reflect.Method getLevelMethod = raceExpManagerClass.getMethod("getLevel", org.bukkit.entity.Player.class);
                    return (Integer) getLevelMethod.invoke(raceExpManager, player);
                }
            } catch (Exception ex) {
                // 如果无法获取种族等级，返回0
                plugin.getLogger().warning("无法获取玩家种族等级: " + ex.getMessage());
            }
        }
        return 0; // 默认等级
    }
    
    /**
     * 获取玩家荣誉值
     */
    private int getPlayerHonor(Player player) {
        try {
            // 尝试通过PAPI变量获取荣誉值
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
            String value = (String) setPlaceholdersMethod.invoke(null, player, "%hr_honor%");
            return Integer.parseInt(value);
        } catch (Exception e) {
            // 如果PAPI不可用，尝试直接访问荣誉系统
            // 这里可以根据实际情况实现荣誉值获取逻辑
            plugin.getLogger().warning("无法获取玩家荣誉值: " + e.getMessage());
        }
        return 0; // 默认荣誉值
    }
    
    /**
     * 检查玩家是否在创建家族的冷却期内
     */
    private boolean isPlayerOnCooldown(UUID playerId, long cooldownHours) {
        Long lastCreation = creationTimestamps.get(playerId);
        if (lastCreation == null) {
            return false; // 没有创建记录，不在冷却期
        }
        
        long cooldownMs = cooldownHours * 60 * 60 * 1000; // 转换为毫秒
        return System.currentTimeMillis() - lastCreation < cooldownMs;
    }
    
    /**
     * 记录玩家创建家族的时间
     */
    private void recordPlayerCreation(UUID playerId) {
        creationTimestamps.put(playerId, System.currentTimeMillis());
    }

    public synchronized boolean deleteFamily(UUID familyId) {
        Family family = families.remove(familyId);
        if (family == null) {
            return false;
        }
        
        // 从名称索引中移除
        familyNames.remove(family.getName().toLowerCase());

        // 移除所有成员
        for (UUID memberId : family.getMemberIds()) {
            players.remove(memberId);
            storage.deleteMember(memberId);
        }

        // 移除仓库数据
        warehouseSystem.deleteWarehouse(familyId);
        
        // 删除家族
        storage.deleteFamily(familyId);
        return true;
    }
    
    /** 修改家族名称 */
    public synchronized boolean renameFamily(UUID familyId, String newName) {
        // 参数验证
        if (familyId == null) {
            plugin.getLogger().warning("renameFamily: familyId is null");
            return false;
        }
        
        if (newName == null || newName.trim().isEmpty()) {
            plugin.getLogger().warning("renameFamily: newName is null or empty");
            return false;
        }
        
        // 家族名称长度检查
        newName = newName.trim();
        if (newName.length() < 2 || newName.length() > 20) {
            plugin.getLogger().warning("renameFamily: newName length must be between 2 and 20 characters");
            return false;
        }
        
        // 检查新名称是否已存在
        Family existingFamily = getFamilyByName(newName);
        if (existingFamily != null && !existingFamily.getId().equals(familyId)) {
            plugin.getLogger().warning("renameFamily: newName already exists: " + newName);
            return false;
        }
        
        Family family = families.get(familyId);
        if (family != null) {
            // 从名称索引中移除旧名称
            familyNames.remove(family.getName().toLowerCase());
            
            // 更新家族名称
            family.setName(newName);
            
            // 在名称索引中添加新名称
            familyNames.put(newName.toLowerCase(), familyId);
            
            storage.saveFamily(family);
            return true;
        }
        
        plugin.getLogger().warning("renameFamily: family not found: " + familyId);
        return false;
    }

    public synchronized boolean addMember(Family family, Player player, String position) {
        // 参数验证
        if (family == null) {
            plugin.getLogger().warning("addMember: family is null");
            return false;
        }
        
        if (player == null) {
            plugin.getLogger().warning("addMember: player is null");
            return false;
        }
        
        if (position == null || position.trim().isEmpty()) {
            plugin.getLogger().warning("addMember: position is null or empty");
            return false;
        }
        
        // 检查玩家是否已加入其他家族
        if (players.containsKey(player.getUniqueId())) {
            plugin.getLogger().warning("addMember: player already in a family: " + player.getName());
            return false;
        }

        FamilyMember member = new FamilyMember(player.getUniqueId(), family.getId(), position);
        family.addMember(player.getUniqueId());
        players.put(player.getUniqueId(), member);
        
        // 更新家族成员索引
        List<UUID> memberIds = familyMembers.computeIfAbsent(family.getId(), k -> new ArrayList<>());
        if (!memberIds.contains(player.getUniqueId())) {
            memberIds.add(player.getUniqueId());
        }
        
        storage.saveFamily(family);
        storage.saveMember(member);
        
        return true;
    }

    public synchronized boolean removeMember(Family family, UUID playerId) {
        // 参数验证
        if (family == null) {
            plugin.getLogger().warning("removeMember: family is null");
            return false;
        }
        
        if (playerId == null) {
            plugin.getLogger().warning("removeMember: playerId is null");
            return false;
        }
        
        FamilyMember member = players.remove(playerId);
        if (member == null) {
            plugin.getLogger().warning("removeMember: member not found: " + playerId);
            return false;
        }
        
        // 检查玩家是否属于该家族
        if (!member.getFamilyId().equals(family.getId())) {
            plugin.getLogger().warning("removeMember: player does not belong to this family: " + playerId);
            return false;
        }

        family.removeMember(playerId);
        
        // 更新家族成员索引
        List<UUID> memberIds = familyMembers.get(family.getId());
        if (memberIds != null) {
            memberIds.remove(playerId);
        }
        
        storage.saveFamily(family);
        storage.deleteMember(playerId);
        
        return true;
    }

    public boolean changePosition(Family family, UUID playerId, String newPosition) {
        FamilyMember member = players.get(playerId);
        if (member == null || !family.getId().equals(member.getFamilyId())) {
            return false;
        }

        member.setPosition(newPosition);
        storage.saveMember(member);
        return true;
    }

    public boolean canCreateFamily(Player player) {
        // 检查是否已加入家族
        if (players.containsKey(player.getUniqueId())) {
            return false;
        }

        // 检查配置的创建条件
        FileConfiguration config = getConfig();
        
        // 检查游戏币（无论是否OP，经济条件必须达标）
        int moneyReq = config.getInt("create-requirements.money", 10000);
        if (moneyReq > 0) {
            if (!economyHelper.has(player, "gold", moneyReq)) {
                return false;
            }
        }
        
        // 如果玩家有OP权限，无视其他条件
        if (player.isOp()) {
            return true;
        }
        
        // 检查物品
        List<String> itemsReq = config.getStringList("create-requirements.items");
        if (!itemsReq.isEmpty()) {
            if (!checkPlayerItems(player, itemsReq)) {
                return false;
            }
        }
        
        // 其他条件检查（房屋、种族等级、荣誉值、经验等级等）
        // 这些条件将在后续实现
        
        return true;
    }
    
    private boolean checkPlayerItems(Player player, List<String> itemsReq) {
        for (String itemStr : itemsReq) {
            try {
                boolean hasItem = false;
                
                // 检查是否是MM物品（格式：mm:物品ID:数量）
                if (itemStr.startsWith("mm:")) {
                    String[] parts = itemStr.substring(3).split(":");
                    if (parts.length >= 2) {
                        String mmId = parts[0];
                        int amount = Integer.parseInt(parts[1]);
                        // 使用反射调用MythicMobs API检查玩家是否有该物品
                        hasItem = checkMythicMobsItem(player, mmId, amount);
                    }
                } else {
                    // 普通物品（格式：MATERIAL:数量）
                    String[] parts = itemStr.split(":");
                    if (parts.length >= 2) {
                        Material material = Material.valueOf(parts[0]);
                        int amount = Integer.parseInt(parts[1]);
                        
                        // 检查玩家背包中是否有足够的普通物品
                        int count = 0;
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() == material) {
                                count += item.getAmount();
                                if (count >= amount) {
                                    break;
                                }
                            }
                        }
                        hasItem = count >= amount;
                    }
                }
                
                if (!hasItem) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid item format: " + itemStr);
                continue;
            }
        }
        return true;
    }
    
    public Collection<Family> getAllFamilies() {
        return families.values();
    }
    
    public Collection<FamilyMember> getAllMembers() {
        return players.values();
    }
    
    /** 获取指定家族的所有成员 */
    public List<FamilyMember> getAllMembers(UUID familyId) {
        List<FamilyMember> members = new ArrayList<>();
        List<UUID> memberIds = familyMembers.get(familyId);
        if (memberIds != null) {
            for (UUID memberId : memberIds) {
                FamilyMember member = players.get(memberId);
                if (member != null) {
                    members.add(member);
                }
            }
        }
        return members;
    }
    
    /**
     * 使用反射检查玩家是否有足够数量的MythicMobs物品
     */
    private boolean checkMythicMobsItem(Player player, String mmId, int amount) {
        try {
            // 获取MythicMobs主类
            Class<?> mythicMobsClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythicMobsInstance = mythicMobsClass.getMethod("getInstance").invoke(null);
            
            // 获取物品管理器
            Object itemManager = mythicMobsClass.getMethod("getItemManager").invoke(mythicMobsInstance);
            
            // 检查物品是否存在
            Method getItemMethod = itemManager.getClass().getMethod("getItem", String.class);
            Object mythicItem = getItemMethod.invoke(itemManager, mmId);
            if (mythicItem == null) {
                plugin.getLogger().warning("MythicMobs item not found: " + mmId);
                return false;
            }
            
            // 检查玩家背包中的物品数量
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) {
                    // 检查物品是否是MythicMobs物品
                    Method isMythicItemMethod = itemManager.getClass().getMethod("isMythicItem", ItemStack.class);
                    if ((boolean) isMythicItemMethod.invoke(itemManager, item)) {
                        // 获取物品的MythicMobs ID
                        Method getMythicTypeMethod = itemManager.getClass().getMethod("getMythicType", ItemStack.class);
                        String itemId = (String) getMythicTypeMethod.invoke(itemManager, item);
                        if (itemId.equalsIgnoreCase(mmId)) {
                            count += item.getAmount();
                            if (count >= amount) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            return count >= amount;
        } catch (ClassNotFoundException e) {
            // MythicMobs未安装
            plugin.getLogger().warning("MythicMobs plugin not found.");
            return false;
        } catch (Exception e) {
            // 其他错误
            plugin.getLogger().warning("Error checking MythicMobs item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 使用反射扣除玩家的MythicMobs物品
     */
    private void deductMythicMobsItem(Player player, String mmId, int amount) {
        try {
            // 获取MythicMobs主类
            Class<?> mythicMobsClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythicMobsInstance = mythicMobsClass.getMethod("getInstance").invoke(null);
            
            // 获取物品管理器
            Object itemManager = mythicMobsClass.getMethod("getItemManager").invoke(mythicMobsInstance);
            
            // 检查物品是否存在
            Method getItemMethod = itemManager.getClass().getMethod("getItem", String.class);
            Object mythicItem = getItemMethod.invoke(itemManager, mmId);
            if (mythicItem == null) {
                plugin.getLogger().warning("MythicMobs item not found: " + mmId);
                return;
            }
            
            // 扣除玩家背包中的物品
            int remaining = amount;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && remaining > 0) {
                    // 检查物品是否是MythicMobs物品
                    Method isMythicItemMethod = itemManager.getClass().getMethod("isMythicItem", ItemStack.class);
                    if ((boolean) isMythicItemMethod.invoke(itemManager, item)) {
                        // 获取物品的MythicMobs ID
                        Method getMythicTypeMethod = itemManager.getClass().getMethod("getMythicType", ItemStack.class);
                        String itemId = (String) getMythicTypeMethod.invoke(itemManager, item);
                        if (itemId.equalsIgnoreCase(mmId)) {
                            int itemAmount = item.getAmount();
                            if (itemAmount <= remaining) {
                                player.getInventory().remove(item);
                                remaining -= itemAmount;
                            } else {
                                item.setAmount(itemAmount - remaining);
                                remaining = 0;
                            }
                        }
                    }
                }
            }
            
            // 更新玩家背包
            player.updateInventory();
        } catch (ClassNotFoundException e) {
            // MythicMobs未安装
            plugin.getLogger().warning("MythicMobs plugin not found.");
        } catch (Exception e) {
            // 其他错误
            plugin.getLogger().warning("Error deducting MythicMobs item: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 本地经济API接口，模拟外部CustomMoney的EconomyAPI
     */
    /**
     * 经济API代理处理器，用于动态调用外部经济插件API
     */
    private class EconomyApiInvocationHandler implements InvocationHandler {
        private final Object economyApiInstance;
        
        public EconomyApiInvocationHandler(Object economyApiInstance) {
            this.economyApiInstance = economyApiInstance;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 特殊处理has方法，转换为getBalance检查
            if (method.getName().equals("has")) {
                Method getBalanceMethod = economyApiInstance.getClass().getMethod("getBalance", Player.class, String.class);
                int balance = (int) getBalanceMethod.invoke(economyApiInstance, args[0], args[1]);
                return balance >= (int) args[2];
            }
            
            // 特殊处理takeMoney方法，假设存在withdraw或reduce方法
            if (method.getName().equals("takeMoney")) {
                try {
                    // 尝试使用withdraw方法（假设存在）
                    Method withdrawMethod = economyApiInstance.getClass().getMethod("withdrawMoney", Player.class, String.class, int.class);
                    return withdrawMethod.invoke(economyApiInstance, args[0], args[1], args[2]);
                } catch (NoSuchMethodException e) {
                    try {
                        // 尝试使用reduce方法（假设存在）
                        Method reduceMethod = economyApiInstance.getClass().getMethod("reduceBalance", Player.class, String.class, int.class);
                        return reduceMethod.invoke(economyApiInstance, args[0], args[1], args[2]);
                    } catch (NoSuchMethodException ex) {
                        // 尝试使用setBalance方法手动实现扣除
                        Method getBalanceMethod = economyApiInstance.getClass().getMethod("getBalance", Player.class, String.class);
                        Method setBalanceMethod = economyApiInstance.getClass().getMethod("setBalance", Player.class, String.class, int.class);
                        
                        int currentBalance = (int) getBalanceMethod.invoke(economyApiInstance, args[0], args[1]);
                        int newBalance = currentBalance - (int) args[2];
                        
                        if (newBalance < 0) {
                            return false;
                        }
                        
                        setBalanceMethod.invoke(economyApiInstance, args[0], args[1], newBalance);
                        return true;
                    }
                }
            }
            
            // 其他方法直接调用
            Method externalMethod = economyApiInstance.getClass().getMethod(method.getName(), method.getParameterTypes());
            return externalMethod.invoke(economyApiInstance, args);
        }
    }
    
    /**
     * 经济帮助类，使用动态代理直接调用外部经济插件API
     */
    private class EconomyHelper {
        private final Plugin plugin;
        private Object economyApi;
        
        public EconomyHelper(Plugin plugin) {
            this.plugin = plugin;
            initializeEconomyAPI();
        }
        
        private void initializeEconomyAPI() {
            try {
                // 直接创建EconomyAPI实例（如果可能）
                try {
                    Class<?> economyApiClass = Class.forName("org.HUD.customMoney.EconomyAPI");
                    
                    // 尝试获取构造方法
                    try {
                        Constructor<?> constructor = economyApiClass.getConstructor();
                        economyApi = constructor.newInstance();
                    } catch (Exception e) {
                        // 尝试查找所有公共静态方法
                        Method[] methods = economyApiClass.getMethods();
                        for (Method method : methods) {
                            if (method.getReturnType() == economyApiClass && Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                                economyApi = method.invoke(null);
                                break;
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    plugin.getLogger().warning("找不到EconomyAPI类，经济系统功能将不可用");
                    return;
                }
                
                if (economyApi != null) {
                    // 测试API方法是否可用
                    try {
                        Class<?> economyApiClass = economyApi.getClass();
                        Method hasMethod = null;
                        Method takeMoneyMethod = null;
                        
                        // 查找has方法，支持OfflinePlayer和Player
                        for (Method method : economyApiClass.getMethods()) {
                            if (method.getName().equals("has")) {
                                Class<?>[] params = method.getParameterTypes();
                                if (params.length == 3) {
                                    hasMethod = method;
                                    break;
                                }
                            }
                        }
                        
                        // 查找takeMoney方法，支持OfflinePlayer和Player
                        for (Method method : economyApiClass.getMethods()) {
                            if (method.getName().equals("takeMoney")) {
                                Class<?>[] params = method.getParameterTypes();
                                if (params.length == 3) {
                                    takeMoneyMethod = method;
                                    break;
                                }
                            }
                        }
                        
                        if (hasMethod == null || takeMoneyMethod == null) {
                            plugin.getLogger().warning("部分经济API方法未找到，但仍尝试使用");
                        }
                    } catch (Exception e) {
                        // 忽略方法检查失败
                    }
                } else {
                    plugin.getLogger().warning("无法获取EconomyAPI实例，经济系统功能将不可用");
                }
                
            } catch (Exception e) {
                economyApi = null;
            }
        }
        
        /**
         * 检查玩家是否有足够的特定货币
         */
        public boolean has(Player player, String currencyType, int amount) {
            // 如果经济API未初始化，尝试重新初始化
            if (economyApi == null) {
                initializeEconomyAPI();
            }
            
            if (economyApi == null) {
                return true;
            }
            
            try {
                // 直接调用API的has方法
                Class<?> economyApiClass = economyApi.getClass();
                Method hasMethod = economyApiClass.getMethod("has", Player.class, String.class, int.class);
                return (boolean) hasMethod.invoke(economyApi, player, currencyType, amount);
            } catch (Exception e) {
                // 降级方案：使用getBalance方法检查
                try {
                    Class<?> economyApiClass = economyApi.getClass();
                    Method getBalanceMethod = economyApiClass.getMethod("getBalance", Player.class, String.class);
                    int balance = (int) getBalanceMethod.invoke(economyApi, player, currencyType);
                    return balance >= amount;
                } catch (Exception ex) {
                    // 如果经济API不可用，默认返回true（允许操作）
                    return true;
                }
            }
        }
        
        /**
         * 扣除玩家特定货币
         */
        public boolean takeMoney(Player player, String currencyType, int amount) {
            // 如果经济API未初始化，尝试重新初始化
            if (economyApi == null) {
                initializeEconomyAPI();
            }
            
            if (economyApi == null) {
                return true;
            }
            
            try {
                // 直接调用API的takeMoney方法
                Class<?> economyApiClass = economyApi.getClass();
                Method takeMoneyMethod = economyApiClass.getMethod("takeMoney", Player.class, String.class, int.class);
                return (boolean) takeMoneyMethod.invoke(economyApi, player, currencyType, amount);
            } catch (Exception e) {
                // 如果经济API不可用，默认返回true（允许操作）
                return true;
            }
        }
    }
}
