package org.HUD.hotelRoom.family.gui;

import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyMember;
import org.HUD.hotelRoom.family.FamilyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FamilyMemberListGUI implements Listener {
    private static final String GUI_NAME = ChatColor.GOLD + "家族成员列表";
    private static final int GUI_SIZE = 54;
    
    private static FamilyMemberListGUI instance;
    
    private final JavaPlugin plugin;
    private final FamilyManager familyManager;
    
    private FamilyMemberListGUI() {
        this.plugin = org.HUD.hotelRoom.HotelRoom.get();
        this.familyManager = FamilyManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public static FamilyMemberListGUI getInstance() {
        if (instance == null) {
            instance = new FamilyMemberListGUI();
        }
        return instance;
    }
    
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_NAME);
        
        // 获取玩家的家族信息
        FamilyMember member = familyManager.getMember(player);
        if (member == null) {
            player.sendMessage(ChatColor.RED + "你不属于任何家族！");
            return;
        }
        
        Family family = familyManager.getFamily(member.getFamilyId());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        // 设置GUI背景
        setBackground(inv);
        
        // 添加成员信息
        addMembers(inv, family);
        
        // 添加返回按钮
        addBackButton(inv);
        
        player.openInventory(inv);
    }
    
    /** 设置GUI背景 */
    private void setBackground(Inventory inv) {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        meta.setDisplayName(" ");
        background.setItemMeta(meta);
        
        for (int i = 0; i < GUI_SIZE; i++) {
            // 保留特定槽位不设置背景
            if (i == 49) continue; // 返回按钮位置
            if (inv.getItem(i) == null) { // 只设置未使用的槽位
                inv.setItem(i, background);
            }
        }
    }
    
    /** 添加成员信息 */
    private void addMembers(Inventory inv, Family family) {
        List<FamilyMember> members = new ArrayList<>();
        
        // 获取所有家族成员
        for (UUID memberId : family.getMemberIds()) {
            FamilyMember member = familyManager.getMember(memberId);
            if (member != null) {
                members.add(member);
            }
        }
        
        // 根据职位排序成员：族长 > 长老 > 成员
        Collections.sort(members, (m1, m2) -> {
            String pos1 = m1.getPosition();
            String pos2 = m2.getPosition();
            
            if (pos1.equals("leader")) return -1;
            if (pos2.equals("leader")) return 1;
            if (pos1.equals("elder")) return -1;
            if (pos2.equals("elder")) return 1;
            return 0;
        });
        
        // 添加成员到GUI
        int slot = 10;
        for (FamilyMember member : members) {
            if (slot >= 44) break; // 限制显示数量
            
            // 获取玩家对象，如果是离线玩家也尝试获取基本信息
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                ItemStack memberItem = createMemberItem(player, member, family);
                inv.setItem(slot, memberItem);
            } else {
                // 离线玩家的处理
                ItemStack memberItem = createOfflineMemberItem(member, family);
                inv.setItem(slot, memberItem);
            }
            
            // 布局：每行6个，跳过边缘
            if ((slot + 1) % 9 == 0) {
                slot += 3;
            } else {
                slot++;
            }
        }
    }
    
    /** 创建在线成员物品 */
    private ItemStack createMemberItem(Player player, FamilyMember member, Family family) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // 设置玩家头颅
        meta.setOwningPlayer(player);
        
        // 设置名称
        String name = ChatColor.WHITE + player.getName();
        String position = member.getPosition();
        
        if (position.equals("leader")) {
            name = ChatColor.GOLD + "[族长] " + player.getName();
        } else if (position.equals("elder")) {
            name = ChatColor.YELLOW + "[长老] " + player.getName();
        } else {
            name = ChatColor.GREEN + "[成员] " + player.getName();
        }
        
        meta.setDisplayName(name);
        
        // 设置Lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "职位: " + getPositionNameWithColor(position));
        lore.add(ChatColor.GRAY + "加入时间: " + formatTime(member.getJoinTime()));
        lore.add(ChatColor.GRAY + "最后登录: " + formatTime(member.getLastLoginTime()));
        
        // 显示在线状态
        if (player.isOnline()) {
            lore.add(ChatColor.GREEN + "状态: 在线");
        } else {
            lore.add(ChatColor.RED + "状态: 离线");
        }
        
        meta.setLore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    /** 创建离线成员物品 */
    private ItemStack createOfflineMemberItem(FamilyMember member, Family family) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // 对于离线玩家，设置一个默认头像
        String playerName = Bukkit.getOfflinePlayer(member.getPlayerId()).getName();
        if (playerName != null) {
            meta.setDisplayName(ChatColor.GRAY + playerName);
        } else {
            meta.setDisplayName(ChatColor.GRAY + "离线玩家");
        }
        
        // 设置名称
        String name = ChatColor.GRAY + (playerName != null ? playerName : "离线玩家");
        String position = member.getPosition();
        
        if (position.equals("leader")) {
            name = ChatColor.GOLD + "[族长] " + (playerName != null ? playerName : "离线玩家");
        } else if (position.equals("elder")) {
            name = ChatColor.YELLOW + "[长老] " + (playerName != null ? playerName : "离线玩家");
        } else {
            name = ChatColor.GREEN + "[成员] " + (playerName != null ? playerName : "离线玩家");
        }
        
        meta.setDisplayName(name);
        
        // 设置Lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "职位: " + getPositionNameWithColor(position));
        lore.add(ChatColor.GRAY + "加入时间: " + formatTime(member.getJoinTime()));
        lore.add(ChatColor.GRAY + "最后登录: " + formatTime(member.getLastLoginTime()));
        
        lore.add(ChatColor.RED + "状态: 离线");
        
        meta.setLore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    /** 获取带颜色的职位名称 */
    private String getPositionNameWithColor(String position) {
        switch (position) {
            case "leader": return ChatColor.GOLD + "族长";
            case "elder": return ChatColor.YELLOW + "长老";
            case "member": return ChatColor.WHITE + "成员";
            default: return ChatColor.GRAY + "未知";
        }
    }
    
    /** 获取职位中文名称 */
    private String getPositionName(String position) {
        switch (position) {
            case "leader": return "族长";
            case "elder": return "长老";
            case "member": return "成员";
            default: return "未知";
        }
    }
    
    /** 格式化时间 */
    private String formatTime(long time) {
        if (time <= 0) {
            return "未知";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(time);
    }
    
    /** 添加返回按钮 */
    private void addBackButton(Inventory inv) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "返回");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "点击返回家族主菜单");
        meta.setLore(lore);
        
        backButton.setItemMeta(meta);
        inv.setItem(49, backButton);
    }
    
    /** 处理GUI点击事件 */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        if (slot == 49) {
            // 返回按钮
            FamilyMainGUI mainGUI = FamilyMainGUI.getInstance();
            mainGUI.open(player);
        }
    }
}