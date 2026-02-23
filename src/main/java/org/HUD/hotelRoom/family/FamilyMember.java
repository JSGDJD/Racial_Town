package org.HUD.hotelRoom.family;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class FamilyMember {
    private UUID playerId;
    private UUID familyId;
    private String position;
    private Map<String, Double> dailyActivity;
    private long joinTime;
    private long lastLoginTime;

    public FamilyMember(UUID playerId, UUID familyId, String position) {
        this.playerId = playerId;
        this.familyId = familyId;
        this.position = position;
        this.dailyActivity = new HashMap<>();
        this.joinTime = System.currentTimeMillis();
        this.lastLoginTime = System.currentTimeMillis();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Map<String, Double> getDailyActivity() {
        return dailyActivity;
    }

    public double getDailyActivity(String action) {
        return dailyActivity.getOrDefault(action, 0.0);
    }

    public void addDailyActivity(String action, double amount) {
        double current = dailyActivity.getOrDefault(action, 0.0);
        dailyActivity.put(action, current + amount);
    }

    public void resetDailyActivity() {
        dailyActivity.clear();
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    @Override
    public String toString() {
        return "FamilyMember{playerId=" + playerId + ", familyId=" + familyId + ", position='" + position + '\'' + '}';
    }
}
