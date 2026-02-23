package org.HUD.hotelRoom.family.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyManager;
import org.HUD.hotelRoom.family.FamilyMember;
import org.HUD.hotelRoom.family.WarehouseSystem;
import org.HUD.hotelRoom.family.gui.FamilyPositionGUI;
import org.HUD.hotelRoom.family.gui.FamilyJoinGUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FamilyMainGUI implements Listener {
    private static final String GUI_NAME = "家族主菜单";
    private static FamilyMainGUI instance;
    private final HotelRoom plugin;
    private final FamilyManager familyManager;
    private final FamilyPositionGUI positionGUI;
    private final FamilyJoinGUI joinGUI;
    
    // 用于存储正在创建家族的玩家
    private static final List<UUID> creatingFamilies = new ArrayList<>();
    // 用于存储正在加入家族的玩家
    private static final List<UUID> joiningFamilies = new ArrayList<>();
    // 用于存储正在邀请玩家的玩家
    private static final List<UUID> invitingPlayers = new ArrayList<>();
    // 用于存储正在解散家族的族长
    private static final List<UUID> disbandingFamilies = new ArrayList<>();
    // 用于存储正在退出家族的成员
    private static final List<UUID> leavingFamilies = new ArrayList<>();
    // 用于存储正在修改家族名称的玩家
    private static final List<UUID> renamingFamilies = new ArrayList<>();
    
    private FamilyMainGUI() {
        this.plugin = HotelRoom.get();
        this.familyManager = FamilyManager.getInstance();
        this.positionGUI = new FamilyPositionGUI(familyManager);
        this.joinGUI = new FamilyJoinGUI(familyManager);
        Bukkit.getPluginManager().registerEvents(this, HotelRoom.get());
    }
    
    public static FamilyMainGUI getInstance() {
        if (instance == null) {
            instance = new FamilyMainGUI();
        }
        return instance;
    }
    
    /** 打开家族主菜单 */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME); // 使用3行9列的完整空间
        FamilyMember member = familyManager.getMember(player);
        
        if (member == null) {
            // 玩家没有家族，打开创建/加入界面
            openFamilyCreationGUI(player);
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        // 添加功能按钮
        addFamilyInfoButton(inv, family, member);
        addBuffButton(inv, family, member);
        addUpgradeFamilyButton(inv, family, member);
        addAcceptApplicationButton(inv, family, member);
        addInvitePlayerButton(inv, family, member);
        addManagePositionsButton(inv, family, member);
        addTeleportAllButton(inv, family, member);
        addRenameFamilyButton(inv, family, member);
        addLeaveDisbandButton(inv, family, member);
        
        // 添加背景
        addBackground(inv);
        
        player.openInventory(inv);
    }
    
    /** 打开家族创建/加入界面 */
    private void openFamilyCreationGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "家族创建/加入");
        
        // 创建家族按钮
        ItemStack createButton = new ItemStack(Material.DIAMOND_BLOCK);
        ItemMeta meta = createButton.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "创建家族");
        createButton.setItemMeta(meta);
        inv.setItem(11, createButton);
        
        // 加入家族按钮
        ItemStack joinButton = new ItemStack(Material.EMERALD_BLOCK);
        meta = joinButton.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "加入家族");
        joinButton.setItemMeta(meta);
        inv.setItem(15, joinButton);
        
        // 我的邀请按钮
        ItemStack inviteButton = new ItemStack(Material.PAPER);
        meta = inviteButton.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "我的邀请");
        inviteButton.setItemMeta(meta);
        inv.setItem(23, inviteButton);
        
        player.openInventory(inv);
    }
    
    /** 处理GUI点击事件 */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        if (title.equals(GUI_NAME) || title.equals("家族创建/加入") || title.contains("我的邀请")) {
            event.setCancelled(true);
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }
            
            int slot = event.getSlot();
            
            if (title.equals("家族创建/加入")) {
                if (slot == 11 && clickedItem.getType() == Material.DIAMOND_BLOCK) {
                    // 点击创建家族
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "===== 创建家族 =====");
                    player.sendMessage(ChatColor.YELLOW + "请输入家族名称 (2-10 个字符)");
                    player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消创建");
                    creatingFamilies.add(player.getUniqueId());
                } else if (slot == 15 && clickedItem.getType() == Material.EMERALD_BLOCK) {
                    // 点击加入家族
                    joinGUI.open(player);
                } else if (slot == 23 && clickedItem.getType() == Material.PAPER) {
                    // 点击我的邀请
                    showMyInvitations(player);
                }
            } else if (title.equals(GUI_NAME)) {
                // 处理家族主菜单按钮点击
                switch (slot) {
                    case 4:
                        // 家族信息
                        handleFamilyInfo(player, event.isRightClick(), event.isShiftClick());
                        break;
                    case 10:
                        // 职位管理
                        handleManagePositions(player);
                        break;
                    case 11:
                        // 家族加成
                        handleBuffManagement(player);
                        break;
                    case 12:
                        // 升级家族
                        handleUpgradeFamily(player);
                        break;
                    case 13:
                        // 接受申请按钮
                        if (clickedItem.getType() == Material.EMERALD_BLOCK) {
                            // 打开申请管理界面
                            FamilyMember member = familyManager.getMember(player);
                            if (member != null) {
                                Family family = familyManager.getFamily(member.getFamilyId());
                                if (family != null) {
                                    try {
                                        Class<?> guiClass = Class.forName("org.HUD.hotelRoom.family.gui.FamilyApplicationGUI");
                                        Object guiInstance = guiClass.getMethod("getInstance").invoke(null);
                                        guiClass.getMethod("open", Player.class, Family.class).invoke(guiInstance, player, family);
                                    } catch (Exception e) {
                                        player.sendMessage(ChatColor.YELLOW + "申请管理功能加载中...");
                                    }
                                }
                            }
                        }
                        break;
                    case 14:
                        // 邀请玩家
                        handleInvitePlayer(player);
                        break;
                    case 15:
                        // 全员传送
                        handleTeleportAll(player);
                        break;
                    case 16:
                        // 退出/解散家族
                        handleLeaveDisband(player);
                        break;
                    case 23:
                        // 修改家族名称
                        handleRenameFamily(player);
                        break;
                }
            } else if (title.contains("我的邀请")) {
                // 处理我的邀请GUI
                String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                
                if (displayName.equals("返回")) {
                    openFamilyCreationGUI(player);
                } else if (displayName.equals("关闭")) {
                    player.closeInventory();
                } else if (clickedItem.getType() == Material.PAPER) {
                    // 处理邀请点击
                    String familyName = displayName;
                    Family family = familyManager.getFamilyByName(familyName);
                    if (family != null) {
                        if (event.isShiftClick()) {
                            // 拒绝邀请
                            familyManager.removeInvitation(player.getUniqueId(), family.getId());
                            player.sendMessage(ChatColor.GREEN + "已拒绝家族 " + familyName + " 的邀请！");
                            showMyInvitations(player);
                        } else {
                            // 接受邀请
                            boolean success = familyManager.acceptInvitation(family.getId(), player.getUniqueId());
                            if (success) {
                                player.sendMessage(ChatColor.GREEN + "成功加入家族 " + familyName + "！");
                                player.closeInventory();
                                // 需要在主线程中打开GUI
                                Bukkit.getScheduler().runTask(plugin, () -> open(player));
                            } else {
                                player.sendMessage(ChatColor.RED + "接受邀请失败！");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /** 处理玩家聊天事件 */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();
        
        // 处理创建家族
        if (creatingFamilies.contains(player.getUniqueId())) {
            event.setCancelled(true);
            creatingFamilies.remove(player.getUniqueId());
            familyManager.createFamily(player, msg);
            player.sendMessage(ChatColor.GREEN + "家族创建成功");
            // 重新打开GUI以确保状态更新
            open(player);
        }
        // 处理加入家族
        else if (joiningFamilies.contains(player.getUniqueId())) {
            event.setCancelled(true);
            joiningFamilies.remove(player.getUniqueId());
            // 这里应该调用加入家族的方法，先简化处理
            player.sendMessage(ChatColor.GREEN + "加入家族功能已处理");
        }
        // 处理邀请玩家
        else if (invitingPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            invitingPlayers.remove(player.getUniqueId());
            
            String targetPlayerName = msg;
            if (targetPlayerName.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "邀请已取消");
                return;
            }
            
            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "该玩家不在线！");
                return;
            }
            
            FamilyMember member = familyManager.getMember(player);
            if (member == null) {
                player.sendMessage(ChatColor.RED + "你不在家族中！");
                return;
            }
            
            Family family = familyManager.getFamily(member.getFamilyId());
            if (family == null) {
                player.sendMessage(ChatColor.RED + "你的家族不存在！");
                return;
            }
            
            // 检查权限
            if (!hasPermission(member, "invite")) {
                player.sendMessage(ChatColor.RED + "你没有邀请玩家的权限！");
                return;
            }
            
            // 检查目标玩家是否已经属于一个家族
            if (familyManager.getPlayerFamily(targetPlayer) != null) {
                player.sendMessage(ChatColor.RED + "该玩家已经属于一个家族！");
                return;
            }
            
            // 发送邀请
            boolean success = familyManager.sendInvitation(family.getId(), targetPlayer.getUniqueId());
            if (success) {
                player.sendMessage(ChatColor.GREEN + "已向玩家 " + targetPlayerName + " 发送邀请！");
                targetPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " 邀请你加入家族 " + family.getName() + "！");
                targetPlayer.sendMessage(ChatColor.GRAY + "使用 /hr family accept " + family.getName() + " 或在GUI中接受邀请");
            } else {
                player.sendMessage(ChatColor.RED + "发送邀请失败！该玩家可能已经被邀请或属于其他家族。");
            }
        }
        // 处理解散家族
        else if (disbandingFamilies.contains(player.getUniqueId())) {
            event.setCancelled(true);
            disbandingFamilies.remove(player.getUniqueId());
            
            if (msg.equalsIgnoreCase("confirm")) {
                FamilyMember member = familyManager.getMember(player);
                if (member != null) {
                    Family family = familyManager.getFamily(member.getFamilyId());
                    if (family != null && family.getLeaderId().equals(player.getUniqueId())) {
                        boolean success = familyManager.deleteFamily(family.getId());
                        if (success) {
                            player.sendMessage(ChatColor.GREEN + "家族已成功解散！");
                        } else {
                            player.sendMessage(ChatColor.RED + "解散家族失败！");
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "解散家族已取消");
            }
        }
        // 处理退出家族
        else if (leavingFamilies.contains(player.getUniqueId())) {
            event.setCancelled(true);
            leavingFamilies.remove(player.getUniqueId());
            
            if (msg.equalsIgnoreCase("confirm")) {
                FamilyMember member = familyManager.getMember(player);
                if (member != null) {
                    Family family = familyManager.getFamily(member.getFamilyId());
                    if (family != null) {
                        boolean success = familyManager.removeMember(family, player.getUniqueId());
                        if (success) {
                            player.sendMessage(ChatColor.GREEN + "已成功退出家族！");
                        } else {
                            player.sendMessage(ChatColor.RED + "退出家族失败！");
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "退出家族已取消");
            }
        }
        // 处理修改家族名称
        else if (renamingFamilies.contains(player.getUniqueId())) {
            event.setCancelled(true);
            renamingFamilies.remove(player.getUniqueId());
            
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "修改家族名称已取消");
                return;
            }
            
            // 检查名称长度
            if (msg.length() < 2 || msg.length() > 10) {
                player.sendMessage(ChatColor.RED + "家族名称必须在2-10个字符之间！");
                return;
            }
            
            // 检查是否有权限修改
            FamilyMember member = familyManager.getMember(player);
            if (member == null) {
                player.sendMessage(ChatColor.RED + "你不在家族中！");
                return;
            }
            
            Family family = familyManager.getFamily(member.getFamilyId());
            if (family == null) {
                player.sendMessage(ChatColor.RED + "你的家族不存在！");
                return;
            }
            
            if (!family.getLeaderId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "只有族长才能修改家族名称！");
                return;
            }
            
            // 检查新名称是否已存在
            if (familyManager.getFamilyByName(msg) != null) {
                player.sendMessage(ChatColor.RED + "该家族名称已被使用！");
                return;
            }
            
            // 修改家族名称
            boolean success = familyManager.renameFamily(family.getId(), msg);
            if (success) {
                player.sendMessage(ChatColor.GREEN + "家族名称已成功修改为: " + msg);
                // 重新打开GUI以显示新名称
                open(player);
            } else {
                player.sendMessage(ChatColor.RED + "修改家族名称失败！");
            }
        }
    }
    
    /** 处理职位管理 */
    private void handleManagePositions(Player player) {
        player.closeInventory();
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不在家族中！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        positionGUI.open(player, family);
    }
    
    /** 处理家族加成管理 */
    private void handleBuffManagement(Player player) {
        player.closeInventory();
        // 检查是否有FamilyBuffGUI类
        try {
            Class<?> guiClass = Class.forName("org.HUD.hotelRoom.family.gui.FamilyBuffGUI");
            Object guiInstance = guiClass.getMethod("getInstance").invoke(null);
            guiClass.getMethod("open", Player.class).invoke(guiInstance, player);
        } catch (Exception e) {
            player.sendMessage(ChatColor.YELLOW + "家族加成功能暂未实现");
        }
    }
    
    /** 处理升级家族 */
    private void handleUpgradeFamily(Player player) {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "升级家族功能暂未实现");
    }
    
    /** 处理家族信息 */
    private void handleFamilyInfo(Player player, boolean isRightClick, boolean isShiftClick) {
        player.closeInventory();
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不在家族中！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        if (isRightClick) {
            // 右键：打开家族仓库
            WarehouseSystem warehouseSystem = familyManager.getWarehouseSystem();
            // 添加调试信息
            HotelRoom.get().getLogger().info("Player " + player.getName() + " attempting to access warehouse, position: " + member.getPosition());
            warehouseSystem.openWarehouse(player, family.getId());
        } else {
            // 左键：查看成员列表
            // 检查是否有FamilyMemberListGUI类
            try {
                Class<?> guiClass = Class.forName("org.HUD.hotelRoom.family.gui.FamilyMemberListGUI");
                Object guiInstance = guiClass.getMethod("getInstance").invoke(null);
                guiClass.getMethod("open", Player.class).invoke(guiInstance, player);
            } catch (Exception e) {
                player.sendMessage(ChatColor.YELLOW + "成员列表功能暂未实现");
            }
        }
    }
    
    /** 处理邀请玩家 */
    private void handleInvitePlayer(Player player) {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "===== 邀请玩家 =====");
        player.sendMessage(ChatColor.YELLOW + "请输入要邀请的玩家名称");
        player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消邀请");
        invitingPlayers.add(player.getUniqueId());
    }
    
    /** 处理全员传送 */
    private void handleTeleportAll(Player player) {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "全员传送功能暂未实现");
    }
    
    /** 处理退出/解散家族 */
    private void handleLeaveDisband(Player player) {
        player.closeInventory();
        
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不在家族中！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        if (family.getLeaderId().equals(player.getUniqueId())) {
            // 族长解散家族
            player.sendMessage(ChatColor.YELLOW + "===== 解散家族 =====");
            player.sendMessage(ChatColor.RED + "警告：这将永久删除家族及所有数据！");
            player.sendMessage(ChatColor.YELLOW + "请输入 'confirm' 确认解散，输入其他内容取消");
            disbandingFamilies.add(player.getUniqueId());
        } else {
            // 成员退出家族
            player.sendMessage(ChatColor.YELLOW + "===== 退出家族 =====");
            player.sendMessage(ChatColor.YELLOW + "请输入 'confirm' 确认退出，输入其他内容取消");
            leavingFamilies.add(player.getUniqueId());
        }
    }
    
    /** 处理修改家族名称 */
    private void handleRenameFamily(Player player) {
        player.closeInventory();
        
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不在家族中！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        // 只有族长才能修改家族名称
        if (!family.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有族长才能修改家族名称！");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "===== 修改家族名称 =====");
        player.sendMessage(ChatColor.YELLOW + "请输入新的家族名称 (2-10 个字符)");
        player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消修改");
        
        // 使用专门的列表跟踪正在修改家族名称的玩家
        renamingFamilies.add(player.getUniqueId());
    }
    
    /** 添加背景 */
    private static void addBackground(Inventory inv) {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        meta.setDisplayName("");
        background.setItemMeta(meta);
        
        for (int i = 0; i < inv.getSize(); i++) {
            // 检查是否已有按钮，如果没有则设置背景
            if (inv.getItem(i) == null) {
                inv.setItem(i, background);
            }
        }
    }
    
    /** 添加家族信息按钮 */
    private static void addFamilyInfoButton(Inventory inv, Family family, FamilyMember member) {
        ItemStack button = new ItemStack(Material.BOOK);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "家族信息");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "名称：" + ChatColor.WHITE + family.getName());
        lore.add(ChatColor.YELLOW + "等级：" + ChatColor.WHITE + family.getLevel());
        lore.add(ChatColor.YELLOW + "成员：" + ChatColor.WHITE + family.getMemberCount() + "/" + getMemberLimit(family.getLevel()));
        lore.add(ChatColor.YELLOW + "活跃值：" + ChatColor.WHITE + family.getActivity());
        lore.add(ChatColor.YELLOW + "荣誉值：" + ChatColor.WHITE + family.getHonor());
        lore.add("");
        lore.add(ChatColor.GRAY + "左键查看成员列表");
        lore.add(ChatColor.GRAY + "右键打开家族仓库");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(4, button); // 将家族信息按钮移到slot 4
    }
    
    /** 添加升级家族按钮 */
    private static void addUpgradeFamilyButton(Inventory inv, Family family, FamilyMember member) {
        if (!hasPermission(member, "upgrade")) {
            return;
        }
        
        ItemStack button = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "升级家族");
        
        List<String> lore = new ArrayList<>();
        int currentLevel = family.getLevel();
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "当前等级：" + ChatColor.WHITE + currentLevel);
        lore.add(ChatColor.YELLOW + "下一级：" + ChatColor.WHITE + (currentLevel + 1));
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击升级");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(12, button);
    }
    
    /** 添加邀请玩家按钮 */
    private static void addInvitePlayerButton(Inventory inv, Family family, FamilyMember member) {
        if (!hasPermission(member, "invite")) {
            return;
        }
        
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "邀请玩家");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "邀请新成员加入家族");
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击邀请玩家");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(14, button);
    }
    
    /** 添加接受申请按钮 */
    private static void addAcceptApplicationButton(Inventory inv, Family family, FamilyMember member) {
        // 只有族长和副族长才能看到这个按钮
        String position = member.getPosition();
        if (!"leader".equals(position) && !"vice_leader".equals(position)) {
            return;
        }
        
        ItemStack button = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "接受申请");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "查看和处理加入申请");
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击查看申请列表");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(13, button);
    }
    
    /** 添加家族加成按钮 */
    private static void addBuffButton(Inventory inv, Family family, FamilyMember member) {
        ItemStack button = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "家族加成");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "查看和激活家族加成");
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击查看加成列表");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(11, button);
    }
    
    /** 添加职位管理按钮 */
    private static void addManagePositionsButton(Inventory inv, Family family, FamilyMember member) {
        if (!hasPermission(member, "manage_positions")) {
            return;
        }
        
        ItemStack button = new ItemStack(Material.COMPASS);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "职位管理");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "管理家族成员职位");
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击打开职位管理");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(10, button);
    }
    
    /** 添加全员传送按钮 */
    private static void addTeleportAllButton(Inventory inv, Family family, FamilyMember member) {
        if (!hasPermission(member, "teleport_all")) {
            return;
        }
        
        ItemStack button = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "全员传送");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "将所有在线成员传送到你身边");
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击使用全员传送");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(15, button);
    }
    
    /** 添加修改家族名称按钮 */
    private static void addRenameFamilyButton(Inventory inv, Family family, FamilyMember member) {
        // 只有族长才能看到修改名称按钮
        if (!family.getLeaderId().equals(member.getPlayerId())) {
            return;
        }
        
        ItemStack button = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "修改家族名称");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "当前名称：" + ChatColor.WHITE + family.getName());
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击修改家族名称");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(23, button); // 设置在第3行中间位置
    }
    
    /** 添加退出/解散家族按钮 */
    private static void addLeaveDisbandButton(Inventory inv, Family family, FamilyMember member) {
        ItemStack button;
        String name;
        
        if (family.getLeaderId().equals(member.getPlayerId())) {
            // 族长可以解散家族
            button = new ItemStack(Material.BARRIER);
            name = ChatColor.RED + "解散家族";
        } else {
            // 成员只能退出家族
            button = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            name = ChatColor.RED + "退出家族";
        }
        
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "确认要 " + (name.contains("解散") ? "解散" : "退出") + " 家族吗？");
        lore.add("");
        lore.add(ChatColor.GRAY + "左键点击确认");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(16, button);
    }
    
    /** 检查玩家是否有权限执行操作 */
    private static boolean hasPermission(FamilyMember member, String permission) {
        // 族长拥有所有权限
        if (member.getPosition().equals("leader")) {
            return true;
        }
        
        // 长老拥有大部分权限
        if (member.getPosition().equals("elder")) {
            return permission.equals("invite") || permission.equals("kick");
        }
        
        return false;
    }
    
    /** 显示我的邀请 */
    private void showMyInvitations(Player player) {
        player.closeInventory();
        
        List<Family> invites = familyManager.getPlayerInvitations(player.getUniqueId());
        
        if (invites.isEmpty()) {
            player.sendMessage(ChatColor.RED + "你没有收到任何家族邀请！");
            return;
        }
        
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.BLUE + "我的邀请");
        
        // 标题
        ItemStack titleItem = new ItemStack(Material.PAPER);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName(ChatColor.GOLD + "家族邀请列表");
        List<String> titleLore = new ArrayList<>();
        titleLore.add(ChatColor.GRAY + "点击邀请接受或拒绝");
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);
        
        // 填充邀请列表
        int slot = 9;
        for (Family family : invites) {
            if (slot >= 45) break;
            
            ItemStack inviteItem = createInviteItem(family);
            inv.setItem(slot, inviteItem);
            slot++;
        }
        
        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "返回");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "点击返回创建/加入界面");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inv.setItem(48, backItem);
        
        // 关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "关闭");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "点击关闭界面");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(49, closeItem);
        
        player.openInventory(inv);
    }
    
    /** 创建邀请物品 */
    private ItemStack createInviteItem(Family family) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + family.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + family.getLevel());
        lore.add(ChatColor.YELLOW + "成员: " + ChatColor.WHITE + family.getMemberCount() + "/" + getMemberLimit(family.getLevel()));
        lore.add(ChatColor.YELLOW + "族长: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(family.getLeaderId()).getName());
        lore.add("");
        lore.add(ChatColor.GREEN + "点击接受邀请");
        lore.add(ChatColor.RED + "Shift+点击拒绝邀请");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /** 获取家族成员上限 */
    private static int getMemberLimit(int level) {
        return 5 + (level - 1) * 3;
    }
    
    /** 处理仓库关闭事件，保存仓库内容 */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        // 检查是否是家族仓库
        if (title.startsWith("家族仓库 - ")) {
            FamilyMember member = familyManager.getMember(player);
            
            if (member != null) {
                Family family = familyManager.getFamily(member.getFamilyId());
                if (family != null) {
                    WarehouseSystem warehouseSystem = familyManager.getWarehouseSystem();
                    warehouseSystem.saveWarehouse(family.getId());
                }
            }
        }
    }
}