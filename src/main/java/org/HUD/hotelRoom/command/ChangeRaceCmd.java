package org.HUD.hotelRoom.command;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.race.RaceDataStorage;
import org.HUD.hotelRoom.race.RaceAttributeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class ChangeRaceCmd implements TabExecutor {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull org.bukkit.command.Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            return RaceDataStorage.getAllRaces();
        }
        return List.of();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull org.bukkit.command.Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家能使用此命令。");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /changerace <种族名称>");
            player.sendMessage(ChatColor.GRAY + "可用种族: " + String.join(", ", RaceDataStorage.getAllRaces()));
            return true;
        }

        String targetRace = args[0].toLowerCase();
        UUID uuid = player.getUniqueId();
        String currentRace = RaceDataStorage.getPlayerRace(uuid);

        if (targetRace.equals(currentRace)) {
            player.sendMessage(ChatColor.RED + "你已经是这个种族了！");
            return true;
        }

        RaceDataStorage.RaceConfig targetConfig = RaceDataStorage.getRaceConfig(targetRace);
        if (targetConfig == null) {
            player.sendMessage(ChatColor.RED + "种族 " + targetRace + " 不存在！");
            player.sendMessage(ChatColor.GRAY + "可用种族: " + String.join(", ", RaceDataStorage.getAllRaces()));
            return true;
        }

        int currentLevel = RaceDataStorage.getPlayerRaceLevel(uuid);

        RaceDataStorage.RaceConfig currentConfig = RaceDataStorage.getRaceConfig(currentRace);
        String currentDisplayName = currentConfig != null ? currentConfig.displayName : currentRace;
        String targetDisplayName = targetConfig.displayName;

        RaceDataStorage.setPlayerRace(uuid, targetRace, 0, 0);

        player.sendMessage(ChatColor.GREEN + "你已经选择了 " + targetDisplayName + " 种族，等级已经清0。");

        org.bukkit.Bukkit.getScheduler().runTask(HotelRoom.get(), () -> {
            RaceAttributeManager attrManager = RaceAttributeManager.getInstance();
            if (attrManager != null) {
                attrManager.applyRaceAttributes(player);
            }
        });

        return true;
    }
}
