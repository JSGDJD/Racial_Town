package org.HUD.hotelRoom.race.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.HUD.hotelRoom.race.RaceVoiceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 种族语音指令
 * 用法: /racevoice
 */
public class RaceVoiceCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该指令只能由玩家执行！");
            return true;
        }
        
        RaceVoiceManager voiceManager = RaceVoiceManager.getInstance();
        if (voiceManager == null) {
            player.sendMessage(Component.text("§c种族语音系统未初始化！").color(NamedTextColor.RED));
            return true;
        }
        
        // 获取玩家种族
        String playerRace = voiceManager.getPlayerRace(player.getUniqueId());
        
        // 构建语音网页URL
        String url = voiceManager.getVoiceServerUrl() + "?uuid=" + player.getUniqueId().toString();
        
        // 创建可点击的URL组件
        Component clickableUrl = Component.text("  ⚡ 点击这里打开语音界面")
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(HoverEvent.showText(
                Component.text("点击打开种族语音聊天\n\n").color(NamedTextColor.YELLOW)
                    .append(Component.text("URL: ").color(NamedTextColor.GRAY))
                    .append(Component.text(url).color(NamedTextColor.WHITE))
            ));
        
        // 创建复制链接的组件
        Component copyUrl = Component.text("  📋 点击复制链接")
            .color(NamedTextColor.GRAY)
            .clickEvent(ClickEvent.copyToClipboard(url))
            .hoverEvent(HoverEvent.showText(
                Component.text("点击复制链接到剪贴板").color(NamedTextColor.YELLOW)
            ));
        
        // 发送消息给玩家
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  🎙️ 种族语音聊天").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  你的种族: ").color(NamedTextColor.GRAY)
            .append(Component.text(playerRace).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)));
        player.sendMessage(Component.text(""));
        player.sendMessage(clickableUrl);
        player.sendMessage(Component.text(""));
        player.sendMessage(copyUrl);
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        
        return true;
    }
}
