package org.HUD.hotelRoom.util;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;


import java.io.File;
import java.sql.*;
import java.util.*;

public final class SQLiteStorage {

    private static Connection conn;

    public static void open() {
        File file = new File(HotelRoom.get().getDataFolder(), "hotels.db");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
        }
    }
    
    /**
     * 检查并获取有效的数据库连接
     */
    private static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            // 连接已关闭，重新建立连接
            File file = new File(HotelRoom.get().getDataFolder(), "hotels.db");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            HotelRoom.get().getLogger().info("重新建立了SQLiteStorage的数据库连接");
        }
        return conn;
    }

    /* 建表（仅首次） */
    /* 在 createTable() 里追加 */
    private static void createTable() throws SQLException {
        Statement st = getConnection().createStatement();
        st.execute(
                "CREATE TABLE IF NOT EXISTS hotels (" +
                        "name TEXT PRIMARY KEY," +
                        "owner TEXT NOT NULL," +
                        "is_public INTEGER NOT NULL DEFAULT 0," +
                        "is_official INTEGER NOT NULL DEFAULT 0," +
                        "hotel_type TEXT DEFAULT ''," +
                        "world TEXT NOT NULL," +
                        "x1 REAL,y1 REAL,z1 REAL," +
                        "x2 REAL,y2 REAL,z2 REAL)");
        st.execute(
                "CREATE TABLE IF NOT EXISTS hotel_members (" +
                        "hotel TEXT NOT NULL," +
                        "uuid TEXT NOT NULL," +
                        "PRIMARY KEY (hotel, uuid))");
        st.execute(
                "CREATE TABLE IF NOT EXISTS hotel_facade (" +
                        "hotel TEXT NOT NULL," +
                        "x INT NOT NULL,y INT NOT NULL,z INT NOT NULL," +
                        "PRIMARY KEY (hotel,x,y,z))");
        st.execute(
                "CREATE TABLE IF NOT EXISTS placed_blocks (" +
                        "world TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, " +
                        "owner TEXT NOT NULL, " +
                        "PRIMARY KEY(world,x,y,z))");
        st.execute(
                "CREATE TABLE IF NOT EXISTS player_honor (" +
                        "uuid TEXT PRIMARY KEY, honor INTEGER NOT NULL)");
        st.execute(
                "CREATE TABLE IF NOT EXISTS hotel_honorreq (" +
                        "hotel TEXT PRIMARY KEY, required_honor INTEGER NOT NULL)");
        st.execute(
                "CREATE TABLE IF NOT EXISTS honor_change (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "clicked_at INTEGER NOT NULL)");
        st.execute(
                "CREATE TABLE IF NOT EXISTS stored_honor (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "stored_honor INTEGER NOT NULL, " +
                        "last_update INTEGER NOT NULL)");  // 最后更新时间戳

        // 检查并添加 is_public 列（如果不存在）
        try {
            ResultSet rs = st.executeQuery("PRAGMA table_info(hotels)");
            boolean hasIsPublicColumn = false;
            boolean hasIsOfficialColumn = false;
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("is_public".equals(columnName)) {
                    hasIsPublicColumn = true;
                } else if ("is_official".equals(columnName)) {
                    hasIsOfficialColumn = true;
                }
            }
            
            if (!hasIsPublicColumn) {
                st.execute("ALTER TABLE hotels ADD COLUMN is_public INTEGER NOT NULL DEFAULT 0");
            }
            
            if (!hasIsOfficialColumn) {
                st.execute("ALTER TABLE hotels ADD COLUMN is_official INTEGER NOT NULL DEFAULT 0");
            }
            
            // 检查并添加 hotel_type 列（如果不存在）
            boolean hasHotelTypeColumn = false;
            rs = st.executeQuery("PRAGMA table_info(hotels)");
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("hotel_type".equals(columnName)) {
                    hasHotelTypeColumn = true;
                }
            }
            
            if (!hasHotelTypeColumn) {
                st.execute("ALTER TABLE hotels ADD COLUMN hotel_type TEXT DEFAULT ''");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /* 写入或更新 */
    public static void saveHotel(String name, UUID owner, Location[] corners) {
        // 对于现有代码，假设不是公共房屋和官方房屋
        saveHotel(name, owner, corners, false, false);
    }
    
    /* 写入或更新带isPublic字段 */
    public static void saveHotel(String name, UUID owner, Location[] corners, boolean isPublic) {
        // 对于现有代码，假设不是官方房屋
        saveHotel(name, owner, corners, isPublic, false);
    }
    
    /* 写入或更新带isPublic和isOfficial字段 */
    public static void saveHotel(String name, UUID owner, Location[] corners, boolean isPublic, boolean isOfficial) {
        saveHotel(name, owner, corners, isPublic, isOfficial, null);
    }
    
    /* 写入或更新带isPublic、isOfficial和hotelType字段 */
    public static void saveHotel(String name, UUID owner, Location[] corners, boolean isPublic, boolean isOfficial, String hotelType) {
        String sql = "INSERT OR REPLACE INTO hotels (name, owner, is_public, is_official, hotel_type, world, x1, y1, z1, x2, y2, z2) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, owner.toString());
            ps.setInt(3, isPublic ? 1 : 0); // 1表示公共，0表示私有
            ps.setInt(4, isOfficial ? 1 : 0); // 1表示官方，0表示非官方
            ps.setString(5, hotelType != null ? hotelType : ""); // 官方房屋类型
            ps.setString(6, corners[0].getWorld().getName());
            ps.setDouble(7, corners[0].getX());
            ps.setDouble(8, corners[0].getY());
            ps.setDouble(9, corners[0].getZ());
            ps.setDouble(10, corners[1].getX());
            ps.setDouble(11, corners[1].getY());
            ps.setDouble(12, corners[1].getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /* 读取全部 */
    public static Map<String, HotelInfo> loadAll() {
        Map<String, HotelInfo> map = new LinkedHashMap<>();
        String sql = "SELECT * FROM hotels";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String ownerStr = rs.getString("owner");
                int isPublicInt = rs.getInt("is_public");
                int isOfficialInt = rs.getInt("is_official"); // 新增官方房屋标识
                String hotelType = rs.getString("hotel_type"); // 获取酒店类型
                UUID owner;
                if ("NULL".equalsIgnoreCase(ownerStr) || ownerStr == null) {
                    owner = new UUID(0, 0);          // 系统占位 UUID，表示"无主人"
                } else {
                    owner = UUID.fromString(ownerStr);
                }

                String world = rs.getString("world");
                World w = Bukkit.getWorld(world);
                
                if (w == null) {
                    HotelRoom.get().getLogger().warning("酒店 '" + name + "' 的世界 '" + world + "' 未加载，跳过加载此酒店！");
                    continue;
                }
                
                Location[] corners = new Location[2];
                corners[0] = new Location(w,
                        rs.getDouble("x1"), rs.getDouble("y1"), rs.getDouble("z1"));
                corners[1] = new Location(w,
                        rs.getDouble("x2"), rs.getDouble("y2"), rs.getDouble("z2"));
                boolean isPublic = isPublicInt == 1;
                boolean isOfficial = isOfficialInt == 1; // 新增官方房屋标识
                map.put(name, new HotelInfo(name, owner, corners, isPublic, isOfficial, hotelType));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /* 再读成员 */
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT hotel,uuid FROM hotel_members")) {
            while (rs.next()) {
                HotelInfo info = map.get(rs.getString("hotel"));
                if (info != null)
                    try {
                        info.members.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException ignore) {
                    }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /* 再读外观快照（重启后保护的关键） */
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT hotel,x,y,z FROM hotel_facade")) {
            while (rs.next()) {
                HotelInfo hi = map.get(rs.getString("hotel"));
                if (hi == null) continue;
                World w = hi.corners[0].getWorld();
                hi.facade.add(new Location(w,
                        rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void removeHotel(String name) {
        // 先从数据库获取世界信息，避免世界未加载时getWorld()返回null的问题
        String world = null;
        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        
        // 从数据库中获取边界坐标和世界信息
        String sql = "SELECT world, x1, y1, z1, x2, y2, z2 FROM hotels WHERE name = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    world = rs.getString("world");
                    minX = (int) Math.min(rs.getDouble("x1"), rs.getDouble("x2"));
                    maxX = (int) Math.max(rs.getDouble("x1"), rs.getDouble("x2"));
                    minY = (int) Math.min(rs.getDouble("y1"), rs.getDouble("y2"));
                    maxY = (int) Math.max(rs.getDouble("y1"), rs.getDouble("y2"));
                    minZ = (int) Math.min(rs.getDouble("z1"), rs.getDouble("z2"));
                    maxZ = (int) Math.max(rs.getDouble("z1"), rs.getDouble("z2"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        if (world == null) return; // 酒店不存在
        
        /* 级联清三张附表 */
        try (Statement st = getConnection().createStatement()) {
            st.execute("DELETE FROM hotel_members WHERE hotel='" + name + "'");
            st.execute("DELETE FROM hotel_facade   WHERE hotel='" + name + "'");
            st.execute("DELETE FROM placed_blocks WHERE world='" + world +
                    "' AND x BETWEEN " + minX + " AND " + maxX +
                    " AND y BETWEEN " + minY + " AND " + maxY +
                    " AND z BETWEEN " + minZ + " AND " + maxZ);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 反注册空间索引
        HotelInfo info = SelectionMgr.HOTELS.get(name);
        if (info != null) SelectionMgr.getInst().removeIndex(info);


        /* 最后删主表 */
        String deleteSql = "DELETE FROM hotels WHERE name = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(deleteSql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }



    public static void addMember(String hotel, UUID uuid) {
        String sql = "INSERT OR IGNORE INTO hotel_members VALUES (?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeMember(String hotel, UUID uuid) {
        String sql = "DELETE FROM hotel_members WHERE hotel=? AND uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveFacade(String hotel, Set<Location> set) {
        String sql = "INSERT OR IGNORE INTO hotel_facade VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            getConnection().setAutoCommit(false);
            int count = 0;
            for (Location loc : set) {
                ps.setString(1, hotel);
                ps.setInt(2, loc.getBlockX());
                ps.setInt(3, loc.getBlockY());
                ps.setInt(4, loc.getBlockZ());
                ps.addBatch();
                if (++count % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            getConnection().commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try { getConnection().rollback(); } catch (SQLException ignored) {}
        } finally {
            try { getConnection().setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }


    public static Set<Location> loadFacade(String hotel) {
        Set<Location> set = new HashSet<>();
        String sql = "SELECT x,y,z FROM hotel_facade WHERE hotel=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // 世界用酒店第一个角的世界即可
                    HotelInfo hi = SelectionMgr.HOTELS.get(hotel);
                    if (hi == null) return set;          // 异步卸载等异常场景
                    World w = hi.corners[0].getWorld();

                    set.add(new Location(w,
                            rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return set;
    }

    /**
     * 放置时登记
     */
    public static void trackPlace(Block b, UUID who) {
        if (b.isEmpty() || b.isLiquid()) return;
        String sql = "INSERT OR IGNORE INTO placed_blocks VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, b.getWorld().getName());
            ps.setInt(2, b.getX());
            ps.setInt(3, b.getY());
            ps.setInt(4, b.getZ());
            ps.setString(5, who.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询放置者，null=无记录（外观或老方块）
     */
    public static UUID getPlacer(Block b) {
        String sql = "SELECT owner FROM placed_blocks WHERE world=? AND x=? AND y=? AND z=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, b.getWorld().getName());
            ps.setInt(2, b.getX());
            ps.setInt(3, b.getY());
            ps.setInt(4, b.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("owner")) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 方块被毁掉时清记录
     */
    public static void untrackPlace(Block b) {
        String sql = "DELETE FROM placed_blocks WHERE world=? AND x=? AND y=? AND z=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, b.getWorld().getName());
            ps.setInt(2, b.getX());
            ps.setInt(3, b.getY());
            ps.setInt(4, b.getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ====== 荣誉值 ====== */
    public static int getHonor(UUID uuid) {
        String sql = "SELECT honor FROM player_honor WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("honor") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setHonor(UUID uuid, int honor) {
        logHonorChange(uuid);
        String sql = "INSERT OR REPLACE INTO player_honor VALUES (?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, honor);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addHonor(UUID uuid, int delta) {
        setHonor(uuid, getHonor(uuid) + delta);
        logHonorChange(uuid);
    }

    /* ====== 领地门槛 ====== */
    public static void setHonorReq(String hotel, int req) {
        String sql = "INSERT OR REPLACE INTO hotel_honorreq VALUES (?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            ps.setInt(2, req);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getHonorReq(String hotel) {
        String sql = "SELECT required_honor FROM hotel_honorreq WHERE hotel=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("required_honor") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; // ← 新增：数据库异常时默认返回 0（无门槛）
    }

    /** 清掉指定酒店范围内的所有 placed_blocks 记录（领取或删除时调用） */
    public static void untrackPlaceInHotel(String hotel) {
        // 从数据库获取世界信息，避免世界未加载时getWorld()返回null的问题
        String world = null;
        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        
        // 从数据库中获取边界坐标和世界信息
        String sql = "SELECT world, x1, y1, z1, x2, y2, z2 FROM hotels WHERE name = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    world = rs.getString("world");
                    minX = (int) Math.min(rs.getDouble("x1"), rs.getDouble("x2"));
                    maxX = (int) Math.max(rs.getDouble("x1"), rs.getDouble("x2"));
                    minY = (int) Math.min(rs.getDouble("y1"), rs.getDouble("y2"));
                    maxY = (int) Math.max(rs.getDouble("y1"), rs.getDouble("y2"));
                    minZ = (int) Math.min(rs.getDouble("z1"), rs.getDouble("z2"));
                    maxZ = (int) Math.max(rs.getDouble("z1"), rs.getDouble("z2"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        if (world == null) return; // 酒店不存在

        String deleteSql = "DELETE FROM placed_blocks WHERE world=? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?";
        try (PreparedStatement ps = getConnection().prepareStatement(deleteSql)) {
            ps.setString(1, world);
            ps.setInt(2, minX); ps.setInt(3, maxX);
            ps.setInt(4, minY); ps.setInt(5, maxY);
            ps.setInt(6, minZ); ps.setInt(7, maxZ);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }


    /**
     * 根据酒店名返回边界对角坐标数组（工具方法）
     */
    private static Location[] getHotelCorners(String hotel) {
        HotelInfo hi = SelectionMgr.HOTELS.get(hotel);
        return hi == null ? null : hi.corners;
    }

    public static Set<Location> loadPlacedBlocksInHotel(String hotel) {
        Set<Location> set = new HashSet<>();
        
        // 从数据库获取世界信息，避免世界未加载时getWorld()返回null的问题
        String world = null;
        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        
        // 从数据库中获取边界坐标和世界信息
        String sql = "SELECT world, x1, y1, z1, x2, y2, z2 FROM hotels WHERE name = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, hotel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    world = rs.getString("world");
                    minX = (int) Math.min(rs.getDouble("x1"), rs.getDouble("x2"));
                    maxX = (int) Math.max(rs.getDouble("x1"), rs.getDouble("x2"));
                    minY = (int) Math.min(rs.getDouble("y1"), rs.getDouble("y2"));
                    maxY = (int) Math.max(rs.getDouble("y1"), rs.getDouble("y2"));
                    minZ = (int) Math.min(rs.getDouble("z1"), rs.getDouble("z2"));
                    maxZ = (int) Math.max(rs.getDouble("z1"), rs.getDouble("z2"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return set;
        }
        
        if (world == null) return set; // 酒店不存在
        
        String querySql = "SELECT x,y,z FROM placed_blocks WHERE world=? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?";
        try (PreparedStatement ps = getConnection().prepareStatement(querySql)) {
            ps.setString(1, world);
            ps.setInt(2, minX); ps.setInt(3, maxX);
            ps.setInt(4, minY); ps.setInt(5, maxY);
            ps.setInt(6, minZ); ps.setInt(7, maxZ);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(new Location(Bukkit.getWorld(world), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return set;
    }

    /* 新增：取最后一次荣誉变化时间，用于首次启动时兜底 */
    public static long getLastHonorTime(UUID uuid){
        String sql = "SELECT MAX(clicked_at) FROM honor_change WHERE uuid=?";
        try(PreparedStatement ps = getConnection().prepareStatement(sql)){
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if(rs.next() && rs.getLong(1)>0) return rs.getLong(1);
        }catch(SQLException e){e.printStackTrace();}
        return System.currentTimeMillis(); // 找不到就当作现在
    }

    /* 任何荣誉变动时顺便写一行时间戳（供 HonorDecayManager 查询）*/
    private static void logHonorChange(UUID uuid){
        String sql = "INSERT OR REPLACE INTO honor_change(uuid,clicked_at) VALUES(?,?)";
        try(PreparedStatement ps = getConnection().prepareStatement(sql)){
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        }catch(SQLException e){e.printStackTrace();}
    }

    public static int getStoredHonor(UUID uuid) {
        String sql = "SELECT stored_honor FROM stored_honor WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("stored_honor") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setStoredHonor(UUID uuid, int storedHonor) {
        String sql = "INSERT OR REPLACE INTO stored_honor VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, storedHonor);
            ps.setLong(3, System.currentTimeMillis()); // 更新时间戳
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static long getLastStoredHonorUpdate(UUID uuid) {
        String sql = "SELECT last_update FROM stored_honor WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("last_update") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void clearStoredHonor(UUID uuid) {
        String sql = "DELETE FROM stored_honor WHERE uuid=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
