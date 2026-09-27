<div align="center">

![AccountKeeper Logo](ak.png)

# AccountKeeper

### 一款简洁易用的个人财务管理 Android 应用

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5-blue.svg)](https://developer.android.com/jetpack/compose)

[English](#english) | [简体中文](#简体中文)

</div>

---

## 简体中文

### 特性

- **完整记账功能** - 记录收入和支出，支持自定义分类和备注
- **资产管理** - 追踪资产和负债，管理借贷记录，支持附件，多种状态
- **智能统计分析** - 多维度数据展示，图表可视化，分类筛选
- **账单自动导入** - 支持微信和支付宝账单（Excel/CSV）导入，智能退款处理
- **搜索功能** - 关键词搜索快速定位交易记录
- **ZIP 全量备份** - 支持交易、资产和附件的完整备份与恢复
- **多重数据备份** - ZIP 备份 + 自动备份，数据安全无忧
- **用户反馈** - 内置反馈功能，一键发送邮件反馈问题或建议
- **现代化界面** - Material 3 设计，支持深色模式
- **国际化支持** - 中英文切换，多种货币符号
- **流畅交互** - 滑动删除、批量操作、分页加载

### 更新日志

#### v1.5.1 (最新版本)
- 新增用户反馈功能，支持邮件反馈
- 修复账单文件列表无法滚动的问题
- 优化账单列表布局，更加紧凑

#### v1.1.21
- ZIP 全量备份功能，支持交易、资产和附件
- 数据管理界面优化，新增独立删除按钮
- 账单重新导入功能
- 关于界面版本号动态绑定
- 全局页面布局优化，紧凑美观
- 帮助教程内容完善

#### v1.1.20
- 新增资产管理功能，支持资产/负债追踪
- 新增首页搜索功能，快速定位交易
- 新增统计页面分类筛选，点击排行榜查看详情
- 完善微信/支付宝账单导入，支持 Excel 格式
- 分页加载优化，大数据量性能提升
- 界面布局优化

#### v1.0.0
- 基础收支记录功能
- 统计分析功能
- 分类管理
- CSV 导入导出
- 本地备份系统
- 主题切换、国际化支持

### 截图

<div align="center">
  <img src="https://via.placeholder.com/200x400/1f77b4/ffffff?text=Home" width="200" />
  <img src="https://via.placeholder.com/200x400/ff7f0e/ffffff?text=Statistics" width="200" />
  <img src="https://via.placeholder.com/200x400/2ca02c/ffffff?text=Assets" width="200" />
  <img src="https://via.placeholder.com/200x400/d62728/ffffff?text=Settings" width="200" />
</div>

### 快速开始

#### 下载安装

从 [Releases](https://github.com/miaotenone/AccountKeeper/releases) 下载最新的 APK 文件安装。

#### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/miaotenone/AccountKeeper.git
cd AccountKeeper

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

### 使用指南

#### 基础操作

| 操作 | 方法 |
|------|------|
| 添加交易 | 点击首页右下角 + 按钮 |
| 编辑交易 | 点击交易卡片 |
| 删除交易 | 向左滑动交易卡片 |
| 批量操作 | 长按交易卡片进入选择模式 |
| 搜索记录 | 点击首页搜索图标，输入关键词 |

#### 数据管理

- **ZIP 导出** - 设置 → 数据管理 → 导出全量账本（含交易、资产、附件）
- **ZIP 导入** - 设置 → 数据管理 → 导入 ZIP 备份文件
- **账单导入** - 设置 → 数据管理 → 导入微信/支付宝账单（支持 Excel 和 CSV）
- **账单管理** - 可查看已导入账单，支持重新导入和删除
- **自动备份** - 设置 → 数据管理 → 开启本地自动备份
- **清除数据** - 支持单独清除交易或资产记录

#### 资产管理

- **添加资产** - 资产页面点击 + 按钮，记录借贷信息
- **资产状态** - 支持进行中、已完成、已取消三种状态
- **附件支持** - 可添加图片等附件作为凭证
- **资产分类** - 支持借出（正资产）和借入（负资产）

详细教程请查看应用内的帮助页面（设置 → 关于 → 帮助教程）。


### 详细使用手册

#### 1. 首次使用

安装并打开应用后，首页用于记录日常收入和支出，底部导航可进入首页、统计、资产和设置。应用数据默认保存在本机，不会自动上传云端。首次使用建议先进入“设置 → 分类配置”，检查收入、支出和资产分类，再根据需要调整主题、语言和货币符号。

#### 2. 记录和管理交易

1. 在首页点击右下角“+”，选择收入或支出，输入金额、日期、分类和备注后保存。
2. 点击交易卡片可以编辑记录；向左滑动可以删除，删除前会按设置决定是否需要确认。
3. 长按交易卡片进入多选模式，可批量选择并删除；顶部的搜索入口可按备注或分类快速查找。
4. 首页可切换本月、今年或全部时间范围，也可以使用日期、分类和排序筛选。统计页面提供时间范围、收入/支出类型、分类排行和趋势图；点击分类排行可查看该分类的交易。
5. 金额必须大于零并选择分类。导入账单产生的交易会标记为微信或支付宝来源，重新导入相同文件不会重复写入已有交易 ID。

#### 3. 资产和负债

资产页中的分类只有两个方向：正资产和负资产。正资产表示可计入资产的项目，负资产表示债务或负债。默认分类包括现金存款、投资资产、固定资产、应收/借出、押金/预付款、信用卡、贷款/分期、借入/应付和其他负债。

添加资产时填写名称、金额、日期、方向和分类，可在详情区域填写分类字段，也可以使用顶部“添加附件”保存图片或文档凭证。资产状态按“未选择 → 进行中 → 确定拥有 → 确定失去 → 未选择”循环：只有“确定拥有”会参与统计，确定失去、进行中和未选择均按零计算。资产统计公式为“净资产 = 资产 - 总负债”，负资产金额在总负债中按正数显示。

采购审批通过后会生成一条进行中的资产。编辑这条资产时，在来源审批区域选择符合条件的支出交易：交易类型为支出、费用分类和金额一致、交易日期不早于采购日期，且没有被其他资产占用。选择交易只建立关联，不会立即变为确定拥有；保存后仍需执行确认采购完成。普通资产无需来源审批交易即可确定拥有。已经关联交易的资产变为确定失去时仍保留交易和审批关联，便于审计追溯。

#### 4. 审批中心和采购申请

在审批中心可以新建采购申请，填写提交日期、可编辑的采购日期、事由、物品名称、规格、数量、费用分类、资产分类和审批详情字段。提交日期由系统在首次提交时记录，重新提交不会改写原提交日期；审批详情同时展示提交日期和采购日期。

待审批申请可由审批人查看详情并填写审批意见。历史页支持搜索申请编号、事由、物品名称、规格和审批意见，并可按开始日期、结束日期、费用分类、资产分类、审批类型和审批状态组合筛选。历史审批可以左滑删除，也可以长按进入多选删除；有关联资产时，只有确定失去的资产允许解除来源审批后删除，其他状态会拒绝删除。删除审批不会删除关联交易、资产或资产附件。

#### 5. 微信账单：如何获取和导入

1. 打开微信，进入“我 → 服务”（旧版可能显示“支付”）→ “钱包” → “账单”。
2. 在账单页点击右上角“…”并进入账单下载、账单导出或类似入口。微信不同版本的名称可能略有差异。
3. 选择需要的时间范围，按微信页面要求填写账单用途和接收邮箱并提交申请。下载邮件中的压缩包到手机或电脑后先解压。
4. 保留微信生成的原始 `.csv`、`.xls` 或 `.xlsx` 文件，不要用表格软件改写表头、列顺序或文件内容。
5. 在 AccountKeeper 中进入“设置 → 数据管理 → 第三方账单导入 → 导入微信账单”，选择解压后的原始文件，等待导入结果提示。

当前微信解析器支持常见表头“交易时间、交易类型、交易对方、商品、收/支、金额(元)、支付方式、当前状态、交易单号、商户单号、备注”，也兼容精简 CSV 表头。金额必须是有效正数，充值、提现等“不计收支”或无法识别的行不会作为普通交易导入。已全额退款的原支出会排除；可以配对的退款和原交易会按退款规则成对排除，结果提示会显示排除数量。

#### 6. 支付宝账单：如何获取和导入

1. 打开支付宝，进入“我的 → 账单”，点击右上角“…”或账单下载/交易流水证明入口。
2. 选择时间范围并按页面要求填写接收邮箱，提交后下载邮件中的压缩包并解压。
3. 选择平台生成的原始 `.csv`、`.xls` 或 `.xlsx` 文件。不要删除说明行、修改表头或另存为其他格式。
4. 在 AccountKeeper 中进入“设置 → 数据管理 → 第三方账单导入 → 导入支付宝账单”，选择文件并等待结果。

当前支付宝解析器兼容常见 12 列新格式（交易时间、交易分类、交易对方、对方账号、商品说明、收/支、金额、收/付款方式、交易状态、交易订单号、商家订单号、备注）、8 列格式以及旧版 7 列以上格式。交易关闭、不计收支、金额为零或无法识别的行会被跳过。CSV 会自动尝试常见中文编码；如果出现乱码，请重新选择平台原始文件。

#### 7. 账单文件管理和重复处理

导入成功后，原始账单文件会保存到“第三方账单文件管理”，可查看类型、大小和时间，也可重新解析或删除文件。应用按文件摘要避免同一个账单文件重复保存，并按稳定交易 ID 避免交易重复导入。删除账单文件只删除保存的原始文件及其索引，不会自动删除已经写入的交易；如需删除交易，请到首页单独操作。重新导入前应确认时间范围没有和已有账单重叠。

#### 8. 分类配置和详情字段

在“设置 → 数据管理 → 分类配置”中管理收入、支出和资产分类。默认分类不能随意删除；自定义分类可以按页面提供的编辑和删除操作管理。资产分类可切换正资产/负资产方向。

选中资产分类后进入“编辑字段”，可以新增 TEXT、NUMBER、DATE、BOOLEAN 和 SINGLE_CHOICE 字段，设置字段名称、类型、选项、排序和是否必填。默认分类带有常用详情模板，自定义分类会补齐通用模板；系统按分类和字段名去重，不会覆盖已有字段。内置字段名称在中文界面固定使用中文，字段类型和操作按钮会随中英文界面切换；自定义字段名称和选项保留原始输入。

#### 9. 附件

交易、资产和审批详情可在附件区域添加图片或文档。附件列表为空时“暂无附件”只是状态显示，添加入口位于顶部“添加附件”按钮。点击已有附件可预览，删除记录时会同时清理对应的附件文件。账单原始文件由账单文件管理单独维护。

#### 10. 全量归档和恢复

进入“设置 → 数据管理 → 全量数据归档”。导出 ZIP 会保存交易分类、资产分类、审批、交易、资产、详情字段和值、附件、账单文件以及必要设置，是跨设备迁移和灾备的推荐方式。导入 ZIP 是覆盖式恢复，会先清理当前业务数据，再按外键依赖顺序写入；备份文件本身不会被清理。

导入前不要打开或修改 ZIP 内的 `data.json`、附件文件和账单文件。若文件缺失、数据引用无效或校验失败，应用应提示失败原因并恢复导入前的保护快照。恢复后请核对交易、资产、审批、分类、字段和值的数量，以及附件和账单文件是否存在。

#### 11. 本地自动备份、定时备份和备份管理柜

本地自动备份按数据变更次数生成完整 ZIP；“每 N 次变更备份一次”用于设置触发阈值，默认值为 20。定时备份由独立的定时任务按天数运行，两个开关互不代替。备份保留份数只限制完整本地备份数量，超出时删除最旧备份。

在本地备份管理柜可以查看自动和手动备份，选择“还原此备份”后确认恢复影响。自动备份和手动全量归档使用同一套完整恢复逻辑，恢复包含字段、分类、审批、资产、交易、附件和账单文件。删除全部业务数据会同时清理数据库记录和业务附件、账单文件，但保留备份 ZIP 和保护快照目录中的文件；执行前务必先导出一份独立归档。

#### 12. 个性化和常见问题

在“设置 → 个性化”切换深色/浅色主题、中文/English 和货币符号。语言和部分显示设置按应用提示生效，切换后请重新打开相关页面检查文案。

账单导入为空时，优先检查文件是否已解压、扩展名是否为 CSV/XLS/XLSX、表头是否完整、文件是否被表格软件改写，以及时间范围内是否只有不计收支或已退款记录。ZIP 恢复前请保留保护备份；删除交易、资产或审批前请确认，因为普通删除操作不能直接撤销。
### 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.x | 开发语言 |
| Jetpack Compose | 1.5.x | UI 框架 |
| Material 3 | - | 设计规范 |
| Room | 2.6.x | 数据库 ORM |
| Hilt | 2.48.x | 依赖注入 |
| Navigation | 2.7.x | 导航组件 |
| DataStore | - | 轻量级存储 |
| Paging 3 | - | 分页加载 |
| Apache POI | 5.2.x | Excel 解析 |
| KSP | - | 注解处理器 |

### 项目结构

```
app/
├── data/              # 数据层
│   ├── local/        # 本地数据库
│   ├── model/        # 数据模型
│   └── repository/   # 数据仓库
├── di/               # 依赖注入
├── ui/
│   ├── navigation/   # 导航配置
│   ├── screens/      # 页面
│   ├── theme/        # 主题
│   └── viewmodel/    # 视图模型
└── utils/            # 工具类
```

### 贡献

欢迎贡献代码、报告问题或提出建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开源协议

本项目采用 [MIT 协议](LICENSE) 开源。

### 作者

**Ricky Miao** - [GitHub](https://github.com/miaotenone)

### 联系方式

- 邮箱: rickymiao63@163.com
- GitHub: [miaotenone](https://github.com/miaotenone/AccountKeeper)

### 致谢

感谢所有为这个项目贡献的开发者和用户！

---

## English

### Features

- **Complete Bookkeeping** - Record income and expenses with custom categories and notes
- **Asset Management** - Track assets and liabilities, manage loan records with attachments and statuses
- **Smart Statistics** - Multi-dimensional data display with visual charts and category filtering
- **Bill Auto Import** - Support for WeChat and Alipay bills (Excel/CSV) with smart refund handling
- **Search Function** - Quick keyword search to locate transactions
- **ZIP Full Backup** - Complete backup and restore for transactions, assets and attachments
- **Multiple Data Backups** - ZIP backup + auto backup for data security
- **User Feedback** - Built-in feedback feature for sending issues or suggestions via email
- **Modern Interface** - Material 3 design with dark mode support
- **Internationalization** - Chinese/English switching, multiple currency symbols
- **Smooth Interactions** - Swipe-to-delete, batch operations, paginated loading

### Changelog

#### v1.5.1 (Latest)
- Added user feedback feature via email
- Fixed scroll issue in bill file list
- Optimized bill list layout for compact display

#### v1.1.21
- ZIP full backup for transactions, assets and attachments
- Data management UI optimization with separate delete buttons
- Bill re-import functionality
- Dynamic version binding on About page
- Global layout optimization, more compact design
- Improved help tutorial content

#### v1.1.20
- Added asset management for tracking assets/liabilities
- Added home page search functionality
- Added statistics page category filtering
- Improved WeChat/Alipay bill import with Excel support
- Optimized pagination for better performance
- UI layout improvements

#### v1.0.0
- Basic income/expense recording
- Statistics and analysis
- Category management
- CSV import/export
- Local backup system
- Theme switching, i18n support

### Screenshots

<div align="center">
  <img src="https://via.placeholder.com/200x400/1f77b4/ffffff?text=Home" width="200" />
  <img src="https://via.placeholder.com/200x400/ff7f0e/ffffff?text=Statistics" width="200" />
  <img src="https://via.placeholder.com/200x400/2ca02c/ffffff?text=Assets" width="200" />
  <img src="https://via.placeholder.com/200x400/d62728/ffffff?text=Settings" width="200" />
</div>

### Getting Started

#### Download & Install

Download the latest APK from [Releases](https://github.com/miaotenone/AccountKeeper/releases).

#### Build from Source

```bash
# Clone repository
git clone https://github.com/miaotenone/AccountKeeper.git
cd AccountKeeper

# Build Debug version
./gradlew assembleDebug

# Build Release version
./gradlew assembleRelease

# Install to device
./gradlew installDebug
```

### Usage Guide

#### Basic Operations

| Action | Method |
|--------|--------|
| Add Transaction | Tap + button at bottom right of home page |
| Edit Transaction | Tap on a transaction card |
| Delete Transaction | Swipe left on a transaction card |
| Batch Operations | Long press a transaction card |
| Search Records | Tap search icon, enter keywords |

#### Data Management

- **ZIP Export** - Settings → Data Management → Export Full Ledger (transactions, assets, attachments)
- **ZIP Import** - Settings → Data Management → Import ZIP backup file
- **Bill Import** - Settings → Data Management → Import WeChat/Alipay Bill (Excel/CSV)
- **Bill Management** - View imported bills, re-import or delete
- **Auto Backup** - Settings → Data Management → Enable Local Auto Backup
- **Clear Data** - Separate clearing for transactions or assets

#### Asset Management

- **Add Asset** - Tap + on Assets page to record loan information
- **Asset Status** - Supports In Progress, Completed, Cancelled statuses
- **Attachments** - Add images and other files as evidence
- **Asset Categories** - Supports lending (positive) and borrowing (negative)

For detailed tutorials, check the Help page in the app (Settings → About → Help Tutorial).


### Detailed User Guide

#### 1. Getting started

After installation, use Home for daily income and expense records. The bottom navigation opens Home, Statistics, Assets, and Settings. Data is stored locally on the device and is not uploaded automatically. Before entering data, open Settings → Category Settings to review income, expense, and asset categories, then configure theme, language, and currency if needed.

#### 2. Transactions and statistics

1. Tap the + button on Home, choose Income or Expense, enter amount, date, category, and note, then save.
2. Tap a transaction card to edit it. Swipe left to delete; confirmation follows the current setting.
3. Long-press a transaction card to enter multi-select mode for batch deletion. The top search entry searches notes and category names.
4. Home supports month, year, and all-time ranges plus date, category, and sort filters. Statistics provides time ranges, income/expense analysis, category rankings, and trends; tap a ranking item to view its transactions.
5. Amounts must be greater than zero and a category is required. Imported bills keep their WeChat or Alipay source, and importing the same transaction again does not create another record with the same ID.

#### 3. Assets and liabilities

Asset categories use two directions: Positive and Negative. Positive means an item counted as an asset; Negative means debt or liability. Defaults include Cash and deposits, Investments, Fixed assets, Receivables/lending, Deposits/prepayments, Credit cards, Loans/installments, Borrowed/payables, and Other liabilities.

When adding an asset, enter name, amount, date, direction, and category. Expand the detail area for category-specific fields, and use the top Add attachment button for image or document evidence. Statuses cycle as Not selected → In progress → Confirmed owned → Confirmed lost → Not selected. Only Confirmed owned contributes to totals. Positive assets contribute to Assets, negative assets contribute to Total liabilities, and Net worth is Assets - Total liabilities.

An approved purchase request creates one In progress asset. Edit that asset and select a matching expense transaction in the source approval area: it must be an expense, use the same expense category and amount, be dated on or after the purchase date, and not already be linked to another asset. Selecting a transaction only links it; the asset remains In progress until purchase completion is confirmed. Ordinary assets do not require a source-approval transaction to become owned. If a linked asset becomes lost, its transaction and approval links remain for audit history.

#### 4. Approval center and purchase requests

Create a purchase request in the Approval Center with submission date, editable purchase date, reason, item name, specification, quantity, expense category, asset category, and approval detail fields. Submission date is written on the first submission and remains unchanged when resubmitting. Approval details show both submission and purchase dates.

Reviewers can open pending requests and enter an approval note. History provides top-level search for request number, reason, item name, specification, and approval notes. Filters can combine start date, end date, expense category, asset category, approval type, and approval status. History cards support swipe-left deletion and long-press multi-select deletion. A linked approval can be deleted only when its asset is Confirmed lost and the source link is cleared; other asset states are protected. Deleting an approval does not delete its linked transaction, asset, or asset attachments.

#### 5. How to obtain and import a WeChat bill

1. Open WeChat and go to Me → Services (older versions may show Payment) → Wallet → Bill.
2. Tap the … menu and open bill download, bill export, or the similarly named option. Labels vary by WeChat version.
3. Choose the required date range, provide the requested purpose and receiving email, and submit. Download the ZIP attachment from email and extract it first.
4. Keep the original `.csv`, `.xls`, or `.xlsx` file generated by WeChat. Do not rewrite its headers, column order, or contents in a spreadsheet application.
5. In AccountKeeper, open Settings → Data Management → Third-party Bill Import → Import WeChat Bill, select the extracted original file, and wait for the result.

The WeChat parser supports common headers such as transaction time, transaction type, counterparty, product, income/expense, amount, payment method, status, transaction ID, merchant order ID, and remark, plus a compact CSV layout. Rows with invalid or zero amounts, non-income/expense operations such as top-ups or withdrawals, or unrecognized headers are skipped. Fully refunded expenses are excluded; matched refunds and original transactions may be excluded as a pair, with the excluded count shown in the result.

#### 6. How to obtain and import an Alipay bill

1. Open Alipay and go to Me → Bill, then tap … or the bill download/transaction statement option.
2. Select the date range and receiving email, submit the request, download the email ZIP, and extract it.
3. Select the original `.csv`, `.xls`, or `.xlsx` generated by Alipay. Do not remove explanation rows, modify headers, or save it into a different format.
4. In AccountKeeper, open Settings → Data Management → Third-party Bill Import → Import Alipay Bill and select the file.

The Alipay parser supports the common 12-column format, the compact 8-column format, and older 7-or-more-column formats. Closed transactions, non-income/expense rows, zero amounts, and unrecognized rows are skipped. CSV reading tries common Chinese encodings automatically; if text is garbled, select the original platform file again.

#### 7. Bill file management and duplicates

Imported originals appear in Third-party Bill File Management with type, size, and time. You can re-parse or delete a file. File digests prevent the same original bill from being stored repeatedly, and stable transaction IDs prevent duplicate transactions. Deleting a bill file removes the stored original and its index, but does not delete transactions already imported; delete those from Home if needed. Use non-overlapping date ranges when importing regularly.

#### 8. Categories and detail fields

Open Settings → Data Management → Category Settings to manage income, expense, and asset categories. Default categories are protected; custom categories can be edited or deleted according to the page actions. Asset categories can switch between Positive and Negative.

For an asset category, open Edit fields to add TEXT, NUMBER, DATE, BOOLEAN, or SINGLE_CHOICE fields, with name, type, options, order, and required settings. Default categories have specialized templates and custom categories receive a general template. Seeding is deduplicated by category and field name and does not overwrite existing fields. Built-in field names remain Chinese in both languages, while field types and editor actions follow the current language; custom names and option values remain exactly as entered.

#### 9. Attachments

Transactions, assets, and approvals can store image or document evidence. When there are no attachments, the empty state is informational; use the top Add attachment button. Existing files can be previewed. Deleting a record also removes its related attachment files. Original bill files are managed separately in Bill File Management.

#### 10. Full archive and restore

Open Settings → Data Management → Full Data Archive. A ZIP export contains transaction categories, asset categories, approvals, transactions, assets, detail field definitions and values, attachments, bill files, and required settings. ZIP restore replaces current business data, then imports records in foreign-key order; backup files themselves are not removed.

Do not edit `data.json`, attachment files, or bill files inside the ZIP before importing. Missing files, invalid references, or integrity failures should show a clear error and restore the protection snapshot created before import. After a successful restore, verify counts and links for transactions, assets, approvals, categories, fields, attachments, and bill files.

#### 11. Local, scheduled, and vault backups

Local auto backup creates a full ZIP after a configured number of data changes; the default change threshold is 20. Scheduled backup runs independently according to its day interval. The two switches are independent. Retention limits the number of full local backups, deleting the oldest when the limit is exceeded.

The Local Backup Vault lists automatic and manual backups. Choose Restore this backup and confirm the impact in the full-page dialog. Automatic and manual restores use the same full restore path, including fields, categories, approvals, assets, transactions, attachments, and bill files. Clear All Business Data also removes business attachment and bill files while preserving backup ZIPs and protection snapshots. Export an independent archive before clearing data.

#### 12. Personalization and troubleshooting

Settings → Personalization controls light/dark theme, Chinese/English, and currency symbol. Follow the app prompt and reopen pages after changing language or display settings.

If a bill imports no transactions, check that the file was extracted, has a CSV/XLS/XLSX extension, retains its original headers, was not rewritten by spreadsheet software, and contains income/expense rows in the selected range. Keep a protection backup before ZIP restore or destructive deletion; normal deletion cannot be directly undone.
### Tech Stack

| Technology | Version | Description |
|------------|---------|-------------|
| Kotlin | 1.9.x | Development Language |
| Jetpack Compose | 1.5.x | UI Framework |
| Material 3 | - | Design System |
| Room | 2.6.x | Database ORM |
| Hilt | 2.48.x | Dependency Injection |
| Navigation | 2.7.x | Navigation Component |
| DataStore | - | Lightweight Storage |
| Paging 3 | - | Paginated Loading |
| Apache POI | 5.2.x | Excel Parsing |
| KSP | - | Annotation Processor |

### Project Structure

```
app/
├── data/              # Data Layer
│   ├── local/        # Local Database
│   ├── model/        # Data Models
│   └── repository/   # Data Repositories
├── di/               # Dependency Injection
├── ui/
│   ├── navigation/   # Navigation Configuration
│   ├── screens/      # Screens
│   ├── theme/        # Theme
│   └── viewmodel/    # ViewModels
└── utils/            # Utilities
```

### Contributing

Contributions are welcome! Feel free to submit issues, feature requests, or pull requests.

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### License

This project is licensed under the [MIT License](LICENSE).

### Author

**Ricky Miao** - [GitHub](https://github.com/miaotenone)

### Contact

- Email: rickymiao63@163.com
- GitHub: [miaotenone](https://github.com/miaotenone/AccountKeeper)

### Acknowledgments

Thanks to all developers and users who contribute to this project!

---

<div align="center">
  <sub>Built with love by <a href="https://github.com/miaotenone">Ricky Miao</a></sub>
</div>