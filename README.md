<div align="center">

![AccountKeeper Logo](ak.png)

# AccountKeeper

### 📱 一款简洁易用的个人财务管理 Android 应用

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5-blue.svg)](https://developer.android.com/jetpack/compose)

[English](#english) | [简体中文](#简体中文)

</div>

---

## 简体中文

### ✨ 特性

- 💰 **完整记账功能** - 记录收入和支出，支持自定义分类和备注
- 📊 **智能统计分析** - 多维度数据展示，图表可视化
- 🧾 **账单自动导入** - 支持微信和支付宝账单 CSV 导入
- 💾 **多重数据备份** - 自动备份 + 手动备份，数据安全无忧
- 🎨 **现代化界面** - Material 3 设计，支持深色模式
- 🌍 **国际化支持** - 中英文切换，多种货币符号
- ⚡ **流畅交互** - 滑动删除、批量操作，高效管理

### 📸 截图

<div align="center">
  <img src="https://via.placeholder.com/200x400/1f77b4/ffffff?text=Home" width="200" />
  <img src="https://via.placeholder.com/200x400/ff7f0e/ffffff?text=Statistics" width="200" />
  <img src="https://via.placeholder.com/200x400/2ca02c/ffffff?text=Settings" width="200" />
  <img src="https://via.placeholder.com/200x400/d62728/ffffff?text=Data" width="200" />
</div>

### 🚀 快速开始

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

### 📖 使用指南

#### 基础操作

1. **添加交易** - 点击首页右下角 + 按钮
2. **编辑交易** - 点击交易卡片
3. **删除交易** - 向左滑动交易卡片
4. **批量操作** - 长按交易卡片进入选择模式

#### 数据管理

- **CSV 导出** - 设置 → 数据管理 → 导出全量账本
- **CSV 导入** - 设置 → 数据管理 → 导入标准备份
- **账单导入** - 设置 → 数据管理 → 导入微信/支付宝账单
- **自动备份** - 设置 → 数据管理 → 开启本地自动备份

详细教程请查看应用内的帮助页面。

### 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.x | 开发语言 |
| Jetpack Compose | 1.5.x | UI 框架 |
| Material 3 | - | 设计规范 |
| Room | 2.6.x | 数据库 ORM |
| Hilt | 2.48.x | 依赖注入 |
| Navigation | 2.7.x | 导航组件 |
| DataStore | - | 轻量级存储 |
| KSP | - | 注解处理器 |

### 📁 项目结构

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

### 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 📝 开源协议

本项目采用 [MIT 协议](LICENSE) 开源。

### 👨‍💻 作者

**Ricky Miao** - [GitHub](https://github.com/miaotenone)

### 📧 联系方式

- 邮箱: rickymiao63@163.com
- GitHub: [miaotenone](https://github.com/miaotenone/AccountKeeper)

### 🙏 致谢

感谢所有为这个项目贡献的开发者和用户！

---

## English

### ✨ Features

- 💰 **Complete Bookkeeping** - Record income and expenses with custom categories and notes
- 📊 **Smart Statistics** - Multi-dimensional data display with visual charts
- 🧾 **Bill Auto Import** - Support for WeChat and Alipay bill CSV import
- 💾 **Multiple Data Backups** - Auto backup + manual backup for data security
- 🎨 **Modern Interface** - Material 3 design with dark mode support
- 🌍 **Internationalization** - Chinese/English switching, multiple currency symbols
- ⚡ **Smooth Interactions** - Swipe-to-delete, batch operations for efficient management

### 📸 Screenshots

<div align="center">
  <img src="https://via.placeholder.com/200x400/1f77b4/ffffff?text=Home" width="200" />
  <img src="https://via.placeholder.com/200x400/ff7f0e/ffffff?text=Statistics" width="200" />
  <img src="https://via.placeholder.com/200x400/2ca02c/ffffff?text=Settings" width="200" />
  <img src="https://via.placeholder.com/200x400/d62728/ffffff?text=Data" width="200" />
</div>

### 🚀 Getting Started

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

### 📖 Usage Guide

#### Basic Operations

1. **Add Transaction** - Tap the + button at bottom right of home page
2. **Edit Transaction** - Tap on a transaction card
3. **Delete Transaction** - Swipe left on a transaction card
4. **Batch Operations** - Long press a transaction card to enter selection mode

#### Data Management

- **CSV Export** - Settings → Data Management → Export Full Ledger
- **CSV Import** - Settings → Data Management → Import Standard Backup
- **Bill Import** - Settings → Data Management → Import WeChat/Alipay Bill
- **Auto Backup** - Settings → Data Management → Enable Local Auto Backup

For detailed tutorials, please check the Help page in the app.

### 🛠️ Tech Stack

| Technology | Version | Description |
|------------|---------|-------------|
| Kotlin | 1.9.x | Development Language |
| Jetpack Compose | 1.5.x | UI Framework |
| Material 3 | - | Design System |
| Room | 2.6.x | Database ORM |
| Hilt | 2.48.x | Dependency Injection |
| Navigation | 2.7.x | Navigation Component |
| DataStore | - | Lightweight Storage |
| KSP | - | Annotation Processor |

### 📁 Project Structure

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

### 🤝 Contributing

Contributions are welcome! Feel free to submit issues, feature requests, or pull requests.

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 📝 License

This project is licensed under the [MIT License](LICENSE).

### 👨‍💻 Author

**Ricky Miao** - [GitHub](https://github.com/miaotenone)

### 📧 Contact

- Email: rickymiao63@163.com
- GitHub: [miaotenone](https://github.com/miaotenone/AccountKeeper)

### 🙏 Acknowledgments

Thanks to all developers and users who contribute to this project!

---

<div align="center">
  <sub>Built with ❤️ by <a href="https://github.com/miaotenone">Ricky Miao</a></sub>
</div>