package org.HUD.hotelRoom.gui;

import org.HUD.hotelRoom.gui.CooldownManager;
import org.HUD.hotelRoom.util.DailyHonorManager;
import org.HUD.hotelRoom.util.SQLiteStorage;
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

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public final class HonorGUI implements Listener {

    private static final String TITLE = "荣誉值操作";
    private static final int SIZE = 27;   // 3 行

    /** 打开指定玩家的荣誉 GUI */
    public static void open(Player operator, UUID targetUUID, String targetName) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE + " - " + targetName);
        refresh(inv, targetUUID, targetName);
        operator.openInventory(inv);
    }

    /** 刷新界面内容 */
    private static void refresh(Inventory inv, UUID targetUUID, String targetName) {
        inv.clear();
        int honor = SQLiteStorage.getHonor(targetUUID);
        int storedHonor = SQLiteStorage.getStoredHonor(targetUUID);
        String dailyLimitInfo = "";
        String storedLimitInfo = "";
        if (DailyHonorManager.isEnabled()) {
            dailyLimitInfo = " (今日已获: " + DailyHonorManager.getDailyHonor(targetUUID) + "/" + DailyHonorManager.getDailyHonorLimit() + ")";
            storedLimitInfo = " (暂存: " + storedHonor + "/" + DailyHonorManager.getStoredHonorLimit() + ")";
        }

        // 信息展示
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GREEN + targetName);
        infoMeta.setLore(Arrays.asList(
                "",
                ChatColor.YELLOW + "当前荣誉值: " + ChatColor.WHITE + honor + dailyLimitInfo,
                ChatColor.YELLOW + "暂存荣誉值: " + ChatColor.WHITE + storedHonor + storedLimitInfo,
                ""
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);

        // 增加按钮
        ItemStack add = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta addMeta = add.getItemMeta();
        addMeta.setDisplayName(ChatColor.GREEN + "↑ 增加荣誉值");
        add.setItemMeta(addMeta);
        inv.setItem(11, add);

        // 减少按钮
        ItemStack sub = new ItemStack(Material.RED_CONCRETE);
        ItemMeta subMeta = sub.getItemMeta();
        subMeta.setDisplayName(ChatColor.RED + "↓ 减少荣誉值");
        sub.setItemMeta(subMeta);
        inv.setItem(15, sub);
    }

    /* ========== 事件拦截 ========== */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith(TITLE)) return;
        e.setCancelled(true);          // 禁止拿取
        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String display = item.getItemMeta().getDisplayName();
        UUID target = lastOpenTarget.get(p.getUniqueId());
        if (target == null) { p.closeInventory(); return; }

// 在 HonorGUI.onClick 里
        if (display.contains("增加荣誉值")) {
            UUID operator = p.getUniqueId();
            if (!CooldownManager.check(operator, CooldownManager.Action.ADD)) {
                p.sendActionBar(ChatColor.RED + "冷却期内只能执行一次增加或减少！剩余：" + CooldownManager.getLeftTime(operator));
                return;
            }
            
            if (DailyHonorManager.isEnabled()) {
                int actualAdded = DailyHonorManager.addHonorWithLimit(target, 1);
                if (actualAdded > 0) {
                    CooldownManager.record(operator, CooldownManager.Action.ADD);
                    p.sendActionBar(ChatColor.GREEN + "已 +1 荣誉值 (实际+ " + actualAdded + ")");
                } else {
                    // 检查是否是由于达到每日限制
                    if (DailyHonorManager.hasReachedDailyLimit(target)) {
                        p.sendActionBar(ChatColor.RED + "该玩家已达到今日荣誉值上限！");
                    } else {
                        p.sendActionBar(ChatColor.RED + "荣誉值已存入暂存池");
                    }
                }
            } else {
                SQLiteStorage.addHonor(target, 1);
                CooldownManager.record(operator, CooldownManager.Action.ADD);
                p.sendActionBar(ChatColor.GREEN + "已 +1 荣誉值");
            }
            
            refresh(e.getInventory(), target, Bukkit.getOfflinePlayer(target).getName());
        } else if (display.contains("减少荣誉值")) {
            UUID operator = p.getUniqueId();
            if (!CooldownManager.check(operator, CooldownManager.Action.SUB)) {
                p.sendActionBar(ChatColor.RED + "冷却期内只能执行一次增加或减少！剩余：" + CooldownManager.getLeftTime(operator));
                return;
            }
            SQLiteStorage.addHonor(target, -1);
            CooldownManager.record(operator, CooldownManager.Action.SUB);
            refresh(e.getInventory(), target, Bukkit.getOfflinePlayer(target).getName());
            p.sendActionBar(ChatColor.RED + "已 -1 荣誉值");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().startsWith(TITLE))
            e.setCancelled(true);
    }

    /* ========== 记录当前打开的目标玩家 ========== */
    private static final Map<UUID, UUID> lastOpenTarget = new java.util.HashMap<>();

    public static void record(Player operator, UUID target) {
        lastOpenTarget.put(operator.getUniqueId(), target);
    }

    public static void unrecord(Player operator) {
        lastOpenTarget.remove(operator.getUniqueId());
    }
}