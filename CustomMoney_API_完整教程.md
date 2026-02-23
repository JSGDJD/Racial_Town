# CustomMoney 经济系统 API 完整教程

## 文档简介

本文档是 CustomMoney 插件经济系统 API 的完整教程，整合了 API 使用示例和详细教程内容，为开发者提供全面的技术参考。

**适用插件**：CustomMoney  
**最后更新**：2026-02-10

---

## 目录

- [1. 插件概述](#1-插件概述)
- [2. 核心类结构](#2-核心类结构)
  - [2.1 CustomMoney 主类](#21-custommoney-主类)
  - [2.2 EconomyAPI 经济接口](#22-economyapi-经济接口)
- [3. 获取 API 实例](#3-获取-api-实例)
  - [3.1 方法一：直接获取](#31-方法一直接获取)
  - [3.2 方法二：通过插件实例获取](#32-方法二通过插件实例获取)
  - [3.3 方法三：使用静态方法获取](#33-方法三使用静态方法获取)
- [4. API 方法详解](#4-api-方法详解)
  - [4.1 获取余额](#41-获取余额)
  - [4.2 货币操作](#42-货币操作)
  - [4.3 余额检查](#43-余额检查)
  - [4.4 总价值计算](#44-总价值计算)
  - [4.5 货币验证](#45-货币验证)
- [5. 离线玩家支持](#5-离线玩家支持)
- [6. 完整代码示例](#6-完整代码示例)
  - [6.1 基本使用示例](#61-基本使用示例)
  - [6.2 商店功能示例](#62-商店功能示例)
- [7. 最佳实践](#7-最佳实践)
- [8. 常见问题](#8-常见问题)
- [9. 注意事项](#9-注意事项)
- [10. 总结](#10-总结)

---

## 1. 插件概述

CustomMoney 是一个 Minecraft Bukkit/Spigot 插件，提供了自定义货币系统和物品鉴定功能。本教程将详细介绍其对外提供的经济 API 的使用方法。

## 2. 核心类结构

### 2.1 CustomMoney 主类

`CustomMoney.java` 是插件的主类，负责初始化插件、数据库、监听器等组件，并提供获取 EconomyAPI 实例的方法。

```java
package org.HUD.customMoney;

// 导入语句...

public final class CustomMoney extends JavaPlugin {
    // 插件实例和组件
    private static CustomMoney instance;
    private DatabaseManager databaseManager;
    // 其他组件...

    @Override
    public void onEnable() {
        // 初始化插件...
        // 初始化经济API
        EconomyAPI.initialize(this);
    }

    // 获取API实例的静态方法
    public static EconomyAPI getEconomyAPI() {
        return EconomyAPI.getInstance();
    }

    // 其他方法...
}
```

### 2.2 EconomyAPI 经济接口

`EconomyAPI.java` 是对外提供的经济系统 API 类，使用单例模式，提供了丰富的经济操作方法。

```java
package org.HUD.customMoney;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.util.Map;

public class EconomyAPI {
    private static EconomyAPI instance;
    private final CustomMoney plugin;

    // 私有构造方法，单例模式
    private EconomyAPI(CustomMoney plugin) {
        this.plugin = plugin;
    }

    // 获取API实例
    public static synchronized EconomyAPI getInstance() {
        // 实现...
    }

    // 经济操作方法...
}
```

## 3. 获取 API 实例

### 3.1 方法一：直接获取

```java
import org.HUD.customMoney.EconomyAPI;

// 获取API实例
EconomyAPI economyAPI = EconomyAPI.getInstance();
```

### 3.2 方法二：通过插件实例获取

```java
import org.bukkit.plugin.java.JavaPlugin;
import org.HUD.customMoney.CustomMoney;
import org.HUD.customMoney.EconomyAPI;

// 获取插件实例
CustomMoney customMoneyPlugin = (CustomMoney) JavaPlugin.getPlugin(CustomMoney.class);

// 通过插件实例获取API
EconomyAPI economyAPI = customMoneyPlugin.getEconomyAPI();
```

### 3.3 方法三：使用静态方法获取

```java
import org.HUD.customMoney.CustomMoney;
import org.HUD.customMoney.EconomyAPI;

// 直接通过静态方法获取
EconomyAPI economyAPI = CustomMoney.getEconomyAPI();
```

## 4. API 方法详解

### 4.1 获取余额

#### 获取所有货币余额

```java
import org.bukkit.entity.Player;
import java.util.Map;

// 获取玩家所有货币余额
Map<String, Integer> balances = economyAPI.getBalances(player);

// 访问特定货币余额
int copper = balances.getOrDefault("copper", 0);   // 铜币
int silver = balances.getOrDefault("silver", 0);   // 银币
int gold = balances.getOrDefault("gold", 0);       // 金币
int spirit = balances.getOrDefault("spirit", 0);   // 灵币

// 输出余额信息
player.sendMessage("铜币: " + copper);
player.sendMessage("银币: " + silver);
player.sendMessage("金币: " + gold);
player.sendMessage("灵币: " + spirit);
```

#### 获取特定货币余额

```java
// 获取特定货币余额
int copper = economyAPI.getBalance(player, "copper");   // 铜币
int silver = economyAPI.getBalance(player, "silver");   // 银币
int gold = economyAPI.getBalance(player, "gold");       // 金币
int spirit = economyAPI.getBalance(player, "spirit");   // 灵币
```

### 4.2 货币操作

#### 给予玩家货币

```java
// 给予玩家100铜币
economyAPI.giveMoney(player, "copper", 100);

// 给予玩家10银币
economyAPI.giveMoney(player, "silver", 10);

// 给予玩家5金币
economyAPI.giveMoney(player, "gold", 5);

// 给予玩家1灵币
economyAPI.giveMoney(player, "spirit", 1);
```

#### 扣除玩家货币

```java
// 扣除玩家50铜币
boolean success1 = economyAPI.takeMoney(player, "copper", 50);
if (success1) {
    player.sendMessage("扣除成功！");
} else {
    player.sendMessage("余额不足！");
}

// 扣除玩家3银币
boolean success2 = economyAPI.takeMoney(player, "silver", 3);

// 扣除玩家5银币
boolean success3 = economyAPI.takeMoney(player, "silver", 5);
```

#### 设置玩家货币余额

```java
// 设置玩家铜币余额为200
economyAPI.setMoney(player, "copper", 200);

// 设置玩家银币余额为50
economyAPI.setMoney(player, "silver", 50);
```

### 4.3 余额检查

```java
// 检查玩家是否有300铜币
if (economyAPI.has(player, "copper", 300)) {
    player.sendMessage("你有足够的铜币！");
} else {
    player.sendMessage("铜币不足！");
}

// 检查玩家是否有20银币
if (economyAPI.has(player, "silver", 20)) {
    player.sendMessage("你有足够的银币！");
} else {
    player.sendMessage("银币不足！");
}

// 检查玩家是否有10金币
if (economyAPI.has(player, "gold", 10)) {
    player.sendMessage("你有足够的金币！");
} else {
    player.sendMessage("金币不足！");
}
```

### 4.4 总价值计算

```java
// 获取所有货币转换为铜币的总价值
int totalValue = economyAPI.getTotalValueInCopper(player);
player.sendMessage("总价值（铜币计）: " + totalValue);
```

### 4.5 货币验证

```java
// 检查货币类型是否有效
if (economyAPI.isValidCurrencyType("copper")) {
    // 有效货币类型
}

if (!economyAPI.isValidCurrencyType("invalid")) {
    // 无效货币类型
}
```

## 5. 离线玩家支持

所有 API 方法都支持离线玩家：

```java
import org.bukkit.OfflinePlayer;
import java.util.UUID;

// 获取离线玩家
OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString("玩家UUID"));

// 获取离线玩家余额
int balance = economyAPI.getBalance(offlinePlayer, "gold");

// 给予离线玩家货币
economyAPI.giveMoney(offlinePlayer, "silver", 10);

// 扣除离线玩家货币
boolean success = economyAPI.takeMoney(offlinePlayer, "copper", 50);
```

## 6. 完整代码示例

### 6.1 基本使用示例

```java
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.HUD.customMoney.CustomMoney;
import org.HUD.customMoney.EconomyAPI;

public class ExamplePlugin extends JavaPlugin implements Listener {
    private EconomyAPI economyAPI;

    @Override
    public void onEnable() {
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        // 初始化EconomyAPI
        if (getServer().getPluginManager().getPlugin("CustomMoney") != null) {
            economyAPI = CustomMoney.getEconomyAPI();
            getLogger().info("成功连接到CustomMoney经济系统！");
        } else {
            getLogger().severe("CustomMoney插件未安装或未加载！");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (economyAPI != null) {
            // 获取玩家余额
            int copper = economyAPI.getBalance(player, "copper");
            int silver = economyAPI.getBalance(player, "silver");
            int gold = economyAPI.getBalance(player, "gold");

            // 发送欢迎消息和余额信息
            player.sendMessage("欢迎回来！你的余额：");
            player.sendMessage("铜币: " + copper);
            player.sendMessage("银币: " + silver);
            player.sendMessage("金币: " + gold);

            // 给予玩家登录奖励
            economyAPI.giveMoney(player, "copper", 50);
            player.sendMessage("登录奖励：50铜币！");
        }
    }
}
```

### 6.2 商店功能示例

```java
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.HUD.customMoney.EconomyAPI;

public class ShopListener implements Listener {
    private EconomyAPI economyAPI;
    private static final int ITEM_PRICE = 10; // 物品价格（银币）

    public ShopListener(EconomyAPI economyAPI) {
        this.economyAPI = economyAPI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        // 检查是否点击了商店物品
        if (isShopItem(event.getCurrentItem())) {
            event.setCancelled(true);

            // 检查玩家余额
            if (economyAPI.has(player, "silver", ITEM_PRICE)) {
                // 扣除货币
                economyAPI.takeMoney(player, "silver", ITEM_PRICE);
                
                // 给予玩家物品
                givePlayerItem(player);
                
                player.sendMessage("购买成功！花费了 " + ITEM_PRICE + " 银币");
            } else {
                player.sendMessage("银币不足！需要 " + ITEM_PRICE + " 银币");
            }
            
            player.closeInventory();
        }
    }

    private boolean isShopItem(org.bukkit.inventory.ItemStack item) {
        // 检查是否为商店物品
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
               item.getItemMeta().getDisplayName().equals("特殊物品");
    }

    private void givePlayerItem(Player player) {
        // 给予玩家物品的逻辑
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(
                org.bukkit.Material.DIAMOND_SWORD);
        // 设置物品属性...
        player.getInventory().addItem(item);
    }
}
```

## 7. 最佳实践

1. **插件依赖声明**：在你的 `plugin.yml` 中声明对 CustomMoney 的依赖：
   ```yaml
   depend: [CustomMoney]
   # 或软依赖
   softdepend: [CustomMoney]
   ```

2. **API 可用性检查**：使用前检查 API 是否可用：
   ```java
   if (getServer().getPluginManager().getPlugin("CustomMoney") != null) {
       economyAPI = CustomMoney.getEconomyAPI();
   }
   ```

3. **货币类型验证**：使用前验证货币类型是否有效：
   ```java
   if (economyAPI.isValidCurrencyType(currencyType)) {
       // 使用货币
   }
   ```

4. **异常处理**：处理可能的异常情况：
   ```java
   try {
       int balance = economyAPI.getBalance(player, "gold");
   } catch (Exception e) {
       getLogger().severe("获取余额失败: " + e.getMessage());
   }
   ```

5. **性能优化**：避免频繁调用 API 方法，适当使用缓存：
   ```java
   // 缓存余额
   Map<String, Integer> balances = economyAPI.getBalances(player);
   // 多次使用缓存的余额
   int totalValue = economyAPI.getTotalValueInCopper(balances);
   ```

## 8. 常见问题

### 8.1 API 实例获取失败

**问题**：调用 `EconomyAPI.getInstance()` 时抛出异常。

**解决方法**：确保 CustomMoney 插件已正确加载，且在插件启用后再获取 API 实例。

### 8.2 货币操作失败

**问题**：给予或扣除货币时没有效果。

**解决方法**：检查货币类型是否正确，确保玩家对象有效，检查插件权限设置。

### 8.3 离线玩家操作失败

**问题**：对离线玩家的操作没有效果。

**解决方法**：确保离线玩家的 UUID 和名称正确，检查数据库连接是否正常。

## 9. 注意事项

1. **插件依赖**：确保 CustomMoney 插件已正确加载，否则 API 将无法使用
2. **货币类型**：所有货币类型名称不区分大小写（copper、Copper、COPPER 都可以）
3. **有效货币类型**：
   - `copper` - 铜币
   - `silver` - 银币
   - `gold` - 金币
   - `spirit` - 灵币
4. **参数验证**：API 方法都有完善的参数验证，无需担心空指针异常
5. **事务支持**：所有操作都支持事务，确保数据一致性

## 10. 总结

CustomMoney 经济系统提供了丰富的 API 方法，允许其他插件轻松集成其经济功能。通过本教程，你应该已经掌握了：

- **获取经济 API 实例**：三种不同的获取方法
- **基本的经济操作**：获取余额、给予、扣除、设置
- **离线玩家的支持**：所有操作都支持离线玩家
- **完整的代码示例**：基本使用和商店功能示例
- **最佳实践**：插件依赖、API 检查、异常处理、性能优化

现在你可以开始在你的插件中使用 CustomMoney 的经济系统了！

---

*注：本文档适用于 CustomMoney 插件的经济系统 API。如有任何问题或建议，请联系插件开发者。*
