# Extra Enchantry v1.1.0 中版本更新设计方案

> 基于 v1.0.1（21 附魔 + 骑兵队事件体系）的中版本规划。
> 三大内容块：**6 个新附魔**（补部位空缺）、**1 个新挑战事件**（远古城市线收束）、**一轮平衡性调整**。
> 所有实现方案沿用 v1.0.1 已验证的技术模式（数据驱动 JSON + Mixin），标 ⚠️ 处为落地前需按 26.2 反编译验证的 API 点。

---

## 一、版本定位与目标

| 项 | 内容 |
| --- | --- |
| 版本号 | 1.0.1 → **1.1.0**（中版本：内容新增 + 平衡调整，无破坏性变更） |
| 兼容 | 存档可直接升级；不改动既有附魔 ID / NBT 结构 |
| 设计原则 | ① 不堆叠同类效果，每个新附魔填补明确的部位 / 玩法空缺；② 数值与克制关系接入现有网络（破限、壁垒、断罪等）；③ 获取途径与主题绑定（远古城市 / 末地 / 海洋），不一股脑塞 non_treasure |

### 现有体系空缺分析

| 部位 / 玩法 | 现状 | 结论 |
| --- | --- | --- |
| 弓 / 弩 | 只有汲取、蚀命两个"顺带覆盖"的通用附魔 | **缺专属**，远程流派没有构筑核心 |
| 三叉戟 | 同上 | **缺专属**，引雷 / 激流 / 忠诚之外无扩展 |
| 头盔 | 仅假象（防御向） | 缺探索向补充（水下主题空缺） |
| 锄 / 农业 | 无任何附魔 | **完全空缺** |
| 事件 | 仅破限一条挑战线（深板岩 / 监守者主题只有掉落，无事件收束） | 远古城市线缺闭环 |

---

## 二、新增附魔（6 个，章节编号 22–27，按 README 记录规范追加）

### 22. 归羽 (Homing Plume)

**功能**：2 级，弓 / 弩专属。射出的箭 / 弩箭**未命中任何实体**（落地或落空消失）时，按 50% / 100% 概率自动返还到背包（返还带 1 秒飞回动画）；命中实体不返还。药箭同样返还。**与无限互斥**（经济取舍：无限 = 不耗箭但禁药箭，归羽 = 省箭 + 保留药箭）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/homing_plume.json`：

* `max_level: 2`，`weight: 4`（uncommon），`anvil_cost: 4`
* `supported_items/primary_items: "#extra-enchantry:homing_plume_supported"`（新建标签：`#minecraft:enchantable/bow` + `#minecraft:enchantable/crossbow`），`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:infinity"]`（列表写法，同假象对荆棘）
* `effects: {}` 纯 Mixin

**核心逻辑**（⚠️ 需反编译验证箭矢生命周期入口）：

* `mixin/AbstractArrowMixin.java`（新建）：拦截箭矢落地 / despawn 路径，判定 `pickup` 状态与发射者武器的归羽等级 → 概率生成一个"飞回玩家"的临时轨迹实体（复用末影之眼 / 经验球的 seek 运动模式）或直接 `player.getInventory().add()` + 动作栏提示
* 命中判定：箭矢 `onHitEntity` 置 ThreadLocal 标记，落地路径读标记短路返还
* 发射者武器快照：26.2 箭矢在 `hurtServer` 链路中已证明持有发射时武器栈（`source.getWeaponItem()`），生成时即可读归羽等级并写入箭实体字段

**获取途径**：追加进 `minecraft:non_treasure`（附魔台 / 图书管理员 / 宝箱 / 钓鱼）。

---

### 23. 坠星 (Starfall)

**功能**：单级，弩专属宝藏附魔。装填烟花火箭发射时：爆炸伤害 +4、爆炸半径 +1 格，爆炸粒子升级为"星形散射"（末地烛 + 火焰混合粒子）。与多重射击互斥（烟花流 vs 散弹流二选一）。**仅末地城宝箱可获得**（与御风并列，末地城双附魔）。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/starfall.json`：

* `max_level: 1`，`weight: 2`（rare）
* `supported_items/primary_items: "#minecraft:enchantable/crossbow"`，`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:multishot"]`
* 不加入任何获取标签 → 全途径隔离，仅专属掉落池

**核心逻辑**（⚠️ 需反编译验证烟花爆炸参数来源）：

* `mixin/FireworkRocketEntityMixin`（扩展既有类）：拦截爆炸路径读取火箭的"发射者弩"快照（26.2 火箭实体持有 owner / 发射物品信息待验证；若快照不可得，则在发射瞬间由 `CrossbowItem` 的 Mixin 把坠星标记写入火箭的自定义布尔组件，与破限书 `limit_break_source` 同模式）
* 伤害 +4 / 半径 +1：Redirect 爆炸伤害与半径的读取点，按标记放大
* 粒子：服务端 `sendParticles` 广播，无需客户端代码

**获取途径（专属掉落表）**：沿用御风方案 —— `chests/end_city_treasure.json` 第四池（rolls 1）：75% 空 / 15% 坠星附魔书 / 10% 带坠星的弩（`set_damage 0.8-1.0`）。

---

### 24. 霆霓 (Stormsurge)

**功能**：2 级，三叉戟专属。雨天 / 雷雨天 / 水中投掷命中时：额外 2 / 4 点雷电属性伤害，并连锁至 2 格内最近 1 个其他实体（连锁伤害减半）；命中点播放雷声（仅附近可闻，无真实闪电、不引燃、不转化苦力怕）。**与引雷互斥**（引雷管真实闪电，霆霓管稳定增伤）。非宝藏，uncommon。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/stormsurge.json`：

* `max_level: 2`，`weight: 5`，`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/trident"`，`slots: ["mainhand"]`
* `exclusive_set: ["minecraft:channeling"]`
* `effects: {}` 纯 Mixin

**核心逻辑**：

* `LivingEntityMixin` 的 `hurtServer` 链（挂在蚀命之后、断罪之前，与冲阵同位）：武器取 `source.getWeaponItem()`（掷出三叉戟已验证返回三叉戟本身）→ 判定 `!source.isDirect()`（仅投掷触发，近战戳刺不触发）+ 环境判定（`level.isRaining()` / 目标 `isInWaterRainOrBubble()`）
* 连锁：`ServerLevel.getEntitiesOfClass` 2 格选取（排除攻击者 / 主目标 / 友方），复用破阵的选取与防递归 ThreadLocal
* 音效：`level.playSound(null, ...)` 播放雷声（26.2 已确认此 API）；粒子用电力火花

**获取途径**：追加进 `minecraft:non_treasure`。

---

### 25. 藏锋 (Sheathed Edge)

**功能**：3 级，剑 / 斧专属（近战 weapon 标签）。脱离战斗（未造成且未承受伤害）满 5 秒后，首次近战命中额外 +2 / 4 / 6 伤害，并伴随拔刀音效与一道横向刀光粒子。触发后重新计时。与断罪不互斥 —— 藏锋加伤**参与断罪斩杀阈值结算**（与冲阵同一设计逻辑），但被壁垒上限克制。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/sheathed_edge.json`：

* `max_level: 3`，`weight: 2`（rare），`anvil_cost: 4`
* `supported_items/primary_items: "#minecraft:enchantable/sword"` + 斧 —— 走自定义标签 `sheathed_edge_supported`（sword + axe 两个 enchantable 标签聚合，⚠️ 需确认 26.2 斧的 enchantable 标签名），`slots: ["mainhand"]`
* `effects: {}` 纯 Mixin

**核心逻辑** `SheathedEdgeManager` + `LivingEntityMixin`：

* 计时：每玩家 UUID 记录 `lastCombatMs`（造成伤害或承受伤害的 hurtServer 双端点刷新）；当前时间差 ≥ 5000ms 即"藏锋就绪"
* 加伤：hurtServer HEAD 的 `@ModifyVariable`（argsOnly），**定义在蚀命之后、断罪之前**——与冲阵同一位点，数值入斩杀链
* 触发后立刻刷新 `lastCombatMs`；播放音效 + 横扫粒子（破阵同款 `SWEEP_ATTACK`）

**获取途径**：追加进 `minecraft:non_treasure`。

---

### 26. 渊息 (Tideheart)

**功能**：3 级，头盔专属宝藏附魔。水下呼吸时间每级 +15 秒（I/II/III → 30/45/60 秒总氧气），且 III 级时水下挖掘不再减速（等效水下速掘）。**与水下呼吸、水下速掘互斥**（一件头盔只做一件事）。获取：海洋系宝箱（沉船 / 埋藏的宝藏 / 海底废墟）+ 钓鱼宝藏。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/tideheart.json`：

* `max_level: 3`，`weight: 2`（rare）
* `supported_items/primary_items: "#minecraft:enchantable/head_armor"`，`slots: ["head"]`
* `exclusive_set: ["minecraft:respiration", "minecraft:aqua_affinity"]`（列表写法）

**核心逻辑**：

* 氧气上限：⚠️ 需反编译验证 26.2 氧气计算入口（`LivingEntity` 的空气供应字段与 `getMaxAirSupply`）；预计方案为 `@ModifyVariable` / `@Redirect` 在消耗判定前放大上限，活力同款"修改器"思路（瞬态属性或读取点放大）
* III 级挖掘免减速：复用拓阶 `ItemStackMixin#getDestroySpeed` 的注入点，水下惩罚分支读取头盔渊息等级 = 3 时跳过惩罚（⚠️ 需确认水下减速在 26.2 的确切分支位置）

**获取途径**：追加进 `treasure` + `on_random_loot`（钓鱼 / 宝箱），不进 `in_enchanting_table`（无附魔台），另向沉船 / 宝藏掉落表追加专属池（`LootTableEvents.MODIFY`，庇护同款事件追加法，不整表覆盖）。

---

### 27. 丰壤 (Loam)

**功能**：3 级，锄专属。徒手收获完全成熟的作物时：20% / 35% / 50% 概率双倍掉落；III 级额外获得 3×3 范围收获（仅破坏已成熟的作物，未成熟不动）。与精准采集不互斥（作物无精准场景），与时运可叠加（双倍判定独立）。非宝藏，uncommon。

#### 实现方法

**数据定义** `data/extra-enchantry/enchantment/loam.json`：

* `max_level: 3`，`weight: 5`，`anvil_cost: 2`
* `supported_items/primary_items: "#minecraft:enchantable/hoe"`（⚠️ 需确认锄 enchantable 标签在 26.2 的名称），`slots: ["mainhand"]`
* `effects: {}` 纯 Mixin

**核心逻辑**：

* 双倍掉落：fabric 事件 / `BlockBehaviourMixin` 拦截作物方块的掉落生成（⚠️ 优先用 fabric 的 block break / loot 事件，避免与拓阶的 Mixin 争用同一类）；判定作物 `isMaxAge()` 后按概率复制掉落物
* 3×3 范围收获（III）：破坏成功后在相邻 8 格扫描同种成熟作物，逐个走原版破坏流程（掉落实例各自走双倍判定）；⚠️ 需防递归标记（范围破坏不再触发范围破坏）

**获取途径**：追加进 `minecraft:non_treasure`。

---

## 三、新挑战事件：「深暗回响」(Echoes of the Deep)

> 远古城市线的收束。蚀命 / 无踪目前只有"捡了就走"，缺一个与监守者主题相称的事件闭环。
> 与「诸界浩劫」同构但**轻量**（3 波、约 10 分钟、死亡不掉落已有物品）。

### 触发

* 玩家从远古城市宝箱拾取**蚀命或无踪附魔书**时，30% 概率触发（每人 5 分钟冷却；和平难度跳过）
* 提示：幽匿尖啸音效 + 动作栏「深暗在回应……」

### 波次（深蓝色 boss 血条「深暗回响」，每波 +33%）

| 波次 | 构成 |
| --- | --- |
| 第 1 波 | 8 只僵尸（幽匿粒子视觉）+ 4 只骷髅，全部仇恨锁定触发者 |
| 第 2 波 | 4 只监守者亲卫（循声守卫主题的强化僵尸：高生命 + 黑暗效果攻击）+ 2 只幻翼 |
| 第 3 波 | **1 只监守者**（TRIGGERED 生成即钻出动画，沿用诸界浩劫同款方案）+ 6 只蠹虫 |

### 结算

* **成功**：授予进度「深暗征服者」；奖励 60 点经验球 + 3~5 个回响碎片 + 1 本**随机 I~II 级**蚀命 / 无踪附魔书（挑战掉落版，与宝箱版同 ID）
* **失败**（死亡或 15 分钟超时）：生物消散，**无惩罚**（不销毁附魔书 —— 与诸界浩劫的高风险高回报刻意区分，这是"中风险中回报"事件）

### 实现要点

* 触发入口复用破限书的 `PlayerMixin#addItem` + `InventoryMixin` 双钩子（同一游戏刻去重模式照搬）
* 仇恨锁定 / 血条 / 波次推进 / 离线冻结：全部复用 `CataclysmManager` 的既有框架，抽象出公共的 `WaveChallenge` 基类（本次顺带重构，诸界浩劫迁移到基类上）
* 监守者钻出：`EntitySpawnReason.TRIGGERED`（已验证，零成本）

---

## 四、平衡性调整（现有附魔）

| 附魔 | 现状 | 调整 | 理由 |
| --- | --- | --- | --- |
| 破阵 Cleave | III 级溅射 70% 主伤害，最多 3 目标 | 溅射比例 50/60/70% → **45/55/65%**；III 级目标数 3 → 3（不变） | 70%×3 目标的群体 DPS 已超横扫之刃太多，挤压了"互斥二选一"的决策空间 |
| 假象 Decoy | III 级冷却 15 秒、55% 触发、双诱饵 | 冷却 **15s → 18s**；触发率 55% → **50%** | PvP 中双诱饵 + 高触发的脱战频率过高；PvE 影响可忽略 |
| 御风 Windrider | III 级烟花推进 +75% | +75% → **+60%** | 配合烟花弩接力基本等于无限加速，末地移动成本归零 |
| 断罪 Judgement | boss 伤害 ×2 | ×2 → **×1.75** | ×2 叠加蚀命百分比后 boss 战节奏崩坏；保留"对 boss 仍是最优解"的定位 |
| 触及 Reach | 破限后 XI 级（+5.5 格） | **数值不变**，但 XI 级的铁砧经验成本从固定 5 级 → **15 级**（破限费用特表） | +5.5 格在 PvP 是质变，5 级经验过于廉价；不动机制只动价格 |
| 汲取 Siphon | 近战每级 2 HP（III 级 6 HP/击） | **不变**（本周期观察项） | 配合高攻速武器的回血速率待观察，先不动刀 |
| 疾风 Gale | 潜行不生效 | **不变** | 设计意图明确，保留 |
| 余烬 Emberfall | 60 秒虚弱（缓慢 I + 挖掘疲劳 I） | **不变**（本周期观察项） | 金胸甲两次免死的上限（112 耐久 = 2 次）已是天然制约 |

### 克制关系网络更新（新增边）

```
藏锋 ─加伤参与→ 断罪斩杀阈值 ─被钳制于→ 壁垒上限
霆霓 ─互斥→ 引雷          归羽 ─互斥→ 无限
坠星 ─互斥→ 多重射击      渊息 ─互斥→ 水下呼吸 / 水下速掘
```

---

## 五、技术实施要点

### 复用清单（v1.0.1 已验证模式直接沿用）

| 需求 | 复用方案 |
| --- | --- |
| 限级获取 | cost 曲线 + `EnchantRandomlyFunctionMixin` 钳制 + AnvilMenu TAIL 降级（三路封锁模式） |
| 自定义组件标记 | 破限书 `limit_break_source` 同款布尔组件（归羽 / 坠星的发射快照） |
| 掉落表追加 | `LootTableEvents.MODIFY` 事件追加（庇护方案），优于整表覆盖 |
| 互斥 | `exclusive_set` 列表写法 + 追加原版互斥标签 |
| hurtServer 加伤链 | 定义顺序：蚀命 → 冲阵 / 藏锋 / 霆霓 → 断罪（同方法内链式执行） |
| 波次挑战 | 重构 `CataclysmManager` → `WaveChallenge` 基类，深暗回响与诸界浩劫共用 |
| 创造物品栏 | `ExtraEnchantryCreativeTab` 的 `ENCHANTMENTS` 列表追加 6 项 |

### ⚠️ 落地前需反编译验证的 API 点（按 mc-fabric-mod-api-verify 流程）

1. 箭矢落地 / despawn / onHit 的方法名与签名（归羽）
2. 烟花火箭实体的发射者 / 发射物品快照可得性（坠星）
3. 26.2 氧气上限的计算入口（渊息）
4. 水下挖掘减速在 `getDestroySpeed` 链中的确切分支（渊息 III）
5. 斧 / 锄的 enchantable 标签名（藏锋 / 丰壤的 supported 标签）
6. 作物成熟判定 `isMaxAge()` 在 26.2 的等价 API（丰壤）

### README 维护

* 按"后续附魔记录规范"追加第 22–27 节 + 深暗回响事件章节
* 同步更新：附魔总览表、目录、Mixin 清单表（新增 AbstractArrowMixin / SheathedEdgeManager 等）、踩坑记录（新验证的 API 点）

---

## 六、里程碑

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| **1.1.0-alpha** | 6 个新附魔（数据 JSON + Mixin）+ 创造物品栏 | 单人生存全流程可获得、可生效；⚠️ 验证清单全部闭环 |
| **1.1.0-beta** | 「深暗回响」事件 + `WaveChallenge` 重构 + 平衡调整 | 事件三波完整跑通；诸界浩劫回归测试通过 |
| **1.1.0-rc** | 本地化（中 / 英）、README 章节补齐、平衡复测 | 附魔总览表 27 行完整；无已知 bug |

**明确不做（本版本）**：新维度 / 新方块 / 新实体注册（贴图与注册成本高，留给大版本）；附魔数值配置文件化（待社区反馈量上来再说）。
