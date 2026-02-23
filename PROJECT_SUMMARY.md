# AccountKeeper 项目说明

## 项目概述

AccountKeeper 是一个基于 Android 平台的记账应用，使用 Kotlin 语言开发，采用现代 Android 开发最佳实践构建。

## 技术栈

### 核心技术
- **语言**: Kotlin 2.0.21
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 35 (Android 15)
- **构建工具**: Gradle (Kotlin DSL)

### UI 框架
- **Jetpack Compose**: 现代声明式 UI 框架
- **Material 3**: Google 最新设计系统
- **Adaptive Navigation Suite**: 自适应导航组件

### 架构组件
- **Room 2.6.1**: 本地数据库
- **Hilt 2.52**: 依赖注入框架
- **DataStore Preferences**: 数据持久化
- **Jetpack Navigation Compose**: 导航管理
- **Kotlinx Serialization**: JSON 序列化

### 代码生成
- **KSP 2.0.21-1.0.25**: Kotlin 符号处理器

## 项目结构

```
app/src/main/java/com/example/accountkeeper/
├── AccountKeeperApp.kt        # 应用入口
├── MainActivity.kt            # 主 Activity
│
├── data/
│   ├── local/                 # 数据库层
│   │   ├── AppDatabase.kt     # Room 数据库
│   │   ├── TransactionDao.kt  # 交易数据访问对象
│   │   ├── CategoryDao.kt     # 分类数据访问对象
│   │   └── Converters.kt      # 类型转换器
│   ├── model/                 # 数据模型
│   │   ├── Transaction.kt     # 交易实体
│   │   ├── Category.kt        # 分类实体
│   │   └── Enums.kt           # 枚举定义
│   └── repository/            # 仓库层
│       ├── TransactionRepository.kt
│       ├── CategoryRepository.kt
│       └── SettingsRepository.kt
│
├── di/
│   └── DatabaseModule.kt      # Hilt 依赖注入模块
│
├── ui/
│   ├── navigation/
│   │   └── AppNavigation.kt   # 应用导航配置
│   ├── screens/               # 屏幕组件
│   │   ├── HomeScreen.kt      # 首页
│   │   ├── AddEditTransactionScreen.kt  # 添加/编辑交易
│   │   ├── StatisticsScreen.kt           # 统计页面
│   │   └── ImportExportScreen.kt         # 导入导出
│   ├── viewmodel/             # 视图模型
│   │   ├── TransactionViewModel.kt
│   │   ├── CategoryViewModel.kt
│   │   └── SettingsViewModel.kt
│   ├── theme/                 # 主题配置
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── AppStrings.kt
│   └── utils/
│       └── CurrencyUtils.kt   # 货币工具类
```

## 核心功能

### 数据模型

#### Transaction (交易)
- 支持收入和支出类型
- 包含金额、日期、分类、备注
- 支持多种数据来源（手动录入、支付宝、微信）
- 与分类表建立外键关联

#### Category (分类)
- 支持收入和支出分类
- 可标记为默认分类
- 用于组织和筛选交易记录

#### 枚举类型
- `TransactionType`: INCOME (收入) / EXPENSE (支出)
- `TransactionSource`: MANUAL (手动) / ALIPAY (支付宝) / WECHAT (微信)

### 功能模块

1. **首页 (HomeScreen)**: 交易列表展示
2. **添加/编辑交易 (AddEditTransactionScreen)**: 记账功能
3. **统计 (StatisticsScreen)**: 数据统计分析
4. **导入导出 (ImportExportScreen)**: 数据备份与恢复

## 架构设计

项目采用分层架构：

- **数据层**: Room 数据库 + Repository 模式
- **业务层**: ViewModel 处理业务逻辑
- **表现层**: Compose UI + Navigation
- **依赖注入**: Hilt 管理组件生命周期

## 构建与运行

```bash
# 构建项目
./gradlew build

# 安装到设备
./gradlew installDebug

# 生成 Release 包
./gradlew assembleRelease
```

## 依赖版本

- Android Gradle Plugin: 8.7.2
- Compose BOM: 2024.10.01
- Room: 2.6.1
- Hilt: 2.52
- Navigation Compose: 2.8.3

## 应用信息

- **包名**: com.example.accountkeeper
- **版本**: 1.0 (versionCode: 1)
- **Git 仓库**: git@github.com:miaotenone/AccountKeeper.git