package org.HUD.hotelRoom.api;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对外提供的静态 API 接口
 * 其他插件只需依赖此类，无需访问内部实现
 */
public final class HotelRoomAPI {

    /**
     * 判断玩家是否拥有任何领地
     */
    public static boolean hasHotel(Player player) {
        return hasHotel(player.getUniqueId());
    }
    /**
     * 获取指定领地的对角坐标数组
     */
    public static Optional<Location[]> getHotelCorners(String hotelName) {
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        return info == null ? Optional.empty() : Optional.of(info.corners);
    }

    public static boolean hasHotel(UUID uuid) {
        for (HotelInfo info : SelectionMgr.HOTELS.values()) {
            if (info.owner.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取玩家拥有的领地名称（返回第一个）
     */
    public static Optional<String> getHotelName(Player player) {
        return getHotelName(player.getUniqueId());
    }

    public static Optional<String> getHotelName(UUID uuid) {
        for (Map.Entry<String, HotelInfo> entry : SelectionMgr.HOTELS.entrySet()) {
            if (entry.getValue().owner.equals(uuid)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * 获取指定位置的领地名称
     */
    public static Optional<String> getHotelAt(Location location) {
        HotelInfo info = SelectionMgr.HOTELS.values().stream()
                .filter(h -> isInside(h.corners, location))
                .findFirst()
                .orElse(null);
        return info == null ? Optional.empty() : Optional.of(info.name);
    }

    /**
     * 获取领地的主人
     */
    public static Optional<UUID> getOwner(String hotelName) {
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        return info == null ? Optional.empty() : Optional.of(info.owner);
    }

    /**
     * 获取所有领地名称
     */
    public static Set<String> getAllHotelNames() {
        return Set.copyOf(SelectionMgr.HOTELS.keySet());
    }

    /**
     * 获取领地成员（白名单）
     */
    public static Set<UUID> getMembers(String hotelName) {
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        return info == null ? Set.of() : Set.copyOf(info.members);
    }

    /**
     * 判断坐标是否在领地内
     */
    public static boolean isInsideHotel(Location location) {
        return getHotelAt(location).isPresent();
    }

    /**
     * 返回玩家拥有的所有酒店名称（适配外部桥接）
     * @param playerName 玩家名
     * @return 非空 Set
     */
    public static Set<String> getPlayerHotels(String playerName) {
        UUID uuid = Bukkit.getPlayerUniqueId(playerName);
        if (uuid == null) return Set.of();

        return SelectionMgr.HOTELS.entrySet().stream()
                .filter(e -> e.getValue().owner.equals(uuid))
                .filter(e -> !e.getValue().isOfficial)   // 官方房永远不出现在玩家列表
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }


    /**
     * 工具方法：判断坐标是否在立方体选区内
     */
    private static boolean isInside(Location[] corners, Location loc) {
        if (!corners[0].getWorld().equals(loc.getWorld())) return false;
        int x1 = corners[0].getBlockX(), x2 = corners[1].getBlockX();
        int y1 = corners[0].getBlockY(), y2 = corners[1].getBlockY();
        int z1 = corners[0].getBlockZ(), z2 = corners[1].getBlockZ();
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    /**
     * 判断酒店是否为官方房屋
     */
    public static boolean isOfficialHotel(String hotelName) {
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        return info != null && info.isOfficial;
    }

    /**
     * 获取酒店类型
     */
    public static Optional<String> getHotelType(String hotelName) {
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        return info == null ? Optional.empty() : Optional.ofNullable(info.hotelType);
    }

    /**
     * 判断酒店是否为公共房屋
     */
    public static boolean isPublicHotel(String hotelName) {
        HotelInfo info = SelectionMgr.HOTELS.get(hotelName);
        return info != null && info.isPublic;
    }

    /**
     * 获取酒店的第一个对角坐标（最小坐标）
     * @param hotelName 酒店名称
     * @return 第一个对角坐标，如果酒店不存在则返回空Optional
     */
    public static Optional<Location> getHotelMinCorner(String hotelName) {
        Optional<Location[]> corners = getHotelCorners(hotelName);
        return corners.map(c -> c[0]);
    }

    /**
     * 获取酒店的第二个对角坐标（最大坐标）
     * @param hotelName 酒店名称
     * @return 第二个对角坐标，如果酒店不存在则返回空Optional
     */
    public static Optional<Location> getHotelMaxCorner(String hotelName) {
        Optional<Location[]> corners = getHotelCorners(hotelName);
        return corners.map(c -> c[1]);
    }

    public static Set<String> getPlayerAccessibleHotels(String playerName) {
        UUID uuid = Bukkit.getPlayerUniqueId(playerName);
        if (uuid == null) return Set.of();

        return SelectionMgr.HOTELS.entrySet().stream()
                .filter(e -> {
                    return (e.getValue().owner.equals(uuid) && !e.getValue().isOfficial) ||
                            (e.getValue().isPublic && e.getValue().members.contains(uuid));
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }


}