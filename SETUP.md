# Doitsh 开发环境搭建指南

## 环境要求

### 1. JDK 17+
- 下载 [Adoptium JDK 17](https://adoptium.net/temurin/releases/?version=17)
- 安装后设置环境变量：
  ```powershell
  [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\jdk-17.x.x', 'User')
  ```

### 2. Android Studio
- 下载 [Android Studio](https://developer.android.com/studio)
- 安装时勾选 Android SDK、Android SDK Platform、Android Virtual Device
- 设置 `ANDROID_HOME`：
  ```powershell
  [System.Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
  ```

### 3. Android SDK 组件
打开 Android Studio → SDK Manager，确保安装：
- **SDK Platforms**: Android 17 (API 37) — 必须，`compileSdk = 37`
- **SDK Tools**:
  - Android SDK Build-Tools
  - Android SDK Command-line Tools
  - Android Emulator

> **注意**：`compileSdk` 是 **37** 而非 36。技术栈里每个库的 AAR 元数据都声明
> `minCompileSdk=37`，AGP 9 遇到不匹配会直接构建失败。`targetSdk` 仍是 36，
> `minSdk` 是 34 (Android 14)。

### 4. 创建模拟器
Android Studio → Device Manager → Create Device：
- 选择 Pixel 6 或类似设备
- 下载系统镜像 (推荐 API 34 或更高，最低 34)
- 启动模拟器

## 运行项目

### 方法 1：Android Studio (推荐)
1. 打开 Android Studio
2. File → Open → 选择 `F:\GitHub\Doitsh_Android`
3. 等待 Gradle 同步完成
4. 点击运行按钮 ▶️

### 方法 2：命令行
```powershell
# 在项目根目录
.\gradlew.bat assembleDebug

# 安装到模拟器
adb install app\build\outputs\apk\debug\app-debug.apk

# 或者直接用 Gradle 运行
.\gradlew.bat installDebug
```

## 首次 Gradle 同步

wrapper 已经配置好 (Gradle 9.6.0)，直接运行 `.\gradlew.bat` 即可，无需单独安装 Gradle。

如果 `gradlew.bat` 提示找不到 `gradle-wrapper.jar`，需要先生成 wrapper：

```powershell
# 仅在 wrapper jar 缺失时才需要
gradle wrapper --gradle-version 9.6.0
```

或者直接让 Android Studio 自动下载。

## 技术栈版本

所有依赖版本集中在 `gradle/libs.versions.toml`，两个模块都从这里读取：

- **构建工具链**: Gradle 9.6.0 · AGP 9.4.1 · Kotlin 2.4.21 · KSP 2.3.12 · JDK 17+
- **UI**: Compose BOM 2026.09.00 · Material 3 1.5.0-beta01 (Expressive) · Navigation 3 1.2.0
- **数据**: Room 2.8.5 · DataStore 1.2.1 · Ktor client 3.6.0

> AGP 9 自带 Kotlin 支持，因此**不能**再声明 `org.jetbrains.kotlin.android` 插件，
> 否则会报 `Cannot add extension with name 'kotlin'`。Kotlin 配置也从
> `android.kotlinOptions {}` 改成了顶层 `kotlin { compilerOptions { } }`。

## 项目结构

```
app/          # Android 客户端 (Compose + Material 3)
server/       # Ktor 后端服务
shared/       # (未来) 共享代码
```

## 功能特性

### 已实现
- ✅ Material 3 Expressive 主题 (动态色彩 + `MotionScheme.expressive()` + 8 级形状)
- ✅ 沉浸导航栏 (Edge-to-Edge)
- ✅ 自适应导航 (宽屏用 `NavigationRail`，窄屏用底部导航栏)
- ✅ 双模式数据层 (本地 / Self-hosted)
- ✅ Room 数据库
- ✅ Hilt 依赖注入
- ✅ Navigation 3 导航 (Home / Settings)
- ✅ 任务/项目新增对话框 (FAB 菜单)
- ✅ 任务滑动删除
- ✅ 全部文案抽取到 `res/values/strings.xml` (英文)
- ✅ Ktor 客户端 (Ktor 3)
- ⚠️ Ktor 后端源码存在既有编译错误，详见 README "Known issues"

### 待完成
- [ ] 任务编辑 (删除已实现：滑动删除 + 行内删除按钮)
- [ ] 项目管理页面
- [ ] 同步逻辑
- [ ] 服务器部署 (Docker)
- [ ] 迁移 `UserSettings` 到 DataStore (目前是 SharedPreferences)

## 开发建议

1. **Material 3 Expressive**: 实验性 API 通过 `app/build.gradle.kts` 中项目级的
   `-Xopt-in` 编译器参数统一开启，因此各调用点**不需要**再写
   `@OptIn(ExperimentalMaterial3Api::class)`
2. **沉浸体验**: `enableEdgeToEdge()` + `WindowInsets` 处理
3. **性能**: 使用 `LazyColumn` 和 `remember` 优化列表
4. **文案**: 用户可见字符串一律放进 `res/values/strings.xml`，用 `stringResource` 引用
5. **测试**: 在多个屏幕尺寸和设备上测试 (宽屏应显示 `NavigationRail`)

## 故障排查

### Gradle 同步失败
- 检查 `JAVA_HOME` 是否指向 JDK 17+
- 清理缓存：File → Invalidate Caches

### 模拟器启动慢
- 启用硬件加速：Intel HAXM 或 AMD Hyper-V
- 减少模拟器 RAM 分配

### 构建错误
- 确保 `ANDROID_HOME` 正确设置
- 检查网络连接（Gradle 需要下载依赖）
