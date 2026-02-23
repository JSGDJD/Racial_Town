package org.HUD.hotelRoom.gui;

import org.HUD.hotelRoom.util.HotelInfo;
import org.HUD.hotelRoom.util.SelectionMgr;
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
import java.util.Set;
import java.util.UUID;


public final class AbandonConfirmGUI implements Listener {

    private static final String TITLE = "确认放弃领地";
    private static final int SIZE = 27;

    /** 打开确认界面 */
    public static void open(Player player, HotelInfo info) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE + " - " + info.name);
        fillConfirm(inv, info);
        player.openInventory(inv);
    }

    private static void fillConfirm(Inventory inv, HotelInfo info) {
        inv.clear();

        // 信息牌
        ItemStack book = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setDisplayName(ChatColor.YELLOW + "你确定要放弃领地");
        bookMeta.setLore(java.util.List.of(
                ChatColor.GREEN + info.name,
                ChatColor.RED + "此操作不可撤销！"
        ));
        book.setItemMeta(bookMeta);
        inv.setItem(13, book);

        // 确认（绿 Wool）
        ItemStack yes = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yes.getItemMeta();
        yesMeta.setDisplayName(ChatColor.GREEN + "✔ 确认放弃");
        yes.setItemMeta(yesMeta);
        inv.setItem(11, yes);

        // 取消（红 Wool）
        ItemStack no = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = no.getItemMeta();
        noMeta.setDisplayName(ChatColor.RED + "✖ 取消");
        no.setItemMeta(noMeta);
        inv.setItem(15, no);
    }

    /* ---------------- 事件 ---------------- */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith(TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String name = item.getItemMeta().getDisplayName();
        String title = e.getView().getTitle();
        String hotelName = title.substring(title.lastIndexOf('-') + 1).trim();

        /* 统一拿引用，避免重复定义 */
        HotelInfo oldInfo = SelectionMgr.HOTELS.get(hotelName);
        if (oldInfo == null) { p.closeInventory(); return; }

        if (name.contains("确认放弃")) {
            // 检查是否为公共房屋
            if (oldInfo.isPublic) {
                // 对于公共房屋，只需从成员列表中移除玩家
                oldInfo.members.remove(p.getUniqueId());
                p.sendMessage(ChatColor.GREEN + "你已成功放弃公共房屋 '" + hotelName + "'。");
                p.closeInventory();
                return;
            }

            /* 1. 构造系统所有的新对象（私有房屋的处理），保留原有属性 */
            HotelInfo newInfo = new HotelInfo(
                    oldInfo.name,
                    new UUID(0, 0),          // 系统占位
                    oldInfo.corners,
                    oldInfo.isPublic,        // 保留公共属性
                    oldInfo.isOfficial,      // 保留官方属性
                    oldInfo.hotelType        // 保留类型
            );
            newInfo.members.addAll(oldInfo.members);   // 可选：保留白名单
            newInfo.facade.addAll(oldInfo.facade);     // 保留外观快照
            SelectionMgr.HOTELS.put(hotelName, newInfo);
            SelectionMgr.getInst().removeIndex(oldInfo);   // 反注册旧索引
            SelectionMgr.getInst().addIndex(newInfo);


            /* 2. 数据库更新 owner 即可 */
            SQLiteStorage.saveHotel(hotelName, newInfo.owner, newInfo.corners,
                                    oldInfo.isPublic, oldInfo.isOfficial, oldInfo.hotelType);

    /* 3. 不清 facade，也不把玩家方块塞进 facade！
       只清 placed_blocks，让新任主能拆任何东西 */
            SQLiteStorage.untrackPlaceInHotel(hotelName);

            p.sendMessage(ChatColor.GREEN + "领地 '" + hotelName + "' 已归还给系统，所有方块已随房赠送！");
            p.closeInventory();
        } else if (name.contains("取消")) {
            p.sendMessage(ChatColor.YELLOW + "已取消放弃操作。");
            p.closeInventory();
        }
    }


    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().startsWith(TITLE)) e.setCancelled(true);
    }
}