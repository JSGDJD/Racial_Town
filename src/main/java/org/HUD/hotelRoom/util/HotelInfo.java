package org.HUD.hotelRoom.util;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class HotelInfo {
    public final String name;
    public final UUID owner;
    public final Location[] corners;
    public final Set<UUID> members = new HashSet<>();
    public final Set<Location> facade = new HashSet<>();
    public final boolean isPublic; // 是否为公共房屋
    public final boolean isOfficial; // 是否为官方房屋
    public final String hotelType; // 官方房屋类型

    public HotelInfo(String name, UUID owner, Location[] corners) {
        this.name = name;
        this.owner = owner;
        this.corners = corners;
        this.isPublic = false; // 默认不是公共房屋
        this.isOfficial = false; // 默认不是官方房屋
        this.hotelType = null; // 默认无类型
    }
    
    public HotelInfo(String name, UUID owner, Location[] corners, boolean isPublic) {
        this.name = name;
        this.owner = owner;
        this.corners = corners;
        this.isPublic = isPublic;
        this.isOfficial = false; // 默认不是官方房屋
        this.hotelType = null; // 默认无类型
    }
    
    public HotelInfo(String name, UUID owner, Location[] corners, boolean isPublic, boolean isOfficial) {
        this.name = name;
        this.owner = owner;
        this.corners = corners;
        this.isPublic = isPublic;
        this.isOfficial = isOfficial;
        this.hotelType = null; // 默认无类型
    }
    
    public HotelInfo(String name, UUID owner, Location[] corners, boolean isPublic, boolean isOfficial, String hotelType) {
        this.name = name;
        this.owner = owner;
        this.corners = corners;
        this.isPublic = isPublic;
        this.isOfficial = isOfficial;
        this.hotelType = hotelType != null ? hotelType : ""; // 官方房屋类型，如果为null则设为空字符串
    }
}