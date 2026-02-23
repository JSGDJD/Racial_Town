package org.HUD.hotelRoom.family;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;

import org.HUD.hotelRoom.family.storage.FamilyStorage;

public class WarehouseSystem {
    private final Plugin plugin;
    private final FileConfiguration familyConfig; // 引用家族配置
    private final Map<UUID, Inventory> warehouses;
    private final List<String> itemBlacklist;
    private final int warehouseRows;
    private final FamilyStorage storage;

    public WarehouseSystem(Plugin plugin, FileConfiguration familyConfig, FamilyStorage storage) {
        this.plugin = plugin;
        this.familyConfig = familyConfig; // 保存家族配置引用
        this.warehouses = new HashMap<>();
        this.itemBlacklist = new ArrayList<>();
        
        // 从配置文件加载仓库配置
        loadConfig();
        
        this.warehouseRows = familyConfig.getInt("warehouse.rows", 6);
        this.storage = storage;
    }

    private void loadConfig() {
        // 加载黑名单物品
        itemBlacklist.addAll(familyConfig.getStringList("warehouse.blacklist"));
    }

    public Inventory getWarehouse(UUID familyId) {
        if (!warehouses.containsKey(familyId)) {
            // 创建或加载仓库
            Inventory warehouse = createWarehouse(familyId);
            warehouses.put(familyId, warehouse);
            return warehouse;
        }
        return warehouses.get(familyId);
    }

    private Inventory createWarehouse(UUID familyId) {
        Family family = FamilyManager.getInstance().getFamily(familyId);
        String title = "家族仓库 - " + (family != null ? family.getName() : "未知家族");
        
        // 创建仓库界面
        Inventory warehouse = Bukkit.createInventory(null, warehouseRows * 9, title);
        
        // 从文件加载仓库内容
        loadWarehouseContent(familyId, warehouse);
        
        return warehouse;
    }

    public void saveWarehouse(UUID familyId) {
        Inventory warehouse = warehouses.get(familyId);
        if (warehouse != null) {
            saveWarehouseContent(familyId, warehouse);
        }
    }

    public void saveAllWarehouses() {
        for (UUID familyId : warehouses.keySet()) {
            saveWarehouse(familyId);
        }
    }

    public void deleteWarehouse(UUID familyId) {
        // 删除数据库中的仓库数据
        storage.deleteWarehouse(familyId);
        
        // 从内存中移除
        warehouses.remove(familyId);
    }

    private void loadWarehouseContent(UUID familyId, Inventory warehouse) {
        try {
            Map<Integer, byte[]> items = storage.loadWarehouse(familyId);
            
            for (Map.Entry<Integer, byte[]> entry : items.entrySet()) {
                int slot = entry.getKey();
                byte[] itemData = entry.getValue();
                
                if (itemData != null && slot < warehouse.getSize()) {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(itemData);
                         BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
                        ItemStack item = (ItemStack) ois.readObject();
                        warehouse.setItem(slot, item);
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().severe("Failed to deserialize item: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load warehouse content: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveWarehouseContent(UUID familyId, Inventory warehouse) {
        try {
            // 保存所有非空物品槽
            for (int i = 0; i < warehouse.getSize(); i++) {
                ItemStack item = warehouse.getItem(i);
                
                if (item != null) {
                    // 序列化物品 - 使用Bukkit的序列化方法
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                         BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
                        oos.writeObject(item);
                        byte[] itemData = bos.toByteArray();
                        
                        // 保存到数据库
                        storage.saveWarehouseItem(familyId, i, itemData);
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to serialize item: " + e.getMessage());
                        // 记录具体的错误信息，帮助调试
                        plugin.getLogger().warning("Error serializing item at slot " + i + ": " + 
                            (item.hasItemMeta() ? item.getItemMeta().toString() : item.getType().name()));
                    }
                } else {
                    // 删除空槽位
                    storage.deleteWarehouseItem(familyId, i);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save warehouse content: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean canAccessWarehouse(FamilyMember member) {
        if (member == null) {
            plugin.getLogger().warning("canAccessWarehouse: member is null");
            return false;
        }
        
        plugin.getLogger().info("Checking warehouse access for member " + member.getPlayerId() + " with position: " + member.getPosition());
        
        // 检查成员权限 - 现在使用正确的配置
        ConfigurationSection positionSection = familyConfig.getConfigurationSection("positions." + member.getPosition());
        if (positionSection != null) {
            boolean hasPermission = positionSection.getBoolean("permissions.access-warehouse", false);
            plugin.getLogger().info("Position " + member.getPosition() + " access-warehouse permission: " + hasPermission);
            return hasPermission;
        } else {
            plugin.getLogger().warning("Position section not found for: " + member.getPosition());
        }
        
        return false;
    }

    public boolean isItemAllowed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }
        
        String materialName = item.getType().name();
        return !itemBlacklist.contains(materialName);
    }

    public void openWarehouse(Player player, UUID familyId) {
        FamilyManager manager = FamilyManager.getInstance();
        FamilyMember member = manager.getMember(player);
        
        plugin.getLogger().info("Opening warehouse for player " + player.getName() + ", member: " + (member != null ? member.toString() : "null"));
        
        // 检查权限
        if (!canAccessWarehouse(member)) {
            player.sendMessage(familyConfig.getString("messages.warehouse.no-access", "&c你没有权限访问家族仓库！"));
            return;
        }
        
        // 检查家族ID是否匹配
        if (member != null && !member.getFamilyId().equals(familyId)) {
            player.sendMessage(familyConfig.getString("messages.common.error", "&c操作失败：%reason%")
                    .replace("%reason%", "你不是该家族的成员"));
            return;
        }
        
        // 打开仓库
        Inventory warehouse = getWarehouse(familyId);
        player.openInventory(warehouse);
    }

    public void closeWarehouse(UUID familyId) {
        saveWarehouse(familyId);
    }
}