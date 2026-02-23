package org.HUD.hotelRoom.family;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Family {
    private UUID id;
    private String name;
    private UUID leaderId;
    private int level;
    private double honor;
    private double activity;
    private Set<UUID> memberIds;

    public Family(UUID id, String name, UUID leaderId, int level) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.level = level;
        this.honor = 0;
        this.activity = 0;
        this.memberIds = new HashSet<>();
        this.memberIds.add(leaderId);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getHonor() {
        return honor;
    }

    public void setHonor(double honor) {
        this.honor = honor;
    }

    public void addHonor(double amount) {
        this.honor += amount;
    }

    public void removeHonor(double amount) {
        this.honor -= amount;
        if (this.honor < 0) {
            this.honor = 0;
        }
    }

    public double getActivity() {
        return activity;
    }

    public void setActivity(double activity) {
        this.activity = activity;
    }

    public void addActivity(double amount) {
        this.activity += amount;
    }

    public void removeActivity(double amount) {
        this.activity -= amount;
        if (this.activity < 0) {
            this.activity = 0;
        }
    }

    public Set<UUID> getMemberIds() {
        return memberIds;
    }

    public int getMemberCount() {
        return memberIds.size();
    }

    public boolean addMember(UUID memberId) {
        return memberIds.add(memberId);
    }

    public boolean removeMember(UUID memberId) {
        return memberIds.remove(memberId);
    }

    public boolean isMember(UUID memberId) {
        return memberIds.contains(memberId);
    }

    public boolean isLeader(UUID memberId) {
        return leaderId.equals(memberId);
    }
}
