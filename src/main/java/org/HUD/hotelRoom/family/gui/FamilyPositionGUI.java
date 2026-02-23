package org.HUD.hotelRoom.family.gui;

import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyManager;
import org.HUD.hotelRoom.family.FamilyMember;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FamilyPositionGUI implements Listener {
    private static final String TITLE = "职位管理";
    private static final int SIZE = 54;
    
    private final FamilyManager manager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    
    public FamilyPositionGUI(FamilyManager manager) {
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
    }
    
    /** 打开职位管理GUI */
    public void open(Player player, Family family) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        refresh(inv, family);
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }
    
    /** 刷新职位管理GUI */
    private void refresh(Inventory inv, Family family) {
        inv.clear();
        
        // 标题
        ItemStack titleItem = new ItemStack(Material.NAME_TAG);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName(ChatColor.RED + "职位管理");
        List<String> titleLore = new ArrayList<>();
        titleLore.add(ChatColor.YELLOW + "点击成员头像查看可调整的职位");
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);
        
        // 填充成员列表
        List<FamilyMember> members = manager.getAllMembers(family.getId());
        int slot = 9;
        
        for (FamilyMember member : members) {
            if (slot >= SIZE - 9) break; // 避免覆盖底部区域
            
            Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
            String memberName = memberPlayer != null ? memberPlayer.getName() : "离线玩家";
            
            ItemStack memberItem = createPlayerHead(memberPlayer, member.getPlayerId(), memberName);
            ItemMeta meta = memberItem.getItemMeta();
            
            String positionName = manager.getConfig().getString("positions." + member.getPosition() + ".name", member.getPosition());
            meta.setDisplayName(ChatColor.YELLOW + memberName);
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "当前职位: " + ChatColor.WHITE + positionName);
            lore.add("");
            lore.add(ChatColor.GREEN + "点击查看可调整的职位");
            
            meta.setLore(lore);
            memberItem.setItemMeta(meta);
            
            inv.setItem(slot, memberItem);
            slot++;
        }
        
        // 关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "关闭");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "点击关闭职位管理");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(SIZE - 1, closeItem);
    }
    
    /** 创建玩家头像物品 */
    private ItemStack createPlayerHead(Player player, UUID playerId, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (player != null) {
            meta.setOwningPlayer(player);
        } else {
            // 离线玩家使用默认头像
            meta.setDisplayName(name);
        }
        
        head.setItemMeta(meta);
        return head;
    }
    
    /** 显示成员的可调整职位 */
    private void showAvailablePositions(Player player, Family family, FamilyMember member) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ChatColor.RED + "调整职位 - " + Bukkit.getOfflinePlayer(member.getPlayerId()).getName());
        
        // 标题
        ItemStack titleItem = new ItemStack(Material.NAME_TAG);
        ItemMeta titleMeta = titleItem.getItemMeta();
        String memberName = Bukkit.getOfflinePlayer(member.getPlayerId()).getName();
        titleMeta.setDisplayName(ChatColor.YELLOW + memberName + " 的职位调整");
        List<String> titleLore = new ArrayList<>();
        String currentPositionName = manager.getConfig().getString("positions." + member.getPosition() + ".name", member.getPosition());
        titleLore.add(ChatColor.GRAY + "当前职位: " + ChatColor.WHITE + currentPositionName);
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);
        
        // 填充可调整的职位
        List<String> availablePositions = getAvailablePositions(family, member);
        int slot = 18;
        
        for (String position : availablePositions) {
            if (slot >= SIZE - 9) break;
            
            String positionName = manager.getConfig().getString("positions." + position + ".name", position);// 检查职位最大数量
            int maxCount = manager.getConfig().getInt("positions." + position + ".max-count", -1);
            int currentCount = getPositionCount(family, position);
            
            ItemStack positionItem = new ItemStack(Material.IRON_BLOCK);
            ItemMeta meta = positionItem.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + positionName);
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "职位ID: " + ChatColor.WHITE + position);
            
            if (maxCount > 0) {
                lore.add(ChatColor.GRAY + "当前数量: " + ChatColor.WHITE + currentCount + "/" + maxCount);
            } else {
                lore.add(ChatColor.GRAY + "当前数量: " + ChatColor.WHITE + currentCount + "(无限制)");
            }
            
            if (position.equals(member.getPosition())) {
                lore.add(ChatColor.YELLOW + "当前职位");
            } else {
                lore.add(ChatColor.GREEN + "点击设置为该职位");
            }
            
            meta.setLore(lore);
            positionItem.setItemMeta(meta);
            
            inv.setItem(slot, positionItem);
            slot++;
        }
        
        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "返回");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "点击返回成员列表");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inv.setItem(SIZE - 1, backItem);
        
        player.openInventory(inv);
    }
    
    /** 获取可调整的职位列表 */
    private List<String> getAvailablePositions(Family family, FamilyMember member) {
        List<String> positions = new ArrayList<>();
        
        // 获取所有配置的职位
        for (String position : manager.getConfig().getConfigurationSection("positions").getKeys(false)) {
            // 跳过当前职位
            if (position.equals(member.getPosition())) {
                continue;
            }
            
            // 检查职位最大数量
            int maxCount = manager.getConfig().getInt("positions." + position + ".max-count", -1);
            if (maxCount > 0) {
                int currentCount = getPositionCount(family, position);
                if (currentCount >= maxCount) {
                    continue; // 职位已满
                }
            }
            
            // 不能将其他成员设置为族长
            if (position.equals("leader")) {
                continue;
            }
            
            positions.add(position);
        }
        
        return positions;
    }
    
    /** 获取指定职位的成员数量 */
    private int getPositionCount(Family family, String position) {
        int count = 0;
        for (FamilyMember member : manager.getAllMembers(family.getId())) {
            if (member.getPosition().equals(position)) {
                count++;
            }
        }
        return count;
    }
    
    /** 处理GUI点击事件 */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        
        Inventory inv = e.getInventory();
        String title = e.getView().getTitle();
        
        // 检查是否是我们的GUI
        if (!title.contains("职位管理") && !title.contains("调整职位")) return;
        
        e.setCancelled(true);
        
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        FamilyManager manager = FamilyManager.getInstance();
        FamilyMember currentMember = manager.getMember(player);
        if (currentMember == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "你不属于任何家族！");
            return;
        }
        
        Family family = manager.getFamily(currentMember.getFamilyId());
        if (family == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "你的家族不存在！");
            return;
        }
        
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        
        if (title.equals(TITLE)) {
            // 主职位管理GUI
            if (displayName.equals("关闭")) {
                player.closeInventory();
            } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
                // 点击玩家头像，显示可调整的职位
                String memberName = clickedItem.getItemMeta().getDisplayName();
                memberName = ChatColor.stripColor(memberName);
                
                Player targetPlayer = Bukkit.getPlayer(memberName);
                if (targetPlayer != null) {
                    FamilyMember targetMember = manager.getMember(targetPlayer);
                    if (targetMember != null && targetMember.getFamilyId().equals(family.getId())) {
                        showAvailablePositions(player, family, targetMember);
                    }
                }
            }
        } else if (title.contains("调整职位")) {
            // 职位调整GUI
            if (displayName.equals("返回")) {
                // 返回主职位管理GUI
                open(player, family);
            } else if (clickedItem.getType() == Material.IRON_BLOCK) {
                // 点击职位，设置成员职位
                String positionName = displayName;
                
                // 获取目标成员
                String memberName = title.replace(ChatColor.stripColor(ChatColor.RED + "调整职位 - "), "");
                Player targetPlayer = Bukkit.getPlayer(memberName);
                FamilyMember targetMember = null;
                
                if (targetPlayer != null) {
                    targetMember = manager.getMember(targetPlayer);
                } else {
                    // 查找离线玩家
                    for (FamilyMember member : manager.getAllMembers(family.getId())) {
                        if (Bukkit.getOfflinePlayer(member.getPlayerId()).getName().equals(memberName)) {
                            targetMember = member;
                            break;
                        }
                    }
                }
                
                if (targetMember != null) {
                    // 查找职位ID
                    String positionId = "";
                    for (String id : manager.getConfig().getConfigurationSection("positions").getKeys(false)) {
                        if (manager.getConfig().getString("positions." + id + ".name", id).equals(positionName)) {
                            positionId = id;
                            break;
                        }
                    }
                    
                    if (!positionId.isEmpty()) {
                        // 设置新职位
                        boolean success = manager.changePosition(family, targetMember.getPlayerId(), positionId);
                        
                        if (success) {
                            player.sendMessage(ChatColor.GREEN + "已将 " + memberName + " 的职位设置为 " + positionName);
                            
                            // 更新GUI
                            showAvailablePositions(player, family, targetMember);
                        } else {
                            player.sendMessage(ChatColor.RED + "职位调整失败！");
                        }
                    }
                }
            }
        }
    }
    
    /** 处理GUI拖拽事件 */
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title.contains("职位管理") || title.contains("调整职位")) {
            e.setCancelled(true);
        }
    }
}