package org.HUD.hotelRoom.family.storage;

import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyMember;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class FamilyStorage {
    private final Plugin plugin;
    private final FileConfiguration config;
    private Connection connection;

    public FamilyStorage(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            String dbType = config.getString("database.type", "sqlite");
            
            if (dbType.equalsIgnoreCase("mysql")) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            
            // 创建家族表
            createFamilyTable();
            
            // 创建成员表
            createMemberTable();
            
            // 创建仓库表
            createWarehouseTable();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize family database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** 检查并获取有效的数据库连接 */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // 连接已关闭，重新建立连接
            String dbType = config.getString("database.type", "sqlite");
            
            if (dbType.equalsIgnoreCase("mysql")) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
        }
        return connection;
    }
    
    /** 关闭数据库连接 */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Closed family database connection.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close family database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeSQLite() throws SQLException {
        // 连接到SQLite数据库（本地文件）
        File dbFile = new File(plugin.getDataFolder(), "hotelroom.db");
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        plugin.getLogger().info("Connected to SQLite family database.");
    }

    private void initializeMySQL() throws SQLException {
        // 获取MySQL配置
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "hotelroom");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");
        boolean useSSL = config.getBoolean("database.mysql.useSSL", false);
        
        // 创建MySQL连接URL
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        
        // 连接到MySQL数据库
        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("Connected to MySQL family database: " + database);
    }

    private void createFamilyTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS families (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "leader_id VARCHAR(36) NOT NULL, " +
                "level INT NOT NULL DEFAULT 1, " +
                "honor DOUBLE NOT NULL DEFAULT 0, " +
                "activity DOUBLE NOT NULL DEFAULT 0, " +
                "created_at BIGINT NOT NULL" +
                ");";
        
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    private void createMemberTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS family_members (" +
                "player_id VARCHAR(36) PRIMARY KEY, " +
                "family_id VARCHAR(36) NOT NULL, " +
                "position VARCHAR(50) NOT NULL, " +
                "daily_activity TEXT NOT NULL, " +
                "join_time BIGINT NOT NULL, " +
                "last_login_time BIGINT NOT NULL, " +
                "FOREIGN KEY (family_id) REFERENCES families(id)" +
                ");";
        
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    /** 创建仓库表 */
    private void createWarehouseTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS family_warehouse (" +
                "family_id VARCHAR(36) NOT NULL, " +
                "slot INT NOT NULL, " +
                "item_data BLOB NOT NULL, " +
                "PRIMARY KEY (family_id, slot), " +
                "FOREIGN KEY (family_id) REFERENCES families(id)" +
                ");";
        
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public synchronized void saveFamily(Family family) {
        try {
            String dbType = config.getString("database.type", "sqlite");
            String sql;
            
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = "INSERT INTO families " +
                        "(id, name, leader_id, level, honor, activity, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "name = VALUES(name), leader_id = VALUES(leader_id), " +
                        "level = VALUES(level), honor = VALUES(honor), " +
                        "activity = VALUES(activity), created_at = VALUES(created_at);";
            } else {
                sql = "INSERT OR REPLACE INTO families " +
                        "(id, name, leader_id, level, honor, activity, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);";
            }
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, family.getId().toString());
                pstmt.setString(2, family.getName());
                pstmt.setString(3, family.getLeaderId().toString());
                pstmt.setInt(4, family.getLevel());
                pstmt.setDouble(5, family.getHonor());
                pstmt.setDouble(6, family.getActivity());
                pstmt.setLong(7, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save family: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void saveMember(FamilyMember member) {
        try {
            // 序列化每日活跃值
            StringBuilder dailyActivityBuilder = new StringBuilder();
            for (Map.Entry<String, Double> entry : member.getDailyActivity().entrySet()) {
                if (dailyActivityBuilder.length() > 0) {
                    dailyActivityBuilder.append(",");
                }
                dailyActivityBuilder.append(entry.getKey()).append(":").append(entry.getValue());
            }
            
            String dbType = config.getString("database.type", "sqlite");
            String sql;
            
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = "INSERT INTO family_members " +
                        "(player_id, family_id, position, daily_activity, join_time, last_login_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "family_id = VALUES(family_id), position = VALUES(position), " +
                        "daily_activity = VALUES(daily_activity), join_time = VALUES(join_time), " +
                        "last_login_time = VALUES(last_login_time);";
            } else {
                sql = "INSERT OR REPLACE INTO family_members " +
                        "(player_id, family_id, position, daily_activity, join_time, last_login_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?);";
            }
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, member.getPlayerId().toString());
                pstmt.setString(2, member.getFamilyId().toString());
                pstmt.setString(3, member.getPosition());
                pstmt.setString(4, dailyActivityBuilder.toString());
                pstmt.setLong(5, member.getJoinTime());
                pstmt.setLong(6, member.getLastLoginTime());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save family member: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void loadFamilies(Map<UUID, Family> families, Map<UUID, FamilyMember> members) {
        long startTime = System.currentTimeMillis();
        int familiesLoaded = 0;
        int membersLoaded = 0;
        
        try {
            // 加载所有家族
            String sql = "SELECT * FROM families;";
            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    UUID leaderId = UUID.fromString(rs.getString("leader_id"));
                    int level = rs.getInt("level");
                    double honor = rs.getDouble("honor");
                    double activity = rs.getDouble("activity");
                    
                    Family family = new Family(id, name, leaderId, level);
                    family.setHonor(honor);
                    family.setActivity(activity);
                    
                    families.put(id, family);
                    familiesLoaded++;
                }
            }
            
            plugin.getLogger().info("Loaded " + familiesLoaded + " families from database");
            
            // 加载所有成员
            sql = "SELECT * FROM family_members;";
            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_id"));
                    UUID familyId = UUID.fromString(rs.getString("family_id"));
                    String position = rs.getString("position");
                    String dailyActivityStr = rs.getString("daily_activity");
                    long joinTime = rs.getLong("join_time");
                    long lastLoginTime = rs.getLong("last_login_time");
                    
                    FamilyMember member = new FamilyMember(playerId, familyId, position);
                    member.setJoinTime(joinTime);
                    member.setLastLoginTime(lastLoginTime);
                    
                    // 反序列化每日活跃值
                    if (dailyActivityStr != null && !dailyActivityStr.isEmpty()) {
                        String[] entries = dailyActivityStr.split(",");
                        for (String entry : entries) {
                            String[] parts = entry.split(":");
                            if (parts.length == 2) {
                                try {
                                    member.addDailyActivity(parts[0], Double.parseDouble(parts[1]));
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("Invalid activity value for player " + playerId + ": " + parts[1]);
                                }
                            }
                        }
                    }
                    
                    members.put(playerId, member);
                    membersLoaded++;
                    
                    // 将成员添加到家族
                    Family family = families.get(familyId);
                    if (family != null) {
                        family.addMember(playerId);
                    } else {
                        plugin.getLogger().warning("Member " + playerId + " belongs to non-existent family " + familyId);
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + membersLoaded + " family members from database");
            
            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("Finished loading family data in " + (endTime - startTime) + "ms");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load families: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            plugin.getLogger().severe("Unexpected error loading family data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void deleteFamily(UUID familyId) {
        try {
            // 删除成员
            String memberSql = "DELETE FROM family_members WHERE family_id = ?;";
            try (PreparedStatement pstmt = connection.prepareStatement(memberSql)) {
                pstmt.setString(1, familyId.toString());
                pstmt.executeUpdate();
            }
            
            // 删除仓库
            deleteWarehouse(familyId);
            
            // 删除家族
            String familySql = "DELETE FROM families WHERE id = ?;";
            try (PreparedStatement pstmt = connection.prepareStatement(familySql)) {
                pstmt.setString(1, familyId.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete family: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void deleteMember(UUID playerId) {
        try {
            // 删除成员
            String sql = "DELETE FROM family_members WHERE player_id = ?;";
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, playerId.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete family member: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** 保存仓库物品 */
    public void saveWarehouseItem(UUID familyId, int slot, byte[] itemData) {
        try {
            String dbType = config.getString("database.type", "sqlite");
            String sql;
            
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = "INSERT INTO family_warehouse (family_id, slot, item_data) " +
                      "VALUES (?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE item_data = VALUES(item_data);";
            } else {
                sql = "INSERT OR REPLACE INTO family_warehouse (family_id, slot, item_data) " +
                      "VALUES (?, ?, ?);";
            }
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, familyId.toString());
                pstmt.setInt(2, slot);
                pstmt.setBytes(3, itemData);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save warehouse item: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** 加载仓库物品 */
    public Map<Integer, byte[]> loadWarehouse(UUID familyId) {
        Map<Integer, byte[]> items = new HashMap<>();
        
        try {
            String sql = "SELECT slot, item_data FROM family_warehouse WHERE family_id = ?;";
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, familyId.toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        byte[] itemData = rs.getBytes("item_data");
                        items.put(slot, itemData);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load warehouse: " + e.getMessage());
            e.printStackTrace();
        }
        
        return items;
    }
    
    /** 删除仓库物品 */
    public void deleteWarehouseItem(UUID familyId, int slot) {
        try {
            String sql = "DELETE FROM family_warehouse WHERE family_id = ? AND slot = ?;";
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, familyId.toString());
                pstmt.setInt(2, slot);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete warehouse item: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** 删除整个仓库 */
    public void deleteWarehouse(UUID familyId) {
        try {
            String sql = "DELETE FROM family_warehouse WHERE family_id = ?;";
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, familyId.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete warehouse: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void saveFamilies(Collection<Family> families) {
        if (families.isEmpty()) {
            return;
        }
        
        try {
            String dbType = config.getString("database.type", "sqlite");
            String sql;
            
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = "INSERT INTO families " +
                        "(id, name, leader_id, level, honor, activity, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "name = VALUES(name), leader_id = VALUES(leader_id), " +
                        "level = VALUES(level), honor = VALUES(honor), " +
                        "activity = VALUES(activity), created_at = VALUES(created_at);";
            } else {
                sql = "INSERT OR REPLACE INTO families " +
                        "(id, name, leader_id, level, honor, activity, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);";
            }
            
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                int count = 0;
                
                for (Family family : families) {
                    pstmt.setString(1, family.getId().toString());
                    pstmt.setString(2, family.getName());
                    pstmt.setString(3, family.getLeaderId().toString());
                    pstmt.setInt(4, family.getLevel());
                    pstmt.setDouble(5, family.getHonor());
                    pstmt.setDouble(6, family.getActivity());
                    pstmt.setLong(7, now);
                    
                    pstmt.addBatch();
                    count++;
                    
                    // 每50个批量执行一次
                    if (count % 50 == 0) {
                        pstmt.executeBatch();
                    }
                }
                
                // 执行剩余的批量操作
                if (count % 50 != 0) {
                    pstmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save families: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
