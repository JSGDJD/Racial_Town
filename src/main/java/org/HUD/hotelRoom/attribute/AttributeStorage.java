package org.HUD.hotelRoom.attribute;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * 属性数据存储管理器
 * 支持 SQLite 和 MySQL 两种存储方式
 */
public class AttributeStorage {
    
    private static Connection sqliteConn;
    private static HikariDataSource mysqlDataSource;
    private static String storageType = "sqlite";
    
    /**
     * 初始化存储
     */
    public static void initialize() {
        // 读取配置（路径改为 attributes/attributes.yml）
        File configFile = new File(HotelRoom.get().getDataFolder(), "attributes/attributes.yml");
        if (!configFile.exists()) {
            // 如果文件不存在，创建文件夹
            File attributesFolder = new File(HotelRoom.get().getDataFolder(), "attributes");
            if (!attributesFolder.exists()) {
                attributesFolder.mkdirs();
            }
            HotelRoom.get().saveResource("attributes/attributes.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        storageType = config.getString("database.type", "sqlite").toLowerCase();
        
        try {
            if ("mysql".equals(storageType)) {
                initMySQL(config);
            } else {
                initSQLite();
            }
            createTables();
            HotelRoom.get().getLogger().info("属性系统存储已初始化 (" + storageType.toUpperCase() + ")");
        } catch (SQLException e) {
            HotelRoom.get().getLogger().severe("初始化属性存储失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化 SQLite
     */
    private static void initSQLite() throws SQLException {
        // 数据库文件也放入 attributes 文件夹
        File attributesFolder = new File(HotelRoom.get().getDataFolder(), "attributes");
        if (!attributesFolder.exists()) {
            attributesFolder.mkdirs();
        }
        File file = new File(attributesFolder, "attributes.db");
        sqliteConn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }
    
    /**
     * 初始化 MySQL
     */
    private static void initMySQL(YamlConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "hotelroom");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "password");
        int poolSize = config.getInt("database.mysql.pool-size", 10);
        
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                               "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        
        mysqlDataSource = new HikariDataSource(hikariConfig);
    }
    
    /**
     * 获取数据库连接
     */
    private static Connection getConnection() throws SQLException {
        if ("mysql".equals(storageType)) {
            return mysqlDataSource.getConnection();
        } else {
            // SQLite: 检查连接是否有效，无效则重新连接
            if (sqliteConn == null || sqliteConn.isClosed()) {
                initSQLite();
            }
            return sqliteConn;
        }
    }
    
    /**
     * 创建数据表
     */
    private static void createTables() throws SQLException {
        String createTableSQL;
        
        if ("mysql".equals(storageType)) {
            // MySQL: 使用联合主键
            createTableSQL = 
                "CREATE TABLE IF NOT EXISTS player_attributes (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "attribute_key VARCHAR(50) NOT NULL, " +
                "attribute_value DOUBLE NOT NULL, " +
                "last_updated BIGINT NOT NULL, " +
                "PRIMARY KEY (uuid, attribute_key))";
        } else {
            // SQLite: 使用 UNIQUE 约束
            createTableSQL = 
                "CREATE TABLE IF NOT EXISTS player_attributes (" +
                "uuid TEXT NOT NULL, " +
                "attribute_key TEXT NOT NULL, " +
                "attribute_value REAL NOT NULL, " +
                "last_updated INTEGER NOT NULL, " +
                "UNIQUE(uuid, attribute_key))";
        }
        
        Connection conn = null;
        Statement st = null;
        
        try {
            conn = getConnection();
            st = conn.createStatement();
            st.execute(createTableSQL);
            
            // 索引优化
            try {
                st.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON player_attributes(uuid)");
            } catch (SQLException ignored) {
                // MySQL 可能不需要
            }
        } finally {
            if (st != null) st.close();
            if (conn != null && "mysql".equals(storageType)) {
                conn.close();
            }
        }
    }
    
    /**
     * 保存玩家属性
     */
    public static void savePlayerAttributes(UUID playerUUID, Map<String, Double> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return; // 避免空数据操作
        }
        
        String sql = "INSERT OR REPLACE INTO player_attributes (uuid, attribute_key, attribute_value, last_updated) VALUES (?, ?, ?, ?)";
        
        // MySQL 使用不同的语法
        if ("mysql".equals(storageType)) {
            sql = "INSERT INTO player_attributes (uuid, attribute_key, attribute_value, last_updated) " +
                  "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE attribute_value=VALUES(attribute_value), last_updated=VALUES(last_updated)";
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(sql);
            
            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, entry.getKey());
                ps.setDouble(3, entry.getValue());
                ps.setLong(4, System.currentTimeMillis());
                ps.addBatch();
            }
            
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            HotelRoom.get().getLogger().severe("保存玩家属性失败: " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            // 确保资源正确关闭
            try {
                if (ps != null) ps.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    // MySQL 连接需要关闭，SQLite 保持打开
                    if ("mysql".equals(storageType)) {
                        conn.close();
                    }
                }
            } catch (SQLException e) {
                HotelRoom.get().getLogger().severe("关闭数据库连接失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 加载玩家属性
     */
    public static Map<String, Double> loadPlayerAttributes(UUID playerUUID) {
        Map<String, Double> attributes = new HashMap<>();
        String sql = "SELECT attribute_key, attribute_value FROM player_attributes WHERE uuid=?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID.toString());
            
            rs = ps.executeQuery();
            while (rs.next()) {
                String key = rs.getString("attribute_key");
                double value = rs.getDouble("attribute_value");
                attributes.put(key, value);
            }
        } catch (SQLException e) {
            HotelRoom.get().getLogger().severe("加载玩家属性失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // MySQL 连接由连接池管理，需要关闭；SQLite 连接保持打开
                if (conn != null && "mysql".equals(storageType)) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return attributes;
    }
    
    /**
     * 删除玩家属性
     */
    public static void deletePlayerAttributes(UUID playerUUID) {
        String sql = "DELETE FROM player_attributes WHERE uuid=?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            HotelRoom.get().getLogger().severe("删除玩家属性失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null && "mysql".equals(storageType)) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 关闭存储连接
     */
    public static void close() {
        try {
            if (sqliteConn != null && !sqliteConn.isClosed()) {
                sqliteConn.close();
            }
            if (mysqlDataSource != null && !mysqlDataSource.isClosed()) {
                mysqlDataSource.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
