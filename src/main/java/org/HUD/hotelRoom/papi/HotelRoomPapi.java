package org.HUD.hotelRoom.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.attribute.PlayerAttribute;
import org.HUD.hotelRoom.race.RaceDataStorage;
import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.protection.ProtectionListener;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.family.FamilyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.Map;

public final class HotelRoomPapi extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "hotelroom";          // 前缀：%hotelroom_xxx%
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;                 // 不随 /papi reload 卸载
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player p, @NotNull String params) {
        if (p == null) return "";
        
        // 处理家族相关占位符
        String familyResult = handleFamilyPlaceholders(p, params);
        if (familyResult != null) {
            return familyResult;
        }

        // 添加玩家荣誉值查询
        if (params.equals("player_honor")) {
            return String.valueOf(SQLiteStorage.getHonor(p.getUniqueId()));
        }
        
        // 添加玩家血量查询（保留一位小数）
        if (params.equals("player_health")) {
            return String.format("%.1f", p.getHealth());
        }
        
        // 添加玩家最大血量查询（保留一位小数）
        if (params.equals("player_max_health")) {
            return String.format("%.1f", p.getMaxHealth());
        }
        
        // 添加玩家血量百分比查询
        if (params.equals("player_health_percent")) {
            double percent = (p.getHealth() / p.getMaxHealth()) * 100;
            return String.format("%.1f", percent);
        }
        
        // 添加玩家属性查询（格式: player_attr_属性名）
        if (params.startsWith("player_attr_")) {
            String attrName = params.substring(12); // 移除 "player_attr_" 前缀
            AttributeManager manager = AttributeManager.getInstance();
            if (manager != null && manager.isEnabled()) {
                PlayerAttribute attr = manager.getPlayerAttribute(p.getUniqueId());
                double value = attr.getAttribute(attrName);
                return String.format("%.1f", value);
            }
            return "0.0";
        }

        // 获取玩家种族
        RaceDataStorage.RaceConfig playerRaceConfig = RaceDataStorage.getRaceConfig(RaceDataStorage.getPlayerRace(p.getUniqueId()));
        String playerRaceName = "human";
        String playerDisplayName = "人类";
        if (playerRaceConfig != null) {
            playerRaceName = playerRaceConfig.raceName;
            playerDisplayName = playerRaceConfig.displayName;
        }

        // 返回当前种族显示名称（中文）
        if (params.equals("race")) {
            return playerDisplayName;
        }

        // 返回当前种族内部名称（英文ID）
        if (params.equals("race_name")) {
            return playerRaceName;
        }

        // 检测是否为指定种族（格式: is_race_种族名）
        if (params.startsWith("is_race_")) {
            String targetRace = params.substring(8); // 移除 "is_race_" 前缀
            return playerRaceName.equalsIgnoreCase(targetRace) ? "true" : "false";
        }

        // 显示玩家所有属性（格式: %hotelroom_all_attributes%）
        if (params.equals("all_attributes")) {
            AttributeManager manager = AttributeManager.getInstance();
            if (manager != null && manager.isEnabled()) {
                PlayerAttribute attr = manager.getPlayerAttribute(p.getUniqueId());
                Map<String, Double> allAttrs = attr.getAllAttributes();
                StringBuilder result = new StringBuilder();
                
                for (Map.Entry<String, Double> entry : allAttrs.entrySet()) {
                    String key = entry.getKey();
                    double value = entry.getValue();
                    
                    // 只显示有值的属性
                    if (Math.abs(value) < 0.01) continue;
                    
                    // 获取属性显示名称
                    String displayName = manager.getAttributeDisplayName(key);
                    
                    // 按值的大小排序（可选）
                    result.append(displayName).append("§7: §f").append(String.format("%.1f", value)).append("\n");
                }
                
                return result.toString().trim();
            }
            return "属性系统未启用";
        }

        // 显示单个属性值（格式: %hotelroom_attr_<属性名>%）
        if (params.startsWith("attr_")) {
            String attrName = params.substring(5); // 移除 "attr_" 前缀
            
            // 特殊处理生命值属性，直接从Player对象获取（包含临时加成）
            if (attrName.equals("health")) {
                return String.format("%.1f", p.getHealth());
            }
            
            // 特殊处理最大生命值属性，直接从Player对象获取（包含临时加成）
            if (attrName.equals("max_health")) {
                return String.format("%.1f", p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            }
            
            AttributeManager manager = AttributeManager.getInstance();
            if (manager != null && manager.isEnabled()) {
                PlayerAttribute playerAttr = manager.getPlayerAttribute(p.getUniqueId());
                double value = playerAttr.getAttribute(attrName);
                
                return String.format("%.1f", value);
            }
            return "属性系统未启用";
        }
        
        // 处理怪物相关占位符
        String mobResult = handleMobPlaceholders(p, params);
        if (mobResult != null) {
            return mobResult;
        }

        HotelInfo hi = ProtectionListener.getHotelAt(p.getLocation());
        // 不在任何领地
        if (hi == null) {
            switch (params) {
                case "in_land":      return "false";
                case "land_honor":   return "0";
                case "land_owner":   return "无";
                case "land_name":    return "无";
                case "in_public_hotel": return "false";
                default:             return null;
            }
        }

        // 在领地内
        switch (params) {
            case "in_land":                       return "true";
            case "land_honor":                    return String.valueOf(SQLiteStorage.getHonorReq(hi.name));
            case "land_owner":                    return hi.owner.toString().equals("00000000-0000-0000-0000-000000000000")
                    ? "系统" : Bukkit.getOfflinePlayer(hi.owner).getName();
            case "land_name":                     return hi.name;
            case "public_members_count":          return hi.isPublic ? String.valueOf(hi.members.size()) : "0";
            case "in_public_hotel":               return hi.isPublic ? "true" : "false";
            default:                              return null;
        }
    }
    
    // 家族系统相关占位符
    private String handleFamilyPlaceholders(Player p, String params) {
        FamilyManager familyManager = FamilyManager.getInstance();
        if (familyManager == null) {
            return null;
        }
        
        // 获取玩家家族信息
        var family = familyManager.getPlayerFamily(p);
        
        // 显示玩家所属家族名称，如果没有家族则显示"无"
        if (params.equals("family_name")) {
            return family != null ? family.getName() : "无";
        }
        
        // 显示玩家在家族中的职位
        if (params.equals("family_position")) {
            var member = familyManager.getMember(p);
            if (member != null && family != null) {
                String position = member.getPosition();
                // 获取职位的中文名称
                var config = familyManager.getConfig();
                return config.getString("positions." + position + ".name", position);
            }
            return "无";
        }
        
        // 显示家族等级
        if (params.equals("family_level")) {
            return family != null ? String.valueOf(family.getLevel()) : "0";
        }
        
        // 显示家族成员数量
        if (params.equals("family_member_count")) {
            return family != null ? String.valueOf(family.getMemberCount()) : "0";
        }
        
        return null;
    }
    
    // 怪物相关占位符
    private String handleMobPlaceholders(Player p, String params) {
        // 首先检查是否是怪物相关的占位符
        if (!params.equals("mob_health") && !params.equals("mob_max_health") && 
            !params.equals("mob_health_percent") && !params.equals("mob_name")) {
            return null; // 不是怪物相关占位符，返回null
        }
        
        // 获取玩家正在瞄准的实体
        org.bukkit.entity.Entity target = p.getTargetEntity(20); // 20格范围内的目标
        if (target == null) {
            // 如果没有目标，返回空字符串
            return "";
        }
        
        // 检查是否是生物实体
        if (!(target instanceof org.bukkit.entity.LivingEntity)) {
            // 如果不是生物实体，返回空字符串
            return "";
        }
        
        org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) target;
        
        // 处理怪物血量相关占位符
        if (params.equals("mob_health")) {
            // 返回怪物当前血量
            return String.format("%.1f", livingEntity.getHealth());
        }
        
        if (params.equals("mob_max_health")) {
            // 返回怪物最大血量
            return String.format("%.1f", livingEntity.getMaxHealth());
        }
        
        if (params.equals("mob_health_percent")) {
            // 返回怪物血量百分比
            double health = livingEntity.getHealth();
            double maxHealth = livingEntity.getMaxHealth();
            double percent = (health / maxHealth) * 100;
            return String.format("%.1f", percent);
        }
        
        if (params.equals("mob_name")) {
            // 返回怪物名称
            return livingEntity.getName();
        }
        
        return null;
    }
}