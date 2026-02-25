# AccountKeeper 项目总结

## 项目概述

AccountKeeper 是一款简洁易用的个人财务管理 Android 应用，使用现代 Android 技术栈开发，提供完整的收支记录、统计分析、数据管理等功能。

## 技术栈

### 核心技术
- **语言**: Kotlin
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 35 (Android 15)
- **编译 SDK**: 35
- **构建工具**: Gradle 8.x + Kotlin DSL

### 主要框架和库

#### UI 框架
- **Jetpack Compose**: 现代 UI 框架，完全声明式 UI
- **Material 3**: Google 最新设计规范
- **Material Icons Extended**: 扩展图标库

#### 架构组件
- **Jetpack Navigation Compose**: 导航管理
- **Jetpack Lifecycle**: 生命周期感知组件
- **Hilt**: 依赖注入框架
- **ViewModel**: UI 层数据管理

#### 数据持久化
- **Room Database**: 本地 SQLite 数据库 ORM
- **DataStore Preferences**: 轻量级键值对存储
- **Kotlinx Serialization**: JSON 序列化

#### 其他工具
- **KSP**: Kotlin Symbol Processing（注解处理器）
- **Kotlin Parcelize**: 数据序列化

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/accountkeeper/
│   │   ├── AccountKeeperApp.kt          # 应用入口，Hilt 配置
│   │   ├── MainActivity.kt              # 主 Activity
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt       # Room 数据库配置
│   │   │   │   ├── TransactionDao.kt    # 交易数据访问对象
│   │   │   │   ├── CategoryDao.kt       # 分类数据访问对象
│   │   │   │   └── Converters.kt        # Room 类型转换器
│   │   │   ├── model/
│   │   │   │   ├── Transaction.kt       # 交易数据模型
│   │   │   │   ├── Category.kt          # 分类数据模型
│   │   │   │   └── Enums.kt             # 枚举定义
│   │   │   └── repository/
│   │   │       ├── TransactionRepository.kt  # 交易数据仓库
│   │   │       ├── CategoryRepository.kt     # 分类数据仓库
│   │   │       └── SettingsRepository.kt     # 设置数据仓库
│   │   ├── di/
│   │   │   └── DatabaseModule.kt        # Hilt 依赖注入模块
│   │   ├── ui/
│   │   │   ├── navigation/
│   │   │   │   └── AppNavigation.kt     # 导航配置
│   │   │   ├── screens/
│   │   │   │   ├── HomeScreen.kt        # 首页
│   │   │   │   ├── AddEditTransactionScreen.kt  # 添加/编辑交易
│   │   │   │   ├── StatisticsScreen.kt  # 统计分析
│   │   │   │   ├── SettingsScreen.kt    # 设置主页
│   │   │   │   ├── AppSettingsScreen.kt # 个性化设置
│   │   │   │   ├── CategorySettingsScreen.kt  # 分类管理
│   │   │   │   ├── DataManagementScreen.kt    # 数据管理
│   │   │   │   ├── ImportExportScreen.kt      # 导入导出
│   │   │   │   └── AboutScreen.kt      # 关于页面
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt            # 颜色定义
│   │   │   │   ├── Theme.kt            # 主题配置
│   │   │   │   ├── Type.kt             # 字体样式
│   │   │   │   └── AppStrings.kt       # 国际化字符串
│   │   │   └── viewmodel/
│   │   │       ├── TransactionViewModel.kt    # 交易视图模型
│   │   │       ├── CategoryViewModel.kt       # 分类视图模型
│   │   │       └── SettingsViewModel.kt       # 设置视图模型
│   │   └── utils/
│   │       ├── BackupManager.kt        # 备份管理器
│   │       ├── BillParser.kt           # 账单解析器
│   │       ├── CurrencyUtils.kt        # 货币工具
│   │       ├── FileConverter.kt        # 文件转换器
│   │       └── IdGenerator.kt          # ID 生成器
│   └── res/
│       ├── values/
│       │   ├── strings.xml             # 默认字符串资源
│       │   └── themes.xml              # 主题资源
│       └── drawable/                   # 图片资源
└── build.gradle.kts                    # 应用级构建配置
```

## 核心功能

### 1. 首页 (HomeScreen)

**功能特性**:
- 余额卡片显示总资产、总收入、总支出
- 支持本月/总资产视图切换
- 交易列表按日期分组显示
- 支持点击编辑交易
- 支持向左滑动删除交易
- 支持长按进入批量选择模式
- 批量删除和批量编辑功能

**交互细节**:
- 卡片向左滑动约 30% 显示红色删除背景
- 点击红色背景区域删除交易
- 长按交易卡片进入选择模式
- 选择模式支持多选/取消选择
- 顶部显示已选数量和操作按钮

### 2. 添加/编辑交易 (AddEditTransactionScreen)

**功能特性**:
- 大号数字键盘输入金额
- 收入/支出类型切换
- 日期选择器
- 分类网格选择
- 备注输入
- 自动保存验证

**数据验证**:
- 金额不能为空或零
- 必须选择分类
- 日期默认为当前日期

### 3. 统计分析 (StatisticsScreen)

**时间范围**:
- 日、周、月、年预设范围
- 自定义日期范围选择

**统计类型**:
- 支出统计
- 收入统计
- 综合对比（收入 vs 支出）

**数据展示**:
- 总金额统计
- 时间趋势折线图
- 分类占比饼图
- 分类排行榜

### 4. 设置功能

#### 个性化设置 (AppSettingsScreen)
- 深色/浅色主题切换
- 中英文语言切换
- 多种货币符号支持（¥、$、€、£、₩、₹、₽、฿）
- 重启应用生效

#### 分类管理 (CategorySettingsScreen)
- 支出/收入分类标签切换
- 添加自定义分类
- 重命名分类
- 删除自定义分类
- 预设分类不可删除

### 5. 数据管理 (DataManagementScreen)

#### CSV 导入导出
- 导出全量账本至 CSV 文件
- 导入标准 CSV 备份文件
- 数据合并规则：ID 重复跳过
- 自动创建缺失分类

#### 第三方账单导入
- 支持微信支付账单 CSV 导入
- 支持支付宝账单 CSV 导入
- 自动识别账单类型
- 智能解析交易数据
- 自动匹配或创建分类
- 退款交易自动识别为收入

#### 本地备份系统
- **自动备份**: 每次增删改操作自动创建备份
- **手动备份**: 随时创建命名备份
- **备份管理**: 查看和管理所有备份
- **备份恢复**: 从备份恢复数据
- **保留策略**: 可设置自动备份保留数量（5-50）

### 6. 关于页面 (AboutScreen)

**功能特性**:
- 应用图标和名称
- 版本信息
- 帮助教程弹窗
- GitHub 链接
- 作者联系方式
- 技术支持信息

## 数据模型

### Transaction（交易）
```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: Long,
    val type: TransactionType,      // 收入/支出
    val amount: Double,             // 金额
    val date: Long,                 // 时间戳
    val categoryId: Long?,          // 分类ID
    val note: String,               // 备注
    val source: TransactionSource   // 数据来源（手动/支付宝/微信）
)
```

### Category（分类）
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,               // 分类名称
    val type: TransactionType,      // 收入/支出类型
    val isDefault: Boolean          // 是否预设分类
)
```

### 枚举类型
```kotlin
enum class TransactionType { INCOME, EXPENSE }
enum class TransactionSource { MANUAL, ALIPAY, WECHAT }
```

## 数据库设计

### 表结构

#### transactions 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 交易ID |
| type | String | 交易类型 |
| amount | Double | 金额 |
| date | Long | 时间戳 |
| categoryId | Long (FK) | 分类ID |
| note | String | 备注 |
| source | String | 数据来源 |

#### categories 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 分类ID |
| name | String | 分类名称 |
| type | String | 分类类型 |
| isDefault | Integer | 是否预设 |

### 外键关系
- transactions.categoryId → categories.id
- 删除策略：SET_NULL（删除分类时交易不删除）

## 工具类

### BackupManager（备份管理器）
- 创建自动/手动备份
- 管理备份保留数量
- 获取最新备份文件
- 列出所有备份
- 删除指定备份

### BillParser（账单解析器）
- 解析微信账单 CSV
- 解析支付宝账单 CSV
- 自动识别交易类型
- 智能分类映射
- 处理退款交易

### CurrencyUtils（货币工具）
- 显示格式转换
- 不同货币符号处理
- 金额格式化

### FileConverter（文件转换器）
- CSV 行解析
- 数据转换

### IdGenerator（ID 生成器）
- 唯一 ID 生成
- 基于 UUID 或时间戳

## 国际化

**支持语言**:
- 中文（简体）
- English

**实现方式**:
- 使用 DataStore 存储语言偏好
- AppStrings 数据类管理所有字符串
- 运行时动态切换
- 重启应用生效

## 主题系统

**支持主题**:
- 深色模式（Dark Mode）
- 浅色模式（Light Mode）
- 自动跟随系统

**颜色系统**:
- 主色调：Material 3 规范
- 渐变色：Primary、Secondary、Tertiary
- 语义色：Error、Success、Warning

## 导航结构

```
┌─────────────┐
│   首页     │ ← 底部导航
│   统计     │ ← 底部导航
│   设置     │ ← 底部导航
└─────────────┘
       ↓
   [设置页面]
       ↓
┌──────────────────────────────┐
│  个性化设置  │
│  分类管理     │
│  数据管理     │
└──────────────────────────────┘
       ↓
   [数据管理]
       ↓
┌──────────────────────────────┐
│  导入导出  │
│  第三方账单导入  │
│  本地自动备份    │
└──────────────────────────────┘
```

## 安全特性

### 数据安全
- 本地存储，不上传云端
- 自动备份机制
- 备份加密保护（未来扩展）

### 输入验证
- 金额格式验证
- 分类必选验证
- 日期范围验证

### 删除保护
- 删除确认对话框
- 不可撤销提示
- 批量操作二次确认

## 性能优化

### 数据库优化
- Room 索引优化
- 查询缓存
- 分页加载（未来扩展）

### UI 优化
- LazyColumn 虚拟滚动
- Compose 重组优化
- 状态管理

### 内存优化
- ViewModel 生命周期管理
- 资源及时释放
- 图片加载优化（未来扩展）

## 已知问题和限制

### 当前限制
1. 不支持云同步
2. 备份文件未加密
3. 无生物识别锁
4. 仅支持 CSV 格式导入
5. 统计图表功能有限

### 待优化项
1. 添加数据搜索功能
2. 支持预算管理
3. 添加周期性记账
4. 支持多账户
5. 添加数据图表导出

## 开发指南

### 构建项目
```bash
# 克隆项目
git clone https://github.com/miaotenone/AccountKeeper.git

# 构建发布版本
./gradlew assembleRelease

# 安装调试版本
./gradlew installDebug
```

### 依赖版本
- Kotlin: 1.9.x
- Compose BOM: 2024.x
- Room: 2.6.x
- Hilt: 2.48.x
- Navigation: 2.7.x

### 代码规范
- 遵循 Kotlin 官方代码规范
- 使用 Material 3 设计规范
- MVVM 架构模式
- 单一职责原则

## 版本历史

### v1.0.0 (当前版本)
- 基础收支记录功能
- 统计分析功能
- 分类管理
- CSV 导入导出
- 第三方账单导入
- 本地备份系统
- 主题切换
- 国际化支持
- 滑动删除
- 批量操作

## 技术支持

**作者**: Ricky Miao  
**邮箱**: rickymiao63@163.com  
**GitHub**: https://github.com/miaotenone/AccountKeeper  

## 许可证

本项目为开源项目，遵循 MIT 许可证。

## 致谢

感谢所有为这个项目贡献的开发者和用户！

---

*最后更新: 2025年2月*