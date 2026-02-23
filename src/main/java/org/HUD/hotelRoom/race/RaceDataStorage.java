package org.HUD.hotelRoom.race;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

public class RaceDataStorage {
    
    private static Connection conn;
    private static final int MAX_CACHE_SIZE = 1000; // 最大缓存玩家数
    private static final LinkedHashMap<UUID, String> raceCache = new LinkedHashMap<UUID, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, String> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private static final Map<String, RaceConfig> raceConfigCache = new HashMap<>();
    
    // 种族配置数据结构
    public static class RaceConfig {
        public String raceName;
        public String displayName;
        public String description; // 种族描述
        public String icon;      // 种族图标
        public int maxLevel;
        public int evolutionLevel; // 进化所需等级
        public List<String> evolutionTargets; // 可进化的目标种族列表
        
        public RaceConfig(String raceName, String displayName, String description, String icon, int maxLevel, int evolutionLevel, List<String> evolutionTargets) {
            this.raceName = raceName;
            this.displayName = displayName;
            this.description = description;
            this.icon = icon;
            this.maxLevel = maxLevel;
            this.evolutionLevel = evolutionLevel;
            this.evolutionTargets = evolutionTargets != null ? new ArrayList<>(evolutionTargets) : new ArrayList<>();
        }
    }
    
    /**
     * 初始化数据库连接
     */
    public static void initialize() {
        File file = new File(HotelRoom.get().getDataFolder(), "race_data.db");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTables();
            loadRaceConfigs(); // 加载种族配置
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "无法初始化种族数据存储", e);
        }
    }
    
    /**
     * 检查并获取有效的数据库连接
     */
    private static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            // 连接已关闭，重新建立连接
            File file = new File(HotelRoom.get().getDataFolder(), "race_data.db");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            HotelRoom.get().getLogger().info("重新建立了种族数据存储的数据库连接");
        }
        return conn;
    }
    
    /**
     * 创建必要的数据表
     */
    private static void createTables() throws SQLException {
        Statement st = getConnection().createStatement();
        
        // 玩家种族表
        st.execute(
            "CREATE TABLE IF NOT EXISTS player_race (" +
            "uuid TEXT PRIMARY KEY, " +
            "race TEXT NOT NULL, " +
            "level INTEGER DEFAULT 0, " +
            "experience INTEGER DEFAULT 0, " +
            "last_updated INTEGER NOT NULL)"
        );
        
        // 种族数据表（用于缓存种族配置）
        st.execute(
            "CREATE TABLE IF NOT EXISTS race_config (" +
            "race_name TEXT PRIMARY KEY, " +
            "display_name TEXT, " +
            "description TEXT, " +
            "icon TEXT, " +
            "max_level INTEGER, " +
            "evolution_level INTEGER, " +
            "evolution_targets TEXT, " +
            "config_data TEXT)"
        );
        
        st.close();
    }
    
    /**
     * 从种族配置文件加载所有种族配置
     */
    public static void loadRaceConfigs() {
        loadRaceConfigs(false);
    }

    /**
     * 重新加载种族配置（用于 /hr reload）
     */
    public static void reloadRaceConfigs() {
        loadRaceConfigs(true);
    }

    private static void loadRaceConfigs(boolean forceReload) {
        File racesFolder = new File(HotelRoom.get().getDataFolder(), "races");
        if (!racesFolder.exists()) {
            racesFolder.mkdirs();
        }

        if (forceReload) {
            raceConfigCache.clear();
        }

        List<File> raceFiles = new ArrayList<>();
        scanYamlFiles(racesFolder, raceFiles);
        
        if (!raceFiles.isEmpty()) {
            // 收集所有种族配置，然后批量保存
            List<RaceConfig> configsToSave = new ArrayList<>();
            
            for (File file : raceFiles) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                // 从配置文件中读取race-name字段作为raceName，而不是使用文件名
                String raceName = config.getString("race-name", file.getName().replace(".yml", "").replace(".yaml", ""));
                
                String displayName = config.getString("display-name", raceName);
                String description = config.getString("description", "");
                String icon = config.getString("icon", "PAPER");
                int maxLevel = config.getInt("max-level", 100);
                int evolutionLevel = config.getInt("evolution.evolution-level", -1); // -1表示不能进化
                
                List<String> evolutionTargets = new ArrayList<>();
                if (config.contains("evolution.targets")) {
                    evolutionTargets = config.getStringList("evolution.targets");
                }
                
                RaceConfig raceConfig = new RaceConfig(raceName, displayName, description, icon, maxLevel, evolutionLevel, evolutionTargets);
                configsToSave.add(raceConfig);
                
                if (forceReload) {
                    raceConfigCache.put(raceName, raceConfig);
                } else if (!raceConfigCache.containsKey(raceName)) {
                    raceConfigCache.put(raceName, raceConfig);
                }
            }
            
            // 批量保存所有种族配置
            saveRaceConfigsBatch(configsToSave);
        }
    }
    
    /**
     * 批量保存种族配置
     */
    private static void saveRaceConfigsBatch(List<RaceConfig> configs) {
        if (configs.isEmpty()) {
            return;
        }
        
        String sql = "INSERT OR REPLACE INTO race_config (race_name, display_name, description, icon, max_level, evolution_level, evolution_targets, config_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            
            // 开始事务以提高性能
            connection.setAutoCommit(false);
            
            for (RaceConfig config : configs) {
                String targetsJson = String.join(",", config.evolutionTargets);
                ps.setString(1, config.raceName);
                ps.setString(2, config.displayName);
                ps.setString(3, config.description);
                ps.setString(4, config.icon);
                ps.setInt(5, config.maxLevel);
                ps.setInt(6, config.evolutionLevel);
                ps.setString(7, targetsJson);
                ps.setString(8, ""); // config_data暂时为空
                ps.addBatch();
            }
            
            ps.executeBatch();
            connection.commit(); // 提交事务
            connection.setAutoCommit(true);
            
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "批量保存种族配置失败", e);
            try {
                getConnection().rollback(); // 回滚事务
                getConnection().setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                HotelRoom.get().getLogger().log(Level.SEVERE, "回滚事务失败", rollbackEx);
            }
        }
    }
    
    /**
     * 递归扫描文件夹中的所有yml文件
     */
    private static void scanYamlFiles(File folder, List<File> result) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归扫描子文件夹
                    scanYamlFiles(file, result);
                } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                    result.add(file);
                }
            }
        }
    }
    
    /**
     * 保存种族配置信息
     */
    public static void saveRaceConfig(RaceConfig config) {
        String targetsJson = String.join(",", config.evolutionTargets);
        String sql = "INSERT OR REPLACE INTO race_config (race_name, display_name, description, icon, max_level, evolution_level, evolution_targets, config_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, config.raceName);
            ps.setString(2, config.displayName);
            ps.setString(3, config.description);
            ps.setString(4, config.icon);
            ps.setInt(5, config.maxLevel);
            ps.setInt(6, config.evolutionLevel);
            ps.setString(7, targetsJson);
            ps.setString(8, ""); // config_data暂时为空
            ps.executeUpdate();
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "保存种族配置失败: " + config.raceName, e);
        }
    }
    
    /**
     * 获取种族配置
     */
    public static RaceConfig getRaceConfig(String raceName) {
        if (raceConfigCache.containsKey(raceName)) {
            return raceConfigCache.get(raceName);
        }
        
        // 从数据库加载
        String sql = "SELECT * FROM race_config WHERE race_name=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, raceName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String displayName = rs.getString("display_name");
                    String description = rs.getString("description");
                    String icon = rs.getString("icon");
                    int maxLevel = rs.getInt("max_level");
                    int evolutionLevel = rs.getInt("evolution_level");
                    
                    String targetsStr = rs.getString("evolution_targets");
                    List<String> evolutionTargets = new ArrayList<>();
                    if (targetsStr != null && !targetsStr.isEmpty()) {
                        evolutionTargets = Arrays.asList(targetsStr.split(","));
                    }
                    
                    RaceConfig config = new RaceConfig(raceName, displayName, description, icon, maxLevel, evolutionLevel, evolutionTargets);
                    raceConfigCache.put(raceName, config);
                    return config;
                }
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "获取种族配置失败: " + raceName, e);
        }
        
        return null;
    }
    
    /**
     * 获取所有种族配置
     */
    public static List<RaceConfig> getAllRaceConfigs() {
        List<RaceConfig> configs = new ArrayList<>();
        for (String raceName : getAllRaces()) {
            RaceConfig config = getRaceConfig(raceName);
            if (config != null) {
                configs.add(config);
            }
        }
        return configs;
    }
    
    /**
     * 获取玩家的种族
     */
    public static String getPlayerRace(UUID playerUUID) {
        // 先从缓存获取
        if (raceCache.containsKey(playerUUID)) {
            return raceCache.get(playerUUID);
        }
        
        // 从数据库获取
        String sql = "SELECT race FROM player_race WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String race = rs.getString("race");
                    raceCache.put(playerUUID, race);
                    return race;
                }
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "获取玩家种族失败", e);
        }
        
        // 如果没有记录，返回默认种族
        String defaultRace = getDefaultRace();
        setPlayerRace(playerUUID, defaultRace, 0, 0);
        return defaultRace;
    }
    
    /**
     * 设置玩家的种族
     */
    public static void setPlayerRace(UUID playerUUID, String race, int level, int experience) {
        String sql = "INSERT OR REPLACE INTO player_race (uuid, race, level, experience, last_updated) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, race);
            ps.setInt(3, level);
            ps.setInt(4, experience);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
            
            // 更新缓存
            raceCache.put(playerUUID, race);
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "设置玩家种族失败", e);
        }
    }
    
    /**
     * 获取玩家的种族等级
     */
    public static int getPlayerRaceLevel(UUID playerUUID) {
        String sql = "SELECT level FROM player_race WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "获取玩家种族等级失败", e);
        }
        return 0;
    }
    
    /**
     * 获取玩家的种族经验
     */
    public static int getPlayerRaceExperience(UUID playerUUID) {
        String sql = "SELECT experience FROM player_race WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("experience");
                }
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "获取玩家种族经验失败", e);
        }
        return 0;
    }
    
    /**
     * 添加玩家种族经验
     */
    public static void addPlayerExperience(UUID playerUUID, int amount) {
        int currentExp = getPlayerRaceExperience(playerUUID);
        int currentLevel = getPlayerRaceLevel(playerUUID);
        String race = getPlayerRace(playerUUID);
        setPlayerRace(playerUUID, race, currentLevel, currentExp + amount);
    }
    
    /**
     * 获取所有种族列表
     */
    public static List<String> getAllRaces() {
        List<String> races = new ArrayList<>();
        String sql = "SELECT race_name FROM race_config";
        try {
            try (Statement st = getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    races.add(rs.getString("race_name"));
                }
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "获取所有种族列表失败", e);
        }
        
        if (races.isEmpty()) {
            races = getDefaultRaces();
        }
        
        return races;
    }
    
    /**
     * 获取默认种族列表（硬编码备选）
     */
    private static List<String> getDefaultRaces() {
        List<String> defaultRaces = new ArrayList<>();
        File racesFolder = new File(HotelRoom.get().getDataFolder(), "races");
        if (racesFolder.exists() && racesFolder.isDirectory()) {
            File[] files = racesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String raceName = file.getName().replace(".yml", "").replace(".yaml", "");
                    defaultRaces.add(raceName);
                }
            }
        }
        if (defaultRaces.isEmpty()) {
            defaultRaces.add("human");
            defaultRaces.add("elf");
            defaultRaces.add("dwarf");
            defaultRaces.add("orc");
        }
        return defaultRaces;
    }
    
    /**
     * 获取默认种族
     */
    public static String getDefaultRace() {
        File raceConfigFile = new File(HotelRoom.get().getDataFolder(), "race.yml");
        if (raceConfigFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(raceConfigFile);
            return config.getString("default-race", "human");
        }
        return "human";
    }
    
    /**
     * 检查种族是否可以进化
     */
    public static boolean canEvolve(String raceName, int level) {
        RaceConfig config = getRaceConfig(raceName);
        if (config == null) return false;
        
        return config.evolutionLevel > 0 && level >= config.evolutionLevel && !config.evolutionTargets.isEmpty();
    }
    
    /**
     * 获取可进化的种族列表
     */
    public static List<String> getEvolutionTargets(String raceName) {
        RaceConfig config = getRaceConfig(raceName);
        if (config == null) return new ArrayList<>();
        
        return new ArrayList<>(config.evolutionTargets);
    }
    
    /**
     * 关闭数据库连接
     */
    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().log(Level.SEVERE, "关闭数据库连接失败", e);
        }
        
        raceCache.clear();
        raceConfigCache.clear();
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        raceCache.clear();
        raceConfigCache.clear();
    }
}