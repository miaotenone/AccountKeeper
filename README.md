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