# HotelRoom 插件综合教程文档

## 文档简介

本文档是 HotelRoom 插件的综合性教程文档，整合了房屋创建系统、属性系统设计、自定义属性 PAPI 变量以及家族系统 PAPI 变量的完整内容。文档旨在为服务器管理员和开发者提供全面的技术参考和使用指南。

**适用版本**：HotelRoom 1.5  
**最后更新**：2026-02-10

---

## 目录

- [第一部分：房屋创建系统](#第一部分房屋创建系统)
  - [1.1 概述](#11-概述)
  - [1.2 选区工具](#12-选区工具)
  - [1.3 房屋创建命令](#13-房屋创建命令)
  - [1.4 房屋类型对比](#14-房屋类型对比)
  - [1.5 数据存储](#15-数据存储)
  - [1.6 相关命令](#16-相关命令)
  - [1.7 注意事项](#17-注意事项)
  - [1.8 错误处理](#18-错误处理)
  - [1.9 技术细节](#19-技术细节)
  - [1.10 开发者 API](#110-开发者-api)
- [第二部分：属性系统设计](#第二部分属性系统设计)
  - [2.1 属性分类](#21-属性分类)
  - [2.2 正确的计算流程](#22-正确的计算流程)
  - [2.3 具体实现建议](#23-具体实现建议)
  - [2.4 当前问题修复方案](#24-当前问题修复方案)
- [第三部分：自定义属性 PAPI 变量](#第三部分自定义属性-papi-变量)
  - [3.1 简介](#31-简介)
  - [3.2 自定义属性系统结构](#32-自定义属性系统结构)
  - [3.3 内置自定义属性](#33-内置自定义属性)
  - [3.4 PAPI 变量列表](#34-papi-变量列表)
  - [3.5 使用示例](#35-使用示例)
  - [3.6 自定义属性配置](#36-自定义属性配置)
  - [3.7 属性值范围限制](#37-属性值范围限制)
  - [3.8 注意事项](#38-注意事项)
- [第四部分：家族系统 PAPI 变量](#第四部分家族系统-papi-变量)
  - [4.1 简介](#41-简介)
  - [4.2 家族核心属性](#42-家族核心属性)
  - [4.3 PAPI 变量列表](#43-papi-变量列表)
  - [4.4 使用示例](#44-使用示例)
  - [4.5 职位名称配置](#45-职位名称配置)
  - [4.6 注意事项](#46-注意事项)
- [第五部分：PlaceholderAPI 变量汇总](#第五部分placeholderapi-变量汇总)
  - [5.1 玩家相关变量](#51-玩家相关变量)
  - [5.2 种族相关变量](#52-种族相关变量)
  - [5.3 属性相关变量](#53-属性相关变量)
  - [5.4 领地相关变量](#54-领地相关变量)
  - [5.5 家族相关变量](#55-家族相关变量)
  - [5.6 怪物相关变量](#56-怪物相关变量)
  - [5.7 综合使用示例](#57-综合使用示例)
- [附录](#附录)
  - [版本历史](#版本历史)
  - [技术支持](#技术支持)

---

# 第一部分：房屋创建系统

## 1.1 概述

HotelRoom 插件提供了三种房屋创建方式，管理员可以通过命令创建不同类型的房屋领地。所有创建操作都需要先使用铁斧选择选区。

## 1.2 选区工具

### 使用方法

1. **准备工具**：手持铁斧（Iron Axe）
2. **设置第一个点**：左键点击方块
3. **设置第二个点**：右键点击方块
4. **完成选区**：两个点设置完成后，会显示火焰粒子边框

### 注意事项

- 选区必须为两个对角点
- 粒子边框会实时显示选区范围
- 创建房屋后会自动停止粒子显示
- 只有拥有 `hotelroom.admin` 权限的玩家才能使用铁斧选区

## 1.3 房屋创建命令

### 1. 创建普通酒店领地

**命令格式**：
```
/createhotel <领地名称> [system]
```

**参数说明**：
- `<领地名称>`：房屋的唯一标识名称
- `[system]`：可选参数，指定为 "system" 时表示无主人（系统占位），不指定则创建者为房屋主人

**权限要求**：
- `hotelroom.admin`

**使用示例**：
```
/createhotel 我的家
/createhotel 公共房屋 system
```

**创建规则**：
- 每个玩家只能拥有一个领地（当不使用 system 参数时）
- 领地名称不能重复
- 选区不能与已有领地重叠
- 创建成功后会自动保存到数据库

**返回消息**：
- 成功：`§a酒店领地 '<名称>' 创建成功！` 或 `§a酒店领地 '<名称>' 创建成功！（无主人，可领取）`
- 失败：`§c创建失败，名称已存在或选区无效！`
- 无选区：`§c请先使用铁斧选择两个点！`

### 2. 创建公共酒店

**命令格式**：
```
/createpublichotel <酒店名称>
```

**参数说明**：
- `<酒店名称>`：公共房屋的唯一标识名称

**权限要求**：
- `hotelroom.admin`

**使用示例**：
```
/createpublichotel 市民公寓
```

**创建规则**：
- 无主人，可以被玩家领取
- 领地名称不能重复
- 选区不能与已有领地重叠
- 自动生成外观快照并保存到数据库

**返回消息**：
- 成功：`§a公共酒店 '<名称>' 创建成功！`
- 失败：`§c酒店名称已存在，请使用其他名称。`
- 无选区：`§c请先使用铁斧选择两个对角点。`

### 3. 创建官方酒店

**命令格式**：
```
/createofficialhotel <酒店类型> <酒店名称>
```

**参数说明**：
- `<酒店类型>`：官方房屋类型，可选值：
  - `shop` - 商店
  - `residential` - 住宅
  - `office` - 办公室
  - `apartment` - 公寓
  - `public` - 公共
  - `default` - 默认
- `<酒店名称>`：官方房屋的唯一标识名称

**权限要求**：
- `hotelroom.admin`

**使用示例**：
```
/createofficialhotel shop 城市商店
/createofficialhotel residential 官方住宅
```

**创建规则**：
- 无主人，可以被玩家领取
- 领地名称不能重复
- 选区不能与已有领地重叠
- 自动生成外观快照并保存到数据库
- 创建者会被自动从白名单中移除

**返回消息**：
- 成功：`§a官方酒店 '<名称>' (类型: <类型>) 创建成功！`
- 失败：`§c酒店名称已存在，请使用其他名称。`
- 无选区：`§c请先使用铁斧选择两个对角点。`

## 1.4 房屋类型对比

| 类型 | 命令 | 主人 | 是否公共 | 是否官方 | 类型标识 | 可领取 |
|------|------|------|----------|----------|----------|--------|
| 普通酒店 | `/createhotel` | 玩家或系统 | 否 | 否 | 无 | 看情况 |
| 公共酒店 | `/createpublichotel` | 系统 | 是 | 否 | 无 | 是 |
| 官方酒店 | `/createofficialhotel` | 系统 | 否 | 是 | shop/residential等 | 是 |

## 1.5 数据存储

### 数据库表结构

#### hotels 表
存储房屋基本信息：
- `name`：房屋名称（主键）
- `owner`：主人 UUID
- `is_public`：是否为公共房屋（0=否，1=是）
- `is_official`：是否为官方房屋（0=否，1=是）
- `hotel_type`：官方房屋类型
- `world`：世界名称
- `x1, y1, z1`：第一个角坐标
- `x2, y2, z2`：第二个角坐标

#### hotel_facade 表
存储房屋外观快照：
- `hotel`：房屋名称
- `x, y, z`：方块坐标

#### hotel_members 表
存储房屋成员：
- `hotel`：房屋名称
- `uuid`：成员 UUID

#### placed_blocks 表
存储放置的方块记录：
- `world`：世界名称
- `x, y, z`：方块坐标
- `owner`：放置者 UUID

## 1.6 相关命令

### 领取房屋
```
/hotelroom claim <房屋名称>
```

### 放弃房屋
```
/hotelroom abandon <房屋名称>
```

### 查看房屋列表
```
/hotelroom
```

### 设置房屋荣誉门槛
```
/hotelroom sethonorreq <房屋名称> <荣誉值>
```

## 1.7 注意事项

1. **选区重叠检查**：所有创建命令都会检查选区是否与已有领地重叠
2. **世界加载**：如果房屋所在的世界未加载，该房屋将无法被加载
3. **外观保护**：创建房屋时会自动生成外观快照，用于重启后的保护
4. **空间索引**：所有房屋都会被添加到空间索引中，用于快速查询
5. **权限控制**：只有拥有 `hotelroom.admin` 权限的玩家才能创建房屋

## 1.8 错误处理

### 常见错误及解决方案

| 错误信息 | 原因 | 解决方案 |
|----------|------|----------|
| `§c请先使用铁斧选择两个点！` | 未设置选区 | 使用铁斧左键和右键分别设置两个对角点 |
| `§c酒店名称已存在，请使用其他名称。` | 名称重复 | 使用不同的房屋名称 |
| `§c当前选区与已有领地重叠，请远离后再试！` | 选区重叠 | 选择不与已有领地重叠的区域 |
| `§c你没有权限创建领地！` | 权限不足 | 获取 `hotelroom.admin` 权限 |
| `§c你已拥有领地，无法再次创建！` | 已有领地 | 放弃现有领地或使用 system 参数 |

## 1.9 技术细节

### HotelInfo 类

房屋信息对象，包含以下字段：
- `name`：房屋名称
- `owner`：主人 UUID
- `corners`：选区坐标数组 [Location[2]]
- `members`：成员集合 Set<UUID>
- `facade`：外观快照集合 Set<Location>
- `isPublic`：是否为公共房屋
- `isOfficial`：是否为官方房屋
- `hotelType`：官方房屋类型

### SelectionMgr 类

选区管理器，负责：
- 处理铁斧选区事件
- 显示粒子边框
- 创建房屋领地
- 管理空间索引

### SQLiteStorage 类

数据存储管理器，负责：
- 数据库连接管理
- 房屋信息的保存和加载
- 成员管理
- 外观快照管理
- 方块放置追踪

## 1.10 开发者 API

### 获取房屋信息
```java
HotelInfo hotel = SelectionMgr.HOTELS.get("房屋名称");
```

### 创建房屋（API）
```java
SelectionMgr.getInst().createHotel(player, "房屋名称", ownerUUID);
```

### 保存房屋到数据库
```java
SQLiteStorage.saveHotel(name, owner, corners, isPublic, isOfficial, hotelType);
```

### 删除房屋
```java
SQLiteStorage.removeHotel("房屋名称");
```

---

# 第二部分：属性系统设计

本部分介绍 HotelRoom 插件中属性系统的设计理念和实现方案，帮助开发者理解属性计算流程和优先级机制。

## 2.1 属性分类

### 1. 基础属性 (Base Attributes)
- 来源：种族配置文件
- 特点：固定的属性值，随等级增长
- 示例：基础生命值、基础攻击力等

### 2. 装备属性 (Equipment Attributes)  
- 来源：物品Lore中的自定义属性
- 特点：动态变化，穿戴装备时生效
- 示例：武器上的物理伤害、盔甲上的物理防御

### 3. 手动属性 (Manual Attributes)
- 来源：通过命令直接设置的属性
- 特点：管理员调试用，应该具有最高优先级
- 示例：/attr set 玩家 物理防御 1000

### 4. 临时属性 (Temporary Attributes)
- 来源：药水效果、Buff等
- 特点：有时效性
- 示例：力量药水增加的攻击力

## 2.2 正确的计算流程

### 第一阶段：属性收集
```
最终属性 = 基础属性 + 装备属性 + 手动属性 + 临时属性
```

### 第二阶段：属性应用
1. **攻击方属性**：物理伤害、魔法伤害、暴击率等
2. **防御方属性**：物理防御、魔法防御、护甲值等
3. **环境因素**：PVP加成、PVE加成等

### 第三阶段：伤害计算
```
基础伤害 = 武器基础伤害 + 攻击属性加成
减伤前伤害 = 基础伤害 × (1 + 暴击倍数) × (1 + PVP/PVE加成)

物理攻击最终伤害 = 减伤前伤害 - 物理防御 - 护甲减伤
魔法攻击最终伤害 = 减伤前伤害 - 魔法防御 - 魔法穿透抵消
```

## 2.3 具体实现建议

### 1. 属性合并优先级
```
手动属性 > 装备属性 > 基础属性
```

### 2. 防御属性应用时机
- 在最终伤害计算阶段应用
- 物理防御只对物理攻击生效
- 魔法防御只对魔法攻击生效

### 3. 穿透属性处理
- 护甲穿透：减少目标的有效护甲值
- 魔法穿透：减少目标的有效魔法防御

## 2.4 当前问题修复方案

### 问题1：装备属性未生效
- 检查ItemAttributeParser的正则表达式
- 确保装备属性正确解析并合并到最终属性中

### 问题2：防御属性被无视
- 在CombatAttributeListener中正确应用防御计算
- 确保高防御值能产生显著减伤效果

### 问题3：属性计算顺序混乱
- 建立清晰的属性计算流水线
- 避免属性值被错误覆盖或重复计算

---

# 第三部分：自定义属性 PAPI 变量

本部分详细介绍 HotelRoom 插件中自定义属性系统的核心功能、配置方法和对应的 PlaceholderAPI (PAPI) 变量。

## 3.1 简介

本文档介绍了酒店房间插件(Hotel Room)中自定义属性系统的核心功能、配置方法和对应的 PlaceholderAPI (PAPI) 变量。这些变量可以在聊天、命令、计分板等地方使用，用于展示玩家的自定义属性信息。

所有自定义属性相关的 PAPI 变量都使用 `%hotelroom_xxx%` 格式，其中 `xxx` 是具体的变量名。

## 3.2 自定义属性系统结构

### 3.2.1 核心组件

- **CustomAttributeManager**：管理所有自定义属性的加载、注册和触发
- **PlayerAttribute**：存储单个玩家的所有属性值
- **AttributeManager**：管理属性的注册、获取和保存
- **AttributeStorage**：负责属性数据的持久化存储

### 3.2.2 属性定义文件

自定义属性在 `plugins/HotelRoom/attributes/attributes.yml` 文件中定义，格式如下：

```yaml
custom-attributes:
  my_attribute:
    display-name: "我的属性"
    default-value: 0.0
    min-value: 0.0
    max-value: 10000.0
    type: "passive"
    trigger: "passive"
    formula: "0"
    ignore-defense: false
    effect-type: "damage"
    target: "victim"
    chance: null
    show-message: false
    message: ""
    description: "这是一个自定义属性"
```

## 3.3 内置自定义属性

### 3.3.1 基础属性

| 属性键 | 显示名称 | 默认值 | 最小值 | 最大值 | 描述 |
|-------|---------|-------|-------|-------|------|
| health | 生命值 | 20.0 | 1.0 | 2048.0 | 玩家当前生命值 |
| movement_speed | 移动速度 | 0.2 | 0.0 | 1.0 | 玩家移动速度 |
| attack_speed | 攻击速度 | 4.0 | 0.0 | 24.0 | 玩家攻击速度 |

### 3.3.2 战斗属性

| 属性键 | 显示名称 | 默认值 | 最小值 | 最大值 | 描述 |
|-------|---------|-------|-------|-------|------|
| physical_damage | 物理伤害 | 0.0 | 0.0 | 10000.0 | 物理伤害加成 |
| magic_damage | 魔法伤害 | 0.0 | 0.0 | 10000.0 | 魔法伤害加成 |
| physical_defense | 物理防御 | 0.0 | 0.0 | 1000.0 | 物理防御加成 |
| magic_defense | 魔法防御 | 0.0 | 0.0 | 1000.0 | 魔法防御加成 |
| crit_rate | 暴击率 | 0.0 | 0.0 | 100.0 | 暴击触发概率 |
| crit_damage | 暴击伤害 | 150.0 | 100.0 | 1000.0 | 暴击伤害倍率 |
| dodge_rate | 闪避率 | 0.0 | 0.0 | 100.0 | 闪避攻击概率 |
| block_rate | 格挡率 | 0.0 | 0.0 | 100.0 | 格挡攻击概率 |
| lifesteal | 生命偷取 | 0.0 | 0.0 | 100.0 | 攻击偷取生命值比例 |
| armor_penetration | 护甲穿透 | 0.0 | 0.0 | 1000.0 | 忽略目标护甲值 |
| magic_penetration | 魔法穿透 | 0.0 | 0.0 | 1000.0 | 忽略目标魔法抗性 |

### 3.3.3 资源属性

| 属性键 | 显示名称 | 默认值 | 最小值 | 最大值 | 描述 |
|-------|---------|-------|-------|-------|------|
| mana | 魔法值 | 0.0 | 0.0 | 10000.0 | 玩家当前魔法值 |
| max_mana | 最大魔法值 | 0.0 | 0.0 | 10000.0 | 玩家最大魔法值 |
| health_regen | 生命回复 | 0.0 | 0.0 | 1000.0 | 每秒生命回复量 |
| mana_regen | 魔法回复 | 0.0 | 0.0 | 1000.0 | 每秒魔法回复量 |
| health_regen_percent | 生命回复百分比 | 0.0 | 0.0 | 100.0 | 基于最大生命值的百分比回复 |
| mana_regen_percent | 魔法回复百分比 | 0.0 | 0.0 | 100.0 | 基于最大魔法值的百分比回复 |

### 3.3.4 增益属性

| 属性键 | 显示名称 | 默认值 | 最小值 | 最大值 | 描述 |
|-------|---------|-------|-------|-------|------|
| cooldown_reduction | 冷却缩减 | 0.0 | 0.0 | 100.0 | 技能冷却时间减少比例 |
| exp_bonus | 经验加成 | 0.0 | 0.0 | 100.0 | 获得经验值加成比例 |
| drop_rate_bonus | 掉落加成 | 0.0 | 0.0 | 100.0 | 物品掉落率加成比例 |
| movement_speed_percent | 移动速度百分比 | 0.0 | 0.0 | 100.0 | 基于基础移动速度的百分比加成 |

## 3.4 PAPI 变量列表

### 3.4.1 单个属性变量

使用 `%hotelroom_attr_<属性键>%` 格式可以获取玩家的任意自定义属性值：

| 变量示例 | 说明 | 示例输出 |
|---------|------|---------|
| `%hotelroom_attr_health%` | 玩家当前生命值 | `20.0` |
| `%hotelroom_attr_physical_damage%` | 玩家物理伤害 | `15.5` |
| `%hotelroom_attr_crit_rate%` | 玩家暴击率 | `10.0` |
| `%hotelroom_attr_magic_defense%` | 玩家魔法防御 | `8.5` |

### 3.4.2 玩家属性变量

| 变量名 | 说明 | 示例输出 |
|-------|------|---------|
| `%hotelroom_player_honor%` | 玩家荣誉值 | `1000` |
| `%hotelroom_player_health%` | 玩家当前生命值 | `20.0` |
| `%hotelroom_player_max_health%` | 玩家最大生命值 | `20.0` |
| `%hotelroom_player_health_percent%` | 玩家生命值百分比 | `100.0` |

### 3.4.3 所有属性变量

| 变量名 | 说明 | 示例输出 |
|-------|------|---------|
| `%hotelroom_all_attributes%` | 显示玩家所有非零属性 | `生命值: 20.0<br>物理伤害: 15.5<br>暴击率: 10.0` |

### 3.4.4 自定义属性变量

除了上述内置变量外，所有在 `attributes.yml` 中定义的自定义属性都可以通过 `%hotelroom_attr_<属性键>%` 格式访问。

## 3.5 使用示例

### 3.5.1 在聊天中使用

```
你好，%player_name%！你的属性信息：
生命值: %hotelroom_attr_health%
物理伤害: %hotelroom_attr_physical_damage%
暴击率: %hotelroom_attr_crit_rate%
```

输出示例：
```
你好，Steve！你的属性信息：
生命值: 20.0
物理伤害: 15.5
暴击率: 10.0
```

### 3.5.2 在计分板中使用

```yaml
title: "角色属性"
lines:
  - "生命值: %hotelroom_attr_health%/%hotelroom_attr_max_health%"
  - "物理伤害: %hotelroom_attr_physical_damage%"
  - "魔法伤害: %hotelroom_attr_magic_damage%"
  - "暴击率: %hotelroom_attr_crit_rate%"
  - "暴击伤害: %hotelroom_attr_crit_damage%"
```

### 3.5.3 在命令中使用

```
/tellraw %player_name% ["",{"text":"你的属性信息：","color":"gold"},{"text":"\n生命值：","color":"yellow"},{"text":"%hotelroom_attr_health%","color":"light_purple"},{"text":"\n物理伤害：","color":"yellow"},{"text":"%hotelroom_attr_physical_damage%","color":"red"}]
```

## 3.6 自定义属性配置

### 3.6.1 添加新属性

在 `attributes/attributes.yml` 文件中添加新的属性定义：

```yaml
custom-attributes:
  my_new_attribute:
    display-name: "新属性"
    default-value: 0.0
    min-value: 0.0
    max-value: 100.0
    type: "offensive"
    trigger: "on_attack"
    formula: "my_new_attribute * 0.1"
    effect-type: "damage"
    target: "victim"
    chance: "100"
    show-message: true
    message: "你触发了新属性效果！"
    description: "这是一个新的自定义属性"
```

### 3.6.2 重新加载属性

使用以下命令重新加载自定义属性：

```
/attr reload
```

## 3.7 属性值范围限制

为了保证游戏平衡性，部分属性有内置的范围限制：

| 属性类型 | 最小值 | 最大值 | 示例属性 |
|---------|-------|-------|---------|
| 生命值 | 1.0 | 2048.0 | health |
| 移动速度 | 0.0 | 1.0 | movement_speed |
| 攻击速度 | 0.0 | 24.0 | attack_speed |
| 百分比属性 | 0.0 | 100.0 | crit_rate, dodge_rate |
| 暴击伤害 | 100.0 | 1000.0 | crit_damage |
| 魔法值 | 0.0 | 10000.0 | mana, max_mana |
| 防御属性 | 0.0 | 1000.0 | physical_defense, magic_defense |
| 伤害属性 | 0.0 | 10000.0 | physical_damage, magic_damage |
| 恢复属性 | 0.0 | 1000.0 | health_regen, mana_regen |

## 3.8 注意事项

1. 所有自定义属性的 PAPI 变量都使用 `%hotelroom_attr_<属性键>%` 格式，其中 `<属性键>` 是属性的内部键名，不是显示名称。
2. 使用前请确保已安装 PlaceholderAPI 插件并启用了本插件的 PAPI 扩展。
3. 如果玩家没有某个属性，变量会显示默认值（通常为 0.0）。
4. 自定义属性的效果需要在对应的触发条件下才会生效。
5. 修改属性定义后需要使用 `/attr reload` 命令重新加载。

---

# 第四部分：家族系统 PAPI 变量

本部分介绍 HotelRoom 插件中家族系统的核心属性和对应的 PlaceholderAPI (PAPI) 变量。

## 4.1 简介

本文档介绍了酒店房间插件(Hotel Room)中家族系统的核心属性和对应的 PlaceholderAPI (PAPI) 变量。这些变量可以在聊天、命令、计分板等地方使用，用于展示玩家的家族信息。

所有家族相关的 PAPI 变量都使用 `%hotelroom_xxx%` 格式，其中 `xxx` 是具体的变量名。

## 4.2 家族核心属性

### 4.2.1 家族基本属性

| 属性名 | 类型 | 说明 |
|-------|------|------|
| id | UUID | 家族唯一标识符 |
| name | String | 家族名称 |
| leaderId | UUID | 族长ID |
| level | int | 家族等级 |
| honor | double | 家族荣誉值 |
| activity | double | 家族活跃度 |
| memberIds | Set<UUID> | 家族成员ID集合 |

### 4.2.2 家族成员属性

| 属性名 | 类型 | 说明 |
|-------|------|------|
| playerId | UUID | 玩家ID |
| familyId | UUID | 所属家族ID |
| position | String | 家族职位 |
| dailyActivity | Map<String, Double> | 每日活跃值 |
| joinTime | long | 加入时间 |
| lastLoginTime | long | 最后登录时间 |

## 4.3 PAPI 变量列表

### 4.3.1 家族基本信息

| 变量名 | 说明 | 示例 | 数据类型 |
|-------|------|------|---------|
| `%hotelroom_family_name%` | 玩家所属家族名称 | `荣耀家族` | String |
| `%hotelroom_family_level%` | 玩家所属家族等级 | `3` | int |
| `%hotelroom_family_member_count%` | 玩家所属家族成员数量 | `15` | int |
| `%hotelroom_family_position%` | 玩家在家族中的职位 | `族长` | String |

### 4.3.2 家族详细信息

（以下属性已存在，但尚未实现对应的 PAPI 变量，可根据需求扩展）

| 属性名 | 建议变量名 | 说明 | 数据类型 |
|-------|----------|------|---------|
| honor | `%hotelroom_family_honor%` | 家族荣誉值 | double |
| activity | `%hotelroom_family_activity%` | 家族活跃度 | double |
| leaderId | `%hotelroom_family_leader%` | 家族族长名称 | String |
| memberIds | `%hotelroom_family_members%` | 家族成员列表 | String |
| joinTime | `%hotelroom_family_join_time%` | 玩家加入家族时间 | long |
| lastLoginTime | `%hotelroom_family_last_login%` | 玩家最后登录时间 | long |

## 4.4 使用示例

### 4.4.1 在聊天中使用

```
你好，%player_name%！你属于家族：%hotelroom_family_name%，职位是：%hotelroom_family_position%。
```

输出示例：
```
你好，Steve！你属于家族：荣耀家族，职位是：族长。
```

### 4.4.2 在计分板中使用

```yaml
title: "家族信息"
lines:
  - "家族: %hotelroom_family_name%"
  - "等级: %hotelroom_family_level%"
  - "成员: %hotelroom_family_member_count%"
  - "职位: %hotelroom_family_position%"
```

### 4.4.3 在命令中使用

```
/tellraw %player_name% ["",{"text":"你的家族信息：","color":"gold"},{"text":"\n家族名称：","color":"yellow"},{"text":"%hotelroom_family_name%","color":"light_purple"},{"text":"\n家族等级：","color":"yellow"},{"text":"%hotelroom_family_level%","color":"green"}]
```

## 4.5 职位名称配置

家族职位的中文名称可以在配置文件中进行配置：

```yaml
positions:
  leader:
    name: "族长"
  deputy:
    name: "副族长"
  member:
    name: "成员"
```

配置后，`%hotelroom_family_position%` 变量将显示配置的中文名称。

## 4.6 注意事项

1. 所有家族相关的 PAPI 变量都需要玩家加入家族才能正常显示，否则会显示默认值（如 "无" 或 "0"）。
2. 使用前请确保已安装 PlaceholderAPI 插件并启用了本插件的 PAPI 扩展。
3. 如果玩家没有家族，部分变量会显示默认值（如 "无"、"0"）。
4. 如需添加新的 PAPI 变量，可以修改 `HotelRoomPapi.java` 文件中的 `handleFamilyPlaceholders` 方法。

---

# 第五部分：PlaceholderAPI 变量汇总

本部分汇总了 HotelRoom 插件所有可用的 PlaceholderAPI 变量，方便快速查阅和使用。

## 5.1 玩家相关变量

| 变量 | 说明 | 示例输出 |
|------|------|----------|
| `%hotelroom_player_honor%` | 玩家荣誉值 | `100` |
| `%hotelroom_player_health%` | 玩家当前血量（保留一位小数） | `20.0` |
| `%hotelroom_player_max_health%` | 玩家最大血量（保留一位小数） | `20.0` |
| `%hotelroom_player_health_percent%` | 玩家血量百分比 | `100.0` |
| `%hotelroom_player_attr_<属性名>%` | 玩家指定属性值 | `50.0` |

## 5.2 种族相关变量

| 变量 | 说明 | 示例输出 |
|------|------|----------|
| `%hotelroom_race%` | 当前种族显示名称（中文） | `人类` |
| `%hotelroom_race_name%` | 当前种族内部名称（英文ID） | `human` |
| `%hotelroom_is_race_<种族名>%` | 是否为指定种族 | `true` / `false` |

## 5.3 属性相关变量

| 变量 | 说明 | 示例输出 |
|------|------|----------|
| `%hotelroom_all_attributes%` | 显示玩家所有属性（换行分隔） | 物理伤害: 50.0<br>魔法伤害: 30.0<br>... |
| `%hotelroom_attr_<属性名>%` | 显示单个属性值 | `50.0` |

**常用属性名示例：**
- `physical_damage` - 物理伤害
- `magic_damage` - 魔法伤害
- `crit_rate` - 暴击率
- `crit_damage` - 暴击伤害
- `armor` - 护甲
- `physical_defense` - 物理防御
- `magic_defense` - 魔法防御
- `health` - 生命值
- `max_health` - 最大生命值

## 5.4 领地相关变量

| 变量 | 说明 | 示例输出 |
|------|------|----------|
| `%hotelroom_in_land%` | 是否在领地内 | `true` / `false` |
| `%hotelroom_land_honor%` | 当前领地荣誉门槛 | `100` |
| `%hotelroom_land_owner%` | 当前领地主人 | `玩家名` / `系统` |
| `%hotelroom_land_name%` | 当前领地名称 | `我的家` |
| `%hotelroom_public_members_count%` | 公共领地成员数量 | `5` |
| `%hotelroom_in_public_hotel%` | 是否在公共酒店内 | `true` / `false` |

## 5.5 家族相关变量

| 变量 | 说明 | 示例输出 |
|------|------|----------|
| `%hotelroom_family_name%` | 家族名称 | `龙之家族` |
| `%hotelroom_family_position%` | 家族职位 | `族长` |
| `%hotelroom_family_level%` | 家族等级 | `5` |
| `%hotelroom_family_member_count%` | 家族成员数量 | `20` |

## 5.6 怪物相关变量

| 变量 | 说明 | 示例输出 |
|------|------|----------|
| `%hotelroom_mob_health%` | 瞄准怪物的当前血量 | `50.0` |
| `%hotelroom_mob_max_health%` | 瞄准怪物的最大血量 | `100.0` |
| `%hotelroom_mob_health_percent%` | 瞄准怪物的血量百分比 | `50.0` |
| `%hotelroom_mob_name%` | 瞄准怪物的名称 | `僵尸` |

## 5.7 综合使用示例

### 5.7.1 在记分板中显示玩家信息

```
§a玩家信息
§7荣誉值: §f%hotelroom_player_honor%
§7血量: §f%hotelroom_player_health%/%hotelroom_player_max_health%
§7种族: §f%hotelroom_race%
```

### 5.7.2 在记分板中显示领地信息

```
§a当前位置
§7领地: §f%hotelroom_land_name%
§7主人: §f%hotelroom_land_owner%
§7荣誉门槛: §f%hotelroom_land_honor%
```

### 5.7.3 在记分板中显示怪物信息

```
§a目标信息
§7名称: §f%hotelroom_mob_name%
§7血量: §f%hotelroom_mob_health%/%hotelroom_mob_max_health%
§7血量百分比: §f%hotelroom_mob_health_percent%%
```

### 5.7.4 显示玩家属性

```
§a属性面板
%hotelroom_all_attributes%
```

### 5.7.5 显示家族信息

```
§a家族信息
§7家族: §f%hotelroom_family_name%
§7职位: §f%hotelroom_family_position%
§7等级: §f%hotelroom_family_level%
§7成员: §f%hotelroom_family_member_count%
```

---

# 附录

## 版本历史

### 自定义属性系统
- v1.0: 初始版本，包含基础属性和战斗属性
- v1.1: 添加了资源属性和增益属性
- v1.2: 完善了属性范围限制和效果触发机制

### 家族系统
- v1.0: 初始版本，包含基本的家族信息变量
- v1.1: 计划添加家族荣誉值、活跃度等变量

### 房屋创建系统
- v1.0: 初始版本，包含三种房屋创建方式
- v1.5: 添加官方房屋类型和外观保护功能

## 技术支持

如有任何问题或建议，请联系插件开发者。

---

*注：本文档适用于酒店房间插件(Hotel Room)的综合教程。如有任何问题或建议，请联系插件开发者。*
