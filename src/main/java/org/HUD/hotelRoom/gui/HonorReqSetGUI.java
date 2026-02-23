package org.HUD.hotelRoom.gui;

import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HonorReqSetGUI implements Listener {

    private static final String TITLE = "设置领地荣誉门槛";
    private static final Map<UUID, String> pending = new HashMap<>();

    public static void open(Player p) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, TITLE);
        int slot = 0;
        for (var info : SelectionMgr.HOTELS.values()) {
            ItemStack item = new ItemStack(info.owner.equals(new UUID(0,0))
                    ? Material.LIME_BED : Material.RED_BED);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + info.name);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        pending.put(p.getUniqueId(), name);
        p.closeInventory();
        p.sendMessage(ChatColor.GREEN + "请输入数字设置领地 '" + name + "' 的荣誉门槛：");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!pending.containsKey(p.getUniqueId())) return;
        e.setCancelled(true);
        String input = e.getMessage().trim();
        String hotel = pending.remove(p.getUniqueId());
        try {
            int val = Integer.parseInt(input);
            SQLiteStorage.setHonorReq(hotel, val);
            p.sendMessage(ChatColor.GREEN + "领地 '" + hotel + "' 的荣誉门槛已设为 " + val);
        } catch (NumberFormatException ex) {
            p.sendMessage(ChatColor.RED + "输入无效，请输入整数！");
        }
    }
}
