package org.HUD.hotelRoom.family.gui;

import org.HUD.hotelRoom.HotelRoom;
import org.HUD.hotelRoom.family.Family;
import org.HUD.hotelRoom.family.FamilyManager;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FamilyJoinGUI implements Listener {
    private static final String TITLE = ChatColor.GREEN + "加入家族";
    private static final int SIZE = 54;
    private static final int FAMILY_PER_PAGE = 28;
    
    private final FamilyManager familyManager;
    private int currentPage = 0;
    
    public FamilyJoinGUI(FamilyManager familyManager) {
        this.familyManager = familyManager;
        Bukkit.getPluginManager().registerEvents(this, HotelRoom.get());
    }
    
    /** 打开家族列表GUI */
    public void open(Player player) {
        currentPage = 0;
        showFamilyList(player);
    }
    
    /** 显示家族列表 */
    private void showFamilyList(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        
        // 标题
        ItemStack titleItem = new ItemStack(Material.BOOK);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName(ChatColor.GOLD + "家族列表");
        List<String> titleLore = new ArrayList<>();
        titleLore.add(ChatColor.GRAY + "点击家族查看详细信息");
        titleLore.add(ChatColor.GRAY + "并申请加入");
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);
        
        // 获取所有家族
        List<Family> allFamilies = new ArrayList<>(familyManager.getAllFamilies());
        int totalPages = (int) Math.ceil((double) allFamilies.size() / FAMILY_PER_PAGE);
        
        // 确保页面有效
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
        
        // 填充家族列表
        int startIndex = currentPage * FAMILY_PER_PAGE;
        int endIndex = Math.min(startIndex + FAMILY_PER_PAGE, allFamilies.size());
        int slot = 9;
        
        for (int i = startIndex; i < endIndex; i++) {
            if (slot >= 45) break; // 避免覆盖底部控制按钮
            
            Family family = allFamilies.get(i);
            ItemStack familyItem = createFamilyItem(family);
            inv.setItem(slot, familyItem);
            slot++;
        }
        
        // 分页控制
        if (totalPages > 1) {
            // 上一页按钮
            if (currentPage > 0) {
                ItemStack prevPage = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevPage.getItemMeta();
                prevMeta.setDisplayName(ChatColor.BLUE + "上一页");
                List<String> prevLore = new ArrayList<>();
                prevLore.add(ChatColor.GRAY + "点击查看上一页");
                prevMeta.setLore(prevLore);
                prevPage.setItemMeta(prevMeta);
                inv.setItem(45, prevPage);
            }
            
            // 页码信息
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageInfo.getItemMeta();
            pageMeta.setDisplayName(ChatColor.GREEN + "第 " + (currentPage + 1) + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
            inv.setItem(49, pageInfo);
            
            // 下一页按钮
            if (currentPage < totalPages - 1) {
                ItemStack nextPage = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextPage.getItemMeta();
                nextMeta.setDisplayName(ChatColor.BLUE + "下一页");
                List<String> nextLore = new ArrayList<>();
                nextLore.add(ChatColor.GRAY + "点击查看下一页");
                nextMeta.setLore(nextLore);
                nextPage.setItemMeta(nextMeta);
                inv.setItem(53, nextPage);
            }
        }
        
        // 关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "关闭");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "点击关闭界面");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(48, closeItem);
        
        player.openInventory(inv);
    }
    
    /** 创建家族物品 */
    private ItemStack createFamilyItem(Family family) {
        ItemStack item = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + family.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + family.getLevel());
        lore.add(ChatColor.YELLOW + "成员: " + ChatColor.WHITE + family.getMemberCount() + "/" + getMemberLimit(family.getLevel()));
        lore.add(ChatColor.YELLOW + "族长: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(family.getLeaderId()).getName());
        lore.add(ChatColor.YELLOW + "荣誉值: " + ChatColor.WHITE + (int) family.getHonor());
        lore.add(ChatColor.YELLOW + "活跃值: " + ChatColor.WHITE + (int) family.getActivity());
        lore.add("");
        lore.add(ChatColor.GREEN + "点击查看详细信息");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /** 显示家族详细信息 */
    private void showFamilyDetail(Player player, Family family) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ChatColor.YELLOW + "家族详情 - " + family.getName());
        
        // 家族信息
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + family.getName());
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + family.getLevel());
        infoLore.add(ChatColor.YELLOW + "成员: " + ChatColor.WHITE + family.getMemberCount() + "/" + getMemberLimit(family.getLevel()));
        infoLore.add(ChatColor.YELLOW + "族长: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(family.getLeaderId()).getName());
        infoLore.add(ChatColor.YELLOW + "荣誉值: " + ChatColor.WHITE + (int) family.getHonor());
        infoLore.add(ChatColor.YELLOW + "活跃值: " + ChatColor.WHITE + (int) family.getActivity());
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "点击申请加入该家族");
        
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(13, infoItem);
        
        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "返回列表");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "点击返回家族列表");
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
        inv.setItem(50, closeItem);
        
        player.openInventory(inv);
    }
    
    /** 处理GUI点击事件 */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        
        Inventory inv = e.getInventory();
        String title = e.getView().getTitle();
        
        // 检查是否是我们的GUI
        if (!title.contains("加入家族") && !title.contains("家族详情")) return;
        
        e.setCancelled(true);
        
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        
        if (title.equals(TITLE)) {
            // 家族列表GUI
            if (displayName.equals("关闭")) {
                player.closeInventory();
            } else if (displayName.equals("上一页")) {
                if (currentPage > 0) {
                    currentPage--;
                    showFamilyList(player);
                }
            } else if (displayName.equals("下一页")) {
                currentPage++;
                showFamilyList(player);
            } else if (clickedItem.getType() == Material.GOLDEN_SWORD) {
                // 点击家族，显示详细信息
                String familyName = displayName;
                Family family = familyManager.getFamilyByName(familyName);
                if (family != null) {
                    showFamilyDetail(player, family);
                }
            }
        } else if (title.contains("家族详情")) {
            // 家族详情GUI
            if (displayName.equals("返回列表")) {
                showFamilyList(player);
            } else if (displayName.equals("关闭")) {
                player.closeInventory();
            } else if (clickedItem.getType() == Material.BOOK) {
                // 点击申请加入
                // 正确的家族名称提取方式
                String strippedTitle = ChatColor.stripColor(title);
                String familyName = strippedTitle.replace("家族详情 - ", "");
                Family family = familyManager.getFamilyByName(familyName);
                if (family != null) {
                    applyJoinFamily(player, family);
                } else {
                    // 添加调试信息
                    HotelRoom.get().getLogger().info("FamilyJoinGUI: 找不到家族 " + familyName);
                }
            }
        }
    }
    
    /** 处理GUI拖拽事件 */
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title.contains("加入家族") || title.contains("家族详情")) {
            e.setCancelled(true);
        }
    }
    
    /** 申请加入家族 */
    private void applyJoinFamily(Player player, Family family) {
        player.closeInventory();
        
        // 检查玩家是否已经属于一个家族
        if (familyManager.getPlayerFamily(player) != null) {
            player.sendMessage(ChatColor.RED + "你已经属于一个家族！");
            return;
        }
        
        // 检查家族是否满员
        if (family.getMemberCount() >= getMemberLimit(family.getLevel())) {
            player.sendMessage(ChatColor.RED + "该家族已经满员！");
            return;
        }
        
        // 发送加入申请
        boolean success = familyManager.sendApplication(player.getUniqueId(), family.getId());
        
        if (success) {
            // 发送申请通知给族长
            Player leader = Bukkit.getPlayer(family.getLeaderId());
            if (leader != null) {
                leader.sendMessage(ChatColor.GREEN + "玩家 " + ChatColor.WHITE + player.getName() + ChatColor.GREEN + " 申请加入你的家族 " + ChatColor.YELLOW + family.getName());
                leader.sendMessage(ChatColor.GREEN + "使用 /hr family accept " + player.getName() + " 接受申请");
            }
            
            player.sendMessage(ChatColor.GREEN + "申请已发送给家族族长 " + ChatColor.YELLOW + Bukkit.getOfflinePlayer(family.getLeaderId()).getName());
            player.sendMessage(ChatColor.GREEN + "请等待族长审核...");
        } else {
            player.sendMessage(ChatColor.RED + "发送申请失败！你可能已经申请过该家族或其他家族。");
        }
    }
    
    /** 获取家族成员上限 */
    private int getMemberLimit(int level) {
        return 5 + (level - 1) * 3;
    }
}