package org.HUD.hotelRoom.family.command;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyManager;
import org.HUD.hotelRoom.family.FamilyMember;
import org.HUD.hotelRoom.family.gui.FamilyMainGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FamilyCommand implements CommandExecutor {
    private final FamilyManager familyManager;

    public FamilyCommand(HotelRoom plugin) {
        this.familyManager = FamilyManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&c只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // 打开家族主GUI
            openMainGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept":
                handleAcceptInvite(player, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "disband":
                handleDisband(sender, args);
                break;
            case "setlevel":
                handleSetLevel(sender, args);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "rename":
                handleRename(player, args);
                break;
            case "disbandme":
                handlePlayerDisband(player, args);
                break;
            default:
                sender.sendMessage("&c未知的子命令！使用 /hr family 打开家族GUI。");
                sender.sendMessage("&c可用子命令：accept, invite, rename, disbandme");
        }

        return true;
    }

    private void openMainGUI(Player player) {
        FamilyMainGUI gui = FamilyMainGUI.getInstance();
        gui.open(player);
    }

    private void handleAcceptInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("&c用法：/hr family accept <家族名称|玩家名称>");
            player.sendMessage("&c使用家族名称接受邀请，使用玩家名称接受申请");
            return;
        }

        String targetName = args[1];
        
        // 尝试按家族名称处理（接受邀请）
        Family family = familyManager.getFamilyByName(targetName);
        if (family != null) {
            // 检查玩家是否已经属于一个家族
            if (familyManager.getPlayerFamily(player) != null) {
                player.sendMessage("&c你已经属于一个家族！");
                return;
            }

            // 接受邀请
            boolean success = familyManager.acceptInvitation(family.getId(), player.getUniqueId());
            
            if (success) {
                player.sendMessage("&a成功加入家族 &e" + family.getName() + "&a！");
                HotelRoom.get().getServer().broadcastMessage("&a玩家 &e" + player.getName() + " &a已加入家族 &e" + family.getName() + "&a！");
            } else {
                player.sendMessage("&c接受邀请失败！你可能没有收到该家族的邀请。");
            }
            return;
        }
        
        // 尝试按玩家名称处理（接受申请）
        Player targetPlayer = HotelRoom.get().getServer().getPlayer(targetName);
        if (targetPlayer == null) {
            player.sendMessage("&c找不到该玩家或家族！");
            return;
        }
        
        // 检查当前玩家是否是某个家族的管理员
        Family currentFamily = familyManager.getPlayerFamily(player);
        if (currentFamily == null) {
            player.sendMessage("&c你不属于任何家族！");
            return;
        }
        
        FamilyMember member = familyManager.getMember(player);
        if (member == null || !"leader".equals(member.getPosition())) {
            player.sendMessage("&c只有家族族长可以接受玩家申请！");
            return;
        }
        
        // 检查目标玩家是否有申请加入该家族
        UUID applicationFamilyId = familyManager.getPlayerApplication(targetPlayer.getUniqueId());
        if (applicationFamilyId == null || !applicationFamilyId.equals(currentFamily.getId())) {
            player.sendMessage("&c该玩家没有申请加入你的家族！");
            return;
        }
        
        // 接受申请
        boolean success = familyManager.acceptApplication(targetPlayer.getUniqueId(), currentFamily.getId());
        
        if (success) {
            player.sendMessage("&a成功接受玩家 &e" + targetName + " &a加入家族！");
            targetPlayer.sendMessage("&a你的申请已被接受，成功加入家族 &e" + currentFamily.getName() + "&a！");
            HotelRoom.get().getServer().broadcastMessage("&a玩家 &e" + targetName + " &a已加入家族 &e" + currentFamily.getName() + "&a！");
        } else {
            player.sendMessage("&c接受申请失败！该玩家可能已经加入其他家族。");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("hr.family.admin")) {
            sender.sendMessage("&c你没有权限执行此操作！");
            return;
        }

        familyManager.reload();
        sender.sendMessage("&a家族系统配置已成功重载！");
    }

    private void handleDisband(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hr.family.admin")) {
            sender.sendMessage("&c你没有权限执行此操作！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("&c用法：/hr family disband <家族名称>");
            return;
        }

        String familyName = args[1];
        Family family = familyManager.getFamilyByName(familyName);

        if (family == null) {
            sender.sendMessage("&c找不到该家族！");
            return;
        }

        boolean success = familyManager.deleteFamily(family.getId());
        if (success) {
            sender.sendMessage("&a成功解散家族 &e" + familyName + "&a！");
            // 广播家族解散消息
            HotelRoom.get().getServer().broadcastMessage("&c家族 &e" + familyName + " &c已被管理员强制解散！");
        } else {
            sender.sendMessage("&c解散家族失败！");
        }
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hr.family.admin")) {
            sender.sendMessage("&c你没有权限执行此操作！");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("&c用法：/hr family setlevel <家族名称> <等级>");
            return;
        }

        String familyName = args[1];
        int level;

        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("&c请输入有效的等级数字！");
            return;
        }

        Family family = familyManager.getFamilyByName(familyName);

        if (family == null) {
            sender.sendMessage("&c找不到该家族！");
            return;
        }

        family.setLevel(level);
        familyManager.getStorage().saveFamily(family);
        sender.sendMessage("&a成功将家族 &e" + familyName + "&a 的等级设置为 &e" + level + "&a！");
    }
    
    /**
     * 处理玩家解散自己的家族
     */
    private void handlePlayerDisband(Player player, String[] args) {
        Family family = familyManager.getPlayerFamily(player);
        if (family == null) {
            player.sendMessage("&c你不属于任何家族！");
            return;
        }
        
        if (!family.isLeader(player.getUniqueId())) {
            player.sendMessage("&c只有家族族长可以解散家族！");
            return;
        }
        
        // 确认解散
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage("&c确认要解散家族吗？这将删除所有家族数据！");
            player.sendMessage("&c使用 /hr family disbandme confirm 确认解散");
            return;
        }
        
        String familyName = family.getName();
        boolean success = familyManager.deleteFamily(family.getId());
        
        if (success) {
            player.sendMessage("&a成功解散家族 &e" + familyName + "&a！");
            HotelRoom.get().getServer().broadcastMessage("&c家族 &e" + familyName + " &c已被族长解散！");
        } else {
            player.sendMessage("&c解散家族失败！");
        }
    }
    
    /**
     * 处理修改家族名称
     */
    private void handleRename(Player player, String[] args) {
        Family family = familyManager.getPlayerFamily(player);
        if (family == null) {
            player.sendMessage("&c你不属于任何家族！");
            return;
        }
        
        if (!family.isLeader(player.getUniqueId())) {
            player.sendMessage("&c只有家族族长可以修改家族名称！");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("&c用法：/hr family rename <新家族名称>");
            return;
        }
        
        String newName = String.join(" ", args).substring(args[0].length() + 1);
        // 处理颜色代码
        newName = org.bukkit.ChatColor.translateAlternateColorCodes('&', newName);
        
        // 检查家族名称是否已存在
        if (familyManager.getFamilyByName(newName) != null) {
            player.sendMessage("&c该家族名称已存在！");
            return;
        }
        
        String oldName = family.getName();
        family.setName(newName);
        familyManager.getStorage().saveFamily(family);
        
        player.sendMessage("&a成功将家族名称从 &e" + oldName + "&a 修改为 &e" + newName + "&a！");
    }
    
    /**
     * 处理邀请玩家加入家族
     */
    private void handleInvite(Player player, String[] args) {
        Family family = familyManager.getPlayerFamily(player);
        if (family == null) {
            player.sendMessage("&c你不属于任何家族！");
            return;
        }
        
        FamilyMember member = familyManager.getMember(player);
        if (member == null || !"leader".equals(member.getPosition())) {
            player.sendMessage("&c只有家族族长可以邀请玩家！");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("&c用法：/hr family invite <玩家名称>");
            return;
        }
        
        String playerName = args[1];
        Player targetPlayer = HotelRoom.get().getServer().getPlayer(playerName);
        
        if (targetPlayer == null) {
            player.sendMessage("&c该玩家不在线！");
            return;
        }
        
        if (familyManager.getPlayerFamily(targetPlayer) != null) {
            player.sendMessage("&c该玩家已经属于一个家族！");
            return;
        }
        
        // 发送邀请
        boolean success = familyManager.sendInvitation(family.getId(), targetPlayer.getUniqueId());
        
        if (success) {
            player.sendMessage("&a已向玩家 &e" + targetPlayer.getName() + "&a 发送家族邀请！");
            targetPlayer.sendMessage("&e" + player.getName() + "&a 邀请你加入家族 &e" + family.getName() + "&a！");
            targetPlayer.sendMessage("&a使用 /hr family accept " + family.getName() + " 接受邀请");
        } else {
            player.sendMessage("&c发送邀请失败！该玩家可能已经被邀请或属于其他家族。");
        }
    }
}
