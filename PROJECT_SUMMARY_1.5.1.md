# AccountKeeper 项目功能总结（v1.5.1）

> 本文档基于当前代码（v1.5.1 / versionCode 20）实测分析生成，覆盖项目全部功能模块。

## 1. 项目概述

AccountKeeper 是一款简洁易用的**个人财务管理 Android 应用**，提供完整的收支记账、资产管理、统计分析、第三方账单导入、多重备份与 OTA 更新等功能。

| 项目 | 说明 |
|------|------|
| 应用名称 | AccountKeeper |
| 当前版本 | v1.5.1（versionCode 20） |
| 最低 SDK | 26（Android 8.0） |
| 目标/编译 SDK | 35（Android 15） |
| 数据库版本 | 10（含 1→10 迁移链） |
| 架构模式 | MVVM + 分层架构 |

## 2. 技术栈

### 语言与构建
- **Kotlin 1.9.x**，Java 11 兼容
- **Gradle 8.x + Kotlin DSL**，KSP 注解处理器

### 核心框架
- **Jetpack Compose**（Material 3 + Material Icons Extended + Material3 Adaptive NavigationSuite，BOM 2024.x）
- **Room 2.6.x** — 本地数据库 ORM
- **Hilt 2.48.x** — 依赖注入
- **Navigation Compose 2.7.x** — 类型安全导航（kotlinx.serialization 路由）
- **DataStore Preferences** — 轻量级设置存储
- **Paging 3** — 交易列表分页加载（每页 30 条，预取 15）
- **Apache POI 5.2.5** — Excel 账单解析
- **WorkManager + Hilt Work** — 定时备份任务
- **Kotlinx Serialization / Parcelize** — 序列化

### 权限
`INTERNET`、`REQUEST_INSTALL_PACKAGES`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_DATA_SYNC`、`POST_NOTIFICATIONS`、`DOWNLOAD_WITHOUT_NOTIFICATION`

## 3. 项目结构

```
app/src/main/java/com/example/accountkeeper/
├── AccountKeeperApp.kt          # 应用入口（@HiltAndroidApp）
├── MainActivity.kt              # 主 Activity（底部导航 + 主题/语言/货币注入）
├── data/
│   ├── local/                   # Room 数据库（AppDatabase、3 个 DAO、Converters）
│   ├── model/                   # 数据模型（Transaction/Asset/Category/Attachment/Enums）
│   └── repository/              # 数据仓库（4 个）
├── di/DatabaseModule.kt         # Hilt 模块（数据库 + DAO + 迁移注册）
├── ui/
│   ├── navigation/              # 类型安全路由定义
│   ├── screens/                 # 13 个界面
│   ├── theme/                   # Color / Theme / Type / AppStrings（中英文字符串）
│   └── viewmodel/               # 5 个 ViewModel
├── utils/                       # BackupManager / BillParser / CurrencyUtils / FileConverter / IdGenerator / UpdateManager / UpdateDownloadService
└── worker/BackupWorker.kt       # WorkManager 定时备份任务
```

## 4. 核心功能模块

### 4.1 首页（HomeScreen）

**功能特性**：
- **月度收支卡片**：只显示本月收入、支出和结余；金额默认隐藏，点击卡片或眼睛按钮临时显示，离开首页后恢复隐藏
- **交易列表**：按日期分组，Paging 3 分页加载，每项显示分类、备注、时间、金额
- **搜索入口**：点击顶部搜索图标展开搜索栏，输入关键词跳转搜索结果页
- **点击编辑**：点击交易卡片进入编辑页
- **滑动删除**：左滑超过 30% 露出红色删除背景；行为受「滑动删除确认」设置控制：
  - 确认开启：滑过阈值直接弹出确认对话框，卡片回弹
  - 确认关闭：卡片滑开 70% 露出删除按钮，点击后弹确认框
- **批量操作**：长按卡片进入多选模式，顶部显示已选数量，支持批量删除、单选编辑

### 4.2 添加/编辑交易（AddEditTransactionScreen）

**功能特性**：
- **金额输入**：大号数字键盘，货币符号前缀，自动对焦动画
- **收支切换**：支出/收入渐变分段选择器，切换时清空已选分类
- **日期选择**：Material 3 日期选择器，默认当前日期
- **分类网格**：3 列网格，按当前交易类型过滤，选中卡片放大高亮
- **备注输入**：多行文本
- **校验**：金额必须大于 0，否则不保存

### 4.3 统计分析（StatisticsScreen）

**时间范围**：日 / 周 / 月 / 年 / 自定义（自定义可分别选起止日期）

**统计类型**：支出 / 收入 / 综合对比

**数据展示**：
- 渐变总金额卡片（正确处理负数余额）
- Canvas 环形饼图（Top 8 分类）+ 图例（色点、名称、占比）
- 分类排行榜（色点 + 金额 + 百分比 + 进度条）
- **分类钻取**：点击分类排行项跳转该分类在时间窗口内的全部交易（综合模式不可点击）

### 4.4 资产管理（AssetsScreen）

**功能特性**：
- **资产台账类型**：按实物资产、虚拟资产和用户自定义资产类型汇总；类型可新增、改名或迁移记录后删除
- **当前资产总览**：
  - 无筛选：净资产、实物资产、虚拟资产、资产合计和负债
- **类型维度列表**：按资产类型展开查看当前记录、有效金额与记录数量
  - 有分类筛选：该分类总额、进行中金额、拥有/未拥有金额
- **资产列表**：按日期分组，状态颜色标识（拥有=绿、未拥有=灰、进行中=黄）
- **搜索**：按备注、目标对象、目标账户、分类名、状态名过滤
- **筛选对话框**：日期范围 + 分类（正/负资产徽标）
- **排序对话框**：时间↓/↑、金额↓/↑
- **滑动操作**：左滑露出删除（红），右滑露出状态切换（绿），行为遵循「滑动删除确认」设置
- **批量操作**：长按多选、批量删除
- **状态循环**：点击分类图标切换状态（状态机见 4.6）

**核心计算模型**（AssetViewModel，应用会计语义核心）：
- **正资产分类**（如借出）：`IN_PROGRESS`=钱已出去（不算拥有）；`COMPLETED`=收回（算拥有）；`OWNED`=确定拥有
- **负资产分类**（如借入）：`IN_PROGRESS`=钱在手里（算拥有）；`COMPLETED`=已还（不算）；`OWNED`=确定拥有
- **净资产** = 正资产金额 + 交易余额 − 总负债

### 4.5 预算管理（BudgetScreen）
- 底部主入口，按月设置总支出预算和支出分类预算；新月份首次进入时复制上月预算。
- 实际支出直接从交易表按月份和分类实时聚合；收入和资产记录不计入预算。
- 显示额度、已支出、剩余、进度和超支状态；超支仅提示，不阻止交易保存。
- 未分类支出只计入月度总预算；点击分类预算可查看该月分类交易。

### 4.6 添加/编辑资产（AddEditAssetScreen）

**功能特性**：
- **状态选择**：None / 拥有或未拥有（依分类正负决定）/ 进行中 三段选择器，顶部实时状态徽章
- **日期 / 金额输入**：金额正则校验（`^\d*\.?\d*$`），无效时红框提示
- **分类下拉**：ExposedDropdownMenu，资产类分类带「正/负资产」绿红徽标；空态提示先添加资产分类
- **附加信息**：目标对象、目标账户、备注
- **附件上传**：系统文档选择器支持图片 / Excel（.xls/.xlsx）/ CSV，文件复制进内部存储 `attachments` 目录，生成 Attachment 记录；附件列表显示类型图标、文件名、大小，可移除
- **校验**：金额 > 0 且必须选择分类

### 4.7 资产状态机（AssetViewModel.toggleAssetStatus）

- 正资产分类：`OWNED ↔ IN_PROGRESS`（默认 OWNED）
- 负资产分类：`NOT_OWNED ↔ IN_PROGRESS`（默认 NOT_OWNED）
- 旧数据迁移：`TEMPORARILY_WITH_OTHERS`→IN_PROGRESS（正）/NOT_OWNED（负）；`TEMPORARILY_WITH_ME`→OWNED（正）/IN_PROGRESS（负）

### 4.7 搜索结果页（SearchResultScreen）

- 关键词搜索交易（按备注 + 分类名，SQL LIKE 模糊匹配），Paging 3 分页
- **筛选**：类型（全部/收入/支出）+ 日期范围，活动筛选以可移除 Chip 展示
- **排序**：时间/金额 升降序
- **汇总卡片**：收入/支出总额
- 交易卡片：点击编辑、长按多选、左滑删除

### 4.8 分类交易明细页（CategoryTransactionsScreen）

- 由统计页分类排行钻取进入，展示某分类在指定时间窗口内的全部交易
- Paging 3 分页，按日期分组，只读视图（不可编辑/删除）
- 顶部渐变汇总卡片（该窗口收入/支出总额）

### 4.9 设置（SettingsScreen / AppSettingsScreen）

**快捷设置卡片**（首页设置）：
- 深色模式开关
- 语言切换（中文 ⇄ English）
- 货币循环切换

**通用设置页**：
- 主题：深色 / 浅色（默认深色）
- 语言：中 / 英（对话框选择）
- 货币符号：¥、$、€、£、₩、₹、₽、฿ 共 8 种
- **滑动删除确认**开关（控制左滑是否直接弹确认框）

### 4.10 分类管理（CategorySettingsScreen）

- **三类标签页**：支出 / 收入 / 资产
- **增删改**：添加（重名校验）、重命名、删除（确认对话框）
- **正负资产切换**：资产分类显示「正资产/负资产」SuggestionChip，一键切换
- **默认分类**：显示「默认」徽标
- **种子初始化**（CategoryViewModel 启动时）：去重 + 自动补齐 31 个中文默认分类：
  - 15 个支出（餐饮美食、交通出行、服饰装扮、日用百货、休闲娱乐、文化教育、运动健康、美容美发、住房物业、水电煤气、数码电器、宠物花草、汽车飞机、家庭开支、转出）
  - 7 个收入（职业薪金、投资理财、兼职外快、红包礼金、二手闲置、退款报销、转入）
  - 6 个正资产（借出、应收款、预付款、押金、代付款、投资债权）
  - 6 个负资产（借入、应付款、欠款、信用卡、贷款、分期付款）
- **去重逻辑**：按「名称+类型」分组，保留默认项，其余交易迁移至保留分类后删除

### 4.11 数据管理（DataManagementScreen）

**ZIP 数据管理**：
- **导入 ZIP**：读取备份、自动创建缺失分类、附件复制入内部存储、ID 重复跳过、清理临时文件
- **导出 ZIP**：全量账本（交易 + 资产 + 附件），文件名 `AccountKeeper_Export_yyyyMMdd.zip`

**第三方账单导入**：
- 微信 / 支付宝账单导入按钮（Excel / CSV）
- 自动识别账单类型、智能解析、自动建分类、去重导入、保存原始账单文件
- 导入后显示被排除的退款笔数
- **已导入账单管理**：分组（微信/支付宝/其他），支持重新导入、删除

**本地备份保险库**：
- **自动备份开关**：每次增删改自动触发备份；关闭时若存在备份链会弹确认（保留/删除备份链）
- **定时备份开关**：WorkManager 周期任务
- **备份间隔**：6 / 12 / 24 / 48 / 72 小时
- **保留数量**：滑块 5–50
- **手动备份**：自定义命名创建 ZIP 备份
- **备份保险库**：查看增量链（基准 + 各步骤）+ 手动 ZIP 备份，支持恢复到指定步骤、删除备份

**清空数据**：单独清空交易 / 单独清空资产 / 一键清空全部（含附件警告详情）

### 4.12 导入导出（ImportExportScreen，旧 CSV 界面）

> 与数据管理页并存的旧版界面，目前从设置主界面不可直接到达。

- CSV 导入（两遍合并：建分类 → 插入，ID 冲突跳过，兼容中文表头、4 种日期格式）
- CSV 导出（`ID,Date,Type,Amount,Category,Note`）
- 微信/支付宝账单导入与账单文件管理（同上）
- 本地备份管理（自动备份开关、保留滑块、手动备份、备份保险库、清空备份）
- 应用设置内联项 + 关于入口 + 危险区（清空所有交易）

### 4.13 关于页（AboutScreen）

- 应用图标（AK 字母徽标）、名称、运行时版本号（动态绑定）
- **检查更新**：OTA 更新入口（见第 5 节）
- **帮助教程**：全屏对话框，含项目介绍、首页/统计/资产/数据管理/分类管理/个性化/技巧/FAQ
- **用户反馈**：问题描述（必填）+ 联系方式，一键发送邮件至 rickymiao63@163.com（主题 `[AccountKeeper Feedback]`）
- **GitHub 链接** / **联系方式**（mailto）/ 致谢卡片

## 5. OTA 更新系统

- **双数据源**：优先 GitHub Releases API，失败自动回退 Gitee API
- **版本比较**：语义化版本号比较，从 tag 提取版本号与版本码
- **下载**：前台服务（UpdateDownloadService）下载 APK，支持重定向跟随、进度通知、可取消
- **进度广播**：下载进度/完成/失败通过广播通知 UpdateViewModel 更新 UI
- **安装**：FileProvider 授权安装包，通知点击直接安装
- **UI**：检查更新（加载中/有新版本/已是最新/错误状态）、下载进度条、取消下载、立即安装，下载中禁止关闭对话框

## 6. 备份系统详解

### 6.1 备份类型
| 类型 | 格式 | 内容 | 触发方式 |
|------|------|------|----------|
| CSV 备份 | `.csv` | 交易记录 | 手动 / 每次增删改（旧系统） |
| ZIP 全量备份 | `.zip` | 交易 CSV + 资产 JSON + 附件文件 | 手动 / 自动 |
| 增量备份链 | 基准 ZIP + Delta JSON | 基准全量 + 各步骤变更集 | 每次增删改（自动备份开启时） |
| 定时备份 | ZIP | 全量 | WorkManager 周期任务 |

### 6.2 自动备份机制
- 交易与资产的所有写操作（增/删/改/批量/清空）后调用 `triggerAutoBackup()`：
  1. 读取设置，仅当自动备份开启时执行
  2. 快照全部交易、资产、分类
  3. 无备份链 → 创建基准备份；有备份链 → 恢复最新快照作为「上一状态」，对比生成增量备份（记录新增/修改/删除），按保留上限清理旧增量
- 增量备份支持**恢复到任意步骤**（0=基准，-1=最新）

## 7. 数据模型与数据库

### 7.1 表结构（Room 数据库版本 10）

**transactions**：`id`(PK)、`type`、`amount`、`date`、`categoryId`(FK→categories, SET_NULL)、`note`、`source`（手动/支付宝/微信）

**assets**：`id`(PK)、`date`、`amount`、`status`、`categoryId`(FK, SET_NULL)、`assetTypeId`(FK→asset_types, RESTRICT)、`targetPerson`、`targetAccount`、`note`、`isCompleted`、`attachments`(JSON 字符串)、`createdAt`、`updatedAt`

**asset_types**：`id`(PK)、`name`、`createdAt`、`updatedAt`；首次安装预置实物资产与虚拟资产。

**budgets**：`id`(PK)、`monthKey`、`categoryId`(可空，FK→categories, RESTRICT)、`amount`、`createdAt`、`updatedAt`；每月总预算和分类预算均保证唯一。

**budget_months**：`monthKey`(PK)、`initializedAt`；用于标记月度预算已初始化，避免重复复制。

**categories**：`id`(PK 自增)、`name`、`type`（收入/支出/资产）、`isDefault`、`isPositiveAsset`（资产分类正/负标识）

**settings**：DataStore Preferences（非 Room）— 主题、语言、货币、自动备份、保留数、滑动确认、定时备份、间隔

### 7.2 枚举
```kotlin
enum class TransactionType { INCOME, EXPENSE, ASSET }
enum class TransactionSource { MANUAL, ALIPAY, WECHAT }
enum class AssetStatus { NONE, OWNED, NOT_OWNED, IN_PROGRESS,
                         TEMPORARILY_WITH_ME, TEMPORARILY_WITH_OTHERS }
enum class AttachmentType { IMAGE, EXCEL, CSV, OTHER }
```

### 7.3 迁移历史
- 1→2：重建 transactions 表（移除 AUTOINCREMENT）
- 2→3：新建 assets 表
- 3→4：categories 增加 `isPositiveAsset` 列
- 4→5：assets 增加 `attachments` 列
- 5→6：新建资产类型与预算表，旧资产默认迁移至虚拟资产
- 6→10：补充预算唯一性、月度初始化表、分类外键约束和月度总预算保护触发器

### 7.4 关键查询
- 交易：全部/日期区间/按分类+时间窗口/关键词搜索（联表分类名）/分页
- 资产：全部/日期区间/按状态/关键词搜索/按状态合计（排除已完成）
- 分类删除保护：分类删除时交易/资产外键 SET_NULL，自动去重时批量迁移到保留分类

## 8. 工具类

| 工具 | 职责 |
|------|------|
| `BackupManager` | 备份文件管理、CSV/ZIP 读写、增量链管理、附件复制、账单文件保存/去重/删除、导入导出 |
| `BillParser` | 微信/支付宝账单解析（Excel 11 列 / CSV 8 列 / 支付宝新旧格式），智能分类映射，退款配对（金额+7天内+商家关键词），排除已全额退款交易 |
| `FileConverter` | Excel（POI）→ 行文本、CSV 读取（GBK 优先/UTF-8 回退，中文乱码检测，BOM 清理） |
| `CurrencyUtils` | 货币显示换算（以 ¥ 为基准，内置固定汇率表，仅展示层） |
| `IdGenerator` | 18 位时间戳+计数器 唯一 ID（时间可排序） |
| `UpdateManager` | 版本检查（GitHub/Gitee）、APK 下载、安装、缓存清理 |

## 9. 国际化与主题

- **语言**：中文（默认）/ English，运行时切换（重启生效），AppStrings 数据类管理
- **主题**：深色（默认）/ 浅色，Material 3 动态色，跟随应用设置
- **货币**：8 种符号全局注入（CompositionLocal），所有金额显示统一换算

## 10. 导航结构

```
底部导航（NavigationSuiteScaffold）：
├── 首页  ──→ 添加/编辑交易 ──→ 搜索结果
├── 统计  ──→ 分类交易明细
├── 资产  ──→ 添加/编辑资产
└── 设置  ──→ 数据管理 / 通用设置 / 分类管理 / 关于（更新、帮助、反馈）
```

类型安全路由（`@Serializable`）+ 子路由自动高亮父级导航项。

## 11. 已知限制与路线图

### 当前限制
- 不支持云同步 / 备份文件未加密 / 无生物识别锁
- 仅支持微信、支付宝账单格式（PDF 解析已预留未实现）
- 附件仅本地存储
- 货币汇率为内置固定表（非实时）

### 路线图（参考 TODO.md）
- **1.5.x**：按时间查询账单、更多第三方账单格式、图表增强、预算管理
- **1.6.0**：跨平台（React Native/Flutter）+ iOS、远程 OTA、云端同步
- **1.7.0**：文本/图片（OCR）/音频导入账单
- **1.8.0**：账号体系、共同账户、家庭账本模式
- **2.0.0**：AI 助手、智能分类建议、消费习惯分析

## 12. 开发指南

```bash
# 构建 Debug
./gradlew assembleDebug
# 构建 Release
./gradlew assembleRelease
# 安装到设备
./gradlew installDebug
```

---

*本文档生成于 2026-08-20，基于仓库代码实测分析。*
