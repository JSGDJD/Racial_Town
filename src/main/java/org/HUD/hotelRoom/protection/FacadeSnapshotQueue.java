package org.HUD.hotelRoom.protection;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class FacadeSnapshotQueue {
    private static final BlockingQueue<HotelInfo> QUEUE = new LinkedBlockingQueue<>();

    public static void enqueue(HotelInfo info) { QUEUE.offer(info); }

    public static void startWorker(HotelRoom plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            HotelInfo info = QUEUE.poll();
            if (info == null) return;
            Set<Location> facade = new HashSet<>();
            try {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    facade.addAll(FacadeProtection.snapshot(info.corners));
                    return null;
                }).get();
            } catch (Exception e) { e.printStackTrace(); return; }
            SQLiteStorage.saveFacade(info.name, facade);
        }, 0L, 10L);
    }
}
