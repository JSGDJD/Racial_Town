package org.HUD.hotelRoom.util;

import org.bukkit.Location;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SpatialIndex {
    private static final SpatialIndex INST = new SpatialIndex();
    private final Map<String, Set<HotelInfo>> index = new ConcurrentHashMap<>();

    public static SpatialIndex getInstance() { return INST; }

    private String key(int chunkX, int chunkZ) { return chunkX + "," + chunkZ; }

    /** 注册一个酒店 */
    public void add(HotelInfo info) {
        Location[] c = info.corners;
        int minCX = Math.min(c[0].getBlockX(), c[1].getBlockX()) >> 4;
        int maxCX = Math.max(c[0].getBlockX(), c[1].getBlockX()) >> 4;
        int minCZ = Math.min(c[0].getBlockZ(), c[1].getBlockZ()) >> 4;
        int maxCZ = Math.max(c[0].getBlockZ(), c[1].getBlockZ()) >> 4;
        for (int x = minCX; x <= maxCX; x++)
            for (int z = minCZ; z <= maxCZ; z++)
                index.computeIfAbsent(key(x, z), k -> ConcurrentHashMap.newKeySet()).add(info);
    }

    /** 反注册一个酒店（删除/放弃时调用） */
    public void remove(HotelInfo info) {
        Location[] c = info.corners;
        int minCX = Math.min(c[0].getBlockX(), c[1].getBlockX()) >> 4;
        int maxCX = Math.max(c[0].getBlockX(), c[1].getBlockX()) >> 4;
        int minCZ = Math.min(c[0].getBlockZ(), c[1].getBlockZ()) >> 4;
        int maxCZ = Math.max(c[0].getBlockZ(), c[1].getBlockZ()) >> 4;
        for (int x = minCX; x <= maxCX; x++)
            for (int z = minCZ; z <= maxCZ; z++) {
                Set<HotelInfo> set = index.get(key(x, z));
                if (set != null) { set.remove(info); if (set.isEmpty()) index.remove(key(x, z)); }
            }
    }

    /** O(1) 查找 */
    public HotelInfo get(Location loc) {
        int cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;
        Set<HotelInfo> set = index.get(key(cx, cz));
        if (set == null) return null;
        for (HotelInfo hi : set) if (isInside(hi.corners, loc)) return hi;
        return null;
    }

    /** 拷贝 ProtectionListener.isInside 的静态工具 */
    private boolean isInside(Location[] d, Location loc) {
        if (!d[0].getWorld().equals(loc.getWorld())) return false;
        int x1 = d[0].getBlockX(), x2 = d[1].getBlockX();
        int y1 = d[0].getBlockY(), y2 = d[1].getBlockY();
        int z1 = d[0].getBlockZ(), z2 = d[1].getBlockZ();
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
