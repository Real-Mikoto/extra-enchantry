# 附魔设计提案（未实现）

> **状态：仅设计稿。** 未写任何代码 / JSON / 标签。
> 数值口径、字段命名、稀有度权重、获取途径写法均与 README 既有附魔保持一致，实现时按「后续附魔记录规范」追加章节即可。
> 分档沿用强度排序结论：T0 规则改写 / T1 质变 / T2 强力 / T3 情境。
> 标注 **[需验证]** 的注入点尚未反编译确认，动手前先过 `mc-fabric-mod-api-verify` 流程。

## 总览

| 档位 | 附魔 | 等级 | 稀有度 | 适用物品 | 获取 | 一句话 |
| --- | --- | --- | --- | --- | --- | --- |
| T0 | 超限 Ascend | I | very_rare (1) | 全装备 | 非宝藏 | 其他附魔可超出等级上限 |
| T0 | 誓约 Oathbound | I | rare (2) | 全装备 | 非宝藏 | 死亡不掉落，四件齐经验也不掉 |
| T1 | 巨力 Titan | III | uncommon (5) | 四件护甲 | 非宝藏 | 每级 +1 攻击伤害，叠加上限 +6 |
| T1 | 空跃 Skyward | II | uncommon (5) | 靴子 | 非宝藏 | 空中可再跳，摔落减免 |
| T2 | 处决 Executioner | II | rare (2) | 武器/工具/远程 | 非宝藏 | 目标残血时伤害倍增 |
| T2 | 破阵 Cleave | III | uncommon (5) | 近战武器/工具 | 非宝藏 | 近战溅射多个目标 |
| T3 | 夜行 Nocturne | II | uncommon (5) | 头盔 | 非宝藏 | 夜晚移速加成，白天无效 |
| T3 | 唤雷 Thundercall | II | rare (2) | 三叉戟 | 宝藏 | 雷暴中掷戟概率召落雷 |

***

## T0 规则改写

### A1. 超限 (Ascend)

**功能**：单级。带超限的物品，通过附魔台/图书管理员/宝箱/钓鱼获得的**其他附魔**可超出其等级上限 1 级（锋利 V→VI、保护 IV→V、效率 V→VI）；铁砧融合可继续到上限 +2 级。诅咒附魔不参与超越。

| 等级 | 随机途径上限 | 铁砧融合上限 |
| --- | --- | --- |
| I | 原 max_level + 1 | 原 max_level + 2 |

**数据定义** `data/extra-enchantry/enchantment/ascend.json`：

* `max_level: 1`，`weight: 1`（very_rare，与破限同级——两者都是改规则的），`anvil_cost: 6`
* `supported_items/primary_items: "#extra-enchantry:limit_break_supported"`（复用现成聚合标签，全装备类型）
* `slots: ["any"]`，`effects: {}` 纯 Mixin
* 与破限不互斥、可共存：破限管"互斥"、超限管"等级"，收益相乘

**实现指针**：

* 附魔台：`EnchantmentHelper.getAvailableEnchantmentResults` 的等级上限读取点放宽 +1（复用项目已有 EnchantmentHelperMixin 的 ThreadLocal 传物品模式）
* 宝箱书/交易：`EnchantRandomlyFunction` 的 `nextInt(minLevel, maxLevel)`（已有 EnchantRandomlyFunctionMixin，方向反过来：把 maxLevel 放开为 +1）
* 铁砧：`AnvilMenuMixin`（已有）放行等级 > max_level 的合成结果
* 附魔 JSON 的 `max_level` 本身不动，纯运行时放宽——数据层零改动

**获取途径**：追加进 `minecraft:non_treasure` → 附魔台/图书管理员/宝箱/钓鱼全途径。

**分档理由**：作用域是**整个附魔体系**（含原版 40+ 附魔），收益随身上其他附魔数量线性放大，永远生效、不耗资源——与破限同档的乘法型。

***

### A2. 誓约 (Oathbound)

**功能**：单级。带誓约的物品在玩家死亡时**不掉落、不消失**（重生后仍在身上），消失诅咒对其无效；若四件护甲都带誓约，死亡时**经验值也全额保留**（物品+经验的随身 keepInventory）。

**数据定义** `data/extra-enchantry/enchantment/oathbound.json`：

* `max_level: 1`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#extra-enchantry:limit_break_supported"`，`slots: ["any"]`
* `effects: {}` 纯 Mixin

**实现指针**：

* 死亡掉落链路（`ServerPlayer#die` → 装备/背包掉落）在 26.2 的具体方法名 **[需验证]**：设计上在掉落遍历处跳过带誓约的 ItemStack（保留进背包）
* 经验保留：改写 `experienceLevel/experienceProgress` 的死亡清零路径（**[需验证]**），仅四件护甲全带时生效
* 注意与 `keepInventory` gamerule 的幂等叠加：gamerule 开启时本附魔逻辑应短路
* 实体字段 `@Shadow` 跨类取不到是已知坑（README 踩坑记录），需要上下文时走 `ServerPlayer` 自身的 mixin

**获取途径**：追加进 `minecraft:non_treasure`。

**分档理由**：把"死亡掉落"这条游戏内最痛的全局规则改写成可选项；永久生效、无消耗、无冷却。强度对标 keepInventory，T0。

***

## T1 质变

### B1. 巨力 (Titan)

**功能**：3 级，四件护甲均可附魔。每级每件 +1 近战攻击伤害，多件叠加；普通叠加上限 +6（3 颗心/击），任意护甲带破限时上限失效（最高 4×3=+12）。

| 等级 | 单件加成 | 叠加上限 | 破限叠加上限 |
| --- | --- | --- | --- |
| I | +1 | +2 | +4 |
| II | +1 | +4 | +8 |
| III | +1 | +6 | +12 |

**数据定义** `data/extra-enchantry/enchantment/titan.json`：

* `max_level: 3`，`weight: 5`（uncommon），`anvil_cost: 2`
* `supported_items/primary_items: "#minecraft:enchantable/armor"`，`slots: ["armor"]`
* `effects: {}` —— 加成走属性修改器

**实现指针**：完全复用活力的模式——`LivingEntityMixin.tick` 内向 `Attributes.ATTACK_DAMAGE` 写瞬态修改器（id `extra-enchantry:titan`，`ADD_VALUE`），护甲总等级 × 1、破限豁免上限；tick 双端运行保证客户端攻击力 tooltip 同步。

**获取途径**：追加进 `minecraft:non_treasure`，uncommon 出现率。

**分档理由**：与活力完全同构（活力加血、巨力加伤），常驻、无操作、量级同级——T1。

***

### B2. 空跃 (Skyward)

**功能**：2 级，靴子专属。空中可再跳 I 级 1 次 / II 级 2 次；每级摔落伤害 -25%（II 级共 -50%）。空中跳跃次数在触地、攀爬、进水时重置。

| 等级 | 空中跳跃 | 摔落减免 |
| --- | --- | --- |
| I | 1 次 | -25% |
| II | 2 次 | -50% |

**数据定义** `data/extra-enchantry/enchantment/skyward.json`：

* `max_level: 2`，`weight: 5`（uncommon），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/foot_armor"`，`slots: ["feet"]`
* `exclusive_set`：追加进 `minecraft:exclusive_set/boots` 标签（与冰霜行者/深海探索者/炽焰行者双向互斥，同炽焰行者的做法）

**实现指针**：

* 跳跃计数 + 重置条件：服务端权威判定。客户端跳跃键按下 → 26.2 的输入与 `LivingEntity` 跳跃入口对应关系 **[需验证]**；备选方案是拦截 `LivingEntity.travel` 的跳跃采样并维护"自上次落地以来的跳跃数"
* 摔落减免：可走 `FALL_DAMAGE_MULTIPLIER` 属性瞬态修改器（26.2 已有该属性），或拦 `hurtServer` 的摔落伤害标签分支

**获取途径**：追加进 `minecraft:non_treasure`。

**分档理由**：垂直机动的质变（等效二段跳/三段跳），改变探索与战斗走位的方式，是鞘翅之外最强的立体位移——T1。

***

## T2 强力

### C1. 处决 (Executioner)

**功能**：2 级，武器/工具/远程。目标当前生命低于阈值时，本次伤害按倍率放大；残血收割型乘区，对 Boss 同样按生命比例判定。

| 等级 | 触发阈值（当前/最大生命） | 伤害倍率 |
| --- | --- | --- |
| I | < 25% | ×1.35 |
| II | < 35% | ×1.50 |

**数据定义** `data/extra-enchantry/enchantment/executioner.json`：

* `max_level: 2`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#extra-enchantry:siphon_supported"`（近远程同池，与汲取/蚀命一致），`slots: ["mainhand"]`
* `effects: {}` 纯 Mixin

**实现指针**：`@ModifyVariable` 拦 `hurtServer` 的 `amount`（完全复用蚀命的注入点与 `source.getWeaponItem()` 取武器方式），按 `getHealth()/getMaxHealth()` 比值查表放大；远程投射物同样生效（不衰减——处决本身就是条件型，不再叠远程惩罚）。

**获取途径**：追加进 `minecraft:non_treasure`。

**分档理由**：数值型高频乘区，强度与触及同级；不改变玩法规则、只放大伤害——T2。

***

### C2. 破阵 (Cleave)

**功能**：3 级，近战武器/工具。命中时对 2 格内最多 1/2/3 个额外目标造成 50%/60%/70% 伤害。溅射伤害走完整护甲结算，但**不触发汲取与蚀命**（防滚雪球）；也不触发破阵自身（防递归）。

| 等级 | 额外目标数 | 溅射比例 |
| --- | --- | --- |
| I | 1 | 50% |
| II | 2 | 60% |
| III | 3 | 70% |

**数据定义** `data/extra-enchantry/enchantment/cleave.json`：

* `max_level: 3`，`weight: 5`（uncommon），`anvil_cost: 2`
* `supported_items/primary_items: "#extra-enchantry:cleave_supported"` —— **需新建**自定义物品标签（`weapon` + `mining_loot`，仅近战，不带 trident/bow）
* `slots: ["mainhand"]`
* `exclusive_set: ["minecraft:sweeping_edge"]`（与横扫之刃互斥，避免双份 AoE 叠乘）

**实现指针**：`hurtServer` RETURN 后取攻击者与目标位置，`ServerLevel` 范围查询 LivingEntity，排除已命中者后逐个 `hurt`（共用同一 DamageSource）；用 ThreadLocal 标记位屏蔽递归与汲取/蚀命触发（三处都在 `hurtServer`，需统一短路）。

**获取途径**：追加进 `minecraft:non_treasure`。

**分档理由**：清群效率质变但对单体 Boss 零收益，价值高度依赖场景——典型 T2。

***

## T3 情境

### D1. 夜行 (Nocturne)

**功能**：2 级，头盔。夜晚（天空光照 ≤7）时移动速度 +15%/+25%；II 级额外附赠夜视 I（短时长逐 tick 刷新，避免药水图标闪烁）。白天/地下人造光源环境完全无效果。

| 等级 | 夜晚移速 | 夜视 |
| --- | --- | --- |
| I | +15% | 无 |
| II | +25% | 有 |

**数据定义** `data/extra-enchantry/enchantment/nocturne.json`：

* `max_level: 2`，`weight: 5`（uncommon），`anvil_cost: 2`
* `supported_items/primary_items: "#minecraft:enchantable/head_armor"`，`slots: ["head"]`
* `effects: {}` 纯 Mixin

**实现指针**：复用活力的瞬态修改器模式打 `Attributes.MOVEMENT_SPEED`，按 `Level` 的昼夜/天空光照判定条件切换（具体判定 API **[需验证]**：`isDay()`/`getSkyDarken()`/光照采样三选一）；夜视用 `addEffect(MobEffects.NIGHT_VISION, 220)` 每 tick 刷新。

**获取途径**：追加进 `minecraft:non_treasure`。

**分档理由**：一半时间无效，收益被昼夜硬切一半；与炽焰行者（场景受限）同档——T3。

***

### D2. 唤雷 (Thundercall)

**功能**：2 级，三叉戟专属。雷暴天气掷出并命中实体时，20%/40% 概率召唤落雷（5 秒冷却）；落雷伤害归功于玩家（可正常触发汲取），不会点燃玩家自己。非雷暴天气完全无效果。

| 等级 | 触发概率 | 冷却 |
| --- | --- | --- |
| I | 20% | 5 秒 |
| II | 40% | 5 秒 |

**数据定义** `data/extra-enchantry/enchantment/thundercall.json`：

* `max_level: 2`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/trident"`，`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:channeling"]`（与原版引雷互斥，语义不重叠）
* 命名刻意避开原版"引雷 (Channeling)"

**实现指针**：mixin 拦投射物命中（`AbstractArrow#onHitEntity` 的 TridentEntity 路径，具体入口 **[需验证]**）；`Level.isThundering()` 判定 + `LightningBolt` 实体生成（spawn API **[需验证]**）；冷却复用 AfterglowManager 的 `Map<UUID, Long>` 毫秒时间戳风格。

**获取途径**：**宝藏**——追加进 `treasure` + `tradeable` + `on_random_loot` 三个标签（无附魔台），与炽焰行者同路径。

**分档理由**：被天气锁死（雷暴天气占比极低），但触发时是全 mod 最华丽的爆发；获取也走宝藏系——纯情境 T3。

***

## 设计取舍备注

1. **超限与破限刻意不互斥**：一个管"附魔互斥"、一个管"等级上限"，叠加后收益相乘，构成 mod 内的终极构筑方向（锐利 VII + 无视互斥）。
2. **破阵排除汲取/蚀命**：AoE × 生命偷取 × 最大生命百分比伤害是滚雪球组合，设计上主动剪断。
3. **破阵与横扫之刃互斥**、**唤雷与引雷互斥**：与原版机制重叠的附魔一律声明 exclusive_set，避免双份结算。
4. **巨力上限设计**：普通 +6（= 3 颗心/击）已接近"活力 +50 血"的强度量级；破限突破到 +12 是给破限流的第三条增益线（活力/巨力/保护上限）。
5. **唤雷走宝藏、其余走非宝藏**：与原版三叉戟附魔（激流/引雷均为宝藏）的惯例保持一致。
6. **未实现的公共改动**：破阵需要新建 `cleave_supported` 物品标签；其余全部复用现有标签，不动原版掉落表。
