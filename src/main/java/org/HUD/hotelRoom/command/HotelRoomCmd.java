package org.HUD.hotelRoom.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.HUD.hotelRoom.HotelRoom;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.gui.CooldownManager;
import org.HUD.hotelRoom.gui.HonorReqSetGUI;
import org.HUD.hotelRoom.honor.HonorDecayManager;
import org.HUD.hotelRoom.race.RaceDataStorage;
import org.HUD.hotelRoom.race.RaceAttributeManager;
import org.HUD.hotelRoom.race.RaceExpManager;
import org.HUD.hotelRoom.race.RaceVoiceManager;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.HUD.hotelRoom.family.FamilyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class HotelRoomCmd implements TabExecutor {

    private static final String[] SUB = {
            "createhotel","createofficialhotel","givehotel","removehotel","addhotelplayer","removehotelplayer","honor","sethonorreq","claim","reload","abandon",
            "sethonorreqgui","home"
    };

    /* ======== TabComplete ======== */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull org.bukkit.command.Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if(args.length==1) return List.of(SUB);                       // 子命令
        String sub = args[0];
        if(args.length==2 && (sub.equals("givehotel")||sub.equals("removehotel")||
                sub.equals("addhotelplayer")||sub.equals("removehotelplayer"))) {
            return List.copyOf(SelectionMgr.HOTELS.keySet());         // 酒店名
        }
        if(args.length==3 && (sub.equals("givehotel")||sub.equals("addhotelplayer")||
                sub.equals("removehotelplayer"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).collect(Collectors.toList());
        }
        if(args.length==2 && sub.equals("honor")) return List.of("add","set","remove");
        if(args.length==3 && sub.equals("honor")) return Bukkit.getOnlinePlayers()
                .stream().map(Player::getName).collect(Collectors.toList());
        if(args.length==2 && sub.equals("sethonorreq"))
            return List.copyOf(SelectionMgr.HOTELS.keySet());
        if(args.length==2 && sub.equals("createhotel"))
            return List.of("","system");   // 提示可输入 system
        if(args.length==2 && sub.equals("createofficialhotel"))
            return List.of("","shop","residential","office","apartment","public","default"); // 提示官方房屋类型

        if(args.length==2 && sub.equals("abandon") && sender instanceof Player) {
            // 返回玩家拥有的私有房屋和领取的公共房屋
            Player player = (Player) sender;
            List<String> playerHotels = SelectionMgr.HOTELS.values().stream()
                    .filter(hotel -> hotel.owner.equals(player.getUniqueId()) || 
                           (hotel.isPublic && hotel.members.contains(player.getUniqueId())))
                    .map(hotel -> hotel.name)
                    .collect(Collectors.toList());
            return playerHotels;
        }
        
        if(args.length==2 && sub.equals("claim")) {
            // 返回所有公共房屋名称
            List<String> publicHotels = SelectionMgr.HOTELS.values().stream()
                    .filter(hotel -> hotel.isPublic)
                    .map(hotel -> hotel.name)
                    .collect(Collectors.toList());
            return publicHotels;
        }

        return List.of();
    }

    /* ======== 命令分发 ======== */
    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull org.bukkit.command.Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if(args.length>=1&&args[0].startsWith("hr"))return false; // 让 Brigadier 接管
        if(args.length==0){sender.sendMessage("§c用法: /hotelroom <子命令> [参数...]");return true;}
        String sub = args[0];
        switch(sub){
            case "createhotel": return new CreateCmd(SelectionMgr.getInst()).onCommand(sender,command,label,dropFirst(args));
            case "createofficialhotel": return new CreateOfficialHotelCmd(SelectionMgr.getInst()).onCommand(sender,command,label,dropFirst(args));
            case "givehotel":   return new GiveHotelCmd().onCommand(sender,command,label,dropFirst(args));
            case "removehotel": return new RemoveHotelCmd().onCommand(sender,command,label,dropFirst(args));
            case "addhotelplayer":    return new AddHotelPlayerCmd().onCommand(sender,command,label,dropFirst(args));
            case "removehotelplayer": return new RemoveHotelPlayerCmd().onCommand(sender,command,label,dropFirst(args));
            case "honor":          return new HonorCmd().onCommand(sender,command,label,dropFirst(args));
            case "sethonorreq":    return new SetHonorReqCmd().onCommand(sender,command,label,dropFirst(args));
            case "claim": return new ClaimCmd().onCommand(sender,command,label,dropFirst(args));
            case "reload":
                if (!sender.hasPermission("hotelroom.admin")) {
                    sender.sendMessage("§c你没有权限。");
                    return true;
                }
                HonorDecayManager.reload(HotelRoom.get().getConfig());
                HotelRoom.get().reloadConfig();
                CooldownManager.reload(HotelRoom.get().getConfig());
                HotelRoom.get().getHouseConfig().load();
                
                // 重新加载伤害日志配置
                org.HUD.hotelRoom.attribute.AttributeManager.loadDamageLogConfig();
                
                // 重新加载保护监听器的配置
                HotelRoom.get().getProtectionListener().reloadConfig();
                
                // 重新加载每日荣誉值限制系统配置
                org.HUD.hotelRoom.util.DailyHonorManager.loadConfig(HotelRoom.get().getConfig());
                
                // 重新加载种族经验系统配置
                if (RaceExpManager.getInstance() != null) {
                    RaceExpManager.getInstance().reload();
                }
                
                // 重新加载属性系统
                if (AttributeManager.getInstance() != null) {
                    AttributeManager.getInstance().reload();
                }
                
                // 重新加载种族属性系统
                if (RaceAttributeManager.getInstance() != null) {
                    RaceAttributeManager.getInstance().reload();
                }
                
                // 重新加载种族进化GUI配置
                org.HUD.hotelRoom.gui.RaceEvolutionGUI.reloadConfig();

                // 重新加载种族配置
                RaceDataStorage.reloadRaceConfigs();
                
                // 重新加载种族语音系统配置并重启服务器
                if (RaceVoiceManager.getInstance() != null) {
                    RaceVoiceManager.getInstance().reload();
                    RaceVoiceManager.getInstance().restartServer();
                }
                
                // 重新加载家族系统配置
                if (FamilyManager.getInstance() != null) {
                    FamilyManager.getInstance().reload();
                }
                
                // 重新加载MythicMobs属性系统
                if (org.HUD.hotelRoom.attribute.MythicMobsAttributeManager.getInstance() != null) {
                    org.HUD.hotelRoom.attribute.MythicMobsAttributeManager.getInstance().reload();
                }
                
                sender.sendMessage("§a配置已重载！");
                sender.sendMessage("§e种族语音服务器已重启，新配置已生效！");
                sender.sendMessage("§eMythicMobs属性配置已重载！");
                return true;
            case "abandon": return new AbandonCmd().onCommand(sender,command,label,dropFirst(args));
            case "sethonorreqgui":
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§c只有玩家能使用此命令。");
                    return true;
                }
                HonorReqSetGUI.open(p);
                return true;

            case "home":
                return new HomeCommand().onCommand(sender, command, label, dropFirst(args));


            default:sender.sendMessage("§c未知子命令");return true;
        }
    }

    /* 去掉第一个元素，保持旧命令参数格式 */
    private String[] dropFirst(String[] args){
        if(args.length<=1) return new String[0];
        return Arrays.copyOfRange(args,1,args.length);
    }
}