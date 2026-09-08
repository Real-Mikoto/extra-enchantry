# Extra Enchantry

Minecraft 26.2 (Fabric) 自定义附魔模组。

## 环境

* Minecraft 26.2 / Fabric Loader 0.19.3 / Fabric API 0.158.0+26.2 / Java 25
* 26.2 的关键 API 变化：`ResourceLocation` → `Identifier`；镐 / 斧等工具由**数据驱动**（Tool 组件规则）；附魔全部走 JSON 数据包定义


## 附魔总览

| #   | 附魔                     | 等级  | 可附魔物品                              | 获取方式                                              |
| --- | ---------------------- | --- | ---------------------------------- | ------------------------------------------------- |
| 1 | 凋零保护 Wither Protection | IV  | 四件护甲                               | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼                         |
| 2 | 炽焰行者 Blazing Walker    | II  | 靴子                                 | 宝藏：图书管理员 / 宝箱 / 钓鱼（无附魔台）                          |
| 3 | 破限 Limit Break         | I   | 武器 / 工具 / 护甲 / 弓弩 / 三叉戟 / 钓鱼竿 / 马铠 | 击杀坚守者 0.05%（闪电苦力怕击杀 0.5%）；完成「诸界浩劫」挑战后方可使用         |
| 4 | 拓阶 Tier Break          | III | 镐 / 斧 / 锹 / 锄                      | 击杀凋零 20%（1-3 级随机）                                 |
| 5 | 触及 Reach               | X   | 近战武器 + 工具                          | 非宝藏（仅获得 1 级，铁砧融合升级）                               |
| 6 | 假象 Decoy               | III | 头盔                                 | 非宝藏：与荆棘同稀有度（互斥）                                   |
| 7 | 汲取 Siphon              | III | 武器 / 工具 / 弓弩 / 三叉戟                 | 非宝藏：very_rare，附魔台 / 交易 / 宝箱 / 钓鱼                 |
| 8 | 蚀命 Life Erosion        | III | 武器 / 工具 / 弓弩 / 三叉戟                 | 仅远古城市宝箱（附魔书 / 带附魔的武器工具）                           |
| 9 | 活力 Vitality            | V   | 四件盔甲 / 马铠                          | 非宝藏：普通稀有度，附魔台 / 交易 / 宝箱 / 钓鱼                      |
| 10 | 壁垒 Bulwark             | X   | 胸甲 / 马铠                            | 非宝藏：仅获得 1 级，铁砧融合升级                                |
| 11 | 劫后余辉 Afterglow         | I   | 不死图腾                               | 非宝藏：图书管理员 / 宝箱 / 钓鱼（生存靠铁砧打书）                      |
| 12 | 誓约 Oathbound           | I   | 全装备                                | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼                         |
| 13 | 空跃 Skyward             | II  | 靴子                                 | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼（与冰霜行者 / 深海探索者 / 炽焰行者互斥） |
| 14 | 破阵 Cleave              | III | 近战武器 / 工具                          | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼（与横扫之刃互斥）                |
| 15 | 御风 Windrider           | III | 鞘翅                                 | 仅末地城宝箱（附魔书 / 带附魔的鞘翅）                              |
| 16 | 无踪 Unseen              | II  | 靴子                                 | 仅远古城市宝箱（与靴子系附魔互斥）                                 |
| 17 | 断罪 Judgement           | II  | 近战武器 / 工具                          | 非宝藏：very_rare，附魔台 / 交易 / 宝箱 / 钓鱼                 |
| 18 | 疾风 Gale                | III | 护腿                                 | 非宝藏：随机最高 II 级，III 级需破限铁砧融合                        |
| 19 | 余烬 Emberfall           | I   | 金胸甲 / 金马铠                          | 仅不祥试炼唯一奖励箱（附魔书 / 带附魔的金胸甲）；金马铠走铁砧 + 书              |
| 20 | 冲阵 Shield Charge       | III | 盾牌                                 | 非宝藏：uncommon，附魔台 / 图书管理员 / 宝箱 / 钓鱼                |
| 21 | 不屈 Defiance            | II  | 盾牌                                 | 非宝藏：rare，附魔台 / 图书管理员 / 宝箱 / 钓鱼                    |
| 22 | 庇护 Sanctuary           | III | 盾牌                                 | 试炼密室基础 / 稀有奖励箱（附魔书 / 带附魔的盾牌）                      |
| 23 | 坚壁 Aegis               | III | 盾牌                                 | 非宝藏：rare，附魔台 / 图书管理员 / 宝箱 / 钓鱼                    |
| 24 | 归羽 Homing Plume        | II  | 弓 / 弩                               | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼（与无限互斥）                 |
| 25 | 坠星 Starfall            | I   | 弩                                  | 仅末地城宝箱（附魔书 / 带附魔的弩，与多重射击互斥）                     |
| 26 | 霆霓 Stormsurge          | II  | 三叉戟                                | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼（与引雷互斥）                 |
| 27 | 藏锋 Sheathed Edge       | III | 剑 / 斧                              | 非宝藏：rare，附魔台 / 图书管理员 / 宝箱 / 钓鱼                    |
| 28 | 渊息 Tideheart           | III | 头盔                                 | 宝藏（无附魔台）：海洋系宝箱（沉船 / 宝藏 / 海底废墟）+ 钓鱼；与水下呼吸 / 水下速掘互斥 |
| 29 | 丰壤 Loam                | III | 锄                                  | 非宝藏：附魔台 / 图书管理员 / 宝箱 / 钓鱼                       |
| 30 | 魂铃 Soul Chime          | II  | 耳环                                | 非宝藏：击杀敌对生物回复饥饿与饱和                             |
| 31 | 盾坠 Shield Pendant       | II  | 耳环                                | 非宝藏：受到的伤害 -3%/级                              |
| 32 | 雷鸣扣 Thunder Clasp      | II  | 项链                                 | 非宝藏：雷雨时造成的伤害 +4%/级                              |
| 33 | 翠滴 Verdant Drop          | II  | 项链                                 | 非宝藏：自然恢复 +10%/级                                   |
| 34 | 刃戒 Blade Ring             | II  | 戒指                                 | 非宝藏：攻击速度 +5%/级                                     |
| 35 | 羽环 Plume Ring             | II  | 戒指                                 | 非宝藏：弹射物伤害 -6%/级                                   |
| 36 | 烬镯 Ember Bracelet        | II  | 手镯                                 | 非宝藏：火焰伤害 -10%/级                                    |
| 37 | 潮镯 Tide Bracelet         | II  | 手镯                                 | 非宝藏：游泳效率 +8%/级                                    |
| 38 | 锐牙 Sharp Fang             | III | 狼铠                                 | 非宝藏：狼近战伤害 +10%/级                                  |
| 39 | 哨戒 Vigil                    | II | 狼铠                                 | 非宝藏：狼索敌/跟随范围 +25%/级                              |
| 40 | 回春 Renewal                 | II | 狼铠                                 | 非宝藏：狼每 4 秒回复 1 HP/级                              |


## 目录

* [附魔](#附魔)
  * [1. 凋零保护 (Wither Protection)](#1-凋零保护-wither-protection)
  * [2. 炽焰行者 (Blazing Walker)](#2-炽焰行者-blazing-walker)
  * [3. 破限 (Limit Break)](#3-破限-limit-break)
  * [4. 拓阶 (Tier Break)](#4-拓阶-tier-break)
  * [5. 触及 (Reach)](#5-触及-reach)
  * [6. 假象 (Decoy)](#6-假象-decoy)
  * [7. 汲取 (Siphon)](#7-汲取-siphon)
  * [8. 蚀命 (Life Erosion)](#8-蚀命-life-erosion)
  * [9. 活力 (Vitality)](#9-活力-vitality)
  * [10. 壁垒 (Bulwark)](#10-壁垒-bulwark)
  * [11. 劫后余辉 (Afterglow)](#11-劫后余辉-afterglow)
  * [12. 誓约 (Oathbound)](#12-誓约-oathbound)
  * [13. 空跃 (Skyward)](#13-空跃-skyward)
  * [14. 破阵 (Cleave)](#14-破阵-cleave)
  * [15. 御风 (Windrider)](#15-御风-windrider)
  * [16. 无踪 (Unseen)](#16-无踪-unseen)
  * [17. 断罪 (Judgement)](#17-断罪-judgement)
  * [18. 疾风 (Gale)](#18-疾风-gale)
  * [19. 余烬 (Emberfall)](#19-余烬-emberfall)
  * [盾牌四附魔 (Shield Enchantments)（20~23）](#盾牌四附魔-shield-enchantments)
  * [24. 归羽 (Homing Plume)](#24-归羽-homing-plume)
  * [25. 坠星 (Starfall)](#25-坠星-starfall)
  * [26. 霆霓 (Stormsurge)](#26-霆霓-stormsurge)
  * [27. 藏锋 (Sheathed Edge)](#27-藏锋-sheathed-edge)
  * [28. 渊息 (Tideheart)](#28-渊息-tideheart)
  * [29. 丰壤 (Loam)](#29-丰壤-loam)
  * [配饰八附魔 (Accessory Enchantments)（30~37）](#配饰八附魔-accessory-enchantments3037)
  * [狼铠三附魔 (Wolf Armor Enchantments)（38~40）](#狼铠三附魔-wolf-armor-enchantments3840)
* [版本主题](#版本主题)
  * [1.0.0「诸界浩劫」 (Cataclysm of Realms)](#100诸界浩劫-cataclysm-of-realms)
  * [1.2.0「共鸣与臻藏」 (Resonance & Collector)](#120共鸣与臻藏-resonance--collector)
  * [1.3.0「铭刻与试炼」 (Inscription & Trials)](#130铭刻与试炼-inscription--trials)
    * [1.3.1「铭文纪元」 (Era of Inscription)](#131铭文纪元-era-of-inscription)
    * [1.3.2 修复补丁 (Hotfix)](#132-修复补丁-hotfix)
  * [1.4.0「环佩与獠牙」 (Trinkets & Fangs)](#140环佩与獠牙-trinkets--fangs)
  * [1.4.1 修复与观感打磨 (Polish)](#141-修复与观感打磨-polish)
* [通用技术模式](#通用技术模式)
* [记录规范](#记录规范)

***

## 附魔

### 1. 凋零保护 (Wither Protection)

**功能**：受到凋零效果时，按装备上的附魔总等级缩短凋零持续时间。每级减少 15%，多件叠加，总减少上限 90%。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/wither_protection.json`：

* `max_level: 4`，权重 / 费用曲线复制自火焰保护（获取概率与四类原版保护一致）
* `supported_items: #minecraft:enchantable/armor`（四件护甲）
* `exclusive_set: "#minecraft:exclusive_set/armor"` + 追加进原版 `exclusive_set/armor` 标签（与保护 / 火焰 / 爆炸 / 弹射物保护互斥，双向）
* `effects: {}` —— 效果逻辑无对应数据组件，纯 Mixin 实现

**核心逻辑 Mixin** `mixin/LivingEntityMixin.java`：

`@ModifyVariable` 拦截 `LivingEntity.addEffect(MobEffectInstance, Entity)` 的入参：

* 判定 `effect.is(MobEffects.WITHER)` 且非无限时长
* 遍历 4 个护甲槽位（`EquipmentSlot.isArmor()`），累加各装备上的凋零保护等级
* `reduction = min(0.9, totalLevels × 0.15)`，返回 `effect.withScaledDuration(1.0F - reduction)`

**获取途径**：追加进 `minecraft:non_treasure` 标签 → 自动获得附魔台（`in_enchanting_table`）、图书管理员（`tradeable`）、宝箱 / 钓鱼（`on_random_loot`）全部途径（这三个标签都引用 `#non_treasure`）。

### 2. 炽焰行者 (Blazing Walker)

**功能**：与冰霜行者同构 —— 行走时脚下半径内岩浆凝固成岩浆块（1 级半径 3 格、2 级 4 格），同时免疫踩踏热方块的伤害（含岩浆块）。与冰霜行者、深海探索者互斥。

#### 实现方法

**纯数据驱动**，无 Mixin。`data/extra-enchantry/enchantment/blazing_walker.json` 直接复制冰霜行者结构改两个效果目标：

* `minecraft:location_changed` 效果 → `minecraft:replace_disk`：`block_state` 由 `frosted_ice` 改为 `magma_block`，predicate 中 `matching_blocks`/`matching_fluids` 由 `water` 改为 `lava`（其余不变：上方空气、unobstructed、on_ground 且非载具时触发）
* `minecraft:damage_immunity` 效果：免疫 `burn_from_stepping` 伤害标签（冰霜行者原样保留，恰好覆盖岩浆块踩踏伤害）
* `exclusive_set: "#minecraft:exclusive_set/boots"` + 追加进原版 boots 互斥标签
* 等级 / 权重 / 费用 / 铁砧成本与冰霜行者完全一致

**获取途径**：作为宝藏附魔 —— 追加进 `treasure`、`tradeable`、`on_random_loot` 三个标签（无附魔台）。

### 3. 破限 (Limit Break)

**功能**（单级，作用于带此附魔的物品 / 护甲）：

1. 无视全部附魔互斥（保护四系、精准 / 时运、修补 / 无限、锋利 / 亡灵 / 节肢、致密 / 破甲系、多重 / 穿透等）
2. 无视铁砧 "过于昂贵"—— 合成费用固定为 5 级经验
3. 保护类附魔伤害减免上限从 80% 提升到 100%
4. 跨部位附魔解锁：带破限的腿甲可在铁砧附魔原版摔落保护（原版仅限靴子）
5. 装备上的破限无法被砂轮去除
6. **等级突破（1.0.1）**：带破限的输入在铁砧融合时，所有"逻辑上可增加一级"的附魔等级上限 **+1**（例：两个保护 IV → 保护 V；触及 X → XI）。覆盖 32 种原版附魔（耐久/四保护/摔落/荆棘/水下呼吸/深海探索者/灵魂疾行/锋利三系/抢夺/击退/横扫/效率/破甲/致密/风爆/穿刺/冲刺/力量/冲击/快速装填/多重射击/忠诚/激流/穿透/时运/海之眷顾/饵钓）+ 7 种本 mod 附魔（凋零保护/触及/汲取/蚀命/活力/壁垒/空跃）。仅对可成长附魔生效，单级附魔（精准采集等）不变。

**使用门禁**：破限附魔书在完成「诸界浩劫」守护挑战（四波，见[「1.0.0 诸界浩劫」](#100诸界浩劫-cataclysm-of-realms)）前**无法使用**—— 未达成「无敌」进度时，铁砧上应用破限书会被拦截（清空产出 + 动作栏提示）；达成「无敌」进度即解锁，进度持久化 = 解锁状态持久化。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/limit_break.json`：

* 1 级，`supported_items: #extra-enchantry:limit_break_supported`（自定义物品标签汇总武器 / 工具 / 护甲 / 弓弩等 10 个 enchantable 标签）
* 不加入任何获取标签（仅监守者掉落）

**获取途径**（替代原先覆盖监守者战利品表的做法，见 `LimitBreakManager`）：

* 订阅 fabric 的 `ServerLivingEntityEvents.AFTER_DEATH`：死亡实体是监守者时 ——
  * 普通击杀：`0.05%`（0.0005）概率掉落 1 级破限附魔书；
  * **闪电苦力怕**（`Creeper#isPowered`）击杀：`0.5%`（0.005）概率掉落。
* 附魔书经 `Items.ENCHANTED_BOOK` + `STORED_ENCHANTMENTS` 组件构造；闪电苦力怕来源额外打一个

  自定义布尔组件 `limit_break_source`（区分隐藏进度来源），普通来源不打标记。

**隐藏挑战进度**（`data/extra-enchantry/advancement/hidden_challenges/`，基于 "拾取到破限附魔书" 判定）：

* 根进度 `root`（`impossible` 触发器，永不自动完成，仅作隐藏容器）；
* `obtain_limit_break_book`「极限之证」：拾取任意来源的破限附魔书；
* `obtain_limit_break_book_charged`「雷霆之礼」：拾取闪电苦力怕代杀监守者掉落的破限书。
* 判定入口：`PlayerMixin` 挂 `Player#addItem`（拾取 / 漏斗 / 合成 / 命令统一入口），

  `LimitBreakManager.onItemObtained` 检查是否为带破限的附魔书并 `award` 对应进度。

**Mixin 三处**（互斥检查点经反编译确认 26.2 仅两处）：

1. `mixin/AnvilMenuMixin.java`
* `@Redirect` 拦截 `createResult` 中的 `Enchantment.areCompatible(a, b)`：任一输入物品带破限 → 直接返回 true（互斥全免）
* `@Inject` 在 `createResult` TAIL：产出非空且带破限 → `cost.set(5)`
1. `mixin/EnchantmentHelperMixin.java`（附魔台路径）
* `filterCompatibleEnchantments` 无物品上下文 → 用 **ThreadLocal** 在 `selectEnchantment` HEAD/RETURN 间传递当前物品
* `@Inject`（HEAD, cancellable）拦截 `filterCompatibleEnchantments`：物品带破限 → 跳过过滤
1. `mixin/LivingEntityMixin.java`（保护上限）
* `@Redirect` 拦截 `getDamageAfterMagicAbsorb` 中的 `CombatRules.getDamageAfterMagicAbsorb(damage, protection)`
* 护甲带破限时改用 `damage × (1 - clamp(EPF, 0, 25)/25)`（原版 clamp 上限 20 = 80% 减免，改 25 = 100%）

**等级突破（1.0.1）**：`AnvilMenuMixin` 重定向 `createResult` 内的 `Enchantment#getMaxLevel`——带破限输入时对 `LEVEL_UP_ENCHANTMENTS`（32 原版 + 7 本 mod）返回原版上限 +1。`hasLimitBreakInput` 同步改用 `carriesLimitBreak`（同时检查 ENCHANTMENTS 与 STORED_ENCHANTMENTS），破限**书**作为附加槽也能触发融合加成。配套扩展蚀命比率表（等级 4 → 19%）与壁垒上限表（等级 11 → 1.5 HP）。

**跨部位附魔解锁**（摔落保护上腿甲）：主类订阅 fabric-item-api 的 `EnchantmentEvents.ALLOW_ENCHANTING` 事件 —— 附魔是原版 `feather_falling` 且目标是带破限的腿甲（`#minecraft:enchantable/leg_armor`）时返回 `TriState.TRUE` 跳过 canEnchant（supported_items 部位检查）。**不能**用 Mixin Redirect 拦 `Enchantment.canEnchant`：fabric-item-api 自身的 AnvilMenuMixin 已 Redirect 该调用点（Redirect 独占注入，冲突即 Critical injection failure）。

**砂轮防移除** `mixin/GrindstoneMenuMixin.java`：

* 砂轮 `removeNonCursesFrom` 移除全部非诅咒附魔（**单物品与双物品合并路径都汇于此**，反编译确认 mergeItems 内部也调用它）
* `@Inject` HEAD 时记录装备 ENCHANTMENTS 中的破限 Holder 与等级（ThreadLocal），RETURN 时塞回结果物品
* 附魔书不保护：书去附魔后本就转换为普通书（原版机制），是自愿清除的合理途径

### 4. 拓阶 (Tier Break)

**功能**：3 级，每级 +1 挖掘等级（低级工具可采集原本不可掉落的方块，如木镐 + 1 挖铁矿）。下界合金镐 + 3 级拓阶可挖掘基岩，挖掘时间为黑曜石的两倍，受效率影响，破坏后掉落基岩。

#### 实现方法

**等级体系**（反编译 `ToolMaterial.applyToolProperties` 确认）：工具由 Tool 组件两条规则构成 ——`deniesDrops(#incorrect_for_X_tool)` + `minesAndDrops(#mineable/X, speed)`。标签嵌套推导等级：木 / 金 = 0，石 / 铜 = 1，铁 = 2，钻石 / 下界合金 = 3。

**Mixin 三处**：

1. `mixin/ItemStackMixin.java`
* `@Inject`（HEAD, cancellable）拦截 `isCorrectToolForDrops`：解析 Tool 规则的 incorrect 标签得到基础等级，加拓阶级数后换算新的限制标签，范围内放行（`state.is(旧限制)` 为 false 时返回 true）；基岩特殊分支（下界合金镐 + 3 级）直接 true
* 同样拦截 `getDestroySpeed`：等级提升后以工具正常速度挖掘（否则退化为手速）；基岩返回镐子材质速度（使效率属性生效）
1. `mixin/BlockBehaviourMixin.java`
* `@Redirect` 拦截 `getDestroyProgress` 中的 `BlockState.getDestroySpeed`：基岩原版 destroyTime=-1（进度恒 0 不可破坏）；持下界合金镐 + 3 级拓阶时返回 100（黑曜石 50 的两倍）

**数据**：附魔 JSON（3 级，`#minecraft:enchantable/mining_loot`，仅主手）；`entities/wither.json` 追加 20% 掉落池（等级 `uniform 1-3`）；新增 `blocks/bedrock.json` 掉落表（基岩掉自身）。

### 5. 触及 (Reach)

**功能**：10 级，每级 +0.5 实体交互范围与方块交互范围。非宝藏附魔，随机来源仅能获得 1 级，更高等级只能通过铁砧融合升级（I+I→II...）。

#### 实现方法

**效果（纯数据驱动）**：`data/extra-enchantry/enchantment/reach.json` 的 `minecraft:attributes` 效果（同火焰保护改 `burning_time` 的机制）：

```
"minecraft:attributes": [

  { "attribute": "minecraft:entity_interaction_range", "amount": {"type": "minecraft:linear", "base": 0.5, "per_level_above_first": 0.5}, ... },

  { "attribute": "minecraft:block_interaction_range", ... }

]
```

**"仅 1 级" 的两路拦截**（等级来源经反编译确认分两条路径）：

1. **附魔台 / 钓鱼**（`getAvailableEnchantmentResults` cost 窗口算法，从最高级向下找首个满足 `minCost ≤ cost ≤ maxCost` 的等级）：`min_cost = {base: 5, per_level_above_first: 100}` → 2 级需 cost≥105（附魔台上限 30、钓鱼上限约 40）→ 永远只出 1 级。**纯数据方案，无代码**
2. **宝箱附魔书 / 图书管理员交易**（都走 `enchant_randomly` 函数的 `Mth.nextInt(random, minLevel, maxLevel)` 均匀随机）：`mixin/EnchantRandomlyFunctionMixin.java` 用 `@Redirect` 拦截该调用，触及附魔强制返回 minLevel
* 注意 Redirect 处理器签名：**被重定向调用的参数在前，外围方法参数在后**

**获取**：`supported_items: #extra-enchantry:reach_supported`（weapon + trident + mining_loot 标签，即剑 / 矛 / 斧 / 锤 / 三叉戟 / 镐 / 锄 / 锹）；追加进 `non_treasure`。

### 6. 假象 (Decoy)

**功能**：3 级，头盔专属，与荆棘互斥。当生物仇恨锁定玩家或被其他玩家伤害时触发判定（判定间隔 10 秒，成功后进入全局冷却）。生成复刻玩家外观的诱饵转移仇恨：

| 等级  | 触发概率 | 诱饵数 | 生命值 | 持续   | 冷却   |
| --- | ---- | --- | --- | ---- | ---- |
| I   | 25%  | 1   | 8   | 12 秒 | 25 秒 |
| II  | 40%  | 1   | 16  | 18 秒 | 20 秒 |
| III | 55%  | 2   | 20  | 25 秒 | 15 秒 |

诱饵存在期间玩家处于 "仇恨脱战"—— 即使主动攻击敌人，敌人也优先追击诱饵，直至全部诱饵被摧毁。含两个彩蛋：0.5% 阿西莫夫级联（诱饵攻击主人及其他诱饵）、受击诱饵 5% 嵌套生成更弱的诱饵（诱饵的诱饵…… 递归减弱）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/decoy.json`：

* 权重 / 费用 /anvil_cost 完全复制荆棘（稀有度一致），`supported_items/primary_items: #minecraft:enchantable/head_armor`，`slots: ["head"]`
* `exclusive_set: ["minecraft:thorns"]` —— **直接列表格式**（`RegistryCodecs.homogeneousList` 支持单 ID / 列表 /# 标签三种写法）。互斥是双向的：`areCompatible` 检查双方的 exclusiveSet，单侧声明即完全互斥

**诱饵实体** `entity/DecoyEntity.java`：

* **复用原版盔甲架实体类型**（`new DecoyEntity(...)` 内部调 `ArmorStand(Level,x,y,z)` 固化 ARMOR_STAND 类型）—— 客户端零注册自动渲染，服务器侧保留子类行为
* 外观复刻：头部 = 带主人档案的玩家头颅（`DataComponents.PROFILE` + `ResolvableProfile.createResolved`，客户端自动解析皮肤），盔甲与主副手物品原样复制
* 行为：无重力缓慢游走（每 3-7 秒随机换向，离主人 8 格外自动靠拢）、`hurtServer` 重写为纯血量扣减、超时 / 摧毁时散发淡蓝色全息粒子（`DustParticleOptions(0x66CCFF)` + 电火花）
* 彩蛋：`asimov` 标记的诱饵每秒攻击 2.5 格内主人和其他诱饵；受击时一次性 5% 嵌套判定（生命 / 持续减半，深度上限 5）
* `shouldBeSaved()=false` 永不持久化（防重启残留为普通盔甲架），`interact` 返回 PASS 禁止取放装备

**状态管理** `DecoyManager.java`：

* 每玩家状态：`lastJudgeMs`（10 秒判定间隔）+ `cooldownUntilMs`（全局冷却，期间无论受多少次攻击不再触发）+ 存活诱饵列表
* `tryTrigger`：间隔→冷却→等级→概率四重校验后生成诱饵
* 生成时 `retargetNearbyMobs`：32 格内所有正在仇恨主人的生物立即 `setTarget(诱饵)`（"立刻转移"）

**Mixin 两处**：

1. `mixin/MobMixin.java` —— `@ModifyVariable` 拦截 `Mob.setTarget` 的目标参数（`asValidTarget` 之前）：目标是带诱饵的玩家→改为最近同维度诱饵（无诱饵则先执行触发判定）。**全部仇恨路径**（索敌 / 受击反击 / 仇恨传递）都汇于 setTarget，单点拦截全覆盖
2. `mixin/LivingEntityMixin.java` —— `@Inject` 拦截 `hurtServer` HEAD：ServerPlayer 被**其他玩家**伤害时执行 `tryTrigger`（PvP 触发路径）

**获取途径**：与荆棘同稀有度 → 追加进 `non_treasure`（附魔台 / 图书管理员 / 宝箱 / 钓鱼全途径，获取概率与荆棘一致）。

### 7. 汲取 (Siphon)

**功能**：3 级，可附魔在武器 / 工具 / 远程武器上。每次攻击成功造成伤害时回复自身生命：近战每级回复 1 颗心（I/II/III 级 → 2/4/6 HP）；远程武器（弓 / 弩 / 掷出的三叉戟等投射物命中）获得的回复效果减半（→ 1/2/3 HP）。非宝藏附魔，稀有度 very_rare（`weight: 1`）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/siphon.json`：

* `max_level: 3`，`weight: 1`（原版 12 档稀有度中权重 1 档 = very_rare）
* `supported_items/primary_items: #extra-enchantry:siphon_supported`（自定义物品标签聚合 weapon/mining/bow/crossbow/trident/mace 六个 enchantable 标签，覆盖近远程）
* `slots: ["mainhand"]`（仅主手生效）
* `effects: {}` —— 逻辑纯 Mixin 实现

**核心逻辑 Mixin** `mixin/LivingEntityMixin.java`：

`@Inject` 在 `hurtServer` RETURN（成功造成伤害返回 true 才触发）：

* 攻击者取 `source.getEntity()`（需为 LivingEntity 且非自己）；武器取 `source.getWeaponItem()`—— 近战为攻击者主手物品，箭矢命中时返回发射时的弓 / 弩，掷出的三叉戟返回三叉戟本身（26.2 该方法统一走 directEntity 的武器栈，一处覆盖全部武器来源）
* 远近判定用 `source.isDirect()`（直接伤害 = 近战；投射物等非直接伤害 = 远程减半）
* 回复量 `heal(等级 × 2.0F × (isDirect ? 1.0 : 0.5))`

**获取途径**：追加进 `minecraft:non_treasure` 标签 → 附魔台（`in_enchanting_table` 引用 `#non_treasure`）、图书管理员、宝箱、钓鱼全途径；因 `weight: 1` 实际出现率同荆棘级。

### 8. 蚀命 (Life Erosion)

**功能**：3 级，可附魔在近战武器 / 工具与远程武器上。对生物造成伤害时，额外追加百分比**目标最大生命值**的伤害：I 级 14%、II 级 15%、III 级 17%；远程武器（弓 / 弩 / 掷出的三叉戟等投射物）效果减少 1/4（即 ×0.75）。仅远古城市宝箱可获得（附魔书或自带蚀命的武器 / 工具）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/life_erosion.json`：

* `max_level: 3`，`weight: 1`（very_rare）
* `supported_items/primary_items: #extra-enchantry:life_erosion_supported`（自定义物品标签聚合 weapon/mining/bow/crossbow/trident/mace，即 “武器工具 + 远程”）
* `slots: ["mainhand"]`，`effects: {}` 纯 Mixin 实现

**核心逻辑 Mixin** `mixin/LivingEntityMixin.java`：

`@ModifyVariable`（HEAD, argsOnly）拦截 `hurtServer` 的伤害入参 `amount`：

* 武器取 `source.getWeaponItem()`（同汲取，近战即攻击者主手，箭矢为发射时的弓 / 弩）
* `amount += 目标 getMaxHealth() × {0.14, 0.15, 0.17}[等级-1]`；非直接伤害（`!source.isDirect()`，即投射物）时比率 ×0.75，加成随原始伤害一起走后续护甲 / 魔抗结算

**获取途径（专属掉落表）**：覆盖 `data/minecraft/loot_table/chests/ancient_city.json`—— 完整保留原版两个池（通用战利品 + 盔甲纹饰模板），追加第三池（rolls 1）：

* 70% 空、16% 蚀命附魔书（等级 `uniform 1-3`）、14% 带蚀命的武器 / 工具（铁剑 / 铁斧 / 铁镐 / 钻石剑 / 钻石镐 / 钻石斧，均 `set_damage 0.8-1.0` + 等级随机）
* `set_enchantments` 作用于 `minecraft:book` 时自动转为附魔书（STORED_ENCHANTMENTS，反编译 `SetEnchantmentsFunction.run` 确认）

**排他性保障**：不加入 `non_treasure`/`tradeable`/`on_random_loot` 任何获取标签 ——`enchant_randomly`（宝箱书 / 交易 / 钓鱼随机附魔）从 `#on_random_loot` 标签筛选候选（反编译 `EnchantRandomlyFunction` 确认），不在标签内则全途径隔离，仅远古城市专属池掉落。

### 9. 活力 (Vitality)

**功能**：5 级，四件盔甲与全部马铠均可附魔。每级 +4 点最大生命值，多件叠加，叠加上限 +50；任意护甲带破限时上限失效（最高 4 件 × 5 级 × 4 HP = +80）。稀有度普通（`weight: 10`），非宝藏。血条过长遮挡视野的额外生命不在心形栏中展开，而是在原生命值条上方左对齐以 “❤×n/N” 紧凑显示（n = 当前剩余，N = 盔甲提供的最大颗数）。马铠活力对马生效（26.2 `isArmor()` 覆盖动物 BODY 槽，天然参与同一套求和），骑兵队等持械生物的下界合金套同样照常结算。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/vitality.json`：

* `max_level: 5`，`weight: 10`（原版 common 稀有度权重档）
* `primary_items: #minecraft:enchantable/armor`，`supported_items: #extra-enchantry:vitality_supported`（自定义标签：`#minecraft:enchantable/armor` + 六种马铠），`slots: ["armor"]`
* `effects: {}` —— 加成与 HUD 均为 Mixin 实现

**属性加成 Mixin** `mixin/LivingEntityMixin.java`：

`@Inject` 拦截 `LivingEntity.tick` HEAD（全部 LivingEntity；原为仅 Player，马铠活力上线后放开）：

* `ExtraEnchantry.getVitalityBonus(entity)`：护甲槽（`isArmor()` 含人形四件套与动物 BODY 槽）活力总等级 × 4，无破限则钳制 50；客户端同样调用（计算 HUD 显示值）
* 与上次值不同时向 `Attributes.MAX_HEALTH` 写入 / 移除瞬态修改器（`addOrUpdateTransientModifier`，id `extra-enchantry:vitality`，ADD_VALUE）；tick 双端运行，客户端最大生命值同步正确
* 加成减少时钳制当前生命值，避免血量残留超上限

**HUD 显示 Mixin（客户端）** `client/mixin/HudMixin.java`，26.2 HUD 为渲染状态提取架构（`Hud.extractPlayerHealth` 计算心形行数并提取贴图 / 文本元素）：

1. `@Redirect` ×2 拦截 `extractPlayerHealth` 中的 `Player.getAttributeValue(Attributes.MAX_HEALTH)` 与 `Player.getHealth()`，两者均扣除 / 钳制活力加成 → 心形栏行数与红心数均按基础生命计算，保持原版单行 10 颗心；伤害吸收黄心也只出现在基础行之上（血条不再多行堆叠遮挡视野，也不会把活力血量带出来）
2. `@Inject` 在 `extractPlayerHealth` TAIL：盔甲图标行上方（`extractArmor` HEAD 另一 `@Inject` 捕获盔甲行实际 y = y - (rows-1)×rowHeight - 10，心行增多时自动上移，活力行锚定其上 10 像素）左对齐绘制 `hud/heart/container`+`hud/heart/full` 双层心形图标 + 白色文本 `×n/N`（N = 盔甲活力提供的最大❤ = 加成 / 2；n = 当前剩余❤ = 钳制 (总血量 - 基础上限，0, 加成) 取整后半心粒度，27.9HP→14，27HP→13.5；伤害吸收不计入 getHealth 不影响 n；活力部分位于血量池顶端）

**获取途径**：追加进 `minecraft:non_treasure` → 附魔台 / 图书管理员 / 宝箱 / 钓鱼全途径，普通稀有度出现率。

### 10. 壁垒 (Bulwark)

**功能**：10 级，胸甲与全部马铠可附魔。锁定受到单次伤害的上限：I 级 5.5 颗心（11 HP），每级递减 0.5 颗心，至 X 级 1 颗心（2 HP）。非宝藏，随机来源仅能获得 1 级（同触及），更高等级只能铁砧融合升级。马铠壁垒对马生效（`applyBulwarkCap` 同时读 CHEST 与 BODY 槽取较大者 —— 玩家 BODY 恒空、马 CHEST 恒空，互不干扰）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/bulwark.json`：

* `max_level: 10`，`weight: 5`，`primary_items: #minecraft:enchantable/chest_armor`，`supported_items: #extra-enchantry:bulwark_supported`（自定义标签：`#minecraft:enchantable/chest_armor` + 六种马铠），`slots: ["chest"]`
* 部位约束（仅胸甲）由 `supported_items` 在生存模式天然生效；原版铁砧对**创造模式**（`hasInfiniteMaterials`）与目标为附魔书时豁免 `canEnchant` 部位检查（书间可转移任意附魔）—— 此为原版规则，**刻意不拦截**，创造模式下壁垒书上靴子属原版行为
* "仅 1 级" 双路拦截完全复用触及方案：`min_cost = {base: 5, per_level_above_first: 100}` → 附魔台 / 钓鱼永远只出 1 级（纯数据）；`EnchantRandomlyFunctionMixin` 钳制宝箱书 / 交易的 `enchant_randomly` 到 minLevel（该方法已同时覆盖触及与壁垒）

**核心逻辑 Mixin** `mixin/LivingEntityMixin.java`：

`@Inject`（RETURN, cancellable）注入 `getDamageAfterMagicAbsorb` 方法本身（护甲 / 魔抗减免链的终点）：

* 胸甲（`EquipmentSlot.CHEST`）壁垒等级查表 `BULWARK_CAP = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2}` HP
* `cir.setReturnValue(applyBulwarkCap(self, 原返回值))` —— **钳制发生在防御减免之后**、吸收盾结算之前，上限约束的是实际承伤（吸收 + 掉血合计），不会被护甲二次削减（若钳在 hurtServer 入口，30 伤害先钳 2 再被护甲减 80% 只剩 0.4，出现过强 bug）
* **单点覆盖玩家与非玩家**：`Player` 重写了 `actuallyHurt` 不调 super（曾因此在 PlayerMixin 里尝试双拦截），但其内部对 `getDamageAfterMagicAbsorb` 是虚调用，同样派发到 `LivingEntity` 的唯一实现 —— 注入方法本身即可全覆盖，**且不破坏本 Mixin 对其内部 CombatRules 调用的 Redirect（破限 100% 保护）**。教训：`@Shadow`/`@Invoker` 都只在目标类本类解析（Player 上 shadow 不到 LivingEntity 的方法），跨类调用点优先考虑注入 "被调方法本身" 而非拦截调用点

**获取途径**：追加进 `minecraft:non_treasure`，随机途径仅出 1 级，II 级及以上需铁砧融合。

### 11. 劫后余辉 (Afterglow)

**功能**：单级，**只能附魔在不死图腾上**。带此附魔的不死图腾被触发时，在原版复活效果之外额外给予 "余辉"：生命值立即回满、获得 10 秒锁血（期间生命值不会因任何伤害而降低）、并给予 5 颗伤害吸收的黄色心（10 HP，取自金苹果的伤害吸收效果）。非宝藏附魔，稀有度 rare（`weight: 2`）。效果不做任何持久化 —— 图腾触发时即被消耗，附魔随之消失。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/afterglow.json`：

* `max_level: 1`，`weight: 2`（原版 12 档稀有度中权重 2 档 = rare），`anvil_cost: 4`
* `supported_items/primary_items: "minecraft:totem_of_undying"` —— **直接写单个物品 ID**。不死图腾没有 `enchantable` 组件、也不在任何 `#minecraft:enchantable/*` 标签内，所以没有现成标签可用；`HolderSet` 编解码支持单 ID 字符串，`#tag` 只是可选项
* `slots: ["hand"]`（`EquipmentSlotGroup.HAND`，主手 / 副手持图腾均生效）
* `effects: {}` —— 效果无对应数据组件，纯 Mixin 实现

**触发点 Mixin** `mixin/DeathProtectionMixin.java`：

`@Inject` 在 `DeathProtection.applyEffects(ItemStack, LivingEntity)` 的 RETURN，参数即触发用的图腾栈。

* 原版 `LivingEntity#checkTotemDeathProtection` 的流程是：copy 一份图腾栈 → 消耗原栈 `shrink(1)` → `setHealth(1.0F)` → `deathProtection.applyEffects(副本, this)`
* 两个必须挂在这里（而不是 `checkTotemDeathProtection` 自身）的理由：
1. **参数是图腾栈的副本**（附魔信息完整）；若挂 `checkTotemDeathProtection`，注入点无论 HEAD 还是 RETURN，原栈都已被消耗 / 尚不可知，读不到附魔等级
2. 死亡效果列表的第一项是 `ClearAllStatusEffectsConsumeEffect`，注入点必须**晚于**它，否则施加的效果会被立刻清空
* 消耗发生在注入点之前 → "触发后随图腾一起消失" 由原版机制天然保证，无需额外处理

**效果实现** `AfterglowManager.java` + `ExtraEnchantryEffects.java`：

* `setHealth(getMaxHealth())` 回满
* **余辉状态栏效果**：注册自定义 `extra-enchantry:afterglow` MobEffect（`ExtraEnchantryEffects`，BENEFICIAL 蓝框、暖金色调 0xFFC850、图腾粒子），触发时施加 10 秒实例 —— 药水状态栏自动显示**余辉图标与倒计时**；`isLocked` 直接 `hasEffect` 查询该实例，锁血计时与状态栏共用同一来源，效果到期锁血自动结束（不再用独立的毫秒时间戳 Map）。图标纹理 `assets/extra-enchantry/textures/mob_effect/afterglow.png`（18×18 ARGB，HUD 经 `Hud.getMobEffectSprite` 按 `mob_effect/<id>` 路径规则自动加载）
  * MobEffect 构造器是 protected，用**匿名子类** `new MobEffect(...) {}` 暴露
  * 实例参数 `(holder, duration, amplifier, ambient=false, visible=true, showIcon=true)`—— 非 ambient 保证状态栏显示完整倒计时而非闪烁图标
* 5 颗吸收心的取法：`AbsorptionMobEffect#onEffectStarted` 的公式是 `max(当前, 4 × (1 + amplifier))`，阶梯为 4/8/12 HP，**给不出 10 HP**—— 故先挂上金苹果的伤害吸收效果（`MobEffects.ABSORPTION`，0 级放大、2400 tick = 2 分钟，与金苹果一致，决定黄心的效果来源与时长），再 `setAbsorptionAmount(10.0F)` 精确覆盖（放在 `addEffect` 之后，避免被 `max()` 逻辑回退）
  * 顺序不可颠倒：`addEffect` 时原版图腾的 Absorption II（100 tick）仍生效，amplifier 更低的实例不会触发 `onEffectStarted`，只剩最终的 `setAbsorptionAmount` 生效

**锁血 Mixin** `mixin/PlayerMixin.java`：

`@Inject`（HEAD, cancellable）拦截 `Player#actuallyHurt`：锁血期间取消调用 → 生命值与吸收心都不减少，但击退、受伤音效、盔甲耐久等反馈照常发生（比在 `hurtServer` 处整体取消更贴近 "锁血" 手感）。

* **必须挂在** `Player` **而不是** `LivingEntity` **上**：`Player` 重写了 `actuallyHurt` 且**不调用 super**（自己完整实现了护甲 / 魔抗 / 吸收结算与 `setHealth`），拦截 `LivingEntity` 的版本对玩家完全无效

**获取途径**：追加进 `minecraft:non_treasure` → 自动进入 `in_enchanting_table`/`tradeable`/`on_random_loot`（图书管理员、宝箱、钓鱼）。注意：附魔台槽位的准入检查是 `ItemStack#isEnchantable()`（要求 `DataComponents.ENCHANTABLE` 组件），不死图腾没有该组件，**实际放不进附魔台**；且 `AnvilMenu` 不做该检查（只看 `supported_items`），所以生存中的正常途径是铁砧把劫后余辉附魔书打到不死图腾上。

### 12. 誓约 (Oathbound)

**功能**：单级。带誓约的物品在玩家死亡时不掉落、不消失（消失诅咒同样无效），重生后回到背包原槽位；四件护甲（头 / 胸 / 腿 / 脚）全部带誓约时，死亡时经验值与分数也全额保留（随身 keepInventory）。`keepInventory` 开启时本附魔逻辑自然短路。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/oathbound.json`：

* `max_level: 1`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#extra-enchantry:limit_break_supported"`（复用全装备聚合标签），`slots: ["any"]`
* `effects: {}` 纯 Mixin

**核心逻辑**（26.2 死亡链路见踩坑记录）：

* **物品保留**：`PlayerMixin` 在 `Player#dropEquipment`（玩家死亡物品掉落唯一入口，父类实现为空壳）HEAD 提取全部带誓约物品、TAIL 原槽位放回 ——`destroyVanishingCursedItems`（消失诅咒销毁）与 `inventory.dropAll` 都碰不到它们
* **重生搬运**：`ServerPlayerMixin` 在 `ServerPlayer#restoreFrom` TAIL 补搬运（原版仅 keepInventory / 旁观者时搬运）：部分誓约 → `Inventory#replaceWith` 仅搬背包，经验照常掉落；四件套 → `transferInventoryXpAndScore` 全量搬运（背包 + 经验 + 分数）
* **经验防重复**：`LivingEntityMixin` @Redirect `dropAllDeathLoot` 内的 `dropExperience` 调用 —— 四件套时跳过经验球生成（否则 "保留 + 掉落" 重复），非四件套路径透传原版

**获取途径**：追加进 `minecraft:non_treasure`。

### 13. 空跃 (Skyward)

**功能**：2 级，靴子专属。空中可再跳 I 级 1 次 / II 级 2 次。次数在触地 / 进水 / 攀爬时重置；按住空格不连烧次数（按键沿触发）；骑乘、鞘翅滑翔、创造飞行时不触发。与冰霜行者 / 深海探索者 / 炽焰行者互斥。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/skyward.json`：

* `max_level: 2`，`weight: 5`（uncommon），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/foot_armor"`，`slots: ["feet"]`
* 互斥：追加进原版 `minecraft:exclusive_set/boots` 标签（同炽焰行者的做法）

**核心逻辑**（客户端 / 服务端分工 ——26.2 反编译确认玩家跳跃输入是客户端权威，服务端看不到跳跃键）：

* **空中跳跃**：客户端 `LocalPlayerMixin`（`src/client`，注册于 `extra-enchantry.client.mixins.json`）@Inject `LocalPlayer#aiStep` HEAD：直接读公开字段 `input.keyPresses.jump()` 做按键沿检测，触发时调用 `jumpFromGround()`（26.2 为 public，含跳跃力度与疾跑加跳），手感与原版跳一致

**获取途径**：追加进 `minecraft:non_treasure`。

### 14. 破阵 (Cleave)

**功能**：3 级，近战武器 / 工具。命中主目标后，对 2 格内最多 1/2/3 个额外目标造成主目标伤害的 45%/55%/65%（走完整护甲结算；v1.1.0 由 50%/60%/70% 下调，缩小与横扫之刃的差距），按与主目标的距离由近到远选取。触发时附带范围视觉：每个溅射目标身上生成横扫攻击粒子（原版横扫之刃同款），主目标脚下画一圈 2 格半径的暴击粒子范围指示。溅射**不触发**汲取与蚀命（防滚雪球），也**不触发**破阵自身（防递归）。与横扫之刃互斥。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/cleave.json`：

* `max_level: 3`，`weight: 5`（uncommon），`anvil_cost: 2`
* `supported_items/primary_items: "#extra-enchantry:cleave_supported"`（**新建**自定义物品标签：`#minecraft:enchantable/weapon` + `#minecraft:enchantable/mining`，仅近战，不含 trident/bow）
* `exclusive_set: ["minecraft:sweeping_edge"]`（列表写法，同假象对荆棘）
* `slots: ["mainhand"]`，`effects: {}` 纯 Mixin

**核心逻辑** `CleaveManager` + `LivingEntityMixin`：

* @Inject `hurtServer` RETURN：主目标伤害实际生效后，`ServerLevel.getEntitiesOfClass(LivingEntity, ...)` 以主目标碰撞箱外扩 2 格选目标（排除主目标 / 攻击者 / 旁观者 / 友方 / 死者），共用同一 `DamageSource` 逐个 `hurt`（伤害 = 主目标入参 × 比例）
* ThreadLocal 标记位统一短路三处 hurtServer 注入：破阵 RETURN 自身（防递归）、汲取 RETURN、蚀命 ModifyVariable（防滚雪球）
* **范围视觉** `spawnSplashEffects`：服务端 `ServerLevel#sendParticles` 自动广播附近玩家 —— 溅射目标身体中心（`getY(0.5)`）各一个 `SWEEP_ATTACK` 粒子；主目标脚下（`getY(0.2)`）以 2 格为半径沿圆周均布 16 个 `CRIT` 粒子画范围指示圈

**获取途径**：追加进 `minecraft:non_treasure`。

### 15. 御风 (Windrider)

**功能**：3 级，**鞘翅专属**。I 级滑翔耐久消耗 -50%；II 级在此基础上烟花火箭推进 +50%；III 级将 I、II 级效果各再强化 50%（耐久 -75%、推进 +75%）。仅末地城宝箱可获得。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/windrider.json`：

* `max_level: 3`，`weight: 2`（rare），`supported_items/primary_items: "minecraft:elytra"`（单物品 ID 写法，同劫后余辉的图腾），`slots: ["chest"]`（鞘翅占胸甲槽）
* 不加入任何获取标签 → 附魔台 / 交易 / 随机宝箱全隔离，仅专属掉落池

**核心逻辑两处**：

1. **滑翔耐久** `LivingEntityMixin` —— 26.2 反编译确认鞘翅耐久在 `LivingEntity#updateFallFlying` 内：每 10 tick 计一次、每 2 次（即 20 tick）对**可滑翔装备槽中随机一个** `hurtAndBreak(1, this, slot)`。`@Redirect` 该调用，按 `WINDRIDER_DURABILITY_SKIP = {0.5, 0.5, 0.75}` 概率跳过 → 等效耐久消耗 -50%/-75%
2. **烟花推进** `FireworkRocketEntityMixin`（新建）—— `FireworkRocketEntity#tick` 对**正在滑翔的附着实体**做一次朝视线方向的插值加速后调 `LivingEntity#setDeltaMovement`（该方法内仅此一处以 LivingEntity 为 owner 的调用，其余是火箭自身移动）。`@Redirect` 后取出 “本次推进增量”（新速度 - 旧速度）按 `WINDRIDER_BOOST_FACTOR = {1.0, 1.5, 1.75}` 放大写回，不改动原版插值公式

**获取途径（专属掉落表）**：覆盖 `data/minecraft/loot_table/chests/end_city_treasure.json`—— 保留原版两个池，追加第三池（rolls 1）：70% 空 / 18% 御风附魔书（等级 `uniform 1-3`）/ 12% 带御风的鞘翅（`set_damage 0.9-1.0`）。

### 16. 无踪 (Unseen)

**功能**：2 级，靴子专属。I 级：消除脚步声与落地声，**且不发出 STEP / HIT_GROUND 震动事件**—— 幽匿感测体与监守者无法通过行走 / 落地侦测到穿戴者（摔落伤害、落地粒子、`Block#fallOn` 行为全部保留）；II 级：额外使怪物索敌范围 -30%。与冰霜行者 / 深海探索者 / 炙焰行者 / 空跃互斥。仅远古城市宝箱可获得。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/unseen.json`：

* `max_level: 2`，`weight: 1`（very_rare），`supported_items/primary_items: #minecraft:enchantable/foot_armor`，`slots: ["feet"]`
* `exclusive_set: "#minecraft:exclusive_set/boots"` + 追加进该标签（同炙焰行者 / 空跃的做法，双向互斥）

**核心逻辑两处**：

1. **声音与震动屏蔽** `mixin/EntityMixin.java`（新建）——26.2 反编译确认：
* 脚步声与 `GameEvent.STEP` 震动由**同一个方法**负责：`Entity#vibrationAndSoundEffectsFromBlock(pos, state, playSound, sendEvent, movement)`（param3 控声音、param4 控事件）——`@Inject` HEAD cancellable 返回 false 即同时屏蔽两者（与原版在空气 / 游泳时的返回一致，调用方无副作用）
* 落地的 `GameEvent.HIT_GROUND` 在 `Entity#checkFallDamage` 内经 `Level#gameEvent(Holder, Vec3, Context)` 发出（方法声明在 `LevelAccessor`，调用点 owner 是 `Level`；LivingEntity 重写了 checkFallDamage 但末尾 `invokespecial` 调 super，所以注入 Entity 版本即全覆盖）——`@Redirect` 该调用，带无踪则不转发
1. **索敌范围降低** `LivingEntityMixin` —— `@Inject` 在 `LivingEntity#getVisibilityPercent(Entity)` 的 RETURN 乘 0.7。该方法是原版潜行（×0.8）/ 隐身影响索敌距离的**唯一系数**，与之同构，自动作用于全部 `TargetingConditions` 索敌路径

**获取途径（专属掉落表）**：`chests/ancient_city.json` 追加第四池（rolls 1）：70% 空 / 16% 无踪附魔书（等级 `uniform 1-2`）/ 14% 带无踪的靴子（铁 5 / 锁甲 5 / 钻石 4，均 `set_damage 0.8-1.0`）。不加入任何获取标签，与监守者（声音侦测）主题绑定。

### 17. 断罪 (Judgement)

**功能**：2 级，近战武器 / 工具（复用破阵的 `cleave_supported` 标签：weapon + mining）。目标当前生命（含伤害吸收）≤ 最大生命的 10%/20% 时**直接斩杀**；对 boss（末影龙 / 凋灵 / 监守者）不斩杀，改为该次伤害 ×2；斩杀触发后攻击者 5 秒冷却，触发瞬间目标身上爆发灵魂粒子。非宝藏，very_rare。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/judgement.json`：`max_level: 2`，`weight: 1`，`supported_items/primary_items: #extra-enchantry:cleave_supported`，`slots: ["mainhand"]`。

**核心逻辑** `JudgementManager` + `LivingEntityMixin`：

* `@ModifyVariable`（HEAD, argsOnly）拦截 `hurtServer` 的 `amount`，**定义在蚀命之后** → 同方法内按定义顺序链式执行，蚀命追加的百分比伤害也参与斩杀结算
* 武器取 `source.getWeaponItem()`（同汲取 / 蚀命）；阈值判定用 `getHealth() + getAbsorptionAmount()`（吸收心不绕过斩杀）
* 斩杀实现为把伤害放大到 `maxHealth × 4 + 100`（足以穿透吸收与护甲减免）；冷却按攻击者 UUID 记录毫秒时间戳（Map）
* 防滚雪球：`CleaveManager.isCleaving()` 期间不触发（避免一次挥砍连环斩杀）
* **boss 判定**：26.2 无统一的 `isBoss()`、也无 boss 实体标签（`EntityTypeTags` 只有 RAIDERS/UNDEAD/ARTHROPOD/SENSITIVE_TO_\*），故按带 boss 血条的三个原版 boss 显式 `instanceof`（`ExtraEnchantry.isBossLike`：EnderDragon/WitherBoss/Warden）

**与壁垒的克制关系**：斩杀仍会经过壁垒在 `getDamageAfterMagicAbsorb` 的上限钳制 —— 即壁垒可以挡下断罪（有意的攻防克制，非 bug）。

**获取途径**：追加进 `minecraft:non_treasure` → 附魔台 / 图书管理员 / 宝箱 / 钓鱼，`weight: 1` 出现率极低。

### 18. 疾风 (Gale)

**功能**：护腿专属。每级 +5% 移动速度（I/II → 5%/10%），**潜行时不生效**（保留潜行的战术价值）。常规途径最高 II 级；**III 级（+15%）只能由两个 II 级在任一输入带破限的铁砧上融合得到**。非宝藏，uncommon。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/gale.json`：

* `max_level: 3`，`weight: 5`，`supported_items/primary_items: #minecraft:enchantable/leg_armor`，`slots: ["legs"]`
* **III 级的三路封锁**（与触及 / 壁垒的 “仅 1 级” 同思路，阈值改为 2）：
1. 附魔台 / 钓鱼 / 宝箱 `enchant_with_levels`：`min_cost = {base: 5, per_level_above_first: 25}` → II 级需 cost ≥ 30（附魔台满书架恰好可达）、**III 级需 cost ≥ 55**；注意 `non_treasure` 会流入 `on_random_loot`，而原版宝箱装备的 `enchant_with_levels` 最高给到 **cost 50**（远古城市 / 末地城 30-50），故 III 级阈值必须 > 50 而不能只管附魔台的 30
2. 宝箱书 / 图书管理员交易：`EnchantRandomlyFunctionMixin` 对疾风钳制为 `nextInt(min, Math.min(max, 2))`
3. 铁砧融合：`AnvilMenuMixin` 在 `createResult` TAIL 检查产出，无破限输入时将疾风等级降回 2（重写 `DataComponents.ENCHANTMENTS`）

**速度加成 Mixin** `LivingEntityMixin`：`@Inject` `tick` HEAD（仅 Player，与活力同一模式）——`ExtraEnchantry.getGaleSpeedBonus` 读护腿等级 × 0.05，**潜行时返回 0**（`isCrouching()`）；值变化时向 `Attributes.MOVEMENT_SPEED` 写入 / 移除瞬态修改器（`ADD_MULTIPLIED_BASE`，id `extra-enchantry:gale`）。潜行 / 起身、穿脱护腿都会触发重算。

**获取途径**：追加进 `minecraft:non_treasure`（附魔台 / 交易 / 宝箱 / 钓鱼，随机最高 II 级）。

### 19. 余烬 (Emberfall)

**功能**：单级，**金胸甲与金马铠可附魔**。受到致命伤害时免死一次：保留 1 颗心（2 HP）+ 5 秒锁血，随后进入 60 秒「余烬」虚弱（缓慢 I + 挖掘疲劳 I，状态栏显示自定义图标与倒计时）；与不死图腾共存时**图腾优先**。仅不祥试炼（试炼密室）唯一奖励箱可获得（金马铠走铁砧 + 附魔书）。

* **金胸甲（玩家，CHEST 槽）**：每次触发消耗 **50% 最大耐久**（112 → 56），剩余耐久不足时不触发（不会把胸甲打碎）。
* **金马铠（马匹，BODY 槽）**：马铠不可掉耐久，改用**每实体 60 秒冷却**代替耐久消耗；触发时机体免死（1 颗心 + 5 秒锁血 + 同款虚弱），并把「余烬」效果**同步给骑士**—— 骑士获得同款 5 秒免伤锁血窗口（不继承缓慢 / 挖掘疲劳）。马铠死亡照常掉落，是骑兵队的战利品。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/emberfall.json`：`max_level: 1`，`weight: 2`（rare），`supported_items/primary_items: ["minecraft:golden_chestplate", "minecraft:golden_horse_armor"]`，`slots: ["chest", "body"]`。

**免死触发** `LivingEntityMixin` + `EmberfallManager`：

* `@Inject`（RETURN, cancellable）注入 `LivingEntity#checkTotemDeathProtection`（private）：26.2 链路为 `hurtServer → isDeadOrDying() → if (!checkTotemDeathProtection(source)) die(source)`，**返回 true 即跳过死亡**。选 RETURN 而非 HEAD → 图腾先走，图腾没救命才轮到余烬
* `trySave` 分两路：CHEST 槽金胸甲带余烬且 `isDamageableItem()` → 扣 50% 耐久；BODY 槽金马铠带余烬 → 查 60 秒每实体冷却（`Map<UUID, Long>`）。两路共用 `setHealth(2)`、清空吸收、施加余烬 / 缓慢 / 挖掘疲劳三个 60 秒效果、`sendParticles(SOUL_FIRE_FLAME)` 升腾视觉；马铠路额外给 `getFirstPassenger()`（骑士）施加余烬效果（仅免伤锁血窗口）

**锁血窗口**：不用独立计时器 —— 余烬效果总时长 1200 tick，**剩余时长 > 1100 tick（即前 5 秒）即为锁血期**（同劫后余辉的 “效果实例即计时器” 思路）。锁血拦截两处：`PlayerMixin#actuallyHurt`（玩家，Player 不调 super）与 `LivingEntityMixin#actuallyHurt`（非玩家生物），与余辉锁血共用同一取消点

**自定义状态效果** `ExtraEnchantryEffects.EMBERFALL`：HARMFUL（红框）、暗红色调 `0x8B2500`、结束粒子 `SOUL_FIRE_FLAME`；图标纹理 `assets/extra-enchantry/textures/mob_effect/emberfall.png`（18×18 ARGB 火焰图形，脚本生成）

**获取途径（专属掉落表）**：覆盖 `data/minecraft/loot_table/chests/trial_chambers/reward_ominous_unique.json`（不祥宝库唯一奖励）—— 保留原版 5 个唯一奖励，追加第二池：75% 空 / 15% 余烬附魔书 / 10% 带余烬的金胸甲（`set_damage 0.9-1.0`）。不加入任何获取标签。

### 盾牌四附魔 (Shield Enchantments)

26.2 盾牌机制已数据组件化（`DataComponents.BLOCKS_ATTACKS`，破盾 = 物品冷却、耐久 =`hurtBlockingItem`），四个盾牌附魔（编号 20~23）分别挂组件方法与伤害入口。

#### 20. 冲阵 Shield Charge（III，uncommon）

疾跑持盾（主 / 副手任一）近战命中：额外 **4/6/8 伤害** + **强力击退**（击退强度 0.9/1.1/1.3，方向 = 攻击者→受击者，26.2 `knockback(power, xd, zd, source, damage)` 的 (xd, zd) 为 "实体→击退源" 方向，实体被推离），攻击者 1 秒冷却。挂 hurtServer HEAD（蚀命之后、断罪之前 —— 冲阵加伤参与断罪斩杀阈值结算），逻辑在 `ShieldChargeManager`。

#### 21. 不屈 Defiance（II，rare）

* **I**：盾牌被斧类破盾（26.2 = 盾牌上物品冷却，时长来自攻击者武器组件 `WEAPON.disableBlockingForSeconds` = 5 秒）→ 时长**减半至 2.5 秒** + 等长的**抗性提升 I** 补偿；
* **II**：**完全免疫破盾**，代价是**格挡耐久消耗 ×2**。

实现：新 `BlocksAttacksMixin` 注入组件方法 ——`disable` HEAD 取消（II 免疫）/ `baseSeconds` 减半 + 抗性（I）；`hurtBlockingItem` 的 damage 参数 ×2（II）。

#### 22. 庇护 Sanctuary（III，试炼密室奖励箱）

格挡时每 **2 秒**对 **8 格内所有玩家**（含持有者）回复 **1/2/4 HP**，每次脉冲消耗盾牌 **1 点耐久**（耗尽后格挡中断光环即停）。视觉：受疗者头顶心形粒子 + 持有者周身环绕光点。逻辑在 `SanctuaryManager`（脉冲计时按维度 gameTime，首次举盾起算 2 秒）。

**获取**：`LootTableEvents.MODIFY`（fabric-loot-api-v3）向 `chests/trial_chambers/reward` 与 `reward_rare` 追加池：90% 空 / 7% 庇护书 / 3% 带 1\~3 级庇护的盾牌 ——**事件追加而非整表覆盖**，不丢原版奖励。

#### 23. 坚壁 Aegis（III，rare）

格挡中受到**不可格挡类伤害**（`#minecraft:bypasses_shield` 标签：音波 / 魔法等 —— 原版格挡对这类伤害完全无效，`BlocksAttacks#bypassedBy` 直接放行）时按等级减免 **30/45/60%**。挂 hurtServer HEAD（`isBlocking` + `getItemBlockingWith` 读等级 + 伤害标签判定）。

### 24. 归羽 (Homing Plume)

**功能**：射出的箭 / 弩箭未命中任何实体（插地方或落空）时，按 50% / 100% 概率在 1 秒后自动飞回背包（药箭同样返还）；命中实体不返还。**与无限互斥**（无限 = 不耗箭但禁药箭，归羽 = 省箭 + 保留药箭的经济取舍）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/homing_plume.json`：

* `max_level: 2`，`weight: 5`（uncommon），`anvil_cost: 4`
* `supported_items/primary_items: "#extra-enchantry:homing_plume_supported"`（自定义标签：`#minecraft:enchantable/bow` + `#minecraft:enchantable/crossbow`），`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:infinity"]`（列表写法）
* `effects: {}` 纯 Mixin

**核心逻辑** `mixin/ProjectileWeaponItemMixin.java` + `mixin/AbstractArrowMixin.java` + `HomingPlumeManager.java`：

* 发射快照：26.2 反编译确认弓与弩的箭矢都经 `ProjectileWeaponItem#createProjectile(Level, LivingEntity, ItemStack weapon, ItemStack projectile, boolean)` 生成（弩的重写仅处理烟花，箭矢走 super），RETURN 处把武器归羽等级写入箭实体（`HomingPlumeAccess` 接口）
* 返还判定：`AbstractArrow#onHitEntity` HEAD 置命中标记；`onHitBlock` RETURN 处——无命中标记、拾取态非 DISALLOWED、发射者为 ServerPlayer 时按概率登记延迟任务
* 延迟返还：`HomingPlumeManager` 走 `ServerTickEvents.END_SERVER_TICK`，20 tick 后箭仍存在（未被手动捡走）→ `getPickupItem()`（protected，@Invoker 透传）进背包（满了掉脚下）+ 拾取音 + `discard()`

**获取途径**：追加进 `minecraft:non_treasure`。

### 25. 坠星 (Starfall)

**功能**：装填烟花火箭发射时，爆炸伤害 +4、爆炸半径 +1 格，爆炸粒子升级为星形散射（末地烛 + 烟花混合）。**与多重射击互斥**（烟花流 vs 散弹流二选一）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/starfall.json`：

* `max_level: 1`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/crossbow"`，`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:multishot"]`
* 不加入任何获取标签 → 全途径隔离，仅末地城专属掉落池（与御风同方案）

**核心逻辑** `mixin/CrossbowItemMixin.java` + `mixin/FireworkRocketEntityMixin.java`（扩展既有类）：

* 发射快照：26.2 反编译确认弩发射烟花走 `CrossbowItem#createProjectile` 烟花分支（`new FireworkRocketEntity(level, 烟花栈, 射手, ...)`），RETURN 处给火箭打坠星标记（`StarfallAccess` 接口）——爆炸时射手可能已换武器，必须快照
* 爆炸增强：`dealExplosionDamage(ServerLevel)`（private）内三组常量 `@ModifyConstant` 同步放大——基础伤害 5.0f→9.0f（+4）、半径 5.0d→6.0d、距离平方阈值 25.0d→36.0d（衰减公式随半径同构缩放）
* 星形粒子：`explode` TAIL 按三正交轴 + 体对角线 14 束 `END_ROD` + 中心 `FIREWORK` 散射

**获取途径（专属掉落表）**：`chests/end_city_treasure.json` 第四池（rolls 1）：75% 空 / 15% 坠星附魔书 / 10% 带坠星的弩（`set_damage 0.8-1.0`）。

### 26. 霆霓 (Stormsurge)

**功能**：雨天 / 雷雨天 / 目标在水中时，掷出的三叉戟命中额外 +2 / +4 伤害，并连锁至 2 格内最近 1 个其他实体（连锁伤害减半）；命中点播放引雷同款雷声与电弧粒子（无真实闪电、不引燃）。仅投掷触发，近战戳刺不生效。**与引雷互斥**。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/stormsurge.json`：

* `max_level: 2`，`weight: 5`（uncommon），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/trident"`，`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:channeling"]`
* `effects: {}` 纯 Mixin

**核心逻辑** `StormsurgeManager.java`（挂 `LivingEntityMixin` hurtServer HEAD `@ModifyVariable`，定义在冲阵之后、断罪之前——加伤参与断罪斩杀结算）：

* 武器取 `source.getWeaponItem()`（掷出三叉戟返回三叉戟本身，同蚀命），`!source.isDirect()` 限定投掷
* 环境门：`level.isRaining()` 或目标 `isInWater()`（26.2 已无 `isInWaterRainOrBubble`，拆分判定）
* 连锁：主目标碰撞箱外扩 2 格取最近 1 个（排除攻击者 / 主目标 / 友方 / 死者），共用主目标 DamageSource + ThreadLocal 短路防递归（破阵同款模式）

**获取途径**：追加进 `minecraft:non_treasure`。

### 27. 藏锋 (Sheathed Edge)

**功能**：脱离战斗（未造成且未承受任何伤害）满 5 秒后，首次近战命中额外 +2 / 4 / 6 伤害，伴随拔刀音效与刀光粒子；触发后重新计时。与断罪不互斥——藏锋加伤参与断罪斩杀阈值结算（与冲阵同一设计逻辑），受壁垒上限克制。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/sheathed_edge.json`：

* `max_level: 3`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/sharp_weapon"`（原版标签 = 近战武器 + 斧，与锋利同适用范围），`slots: ["mainhand"]`
* `effects: {}` 纯 Mixin

**核心逻辑** `SheathedEdgeManager.java`（挂 `LivingEntityMixin` hurtServer）：

* 计时：每生物 UUID 记录最近参与战斗时间戳（wall-clock，与断罪冷却同风格）；hurtServer RETURN 伤害生效后受害者与攻击者双记账；无记录视为就绪（开局第一刀即拔刀斩）
* 加伤：HEAD `@ModifyVariable`（argsOnly），就绪则 +2/4/6 并立刻重新计时

**获取途径**：追加进 `minecraft:non_treasure`。

### 28. 渊息 (Tideheart)

**功能**：水下呼吸时间每级 +15 秒（I/II/III 级 → 总氧气 30/45/60 秒），III 级时水下挖掘不再减速（等效水下速掘）。**与水下呼吸、水下速掘互斥**。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/tideheart.json`：

* `max_level: 3`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/head_armor"`，`slots: ["head"]`
* `exclusive_set: ["minecraft:respiration", "minecraft:aqua_affinity"]`（列表写法）

**核心逻辑** `mixin/EntityMixin.java`（扩展）+ `mixin/LivingEntityMixin.java`（扩展）：

* 氧气上限：26.2 反编译确认 `Entity#getMaxAirSupply()` 硬编码返回 300（15 秒），且 `increaseAirSupply` 以它为钳制上限——HEAD 注入按头盔渊息等级返回 `300 + 300×level`，消耗 / 换气回满 / 客户端气泡 HUD 全部自动跟随
* III 级水下免减速：26.2 的水下挖掘惩罚是 `Player#getDestroySpeed` 里的 `Attributes.SUBMERGED_MINING_SPEED` 属性乘算（基础值 0.2），tick 内写入 +0.8 瞬态修改器即恢复 1.0（与活力 / 疾风同一模式，双端执行）

**获取途径**：追加进 `minecraft:treasure`（挡附魔台），另以 `LootTableEvents.MODIFY` 向沉船三类 / 埋藏的宝藏 / 海底废墟大小 / 钓鱼宝藏追加专属池（rolls 1：85% 空 / 15% I~III 级附魔书），庇护同款事件追加法，不进 `on_random_loot` 通用随机池。

### 29. 丰壤 (Loam)

**功能**：收获完全成熟的作物时 20% / 35% / 50% 概率双倍掉落（复制一份含时运等加成后的完整掉落，含种子）；III 级额外 3×3 范围收获——仅破坏同种且已成熟的作物，未成熟不动，每格消耗 1 点耐久，各格独立走双倍判定。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/loam.json`：

* `max_level: 3`，`weight: 5`（uncommon），`anvil_cost: 2`
* `supported_items/primary_items: "#minecraft:hoes"`，`slots: ["mainhand"]`
* `effects: {}` 纯 Mixin

**核心逻辑** `mixin/BlockMixin.java` + `LoamManager.java`：

* 注入点：26.2 反编译确认玩家破坏结算集中在 `Block#playerDestroy(Level, Player, BlockPos, BlockState, BlockEntity, ItemStack)`，RETURN 处判定 `CropBlock.isMaxAge(state)`（public final）+ 锄头丰壤等级
* 双倍掉落：`Block.getDrops(...)`（第六参已是 ItemInstance 新类型，ItemStack 直接实现之）重算一份掉落弹出
* 3×3 范围：邻格走 `ServerLevel#destroyBlock` 原版流程（各自触发双倍判定），ThreadLocal 深度标记防连锁扩散

**获取途径**：追加进 `minecraft:non_treasure`。

### 配饰八附魔 (Accessory Enchantments)（30~37）

1.4.0 新增：八家族各 1 个、按槽位 2/2/2/2 分布（耳环 / 项链 / 戒指 / 手镯各 2，与同族宝石绑定），全部 `max_level: 2`、非宝藏（`non_treasure` 全途径：附魔台 / 图书管理员 / 宝箱 / 钓鱼）。**计入八系共鸣**（阈值 3/5 → 4/7，计件扩展到配饰 4 槽，装备签名缓存同步含配饰）。`AccessoryManager` 统一结算：属性类 tick 值变化才写瞬态修改器（活力同模式）；受伤 / 造成伤害由 `LivingEntityMixin` hurtServer 侧调静态入口（victim 侧 ModifyVariable）；击杀类挂 `ServerLivingEntityEvents.AFTER_DEATH`；自然恢复用自有计时器（零注入原版回血分支）。

#### 30. 魂铃 Soul Chime（II，耳环，灵魂族）

击杀**敌对生物**（`Enemy` 接口，`AFTER_DEATH` 事件）回复 **2 饥饿 + 1 饱和 / 级**（`FoodData#eat(2×level, level)`）；多件佩戴取最高等级（不叠加）。

#### 31. 盾坠 Shield Pendant（II，耳环，守护族）

受到的**全部伤害 -3%/级**（hurtServer HEAD ModifyVariable，多件线性叠加；总减免统一钳制，至少保留 5% 伤害）。

#### 32. 雷鸣扣 Thunder Clasp（II，项链，风暴族）

**雷雨天气**（`isThundering`）造成的伤害 **+4%/级**（attacker 侧 hurtServer HEAD，多件线性叠加）。

#### 33. 翠滴 Verdant Drop（II，项链，自然族）

自然恢复 **+10%/级**——不注入原版回血分支：自有计时器把 80 tick 基线间隔缩短为 `80/(1+加成)`（下限 40 tick），`food≥18` 且未满血时每跳 heal 1 HP。

#### 34. 刃戒 Blade Ring（II，戒指，锋刃族）

攻击速度 **+5%/级**（`ATTACK_SPEED` 瞬态修改器 `ADD_MULTIPLIED_BASE`，值变化才写）。

#### 35. 羽环 Plume Ring（II，戒指，风族）

受到的**弹射物伤害**（`#minecraft:is_projectile`）**-6%/级**（多件线性叠加）。

#### 36. 烬镯 Ember Bracelet（II，手镯，火焰族）

受到的**火焰伤害**（`#minecraft:is_fire`）**-10%/级**（多件线性叠加）。

#### 37. 潮镯 Tide Bracelet（II，手镯，水渊族）

游泳效率 **+8%/级**（`WATER_MOVEMENT_EFFICIENCY` 瞬态修改器）。

### 狼铠三附魔 (Wolf Armor Enchantments)（38~40）

1.4.0 新增。**复用**：活力 / 壁垒对狼铠开放（纯标签变更 `vitality_supported` / `bulwark_supported` += wolf_armor——BODY 槽 `isArmor()` 既有求和路径零代码）。**新增**锐牙 / 哨戒 / 回春由 `WolfArmorManager` 结算（挂 `LivingEntityMixin` tick HEAD 的 `instanceof Wolf` 分支，活力同款瞬态属性修改器；卸甲自动移除全部修改器）。狼铠无 `ENCHANTABLE` 组件（同马铠），**铁砧上书是生存唯一获取途径**（附魔台必然不可用）；**不计玩家共鸣**（计件只扫玩家自身槽位）。首枚狼铠附魔成就「獠牙礼赞」（`AnvilMenuMixin` TAIL 检测授予）。

#### 38. 锐牙 Sharp Fang（III，狼铠）

狼近战伤害 **+10%/级**（`ATTACK_DAMAGE` 瞬态修改器）。

#### 39. 哨戒 Vigil（II，狼铠）

狼索敌 / 跟随范围 **+25%/级**（`FOLLOW_RANGE` 瞬态修改器）。

#### 40. 回春 Renewal（II，狼铠）

狼每 **4 秒**（80 tick，服务器全局 gameTime 节拍，无需每狼计时器）回复 **1 HP/级**，仅存活且未满血时生效。

## 版本主题

### 1.0.0「诸界浩劫」 (Cataclysm of Realms)

模仿原版骷髅马陷阱的骑兵事件，同时接管原版骷髅马陷阱。**破限附魔书在完成「诸界浩劫」挑战前无法使用**（铁砧应用被门禁拦截）。

#### 诸界浩劫（破限书守护挑战，四波）

获得破限附魔书即开启挑战：红色 boss 血条「**诸界浩劫 / Cataclysm of Realms**」（按波推进，每波 +25%）。

* **第 1 波**：精英骑兵队（5 名僵尸马骑兵 + 后方 4 名强化骷髅骑士）+ **5 只从地底钻出的监守者**（`EntitySpawnReason.TRIGGERED` 生成即 EMERGING 钻出动画，与尖啸体召唤一致；生成时拉满对发起者的愤怒并每 10 秒补怒 —— 监守者原生 "已有玩家目标时不切换" 即恒定锁定）。
* **第 2 波**：与游戏难度挂钩的原版袭击**全部 n 波一次性生成**（易 3 / 普通 5 / 困难 7 波），数量与构成按 26.2 反编译 `Raid.RaiderType` 表 + 难度加成，劫掠兽骑手沿用原版规则（第 5 波掠夺者、第 7 波起唤魔者 / 卫道士）——**不是真实袭击**：无袭击进度条、无村庄之主。
* **第 3 波**：僵尸猪灵 / 僵尸疣猪兽 / 猪灵蛮兵 / 岩浆怪部队 + **3 只凋灵**（凋灵保留原版血条与横扫一切的非亡灵仇恨，浩劫感）。
* **第 4 波**：**20 末影人 + 50 末影螨**（该挑战下**无仇恨**，MobMixin 强制空目标）+ **2 只末影龙**（26.2 代码生成的龙没有 DragonFight，**天然没有 boss 血条**）。
* **每波**额外 5 只幻翼 + 5 只恼鬼（侧翼，不计入清波判定）。
* **全程**每 10 秒在玩家四周生成 **10 只闪电苦力怕**：闪电充能（`DATA_IS_POWERED` 访问器）、**引信 30 → 8 tick（减少 3/4）**（`maxSwell` 访问器，26.2 反编译：引信字段已更名 maxSwell，NBT 键仍为 "Fuse"）、**2 倍速**（MOVEMENT_SPEED +100% ADD_MULTIPLIED_TOTAL）。

**限制与结算**：总时限 = 原版袭击超时 ×1.5（反编译 `Raid.RAID_TIMEOUT_TICKS` 48000 → **72000 tick = 60 分钟**）；**玩家死亡或超时即挑战失败**—— 全部挑战生物消散、**背包中的破限附魔书化为灰烬**（仅附魔书，不动已附魔装备）。死亡结算挂 `ServerPlayer#die` HEAD（**背包掉落之前**就地销毁；tick 级检查只作兜底，两路幂等）；四波全部清空即成功 —— 授予「**无敌**」进度并解锁破限。状态为运行时跟踪（重启丢失，失败可再刷书重试）。波间间隔 5 秒；玩家离线时挑战冻结。

**触发与去重**：Player#addItem 与 Inventory#add 双钩子都会对同一本书触发（/give 直调 Inventory#add 不经 addItem，反编译 GiveCommand 确认）—— 按**同一游戏刻去重**，无时间墙：重新拾取掉落的破限书会**立即重开新挑战**。

**仇恨锁定**：所有挑战生物经 `MobMixin` 拦截 `Mob#setTarget`—— 锁定成员强制以挑战发起者为唯一目标（**怪物间误伤不改变仇恨**，HurtByTargetGoal 的反咬也走 setTarget 故一并覆盖）；无仇恨成员强制空目标。**掉落经验翻倍**：`LivingEntity#getExperienceReward`（26.2 public final 统一经验入口）RETURN ×2。

#### 破限书的锁定视觉

破限附魔名使用专用字体 `extra-enchantry:fancy_lb`（拓阶用 `fancy`，服务端 EnchantmentMixin 区分）。客户端 `FontPreparedTextBuilderMixin` 渲染时：**本地玩家未完成「无敌」进度 → 破限名暗灰平色（无金色闪光）**；完成后恢复金色波浪闪光。本地判定经 `ClientAdvancementsAccessor` 读同步进度表（`LimitBreakLockState`），无需额外网络包。

#### 陷阱混编骑兵队（雷雨陷阱召唤，弱一档）

骷髅马 / 僵尸马 50/50 混编 + 同类骑手；钻石全套**保护 II**（无活力）、铁长矛 / 普通弓（无附魔）、**铁马铠**（防僵尸马日光燃烧）、**自然随机移速**。陷阱马本体承载第一名骷髅骑手（原版行为），其余 3 骑散布生成。击杀整支（4 名骑手，跨多支累计）授予「**全歼**」进度。和平难度不消费陷阱。

#### 掉落

两档骑手装备掉落率全部为 0（纯挑战）；**马铠随马死亡照常掉落**（原版行为）。精英队坐骑在挑战结束后保留（骑手死后可收编为坐骑）。

#### 出现机制

* **破限成就触发（诸界浩劫）**：玩家拾取破限附魔书授予「极限之证 / 雷霆之礼」进度后回调 → 开启挑战。每玩家 60 秒刷新保护；破限已解锁后不再召唤；和平难度跳过。
* **原版陷阱挂钩（混编队）**：`SkeletonTrapGoalMixin` 在原版 `SkeletonTrapGoal#tick` **HEAD 取消原版生成并完全接管**—— 复刻原版的状态复位（`setTrap(false)` 移除触发 Goal 防重复）与视觉闪电（`setVisualOnly(true)`）。

#### 关键实现事实（26.2 反编译验证）

* **骑手游控**：26.2 新增乘客控车系统 ——`Mob#getControllingPassenger` 允许首个 `canControlVehicle()` 的 Mob 乘客控车（Zombie/Skeleton 默认可），`Mob#getMoveControl/getNavigation` 在控车时**委托给坐骑的**；`ZombieHorse#isMobControlled()`（首乘客是 Mob）同步放行恐慌抑制。因此骑手 AI（僵尸的 `SpearUseGoal`、骷髅的 `RangedBowAttackGoal`）直接驱动马的导航，零额外代码。
* **监守者钻出**：`Warden#finalizeSpawn` 在 `EntitySpawnReason.TRIGGERED` 时设置 `Pose.EMERGING` + IS_EMERGING 记忆 + 播放 WARDEN_AGITATED—— 生成即完整钻出动画。监守者愤怒会衰减，每 10 秒 `increaseAngerAt` 补回 ANGRY 档。
* **苦力怕引信**：26.2 引信字段为私有 `maxSwell`（NBT 键 "Fuse"），充能位为私有静态 `DATA_IS_POWERED`，均无公开 setter → `CreeperAccessor`（实例 + 静态 @Accessor）。引信只在 SwellGoal 触发后从 maxSwell 递减，预设在生成时即可。
* **末影龙血条**：26.2 龙血条移入 `EnderDragonFight#dragonEvent`—— 代码 / 刷怪蛋生成的龙无 fight 实例，**天然无 boss 血条**（"挑战龙无血条" 零成本满足）。
* **袭击构成**：`Raid.RaiderType` 每波基础表（26.2 重平衡：卫 {0,0,2,0,1,4,2,5} / 唤 {0,0,0,0,0,1,1,2} / 掠 {0,4,3,3,4,4,4,2} / 女巫 {0,0,0,0,3,0,0,1} / 劫 {0,0,0,1,0,1,0,2}）+ 难度加成（人形：易 +0\~1 / 普 +1 / 难 +2；女巫：非 1/2/4 波非简单 +1）。`RAID_TIMEOUT_TICKS = 48000`。
* **铁砧上马铠**：马铠虽无 `ENCHANTABLE` 组件（不进附魔台），但所有物品默认带 `ENCHANTMENTS` 组件（`COMMON_ITEM_COMPONENTS`），铁砧上书路径只查 `canEnchant`（= supported_items）→ 仅改 JSON 即通。
* **马铠掉落**：`AbstractHorse#dropEquipment` 掉落整个马背包（含鞍与 BODY 槽马铠）。
* **26.2 API 改名**：`displayClientMessage` → `sendSystemMessage`（聊天）/ `sendOverlayMessage`（动作栏）；`playNotifySound` 不存在，改用 `level.playSound(null, ...)`；`ServerPlayer#serverLevel()` 不存在，用 `(ServerLevel) player.level()`；`Raider` 在 `world.entity.raid` 包（Pillager/Vindicator 在 `monster.illager`）。

### 1.2.0「共鸣与臻藏」 (Resonance & Collector)

系统深化包：不新增附魔，深化三个系统。

#### 家族共鸣 (Family Resonance)

- **计件规则（§1 修正）**：装备四件 + 主副手上的附魔按**其等级**计入所属家族（`#extra-enchantry:family_*` 八系标签；霆霓双归水/风暴、余烬双归火/灵魂）。
- **档位**：PARTIAL（家族累计 ≥3 级）/ FULL（≥5 级），多系可同时激活；升档聊天提示 + 钟声，降档静默；状态栏 RESONANCE 效果滚动续期（amplifier = 激活家族数 - 1）。**1.4.0 起阈值调整为 4/7**（配饰附魔计入计件，每家族至多 +2 级来源——见 1.4.0 节；阈值经 `resonance/families/*.json` 数据驱动可调）。
- **小加成（PARTIAL）**：多等级附魔按 +1 级结算（`FamilyResonanceManager.effectiveLevel`，已接入活力/壁垒/蚀命/汲取结算点，数值由各表钳制在设计上限内）；单级附魔由各 Manager 就地 +20%（后续版本接入）。
- **完整共鸣（FULL）八系被动**：灵魂·猎魂（击杀后 3 秒隐身）/ 风暴·天象感应（雨天移速 +20%）/ 锋刃·连击（3 秒连击最高 +15%）/ 守护·岿然（击退抗性 +50%）/ 自然·扎根（静止 3 秒后 1 HP/s）/ 水·潮汐亲和（水下移速 +30%、耗氧减半）/ 风·轻盈（摔落 -50%）/ 火焰·炽热之躯（免疫火焰熔岩）。

#### 臻藏奖励 (Collector Rewards)

完成「收藏家 · 全家福」成就（集齐八系家族附魔书）即获臻藏资格（成就即服务端持久化标记，客户端经 `ClientAdvancements` 直接查询，无需网络包）：

1. **臻藏字体**：本模附魔书名称以专属金渐变渲染（`FontPreparedTextBuilderMixin` 按本地臻藏标记覆盖族色）；
2. **臻藏光晕**：本模附魔书恒定显示附魔光效（客户端 `ItemStackFoilMixin` 改写 `hasFoil`；自定义颜色 glint 渲染层留作后续增强）。

#### 隐秘挑战扩充（6 个，hidden_challenges/）

| 挑战 | 条件 |
|------|------|
| 死而不僵 | 劫后余辉锁血期间反杀攻击者 |
| 百步穿杨 | 归羽箭命中距发射点 ≥40 格的目标 |
| 深渊回响 | 头戴渊息潜入水中讨伐远古守卫者 |
| 以彼之道 | 冲阵撞击放倒劫掠兽 |
| 劫火余生 | 骑余烬金马铠在熔岩上累计行进 50 格 |
| 丰收之神的赞许 | 丰壤连锁收获累计 64 株作物（3 秒窗口计数） |

### 1.3.0「铭刻与试炼」 (Inscription & Trials)

系统可见化与主动构筑版本：不新增普通附魔，把八系共鸣做成可理解、可选择、可挑战的完整闭环。

#### 共鸣秘典 (Resonance Codex)

- **配方**：紫水晶碎片 + 书 + 青金石（`recipe/resonance_codex.json`，紫晶上下夹持书与青金石）。
- **右键**：输出「总览」页（八系累计等级与档位）；**潜行右键**：循环切换 总览 → 家族详情 → 试炼 三页（页码运行时按玩家记忆）。纯聊天组件输出，无自定义 GUI；高频查询不播音效。
- **家族详情页**：每系显示 累计等级 / 距下一档差值 / 主调状态，FULL 家族附带可点击「铭刻主调」按钮（走 `/extraenchantry attune`）。
- **命令回退**：`/extraenchantry resonance [player]`（普通玩家查自己，权限等级 2 可查他人）。
- **口径约束（P0）**：全部数值来自 `FamilyResonanceManager` 的快照（`familyTotal` / `tierOf`），秘典与命令无第二套算法。

#### 主调铭刻 (Primary Attunement)

- 仅 FULL 家族可铭刻；同一玩家唯一主调；5 分钟切换冷却（6000 tick，**服务器游戏时间**而非系统时间）。
- 持久化：Fabric Data Attachment（`copyOnDeath` 原生覆盖死亡重生与跨维度；旧存档首次加载默认未铭刻）。
- 失去 FULL 时主调选择保留、收益暂停（聊天提示一次），恢复 FULL 自动续接；不重复播放铭刻反馈。
- **主调收益只强化 FULL 被动，不提高附魔有效等级**（避免与 PARTIAL「+1 级」乘法膨胀）：

| 家族 | 1.2.0 FULL | 主调强化 |
|------|-----------|---------|
| 灵魂 | 击杀后隐身 3 秒 | 隐身 5 秒 |
| 风暴 | 雨天移速 +20% | +25%，雷击伤害 -25% |
| 锋刃 | 3 秒连击最高 +15% | 上限 +20%（4 层） |
| 守护 | 击退抗性 +50% | +70% |
| 自然 | 静止 3 秒后 1 HP/s | 1.5 HP/s，受伤后 2 秒暂停 |
| 水 | 水下移速 +30%、耗氧减半 | +40%、氧气消耗降至 40% |
| 风 | 摔落 -50% | -65% |
| 火焰 | 免疫火焰熔岩 | 熔岩中移速 +15% |

#### 八系共鸣试炼 (Family Trials)

独立进度树 `advancement/family_trials/`（根节点首次 FULL 时显示）；计数前提 = 对应家族 FULL **且** 主调；失去 FULL 暂停（连续窗口中断即重置并提示一次，已完成进度永久保留）；全部计时用服务器游戏时间。

| 试炼 | 完成条件 |
|------|---------|
| 灵魂 · 不息 | 单次隐身期间连续击杀 5 个敌对生物 |
| 风暴 · 逐雷 | 雷暴中 120 秒内移动 600 格且不乘坐载具 |
| 锋刃 · 百炼 | 连击窗口内增伤叠至上限并击败生命值 ≥100 的目标 |
| 守护 · 不动 | 30 秒内承受 80 点原始伤害、位移 ≤8 格并存活 |
| 自然 · 复苏 | 单次站定累计回复 30 HP 且不主动攻击 |
| 水 · 深潜 | 不换气连续潜水 180 秒并击败远古守卫者 |
| 风 · 无坠 | 从 ≥80 格高度落地存活，未用鞘翅/缓降 |
| 火焰 · 浴火 | 熔岩中连续停留 60 秒并移动 ≥100 格 |

#### 家族铭印与大共鸣者 (Family Sigils & Grand Resonator)

- 首次完成试炼授予对应铭印：一个物品类型 + `family_id` 数据组件（非法值降级为「失效铭印」不崩溃）；背包满掉落脚下；重复完成不重复发放。
- **铭印贴图（九型令牌）**：v1.4.0 补完——八族各一枚圆形令牌贴图（家族色基面 + 左上高光弧 / 右下暗部弧 + 中央族徽：魂焰 / 闪电 / 短剑 / 柳叶 / 盾徽 / 三道流线 / 火苗 / 水滴），经 `items/family_sigil.json` 的 **component select**（`minecraft:select` + `property: minecraft:component`，键 `family_id`）按组件值分发模型，`fallback` 为灰底裂纹「风化」贴图（与失效铭印语义对齐）——单物品多形态**纯数据零代码**。
- 集齐八印（八试炼进度全 done）授予「大共鸣者」进度：本模附魔书与共鸣秘典恒显光效（客户端 `GrandResonatorState` 查进度，视觉可降级），进入世界 5 秒后一次称号提示；无数值增益。

#### 数据化规则与诊断 (Data-driven Rules & Debug)

- `data/extra-enchantry/resonance/families/<family>.json`：每家族一份，字段级 `optionalFieldOf` 缺省内置默认；单文件解析失败仅该家族回退默认并记日志；`/reload` 后 1 秒内全量重算（重载清空装备签名缓存）。
- **装备签名缓存**：六槽（物品+附魔组件）+ 维度签名，未变且未到 5 秒兜底间隔时跳过附魔遍历，沿用档位/累计快照；装备变化下一秒立即全量。
- `/extraenchantry debug resonance <player>`（权限等级 2）：八系等级/档位/主调/冷却/缓存新鲜度/试炼运行时计数一次输出。

#### 1.3.0 新增文件索引

| 类型 | 路径 |
|------|------|
| 物品/组件 | `ResonanceCodexItem`、`FamilySigils`（`family_sigil` 物品 + `family_id` 组件） |
| 管理器 | `AttunementManager`（Attachment 持久化）、`FamilyTrialsManager`（八试炼计数）、`ResonanceConfig`（数据化规则 + RulesLoader） |
| 命令 | `ExtraEnchantryCommands`（resonance / attune / debug resonance） |
| Mixin 变更 | `LivingEntityMixin`：新增试炼承伤回调（hurtServer HEAD 最前）、风轻盈走配置、新增风暴主调雷击减伤 |
| 客户端 | `GrandResonatorState`、`ItemStackFoilMixin` 扩展、`ExtraEnchantryClient` 称号提示 |
| 进度 | `family_trials/` root + 8 试炼 + grand_resonator |
| 配方 | `recipe/resonance_codex.json` |
| 规则 | `resonance/families/` 8 个家族 JSON |
| 模型 | `items/resonance_codex.json`、`items/family_sigil.json`（v1.4.0 补完：component select 分发九型专属令牌贴图，替换原版 echo_shard 降级） |

#### 1.3.1「铭文纪元」 (Era of Inscription)

> 完整设计稿：[`DESIGN/1.3.1-design.md`](DESIGN/1.3.1-design.md)（~350 行，含世界观 / 物品表 / 数据 schema / 风险与权衡）

叙事与新手指引版本：不新增附魔 / GUI / NPC，把散落的命名（家族 / 浩劫 / 破限 / 铭刻 / 臻藏）补成一条连贯叙事——初民纪元八家族共举「极限之器」封印浩劫，铭文碎散为附魔残响，玩家是被碎片选中的新一代共鸣者。

##### 世界观「铭文纪元」编年

- 八家族首次获得叙事身份：灵魂「生者暂居，死者长眠」/ 风暴「天不仁而怒」/ 锋刃「一刃既出，万念归尘」/ 自然「根深者不惧风」/ 水渊「行于无形」/ 风「无翅亦可凌云」/ 守护「盾碎之时，魂已无伤」/ 火焰「烬中余温，未灭之心」。
- 浩劫四幕对应四波挑战：深渊苏醒（第 1 波）→ 风暴失序（第 2 波）→ 火焰焚烧（第 3 波）→ 魂灵迷航（第 4 波）；4 张破限残页拼合「极限之器」铭刻图样——揭示破限书需先历浩劫方能启用的叙事原因。
- 「臻藏」补完叙事身份：被铭文承认的标志（金渐变字体 + 光晕即铭文回响重亮，1.2.0 既有视觉）。

##### lore 物品（4 类 14 件，均不进创造栏、不可合成）

| 物品 | 件数 | 获取 | 实现 |
|------|------|------|------|
| 来者手札 | 1 | 首次进入世界自动入包 | `WelcomeLetterItem`：动态 lore 容器——右键按玩家进度翻页（1 总纲 / 2 已解锁家族铭文 / 3 极限之器图样 / 4 终章），页码状态存玩家侧，手札丢弃不影响解锁 |
| 家族铭文 | 8 | 各家族首次 FULL 自动派发 | `FamilyInscriptionItem` + `family_id` 组件（复用铭印组件）：右键研读该家族证词，名称按族着色 |
| 破限残页 | 4 | 浩劫每波完成派发（失败不回收） | `LimitBreakShardItem` + `shard_id` 组件：右键研读该幕叙事，名称按幕着色（深岩/雷光/烬红/末影紫） |
| 编年史卷轴 | 1 | 达成大共鸣者派发 | `ChronicleScrollItem`：完整编年右键展开（全文硬编码 lang，不做数据驱动） |

##### 被动引导（仅首次，不卡进度）

- 一次性触发持久化：`LoreTriggerManager`（Fabric Attachment `lore_triggers`，`copyOnDeath`），`fireOnce` 幂等语义；共鸣扫描每秒重检不会重复触发。
- 8 个触发点：首次进游戏（手札）/ 首次拾取本模附魔书（秒级背包扫描，触发后永不再扫）/ 首次合成秘典（`onCraftedBy`）/ 任意家族首次 PARTIAL / 家族首次 FULL（铭文）/ 浩劫每波（残页 + 幕格言）/ 浩劫全清（图样收录）/ 大共鸣者（卷轴）。
- 静默规则：`lore/onboarding.json` 的 `silence_onboarding: true` 关闭全部 chat 提示（物品照发）；不弹窗、不发包、不写客户端日志。
- **升档追发**：1.3.0 玩家登录后按已有进度补发（「无敌」→ 四残页、grand_resonator → 卷轴、家族试炼完成 → 该族铭文——试炼完成必然达成过 FULL），一次性汇总提示，幂等安全。

##### onboarding/ 引导成就树（新增第 6 棵树）

| 成就 | 触发 |
|------|------|
| 铭文纪元·序章（root） | `minecraft:tick`（数据驱动） |
| 浩劫之门的钥匙 | `inventory_changed` + `stored_enchantments` 谓词（数据驱动，与隐藏「极限之证」并存——可见引导 + 隐藏彩蛋） |
| 初见共鸣 | 首次右键秘典（impossible + 代码授予） |
| 第一抹铭刻 | 首次铭刻主调（impossible + 代码授予，与首次铭刻格言同点位） |

##### 数据驱动 lore（`data/extra-enchantry/lore/`）

`onboarding.json`（静默开关 + 手札第 1/3/4 页）、`family_inscriptions/` ×8、`limit_break_shards/` ×4、`cataclysm/` ×4（波次完成格言）、`attunement/first_attune.json`（八族首次铭刻格言）。`LoreLoader` 单监听器整目录读原始 JSON（`ExtraCodecs.JSON`）按路径分派手工解析，单文件失败仅该条目回退内置默认（中文文案硬编码于 `LoreLoader`，数据包即完整可编辑副本）。

##### 既有系统叙事绑定（零机制改动）

- 浩劫波次完成追加幕标题 + 格言（数据驱动 `cataclysm/act_*.json`）+ 残页派发（`CavalryManager` 清波 / 成功结算两处钩子——第 4 波走 `succeedCataclysm` 分支）。
- 八试炼成就改授「修行名号」：灵魂·不息 → **魂行者的顿悟**（风暴→驭雷者的觉醒 / 锋刃→刃宗师的圆熟 / 守护→盾圣者的岿然 / 自然→大地之子的回春 / 水→渊息者的潜入 / 风→凌虚者的着陆 / 火焰→烬心者的浴炼），描述保留原试炼名——纯 lang 改动。
- 首次铭刻追加家族格言（两行，家族色渲染；此后铭刻不再重复）。
- 主类零新 Mixin：登录走 `ServerPlayConnectionEvents.JOIN`，合成走 `Item#onCraftedBy` 覆写，家族 / 波次 / 铭刻 / 大共鸣者全部为既有管理器的回调解用。

##### 1.3.1 新增文件索引

| 类型 | 路径 |
|------|------|
| 物品/组件 | `WelcomeLetterItem`、`FamilyInscriptionItem`（复用 `family_id`）、`LimitBreakShardItem`（新增 `shard_id` 组件）、`ChronicleScrollItem` |
| 管理器 | `OnboardingManager`（登录追发 / 秒级拾书检测 / 8 触发点钩子）、`LoreTriggerManager`（一次性触发 Attachment）、`LoreLoader`（lore 数据 + Loader） |
| 钩子变更 | `FamilyResonanceManager`（PARTIAL/FULL 升档处）、`CavalryManager`（清波 / 成功结算）、`FamilySigils`（grand award 返回值）、`AttunementManager`（铭刻成功）、`ResonanceCodexItem`（use / onCraftedBy） |
| 进度 | `onboarding/` root + 3 节点 |
| 数据 | `lore/` 18 个 JSON；`tags/enchantment/families.json`（八系总标签，拾书检测用） |
| 资源 | 贴图 4 张（信封 / 盾形纹章 / 残页 / 卷轴，16×16 脚本生成）+ `items/` / `models/item/` 各 4 |
| 语言 | zh_cn / en_us 各 +66 条（物品 / 提示 / 手札 / 编年史 / 引导成就 / 试炼名号改写） |

#### 1.3.2 修复补丁 (Hotfix)

纯修复版本，无新附魔 / 新系统：

* **渊息世界创建 NPE 修复**（自 1.1.0 起带雷）：`Entity#<init>` 的 defineSyncker 在定义 `DATA_AIR_SUPPLY_ID` 初值时就回调 `getMaxAirSupply()`——此时 `LivingEntity#equipment` 字段尚未初始化（子类构造体才赋值），Mixin 里 `instanceof LivingEntity` 已为真但 `getItemBySlot` 必 NPE，实体构造直接失败（新世界 / 登录报 "Couldn't place player in world"，服务器停止）。`EntityMixin#tideheartMaxAir` 加 try-catch 防护：构造早期视为无渊息，走原版上限 300
* **共鸣状态效果图标修复**（自 1.2.0 起缺失）：26.2 状态效果图标走 GUI 图集而非独立贴图——`Hud#getMobEffectSprite` 把效果 ID 前缀 `mob_effect/` 后交给 `blitSprite`，sprite 必须注册进 `textures/atlas/gui.png` 图集。RESONANCE 图标因未注册一直走 missing sprite fallback（日志 "Using missing texture"），新增 `assets/extra-enchantry/atlases/gui.json`（directory source：`mob_effect` → 前缀 `mob_effect/`，与原版 gui.json 同款）后生效
* **resonance.png 重生成**：旧生成器每扫描行多写一个字节（System.Drawing 能读但 STB 报 `Corrupt PNG`，atlas stitch 阶段 IOException，sprite 永久 missing），新文件通过 `高 × (1 + 宽×4)` IDAT 长度校验

开发客户端实测：世界正常创建、玩家正常落位、零贴图报错、退出存档干净。

### 1.4.0「环佩与獠牙」 (Trinkets & Fangs)

装备面扩展版本：配饰栏系统 + 配饰附魔与宝石合成 + 狼铠附魔 + 火焰家族防火设定。完整设计稿见 [`DESIGN/1.4.0-design.md`](DESIGN/1.4.0-design.md)。

#### 配饰栏（耳环 / 项链 / 戒指 / 手镯，集成原版背包）

* **槽位接入**：`InventoryMenuMixin` 构造 TAIL 追加 4 个 `AccessorySlot`（下标 46~49，副手 45 之后）——生存背包直接可用；创造玩家页签因原版 selectTab 会把 ItemPickerMenu 槽位列表整体替换为 InventoryMenu 槽位的包装（点击按 containerId 0 路由回服务端 InventoryMenu），同样直接可用。**独立 GUI / 按键 / 网络包方案已废弃**
* **生存背包**：配饰按钮在盾牌列（x=77）头盔行（y=8）——与盾牌同列、与头盔平齐、同尺寸；点击后 4 配饰槽在护甲列左侧（x=-11）平滑滑入（与护甲同排同尺寸）
* **创造玩家页签**：盾牌栏下移与胸甲平齐（(35,20)→(35,33)）、配饰按钮在头盔左侧头盔行（(35,6)）；展开后配饰 2×2 出现在装备栏左侧（(16/35, 6/33)），按钮与盾牌列让位滑到面板左缘外（x=-3），再点收回。26.2 创造玩家页签护甲为 2×2 环绕人物（纸娃娃）布局
* **动画与门控**：`isActive()` 为 26.2 纯客户端概念（AbstractContainerMenu / Slot 内部零引用，反编译确认）——折叠时槽位不渲染、不可悬停点击；滑入动画由 Screen 自绘（槽位底纹提取自原版像素 + 幽灵图标 + 动画帧物品 `extractor.item`）
* **存储**：服务端权威 Fabric Data Attachment `accessories`（ItemStack[4]，**非** copyOnDeath）；Attachment 快照语义——所有写路径收敛到 `AccessoryContainer.setItem → setAttached`
* **死亡**：随背包掉落（`dropEquipment` HEAD 同点位）；keepInventory / keepEverything / 旁观者经 `restoreFrom` TAIL 显式搬运；带誓约配饰暂存回插（誓约扩展到 4 槽配饰）
* **右键快捷穿戴**：占用则交换回主手；shift 智能移动（`quickMoveStack` HEAD 拦截：配饰物品进匹配空槽 / 配饰槽物品进背包）

#### 物品与合成（24 新物品，两段式美术产线脚本生成贴图）

* **8 家族宝石**：家族材料 + 固定盔甲纹饰模板（幽寂/闪电/恼鬼/野性/守卫/涡流/猪灵/潮汐一一配对，模板可复制再生）；首次合成播家族格言（静默开关沿用）
* **16 配饰**（4 槽 × 铜/铁/金/钻四材质）：材质 ×N + 槽位绑定宝石合成，**宝石被动按材质传导**（铜 100% / 铁 125% / 金 150% / 钻 200%）；宝石合成时决定、不可事后更换（配方 result components 写 `socketed_gem`）；耳坠贴图成对
* **宝石被动**（8 条微缩被动，全身至多 4 条）：魂珀击杀 +5 经验 / 雷光石雷雨 +3% 移速 / 刃晶 +2% 攻速 / 萌芽晶 +5% 自然恢复 / 盾纹玉 -2% 受伤 / 风羽晶 -3% 弹射物伤 / 烬心石 -4% 火伤 / 潮汐珠 +5% 游泳效率

#### 配饰附魔（编号 30~37，8 个，计入八系共鸣）

* **8 个附魔**：魂铃 / 盾坠 / 雷鸣扣 / 翠滴 / 刃戒 / 羽环 / 烬镯 / 潮镯（每家族 1 个，槽位 2/2/2/2 分布，与宝石同族绑定）
* **统一结算**：`AccessoryManager` 静态入口集中处理——属性类 tick 值变化才写瞬态修改器、受伤 / 造成伤害走 `hurtServer` ModifyVariable、击杀走 `AFTER_DEATH`、自然恢复用自有计时器，**零注入原版回血分支**
* **共鸣阈值 3/5 → 4/7**：计件扩展到配饰 4 槽，装备签名缓存同步含配饰

#### 狼铠附魔（编号 38~40）

* **复用**：活力 / 壁垒对狼铠开放（`vitality_supported` / `bulwark_supported` += wolf_armor，BODY 槽 `isArmor()` 既有路径零代码）
* **新增**：锐牙（近战伤害 +10%/级）/ 哨戒（索敌跟随 +25%/级）/ 回春（每 4 秒回 1 HP/级）——`WolfArmorManager` 挂 `LivingEntityMixin` tick 的 Wolf 分支；铁砧上书获取（狼铠无 ENCHANTABLE 组件同马铠）；不计玩家共鸣
* **成就**：「环佩琳琅」（首次穿戴）、「獠牙礼赞」（AnvilMenuMixin TAIL 检测首次狼铠附魔）

#### 火焰家族防火（新设定「烬火不侵」）

* **触发条件**（任一即生效）：携带火焰家族附魔（`#extra-enchantry:family_fire`：炽焰行者 / 余烬 / 烬镯 / 劫后余辉，**含附魔书**）或镶嵌烬心石
* **效果**：掉落物**免疫火 / 岩浆烧毁**（`ItemEntityMixin` `hurtServer` HEAD 取消，与下界合金同款行为）
* **动态判定**：即时生效，砂轮磨掉附魔即失效；既有旧装备**零迁移受益**（首次进入 1.4.0 玩家不会自动获得该效果）

### 1.4.1 修复与观感打磨 (Polish)

渲染修复 + 铭印专属贴图补完 + 文案精简，无玩法数值变化。

#### 视觉修复

* **幽灵图标 sprite id 修正**（配饰空槽紫黑块）：26.2 GUI 图集 directory source（`source: gui/sprites` + `prefix: ""`）按「前缀 + 文件相对路径」生成 sprite id——裸文件名 `ghost_<slot>`（原版同例 `hud/heart/full`），原代码误引 `gui/ghost_<slot>` 导致 blitSprite 落空；`AccessorySlot#getNoItemIcon` 与 `AccessoryColumnRenderer#GHOSTS` 同步修正，回归测试新增 prefix 空串断言
* **生存背包配饰按钮**：移除 panel_fill 衬底——原版纸娃娃渲染区右缘 x=75（`extractEntityInInventory` 参数 26~75），衬底左缘 <76 会压进玩家模型 1~2px，且纯色与面板纹理存在色阶差、四面显色缝；按钮贴图 18×18 自带不透明槽框底，直接绘制即可
* **创造模式物品栏**：固定条衬底（x33~52）常驻遮盖烤入 `tab_inventory.png` 的旧副手框残影——盾牌栏下移后项链/手镯槽框盖不到 y23~31 中段，残影恰落在两格之间；滑出列衬底独立绘制、折叠时与固定条重合跳过

#### 铭印专属贴图（九型令牌）

* 八族各一枚圆形令牌贴图（家族色基面 + 中央族徽）+ 灰底裂纹「风化」失效兜底（共 9 型），`items/family_sigil.json` 经 **component select**（`minecraft:select` + `property: minecraft:component`，键 `extra-enchantry:family_id`）按组件值分发模型，`fallback` 对齐失效铭印语义——单物品多形态**纯数据零代码**，替换 v1.4.0 的原版 echo_shard 降级

#### 文案

* 八种家族宝石物品介绍精简：删除「残响」后的合成途径括注（获取途径仍见配方图鉴与合成书），其余文字不变

## 通用技术模式

### 项目结构

```
src/main/resources/

├── data/extra-enchantry/enchantment/\*.json     附魔定义

├── data/extra-enchantry/tags/item/\*.json        自定义物品标签（可附魔物品；1.4.0 增 accessory/ 四槽位准入 + wolf_armor_supported）

├── data/extra-enchantry/tags/enchantment/family_\*.json  八家族附魔标签（字体/成就共用）

├── data/extra-enchantry/advancement/...          成就树（collector 图鉴 / usage 实战 / hidden_challenges 隐秘挑战 / onboarding 引导 / family_trials 试炼）

├── data/extra-enchantry/recipe/...               配方（共鸣秘典 / 1.4.0 八宝石 + 32 配饰材质×宝石变体，result components 写 socketed_gem）

├── data/extra-enchantry/lore/...                 1.3.1 叙事文本（手札页 / 家族铭文 / 破限残页 / 浩劫四幕 / 首刻格言）

├── data/minecraft/tags/enchantment/\*.json       追加原版标签（互斥组/宝藏性/交易）

├── data/minecraft/loot_table/...                掉落表覆盖（warden/wither/矿石/基岩/远古城市/末地城/试炼密室）

├── assets/extra-enchantry/atlases/gui.json      效果图标 GUI 图集注册（v1.3.2，directory source）

├── assets/extra-enchantry/items/ + models/item/  物品客户端模型（1.3.1 起 JSON 同步维护）

├── assets/extra-enchantry/font/fancy_\*.json     八家族附魔名称字体

└── assets/extra-enchantry/lang/\*.json           翻译

src/main/java/realmikoto/extraenchantry/

├── ExtraEnchantry.java                          附魔 ResourceKey 注册 + 判定辅助

├── FxHelper.java                                粒子/音效统一封装（见「视听反馈」）

├── Advancements.java                            代码授予成就统一入口（见「成就体系」）

├── \*Manager.java                                各附魔的状态与结算（1.4.0 增 AccessoryManager / WolfArmorManager；1.3.x 增 FamilyResonance / FamilyTrials / Attunement / Onboarding / LoreTrigger / Cavalry）

├── Accessories.java / AccessoryItem / GemItem / AccessoryContainer / AccessorySlot / AccessoryAttachments   1.4.0 环佩体系（注册中心 + 物品 + Attachment 容器）

├── LoreLoader.java / TideheartAir.java          lore 数据加载 / 渊息公式（纯函数供单测）

└── mixin/\*.java                                 效果逻辑

src/client/java/realmikoto/extraenchantry/client/

├── mixin/\*.java                                 客户端渲染/输入（字体动效、HUD、空跃、背包配饰栏三件套）

└── AccessoryColumnRenderer / AccessoryHudState / SlotReposition   1.4.0 配饰栏自绘（列背景/幽灵图标/动画帧 + 展开状态 + 槽位原地重定位接口）
```
### 代码注册约定

每个附魔在 `ExtraEnchantry.java` 注册 ResourceKey（数据驱动附魔是动态注册表，无法静态引用）：

```
public static final ResourceKey\<Enchantment> REACH =

        ResourceKey.create(Registries.ENCHANTMENT, id("reach"));
```

物品上的等级读取统一用：`stack.getEnchantments()` 遍历 `keySet()` 匹配 `holder.is(KEY)` 后 `getLevel(holder)`。

### 创造模式物品栏

`ExtraEnchantryCreativeTab.java` 将本 mod 全部附魔书单列一页（`extra-enchantry:enchantments`）：

* `FabricCreativeModeTab.builder()`（fabric-creative-tab-api-v1）→ `title/icon/displayItems/build`，注册进 `BuiltInRegistries.CREATIVE_MODE_TAB`
* `displayItems` 回调在打开物品栏时执行，此时数据包已加载：`parameters.holders().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY)` 取 Holder
* 附魔书构造：`Items.ENCHANTED_BOOK` + `DataComponents.STORED_ENCHANTMENTS`（`ItemEnchantments.Mutable.set(holder, level)`）
* **新增附魔时**：在该类的 `ENCHANTMENTS` 列表追加 `(KEY, 最大等级)` 即自动列出全部等级的附魔书

### 视听反馈（FxHelper）

所有粒子/音效必须经过 `FxHelper.java`，不在业务代码里裸写 `sendParticles`/`playSound`：

* 封装：`burst`（实体中心爆发）/ `burstAt`（定点）/ `ring`（环）/ `trail`（两点间排点）/ `play`（音效）/ `pitchForLevel`（等级变调）/ `throttle`（按 UUID+key 节流）
* `play` 有 `SoundEvent` 与 `Holder<SoundEvent>` 两个重载——26.2 的 `SoundEvents` 常量两种类型并存（如 NOTE_BLOCK_\*、SHIELD_BLOCK、SOUL_ESCAPE 是 Holder），调用前 `javap` 确认字段类型
* **分级约定（详见 DESIGN_aesthetics.md）**：L1 触发确认（每次生效都给，短促）/ L2 持续氛围（必须 `throttle`，10–20 tick）/ L3 高光时刻（免死、处决等稀有事件才允许大场面）。常态生效的效果（如触及）只给粒子不配音效，防吵
* 客户端独占反馈（如空跃振翅音）放 client source set 的 Mixin，服务端不可见的实体状态别往服务端发
* 新增纹理 / 效果图标 / 专属音效走「美术/音效资源生成」脚本化管线（见下节），产物目录镜像 `src/main/resources/assets/extra-enchantry/`，复制即用

### 美术/音效资源生成（脚本化资源管线）

新增物品纹理 / 效果图标 / 专属音效时，优先在 `DESIGN/1.5.0-assets/generate_assets.py` 加绘制函数与合成配方后重跑，不手写二进制资源。纯标准库（无 Pillow / numpy 依赖），确定性输出（同输入重跑结果逐字节一致），清单与集成说明见 `DESIGN/1.5.0-assets/README.md`：

* **纹理（16×16 物品 / 18×18 效果图标 PNG）**：纯 Python 像素绘制 + 手写 PNG 编码（`zlib` + `struct`），支持辉光（径向 alpha 衰减）/ 圆 / 矩形 / 线段 / 逐像素混合；混合时存量 alpha 是 0–255 整数、新 alpha 是 0–1 浮点，**必须先归一化再混**，否则算出 >255 的通道值直接 `bytes()` 报错
* **PNG 自检（呼应踩坑记录）**：NativeImage(STB) 要求每行恰好 `1 filter 字节 + 宽×4 字节 RGBA`，生成后可 `len(zlib.decompress(IDAT)) == 高 × (1 + 宽×4)` 校验，多一个字节即 `Corrupt PNG` 且 atlas sprite 永久 missing
* **效果图标落盘即生效**：26.2 效果图标走 GUI 图集，`atlases/gui.json` 的 directory source 已注册（v1.3.2 起），新图标放入 `textures/mob_effect/` 无需改图集配置
* **物品模型 JSON**：`models/item/*.json` 统一模板（`item/generated` + `layer0`）批量生成，与注册代码一一对应
* **音效（OGG Vorbis）**：确定性合成——正弦 / 指数与线性扫频 / 固定 seed 噪声 / 包络 / 混音，写 WAV 后 ffmpeg `-c:a libvorbis` 转 44.1 kHz 单声道 OGG；ffmpeg 缺失自动回退保留 WAV。配方即"乐谱"：调基频 / 时长 / 泛音 / 颤音参数即可改风格，重跑覆盖
* **sounds.json**：脚本同步生成注册（`extra-enchantry:xxx` 键）；代码侧 `Registry.register(Registries.SOUND_EVENT, ...)` 后统一经 FxHelper 播放（音量 ≤1.0 / SoundSource.PLAYERS / 5 tick 节流，见「视听反馈」）
* **粒子不生成纹理**：遵循 DESIGN_aesthetics 约定不注册自定义粒子类型，全部映射原版粒子（服务端 `sendParticles` 直接引用），资源管线不含粒子纹理
* **性能纪律**：合成循环避免在列表推导内重复调用会生成长列表的函数（曾踩推导内嵌 `sine()` 的 O(n²)≈50 亿次运算坑，先提出循环再逐元素相乘）

### 附魔名称字体体系

附魔名称按稀有度三级着色，`EnchantmentMixin` 注入 `Enchantment#getFullname` RETURN、用带样式的空父组件包裹原名（子组件继承字体+颜色，不动原名本体）：

* **T0 传说**（破限/拓阶）：金色 + `fancy_lb` 字体
* **T1 八家族**：`FAMILY_STYLES` 映射（ResourceKey → 字体 ID + 基础色）——灵魂 #4FD8E8 / 雷光 #B8F4FF / 锋刃 #E8E8F0 / 自然 #6FE86F / 深渊 #3F76E4 / 风 #D8F0F0 / 守护 #7FA8C9 / 火焰 #FF7A2A；字体定义在 `assets/extra-enchantry/font/fancy_{族}.json`（均基于 fancy.json 的 uncial_antiqua + zcool_xiaowei 双语覆盖，每族一份便于独立调整）
* **T2 普通**：不动，保持原版灰色
* **逐字动效**：客户端 `FontPreparedTextBuilderMixin` Redirect `Style#getColor`，按字符索引做 HSV 波形偏移（按字体 ID 分派各族相位/速度参数），形成颜色沿文字流动的效果；破限名另随「无敌」进度切换锁定暗灰/解锁金色
* **新增附魔时**：在 `FAMILY_STYLES` 选族登记（或明确留在 T2），并同步把附魔加入对应 `tags/enchantment/family_{族}.json`——字体族与成就图鉴族共用同一份划分，两处必须一致

### 成就体系

三棵树，按"能否用数据驱动表达"选触发方式：

* **collector/**（图鉴树，纯数据驱动）：根（tick 触发）→ 家族节点（`inventory_changed` + `stored_enchantments` 谓词引用 `#extra-enchantry:family_{族}` 标签）→ 集齐节点（各族 AND）。**新增附魔时**：归族即自动进入图鉴判定，无需改成就 JSON
* **usage/**（实战树，代码授予）：JSON 用 `minecraft:impossible` 触发器（任何游戏事件都无法自然完成），由效果代码在生效处调 `Advancements.award(ServerPlayer, ResourceKey)` 授予。criterion 名统一 `triggered`；ResourceKey 常量在 `Advancements.java` 集中声明。**新增附魔时**：有"首次成功使用"纪念价值的，加一个 usage 成就 + 一句 award 调用
* **hidden_challenges/**（隐秘挑战，代码授予）：v1.0 既有模式，criterion 自带命名空间（如 `extra-enchantry:obtained`），授予逻辑在各 Manager 内（LimitBreakManager/CavalryManager）
* 代码授予的查找方式（26.2）：成就**不是**注册表，`server.getAdvancements().get(key.identifier())` 拿 `AdvancementHolder` 再 `player.getAdvancements().award(holder, criterion)`；award 幂等，重复触发安全

### Mixin 清单

| Mixin                        | 目标                        | 作用                                                                                                                                                                                 |
| ---------------------------- | ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| LivingEntityMixin            | `LivingEntity`            | 凋零时长缩短；破限保护上限 100%；汲取攻击回血；蚀命额外最大生命值伤害；断罪斩杀；壁垒单次伤害上限；活力最大生命值修改器（含马铠）；疾风移速修改器；无踪索敌降低；御风滑翔耐久；余烬免死 + 锁血；破阵溅射；誓约四件套经验球跳过；假象 PvP 触发；诸界浩劫挑战生物经验翻倍；冲阵疾跑撞击加伤 + 击退；坚壁格挡减免不可格挡类伤害；庇护团队光环 tick；霆霓投掷加伤 + 连锁；藏锋拔刀加伤 + 战斗记账；渊息 III 级水下挖掘修改器 |
| AnvilMenuMixin               | `AnvilMenu`               | 破限互斥无视 + 铁砧费用 5 级（触及 XI 级产物特判 15 级，v1.1.0 起）；疾风 III 级需破限（无破限时产出降回 II 级）；破限门禁：未击败骑兵队时拦截破限书应用                                                                                                                   |
| EnchantmentHelperMixin       | `EnchantmentHelper`       | 破限附魔台互斥无视（ThreadLocal 传物品）                                                                                                                                                         |
| ItemStackMixin               | `ItemStack`               | 拓阶挖掘等级 / 速度                                                                                                                                                                        |
| BlockBehaviourMixin          | `BlockBehaviour`          | 拓阶基岩可破坏                                                                                                                                                                            |
| EnchantmentMixin             | `Enchantment`             | 附魔名称三级样式：T0 破限/拓阶金色 fancy_lb；T1 八家族字体+基础色（FAMILY_STYLES）；T2 不动（见「附魔名称字体体系」）                                                                        |
| EnchantRandomlyFunctionMixin | `EnchantRandomlyFunction` | 触及 / 壁垒随机来源钳到 1 级；疾风钳到最高 2 级                                                                                                                                                       |
| MobMixin                     | `Mob`                     | setTarget 统一裁决：假象仇恨重定向 + 触发；诸界浩劫挑战生物仇恨锁定玩家 / 无仇恨（误伤不改仇恨）                                                                                                                           |
| GrindstoneMenuMixin          | `GrindstoneMenu`          | 破限砂轮防移除（removeNonCursesFrom HEAD/RETURN）                                                                                                                                           |
| DeathProtectionMixin         | `DeathProtection`         | 劫后余辉：不死图腾生效时读取图腾栈副本的附魔并触发余辉                                                                                                                                                        |
| PlayerMixin                  | `Player`                  | 锁血（余辉 / 余烬）期间取消 `actuallyHurt`（必须挂 Player，它不调 super）；誓约：死亡掉落前提取 / 掉落后回插带誓约物品；破限：`addItem` 检测破限附魔书授予隐藏进度并触发守护骑兵队                                                                    |
| ServerPlayerMixin            | `ServerPlayer`            | 誓约：重生 restoreFrom 时搬运誓约物品（四件套连带经验与分数，部分誓约仅背包）；诸界浩劫：die HEAD 死亡瞬间结算失败（背包掉落前销毁破限书）                                                                                                   |
| EntityMixin                  | `Entity`                  | 无踪 I：屏蔽脚步声与 STEP 震动（vibrationAndSoundEffectsFromBlock）+ 屏蔽落地 HIT_GROUND 震动（checkFallDamage 内 gameEvent Redirect）                                                                  |
| SkeletonTrapGoalMixin        | `SkeletonTrapGoal`        | 僵尸马骑兵队：原版骷髅马陷阱触发时 HEAD 取消原版生成，替换为较弱的骷髅马 / 僵尸马混编骑兵队（保留视觉闪电）                                                                                                                         |
| InventoryMixin               | `Inventory`               | 破限书检测兜底：/give 直调 Inventory#add 不经 Player#addItem（反编译 GiveCommand 确认），补挂同一入口                                                                                                        |
| ItemCombinerMenuAccessor     | `ItemCombinerMenu`        | 访问器：暴露 protected 的 `player` 字段（声明于父类，AnvilMenuMixin 无法 @Shadow）供破限门禁取玩家                                                                                                            |
| BlocksAttacksMixin           | `BlocksAttacks`           | 不屈：II 免疫破盾（disable HEAD 取消）、I 破盾时长减半 + 抗性提升、II 格挡耐久消耗 ×2（hurtBlockingItem damage ×2）                                                                                               |
| CreeperAccessor              | `Creeper`                 | 访问器：私有充能位 DATA_IS_POWERED（静态）+ 引信 maxSwell—— 诸界浩劫的闪电苦力怕（引信 30→8）                                                                                                                 |
| FireworkRocketEntityMixin    | `FireworkRocketEntity`    | 御风 II/III：烟花对滑翔者的推进增量按倍率放大（tick 内 setDeltaMovement Redirect)；坠星：爆炸伤害 +4 / 半径 +1（dealExplosionDamage 三组常量 ModifyConstant）+ 星形粒子                                                                                                                       |
| AbstractArrowMixin           | `AbstractArrow`           | 归羽：发射快照等级存箭实体（HomingPlumeAccess）；命中实体置标记；插地方按概率登记延迟返还（注意 26.2 包名为 `projectile.arrow`）                                                                                                                                  |
| ProjectileWeaponItemMixin    | `ProjectileWeaponItem`    | 归羽：createProjectile RETURN 把武器归羽等级写入箭矢（弓弩共用此生成点）                                                                                                                                                                                          |
| CrossbowItemMixin            | `CrossbowItem`            | 坠星：createProjectile RETURN 给烟花火箭打坠星标记（StarfallAccess）                                                                                                                                                                                            |
| InventoryMenuMixin           | `InventoryMenu`           | 配饰槽进背包菜单：构造 TAIL 追加 4 个 AccessorySlot（46~49）+ quickMoveStack shift 智能移动（mixin 继承 AbstractContainerMenu 以访问 protected 成员）                                                               |
| ItemEntityMixin              | `ItemEntity`              | 火焰家族烧毁免疫：带火焰系附魔 / 烬心石配饰的掉落物免疫火与岩浆伤害（hurtServer HEAD 取消，下界合金同款）                                                                    |
| BlockMixin                   | `Block`                   | 丰壤：playerDestroy RETURN 判定成熟作物 + 丰壤锄头 → 双倍掉落 / 3×3 范围收获（LoamManager）                                                                                                                                                                    |

客户端 Mixin（`src/client/java/.../client/mixin/`，注册于 `extra-enchantry.client.mixins.json`）：

| Mixin                        | 目标                         | 作用                                        |
| ---------------------------- | -------------------------- | ----------------------------------------- |
| HudMixin                     | `Hud`                      | 活力：血条隐藏加成保持单行 + 上方 “❤×n/N” 紧凑显示           |
| FontPreparedTextBuilderMixin | `Font$PreparedTextBuilder` | 家族字体逐字 HSV 波形动效（按字体 ID 分派 8 族参数）；拓阶金色波浪闪光；破限名分锁定态：未完成「无敌」进度暗灰平色，完成恢复金色闪光 |
| ClientAdvancementsAccessor   | `ClientAdvancements`       | 访问器：暴露私有进度表，供破限锁定态的本地判定                   |
| LocalPlayerMixin             | `LocalPlayer`              | 空跃：客户端空中跳跃（aiStep 按键沿检测 + jumpFromGround）+ 踏空云粒子与振翅音 |
| InventoryScreenMixin         | `InventoryScreen`         | 生存背包配饰栏：护甲左侧配饰列 + 盾牌列头盔行按钮（extractBackground TAIL 自绘 + 动画）                                                                                                                |
| CreativeModeInventoryScreenMixin | `CreativeModeInventoryScreen` | 创造玩家页签：盾牌下移与胸甲平齐、头盔左侧按钮、配饰 2×2（selectTab TAIL 重定位 45/46~49 槽包装 + 展开时原地替换条目）                                                                             |
| AbstractContainerScreenMixin | `AbstractContainerScreen`  | 生存背包配饰按钮点击网关（InventoryScreen 未覆写 mouseClicked，基类注入按实例门控；leftPos/topPos @Shadow——声明于目标类自身）                                                                        |
| ItemStackFoilMixin           | `ItemStack`                | 臻藏光晕：臻藏资格 / 大共鸣者时本模附魔书与共鸣秘典恒显附魔光效（hasFoil RETURN 改写；进度缺失时静默降级普通外观） |

### 踩坑记录（26.2）

* `@Inject` 有返回值的方法必须用 `CallbackInfoReturnable`，否则类加载时崩溃（懒加载，主菜单不报错进世界才炸）
* `@Redirect` 处理器参数顺序：被重定向调用参数在前、外围方法参数在后
* private 内部类（如 `Font$PreparedTextBuilder`）用 `@Mixin(targets = "全限定名")` 字符串引用
* ttf 字体 provider 字段名仍是 `file`；`enchant_randomly`（宝箱书与图书管理员共用）等级是 `nextInt(minLevel, maxLevel)` 均匀分布，cost 曲线管不到它
  * `Level.isClientSide` 是私有字段，需用 `isClientSide()` 方法
  * 附魔 JSON 的 `supported_items` 若为数组，**数组元素只能是物品 ID，不能混用 `#tag` 引用**（报 `Not a valid resource location: #... Non [a-z0-9_.-] character in namespace`，世界创建卡 "正在准备生成世界"）—— 标签只能以单字符串形式出现；"原标签 + 追加物品" 的混编走自定义物品标签（标签 values 内可引用其他标签，如 vitality_supported/bulwark_supported）

* 复用原版实体类型生成自定义子类实例：直接 `new 子类(Level,x,y,z)` + `addFreshEntity`，客户端走原版渲染，无需注册（配合 `shouldBeSaved()=false` 防持久化）
* `exclusive_set` 支持 `"#tag"`、`["id1","id2"]` 列表、单 `"id"` 三种 JSON 写法（`RegistryCodecs.homogeneousList`）
* `@Shadow` 字段只在目标类本类解析，**父类字段 shadow 不到**（如 `ItemCombinerMenu.player`）；需要上下文时改走 Fabric 事件或构造器注入
* **Redirect 是独占注入**：fabric-api 自身的 Mixin（如 fabric-item-api 对 `AnvilMenu.createResult` 中 `canEnchant` 的 Redirect）已占用的调用点不能再 Redirect，会 Critical injection failure—— 改用官方事件（`EnchantmentEvents.ALLOW_ENCHANTING`）
* `Enchantment` 实例无自身 ID，识别身份要么经注册表反查（`registry.getResourceKey`），要么订阅事件拿 `Holder`
* 26.2 HUD 为**渲染状态提取架构**：无旧版 `InGameHud.render`，改为 `Hud.extractPlayerHealth(GuiGraphicsExtractor, ...)` 提取贴图 / 文本元素；心形行数由 `player.getAttributeValue(MAX_HEALTH)` 驱动，`@Redirect` 该调用即可控制行数；绘制用 `extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h)` / `extractor.text(font, str, x, y, color)`，心形图标路径 `hud/heart/full`（`Hud$HeartType` 为包私有，直接硬编 Identifier 即可）
* 附魔 JSON 的 `supported_items`/`primary_items` **列表内不能写** `#tag` **引用**（数组内每个元素按纯 Identifier 解析，`#` 开头报 “Not a valid resource location” 导致注册表加载崩溃）；需要聚合多个标签时必须建自定义物品标签（`data/<ns>/tags/item/xxx.json` 的 `values` 支持 `#tag` 嵌套）再整体引用为单字符串 `"#ns:xxx"`，如 `reach_supported`/`siphon_supported`/`life_erosion_supported`
* `GuiGraphicsExtractor.text(...)` 的颜色参数是 ARGB，且 **alpha==0 时直接 return 不渲染**（旧版自动补 alpha 的行为已移除）—— 写 `0xFFFFFF` 会导致文字完全不可见，必须用 `0xFFFFFFFF` 这类带 alpha 的值；血条心形外观 = `hud/heart/container` 黑底 + `hud/heart/full` 叠加，单独画 full 缺黑底轮廓与血条观感不一致
* HUD 自定义行不要写死 y 偏移：心行数随伤害吸收（金苹果黄心）/ 高血量动态变化，盔甲行 y = 基准 - (心行数 - 1)× 行高 - 10 会随之上移 —— 应 `@Inject` 捕获 `extractArmor(extractor, player, y, rows, rowHeight, x)` 的实参自行推算盔甲行 y，锚定其上方才能永久不错位；另外玩家受伤后 `getHealth()` 常带浮点残差（如 27.857931），直接换算显示会产生长小数，需按原版心形显示的 ceil 半心粒度取整
* 隐藏额外生命只重定向 MAX_HEALTH 不够：`extractHearts` 的红心绘制条件是 `i×2 < health`（当前血量）、循环上限含 `displayHealth = ceil(health/2)`—— 高血量时会把额外生命直接画成多行红心（伤害吸收黄心改变行数时显形）。必须同时 `@Redirect` 该方法内的 `Player.getHealth()` 调用，把血量也钳制到基础上限
* `Player` **重写了** `actuallyHurt` **且不调用 super**（自己完整实现了护甲 / 魔抗 / 吸收结算与 `setHealth`），因此针对玩家扣血链路的注入必须挂在 `Player` 上；挂 `LivingEntity` 无效。壁垒 Bulwark 曾因此对玩家不生效，**现已修复**：不再依赖调用点 Redirect，改为 @Inject `getDamageAfterMagicAbsorb` 方法本身的 RETURN——`Player#actuallyHurt` 对该方法内部是**虚调用**、派发到 `LivingEntity` 的唯一实现，单点注入即同时覆盖玩家与非玩家（见 `LivingEntityMixin#extraenchantry$bulwarkCapDamage`）
* 玩家扣血链路：`Player#hurtServer` → `super`（`LivingEntity#hurtServer`，`hurtServer` 相关的 @Inject/@ModifyVariable 仍然有效）→ 虚调用 `actuallyHurt` → `Player#actuallyHurt`（**终点**）。另外 26.2 在 `LivingEntity` 与 `Player` 之间插入了 `Avatar` 抽象类（不重写扣血相关方法）
* **26.2 玩家死亡链路（誓约的实现依据，反编译确认）**：`ServerPlayer#die` 不清经验、只调 `dropAllDeathLoot`（继承自 `LivingEntity`）→ `dropFromLootTable` → `dropCustomDeathLoot`（**空壳**）→ `dropEquipment` → `dropExperience(level, attacker)`。玩家的**全部物品掉落集中在** `Player#dropEquipment`（keepInventory 判断 → `destroyVanishingCursedItems` → `inventory.dropAll()`；父类 `LivingEntity#dropEquipment` 是空壳）。**经验不清零**：keepInventory 关闭时经 `dropExperience` 掉出 min (等级 ×7, 100) 点经验球，玩家经验靠 "旧对象弃置" 丢失。重生时 `ServerPlayer#restoreFrom(old, keepEverything)` 仅在 keepEverything（非死亡重生）、keepInventory 开启、或旧玩家为旁观者时调用 private 方法 `transferInventoryXpAndScore`（`Inventory#replaceWith` + 经验 + 分数整体搬运）
* **26.2 Inventory 结构变更**：背包物品（36 格 `items` NonNullList）与装备（新抽象 `EntityEquipment`，EnumMap\<EquipmentSlot, ItemStack>）**分离存储**；但 `Inventory#getContainerSize/getItem/setItem/removeItemNoUpdate` 仍通过内部 `EQUIPMENT_SLOT_MAPPING` 把装备槽映射进 Container 索引 —— 遍历 Container 接口即可覆盖全部槽位（誓约的提取 / 回插依赖这一点）
* `GameRules#getBoolean` 在 26.2 改为泛型 `GameRules#get(GameRule<T>)`（直接返回值）
* `@Redirect` 对**虚方法调用**的处理器必须显式带上接收者参数（本类自调用也要写：`(LivingEntity self, 原参数...)`）；静态调用不需要（如既有壁垒 Redirect 里的 `CombatRules`）
* **玩家跳跃输入是客户端权威**：服务端看不到跳跃键状态（`jumping` 字段服务端对玩家不生效），多段跳类效果必须做在客户端。26.2 中 `LivingEntity#jumpFromGround()` 与 `isJumping()/setJumping()` 已是 public；`LocalPlayer#aiStep` 内直接读公开字段 `input.keyPresses.jump()`（`ClientInput.keyPresses` 为 public，`Input` 是 record）拿本 tick 最新按键 —— 比读 `jumping` 字段更可靠（字段由 `applyInput` 在别处刷新，时序不保证）
* 不死图腾的触发链路：`LivingEntity#checkTotemDeathProtection(DamageSource)`（private，被 `hurtServer` 在 `isDeadOrDying()` 后调用）→ copy 图腾栈 → `shrink(1)` 消耗 → `setHealth(1.0F)` → `DeathProtection#applyEffects(副本, this)`。要在触发时读取图腾上的数据，只能从 `applyEffects` 的栈副本参数拿；且其死亡效果列表首项是 `ClearAllStatusEffectsConsumeEffect`，注入点必须晚于它。伤害吸收量与等级的关系是 `max(当前, 4×(1+amplifier))`（`AbsorptionMobEffect#onEffectStarted`），阶梯 4/8/12 HP 给不出奇数颗心，需要 `setAbsorptionAmount` 精确覆盖
* 附魔台槽位的准入检查是 `ItemStack#isEnchantable()`（要求物品带 `DataComponents.ENCHANTABLE` 组件），与附魔的 `supported_items` 无关；`AnvilMenu` 不做该检查（只看 `canEnchant`/`supported_items`）。所以给无 `enchantable` 组件的物品（如不死图腾）做附魔时，附魔台途径必然不可用，铁砧才是生存途径
* **26.2 声音 / 震动事件位置（无踪的实现依据）**：脚步声与 `GameEvent.STEP` 同在 `Entity#vibrationAndSoundEffectsFromBlock(pos, state, playSound, sendEvent, movement)`（private boolean，param3/param4 分别控声音与事件，HEAD 取消返回 false 即两者全屏蔽）；`playStepSound` 只管音效、**不发事件**。落地的 `GameEvent.HIT_GROUND` 在 `Entity#checkFallDamage` 内；LivingEntity 虽重写了 `checkFallDamage`，但末尾 `invokespecial Entity.checkFallDamage` 调 super，所以只需注入 Entity 版本
* `@Redirect` 的 target owner 必须与字节码调用点一致，而非方法的声明类：`gameEvent(Holder, Vec3, Context)` 声明在 `LevelAccessor`，但调用点写的是 `Lnet/minecraft/world/level/Level;gameEvent(...)`，target 就得写 Level（同理 `setDeltaMovement` 写调用点的 `LivingEntity` 而非 `Entity`）。同一方法内存在**多个同名调用**时，用不同 owner 区分可避免 Redirect “too many targets”（烟花 tick 内火箭自身移动与推动滑翔者分别以 FireworkRocketEntity / LivingEntity 为 owner）
* **鞘翅滑翔耐久（26.2）**：在 `LivingEntity#updateFallFlying`（protected），每 10 tick 计一次、每 2 次对**可滑翔装备槽中随机一个** `hurtAndBreak(1, this, slot)`，并伴随 `GameEvent.ELYTRA_GLIDE`；想降低耐久消耗就 Redirect 该 hurtAndBreak（概率跳过比改 tick 计数更不易出错）
* **烟花推进（御风的实现依据）**：`FireworkRocketEntity#tick` 中 `isAttachedToEntity() && attachedToEntity.isFallFlying()` 分支对滑翔者做朝视线方向的插值加速（常量 1.5/0.1/0.5）后调 `LivingEntity#setDeltaMovement`；Redirect 该调用后用 “新速度 - 旧速度” 取出推进增量再按倍率写回，可不动原版公式（旧速度在调用时尚未写入，`getDeltaMovement()` 读到的就是它）
* **索敌距离的唯一系数是** `LivingEntity#getVisibilityPercent(Entity)`（原版潜行 ×0.8、隐身按护甲覆盖率再乘），`TargetingConditions` 用它缩放 follow range—— 改 “怪物更难发现你” 类效果在 RETURN 乘系数即可，无需改 Mob 索敌逻辑
* **自定义免死效果挂在** `checkTotemDeathProtection` **的 RETURN**（而非 HEAD）：`hurtServer` 的判定是 `if (isDeadOrDying()) { if (!checkTotemDeathProtection(source)) die(source); }`，返回 true 即跳过死亡；选 RETURN + `!cir.getReturnValueZ()` 可天然让**不死图腾优先**，图腾没救命才轮到自定义免死（余烬）。锁血则复用 `actuallyHurt` HEAD 取消（玩家挂 Player、非玩家挂 LivingEntity，两处都要）；锁血时长可用 “效果剩余时长> 总时长 - 窗口” 判定，无需独立计时器
* **26.2 没有统一的 boss 判定**：`Entity`/`LivingEntity`/`Mob` 均无 `isBoss()`，`EntityTypeTags` 也只有 RAIDERS/UNDEAD/ARTHROPOD/SENSITIVE_TO_\*，没有 boss 标签 —— 需要区分 boss 时只能显式 `instanceof`（EnderDragon / WitherBoss / Warden）
* **铁砧限制附魔等级上限的可行做法**：附魔本身的 `max_level` 不能条件化（静态数据），故把 `max_level` 写到真实上限（如疾风 3），再用三路封锁限制获取：cost 曲线（附魔台 / 钓鱼）+ `enchant_randomly` 钳制（宝箱书 / 交易）+ `AnvilMenu.createResult` TAIL 检查产出、不满足条件时用 `ItemEnchantments.Mutable` 降级并 `stack.set(DataComponents.ENCHANTMENTS, ...)` 重写（产出槽持的是同一 ItemStack 实例，原地改组件即生效）
* **限级 cost 曲线不能只盯附魔台的 30**：加入 `non_treasure` 的附魔会经 `#non_treasure` 流入 `on_random_loot` 与 `tradeable`（反编译两个原版标签确认都引用 `#non_treasure`），而原版宝箱装备的 `enchant_with_levels` 给到 **cost 50**（远古城市 / 末地城 30-50）—— 所以 “最高只能随机到 N 级” 的阈值必须按 50 算（疾风 III 级 min_cost 定在 55），否则高级附魔会从宝箱装备里泄出
* **26.2 箭矢分包**：`AbstractArrow` 已从 `projectile` 移至 `projectile.arrow` 分包（`ThrownTrident`/`Arrow`/`SpectralArrow` 同移）；其 `onHitEntity`/`onHitBlock`/`tickDespawn`/`getPickupItem`（protected）结构不变，`pickup` 为 public 字段（`AbstractArrow$Pickup`：DISALLOWED/ALLOWED/CREATIVE_ONLY）
* **Mixin 里调目标类的 protected 方法**：经 `((Target)(Object)this).protectedMethod()` 强转调用编译不过（protected 只认子类类型），标准做法是 `@Invoker("方法名")` 声明抽象方法透传——注意注解在 `org.spongepowered.asm.mixin.gen.Invoker`（不是 injection 包）
* **26.2 烟花爆炸结构（坠星的实现依据）**：`FireworkRocketEntity#dealExplosionDamage(ServerLevel)`（private）基础伤害 = `5.0f + 2×爆炸星数`，半径 5.0d（AABB 外扩 + 距离平方阈值 25.0d + `(5-距离)/5` 衰减）；弩发射烟花走 `CrossbowItem#createProjectile` 烟花分支并把射手作为 owner 传入。增强爆炸用三组 `@ModifyConstant`（5.0f / 5.0d / 25.0d）**同步**缩放，衰减公式才能保持一致
* **26.2 氧气体系（渊息的实现依据）**：氧气上限 = `Entity#getMaxAirSupply()` 硬编码 `sipush 300`（15 秒），`increaseAirSupply` 以它为钳制上限——HEAD 注入放大即全链路生效；原版水下呼吸改走 `Attributes.OXYGEN_BONUS`（`decreaseAirSupply` 内判定），与上限放大是两条独立路径
* **26.2 水下挖掘惩罚是属性不是分支**：`Player#getDestroySpeed` 尾部 `isEyeInFluid(WATER)` 时乘 `Attributes.SUBMERGED_MINING_SPEED`（基础 0.2）——免减速写 +0.8 瞬态修改器即可，无需改流程（原版水下速掘同机制）
* **26.2 `Block.getDrops` 第六参已改为 `ItemInstance`**（新接口，`ItemStack` 直接实现之）——看到签名别慌，ItemStack 原样传入即可；`Block#playerDestroy` 签名仍是 ItemStack
* **`isInWaterRainOrBubble()` 在 26.2 已移除**：水中 / 雨中需拆成 `isInWater()` + `level.isRaining()`（或 `isRainingAt(BlockPos)`）自行组合
* **成就 ID 含子目录路径**：`ServerAdvancementManager extends SimpleJsonResourceReloadListener`，成就 ID = JSON 相对 `data/<ns>/advancement/` 的完整路径——`advancement/usage/foo.json` 的 ID 是 `ns:usage/foo`。代码查找（`server.getAdvancements().get(...)` / `ClientAdvancements.get(...)`）与 JSON 内 `parent` 引用都必须带前缀，否则静默返回 null（授予不生效、客户端进度查询恒 false）。v1.0 曾因此 4 个隐藏成就从未授予且破限书锁定判定恒锁，v1.1.0 已修（LimitBreakManager/CavalryManager/LimitBreakLockState 三处）
* **26.2 状态效果图标走 GUI 图集而非独立贴图**（v1.3.2 踩坑）：`Hud#getMobEffectSprite` 把效果 ID 前缀 `mob_effect/` 后交给 `graphics.blitSprite`——**sprite 必须注册进 `textures/atlas/gui.png` 图集**，mod 需自带 `assets/<ns>/atlases/gui.json`（`{"type": "minecraft:directory", "source": "mob_effect", "prefix": "mob_effect/"}`，与原版 gui.json 同款 directory source）。v1.2.0 起共鸣图标一直走 missing sprite fallback（日志 "Using missing texture"），v1.3.2 补注册后生效
* **NativeImage(STB) 对 PNG 校验严格**（v1.3.2 踩坑）：脚本生成 PNG 时每行必须恰好是 `1 filter 字节 + 宽×4 字节(RGBA)`，多一个字节 System.Drawing 能读但 STB 报 `Corrupt PNG`（atlas stitch 阶段 IOException，sprite 永久 missing）。用 Python 校验：`zlib.decompress(IDAT)` 后长度应等于 `高 × (1 + 宽×4)`
* **实体构造早期钩子的装备陷阱**（v1.3.2 踩坑）：`Entity#<init>` 的 defineSyncker 在定义 `DATA_AIR_SUPPLY_ID` 初值时就回调 `getMaxAirSupply()`——此时 `LivingEntity#equipment` 字段尚未初始化（子类构造体才赋值），Mixin 里 `instanceof LivingEntity` 已为真但 `getItemBySlot` 必 NPE，**实体构造直接失败**（新世界/登录 "Couldn't place player in world"，服务器停止）。渊息的 `tideheartMaxAir` 自 1.1.0 起带此雷，v1.3.2 加 try-catch NPE 防护（构造早期视为无渊息走原版上限）
* **26.2 创造页签槽位列表不可替换**（v1.4.0 踩坑）：`CreativeModeInventoryScreen#selectTab` 把 ItemPickerMenu 槽位列表**整体替换**为 InventoryMenu 槽位的 SlotWrapper 包装（点击按 containerId 0 路由回服务端 InventoryMenu），而 `slotClicked` 把点击槽**硬转 SlotWrapper**——向列表插入/替换自定义槽位条目会每次点击 ClassCastException。重定位只能**原地改坐标**：向 `Slot` 基类织入 `@Shadow @Final @Mutable x/y` 改写接口（SlotMixin + SlotReposition），列表结构保持原样
* **Mixin 访问目标类 protected 成员的标准手法**（v1.4.0）：`InventoryMenuMixin extends AbstractContainerMenu`——mixin 继承目标类的父类即可访问 protected 的 `addSlot` / `moveItemStackTo`（构造器仅 `super(type, containerId)` 服务访问、不参与合并）；比 `@Invoker` 逐个透传省事
* **`Slot#isActive` 是纯客户端概念**（v1.4.0，反编译确认）：26.2 的 `AbstractContainerMenu` / `Slot` 服务端路径**零调用** isActive——折叠/展开门控只影响渲染与点击判定，服务端逻辑完全无感知（shift 移动不做门禁的依据：折叠时放入的物品展开后可见，属可接受便利）
* **Fabric Data Attachment 是快照语义**（v1.4.0）：`getAttached` 返回的是落盘快照，**原地修改其中的 ItemStack 不会自动保存**——所有写路径必须收敛到 `setAttached` 回写（配饰统一走 `AccessoryContainer.setItem → setChanged → setAttached`；Menu `removed` 钩子做关闭界面时的最终写回兜底）
* **26.2 物品模型可按任意数据组件值分发**（v1.4.0，反编译确认）：`items/*.json` 支持 `{"type": "minecraft:select", "property": "minecraft:component", "component": "<ns:组件>", "cases": [{"when": "<codec 值>", "model": {...}}], "fallback": {...}}`——`ComponentContents` 经组件自身的 codec 解析 `when` 值（组件须非 transient 且带 persistent codec），`fallback` 兜底无组件/无匹配。对"单物品类型 + 标识组件"（铭印 / 铭文 / 残页类）是**纯数据零代码**的多形态方案，无需 custom_model_data 旁路或客户端渲染 Mixin（家族铭印九型令牌即此实现）
* **生存背包纸娃娃渲染区 = GUI 坐标 (26~75, 8~78)**（v1.4.1 踩坑，反编译 `InventoryScreen#extractBackground`：`extractEntityInInventoryFollowsMouse(xo+26, yo+8, xo+75, yo+78, ...)`）——自绘 UI 元素左缘必须 ≥76，否则压进玩家模型 1~2px（配饰按钮的 panel_fill 衬底左缘 74 压住右缘 75 两列，观感即"按钮左侧向玩家界面突出"）。另：**纯色 panel_fill 与面板纹理贴图存在色阶差，面板内自绘元素能不用衬底就不用**（自带不透明底的贴图直接绘制即可）；衬底只用于面板外（悬浮在游戏世界上方）的区域成形

## 记录规范

### 附魔记录规范

> **约定：每新增一个附魔，必须在本 README 追加对应章节（编号接续「附魔总览」表的行号）。**

新增附魔时按以下格式记录（保持与现有章节一致）：

```
### N. 附魔名 (English Name)

**功能**：<一句话描述效果与数值>

#### 实现方法

**数据定义** `<json 路径>`：

- <关键字段与取值理由>

**核心逻辑** `<代码路径>`：

- <Mixin 注入点与机制>

**获取途径**：<标签/掉落表/交易等>
```

涉及通用机制变化的（新 Mixin、新标签体系、原版行为覆写）需同步更新 "通用技术模式" 和 "踩坑记录" 章节。

新增附魔的配套登记清单（除章节外逐项过）：

1. `ExtraEnchantry.java` 注册 ResourceKey + 等级读取辅助
2. `ExtraEnchantryCreativeTab.java` 的 `ENCHANTMENTS` 列表追加（自动列出全部等级附魔书）
3. **归族**：`EnchantmentMixin.FAMILY_STYLES` 选族登记（或明确留 T2），同步加入 `tags/enchantment/family_{族}.json`——字体与图鉴成就共用该划分
4. **视听反馈**：按「视听反馈」分级约定经 `FxHelper` 补齐 L1（L2/L3 视稀有度），常态效果记得 `throttle`
5. **成就**（可选）：有"首次成功使用"纪念价值的加 `advancement/usage/` JSON + `Advancements.java` 常量 + 一句 award 调用
6. `zh_cn.json` / `en_us.json` 同步翻译（附魔名 + 成就 title/description）
