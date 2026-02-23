package org.HUD.hotelRoom.family.gui;

import org.HUD.hotelRoom.family.Family;
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
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FamilyApplicationGUI implements Listener {
    private static final String GUI_NAME = ChatColor.BLUE + "家族申请管理";
    private static FamilyApplicationGUI instance;
    private final Plugin plugin;
    private final FamilyManager familyManager;

    private FamilyApplicationGUI(Plugin plugin) {
        this.plugin = plugin;
        this.familyManager = FamilyManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static synchronized void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new FamilyApplicationGUI(plugin);
        }
    }

    public static FamilyApplicationGUI getInstance() {
        return instance;
    }

    /**
     * 打开家族申请管理界面
     * @param player 打开界面的玩家
     * @param family 要管理的家族
     */
    public void open(Player player, Family family) {
        // 创建一个54格的界面（6行9列）
        Inventory inv = Bukkit.createInventory(null, 54, GUI_NAME);

        // 添加标题项
        ItemStack titleItem = new ItemStack(Material.PAPER);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName(ChatColor.GOLD + "家族申请列表");
        List<String> titleLore = new ArrayList<>();
        titleLore.add(ChatColor.GRAY + "点击玩家接受或拒绝申请");
        titleMeta.setLore(titleLore);
        titleItem.setItemMeta(titleMeta);
        inv.setItem(4, titleItem);

        // 获取该家族的所有申请
        List<UUID> applications = familyManager.getFamilyApplications(family.getId());

        if (applications.isEmpty()) {
            // 如果没有申请，显示提示信息
            ItemStack noApplicationItem = new ItemStack(Material.BARRIER);
            ItemMeta noApplicationMeta = noApplicationItem.getItemMeta();
            noApplicationMeta.setDisplayName(ChatColor.RED + "没有申请");
            List<String> noApplicationLore = new ArrayList<>();
            noApplicationLore.add(ChatColor.GRAY + "当前没有玩家申请加入家族");
            noApplicationMeta.setLore(noApplicationLore);
            noApplicationItem.setItemMeta(noApplicationMeta);
            inv.setItem(22, noApplicationItem);
        } else {
            // 填充申请列表
            int slot = 9; // 从第二行开始
            for (UUID playerId : applications) {
                if (slot >= 45) break; // 只显示前36个申请

                ItemStack applicationItem = createApplicationItem(playerId);
                inv.setItem(slot, applicationItem);
                slot++;
            }
        }

        // 添加返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "返回");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "返回家族主菜单");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inv.setItem(48, backItem);

        // 添加关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "关闭");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "关闭申请管理界面");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(49, closeItem);

        // 打开界面
        player.openInventory(inv);
    }

    /**
     * 创建申请玩家的物品
     * @param playerId 玩家的UUID
     * @return 物品堆
     */
    private ItemStack createApplicationItem(UUID playerId) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        // 获取玩家名称
        String playerName = Bukkit.getOfflinePlayer(playerId).getName();
        meta.setDisplayName(ChatColor.YELLOW + (playerName != null ? playerName : "未知玩家"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "玩家ID: " + ChatColor.WHITE + playerId.toString());
        lore.add(ChatColor.YELLOW + "在线状态: " + ChatColor.WHITE + (Bukkit.getPlayer(playerId) != null ? "在线" : "离线"));
        lore.add("");
        lore.add(ChatColor.GREEN + "点击接受申请");
        lore.add(ChatColor.RED + "Shift+点击拒绝申请");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 处理GUI点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (!title.equals(GUI_NAME)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (slot == 48) {
            // 返回按钮
            player.closeInventory();
            FamilyMainGUI.getInstance().open(player);
        } else if (slot == 49) {
            // 关闭按钮
            player.closeInventory();
        } else if (slot >= 9 && slot < 45 && clickedItem.getType() == Material.PLAYER_HEAD) {
            // 处理申请项点击
            // 获取家族信息
            Family currentFamily = familyManager.getPlayerFamily(player);
            if (currentFamily == null) {
                player.sendMessage(ChatColor.RED + "你不属于任何家族！");
                player.closeInventory();
                return;
            }

            // 从物品lore中获取玩家ID
            UUID playerId = null;
            for (String line : clickedItem.getItemMeta().getLore()) {
                if (ChatColor.stripColor(line).startsWith("玩家ID: ")) {
                    String idStr = ChatColor.stripColor(line).replace("玩家ID: ", "");
                    try {
                        playerId = UUID.fromString(idStr);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "获取玩家信息失败！");
                        return;
                    }
                    break;
                }
            }

            if (playerId == null) {
                player.sendMessage(ChatColor.RED + "获取玩家信息失败！");
                return;
            }

            if (event.isShiftClick()) {
                // 拒绝申请
                familyManager.rejectApplication(playerId);
                player.sendMessage(ChatColor.GREEN + "已拒绝玩家 " + displayName + " 的申请！");
                open(player, currentFamily); // 重新打开界面
            } else {
                // 接受申请
                boolean success = familyManager.acceptApplication(playerId, currentFamily.getId());
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "已接受玩家 " + displayName + " 加入家族！");
                    Player targetPlayer = Bukkit.getPlayer(playerId);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(ChatColor.GREEN + "你的申请已被接受，成功加入家族 " + currentFamily.getName() + "！");
                    }
                    open(player, currentFamily); // 重新打开界面
                } else {
                    player.sendMessage(ChatColor.RED + "接受申请失败！该玩家可能已经加入其他家族。");
                }
            }
        }
    }
}