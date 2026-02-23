package org.HUD.hotelRoom.protection;

import org.HUD.hotelRoom.util.HotelInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public final class FacadeProtection {

    /** 创建领地时生成外观快照（忽略空气） */
    public static Set<Location> snapshot(Location[] diagonals) {
        Set<Location> set = new HashSet<>();
        int minX = Math.min(diagonals[0].getBlockX(), diagonals[1].getBlockX());
        int maxX = Math.max(diagonals[0].getBlockX(), diagonals[1].getBlockX());
        int minY = Math.min(diagonals[0].getBlockY(), diagonals[1].getBlockY());
        int maxY = Math.max(diagonals[0].getBlockY(), diagonals[1].getBlockY());
        int minZ = Math.min(diagonals[0].getBlockZ(), diagonals[1].getBlockZ());
        int maxZ = Math.max(diagonals[0].getBlockZ(), diagonals[1].getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = diagonals[0].getWorld().getBlockAt(x, y, z);
                    if (!b.isEmpty() && !b.isLiquid()) { // 非空气/液体才保护
                        set.add(b.getLocation());
                    }
                }
            }
        }
        return set;
    }

    /** 玩家试图破坏的方块是否属于原始外观 */
    public static boolean isFacade(HotelInfo info, Location loc) {
        return info.facade.contains(loc);
    }
}
