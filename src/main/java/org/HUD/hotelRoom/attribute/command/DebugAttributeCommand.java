package org.HUD.hotelRoom.attribute.command;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.attribute.PlayerAttribute;
import org.HUD.hotelRoom.race.RaceDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * 属性调试命令
 * 用于查看玩家详细的属性信息，帮助诊断问题
 */
public class DebugAttributeCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hotelroom.admin")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }
        
        Player target;
        if (args.length == 0) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§c控制台使用需要指定玩家: /debugattr <玩家>");
                return true;
            }
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c玩家 " + args[0] + " 不在线");
                return true;
            }
        }
        
        UUID uuid = target.getUniqueId();
        AttributeManager attrManager = AttributeManager.getInstance();
        
        if (attrManager == null || !attrManager.isEnabled()) {
            sender.sendMessage("§c属性系统未启用");
            return true;
        }
        
        // 获取玩家属性
        PlayerAttribute playerAttr = attrManager.getPlayerAttribute(uuid);
        Map<String, Double> allAttributes = playerAttr.getAllAttributes();
        
        // 获取种族信息
        String race = RaceDataStorage.getPlayerRace(uuid);
        int raceLevel = RaceDataStorage.getPlayerRaceLevel(uuid);
        
        // 显示基本信息
        sender.sendMessage("§6===== 玩家属性调试信息 =====");
        sender.sendMessage("§e玩家: §f" + target.getName());
        sender.sendMessage("§e种族: §f" + (race != null ? race : "无") + " (等级: " + raceLevel + ")");
        sender.sendMessage("");
        
        // 显示关键属性
        sender.sendMessage("§6===== 关键属性值 =====");
        sender.sendMessage("§e物理伤害: §f" + allAttributes.getOrDefault("physical_damage", 0.0));
        sender.sendMessage("§e魔法伤害: §f" + allAttributes.getOrDefault("magic_damage", 0.0));
        sender.sendMessage("§e物理防御: §f" + allAttributes.getOrDefault("physical_defense", 0.0));
        sender.sendMessage("§e魔法防御: §f" + allAttributes.getOrDefault("magic_defense", 0.0));
        sender.sendMessage("§e护甲值: §f" + allAttributes.getOrDefault("armor", 0.0));
        sender.sendMessage("");
        
        // 分析魔法伤害来源
        double magicDamage = allAttributes.getOrDefault("magic_damage", 0.0);
        sender.sendMessage("§6===== 魔法伤害分析 =====");
        sender.sendMessage("§e总魔法伤害: §f" + magicDamage);
        
        // 检查默认属性贡献
        Map<String, Double> defaultAttrs = attrManager.getDefaultAttributes();
        double defaultMagicDamage = defaultAttrs.getOrDefault("magic_damage", 0.0);
        sender.sendMessage("§e默认魔法伤害: §f" + defaultMagicDamage);
        
        // 检查种族贡献
        if (race != null) {
            sender.sendMessage("§e种族魔法伤害: §f需要查看种族配置文件");
        }
        
        sender.sendMessage("§6========================");
        
        return true;
    }
}