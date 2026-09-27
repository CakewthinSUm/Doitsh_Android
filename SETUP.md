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
- **SDK Platforms**: Android 14 (API 34)
- **SDK Tools**:
  - Android SDK Build-Tools
  - Android SDK Command-line Tools
  - Android Emulator

### 4. 创建模拟器
Android Studio → Device Manager → Create Device：
- 选择 Pixel 6 或类似设备
- 下载系统镜像 (推荐 API 34)
- 启动模拟器

## 运行项目

### 方法 1：Android Studio (推荐)
1. 打开 Android Studio
2. File → Open → 选择 `E:\GitHub\Doitsh_Android`
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

如果 `gradlew.bat` 提示找不到 `gradle-wrapper.jar`，需要先生成 wrapper：

```powershell
# 安装 Gradle 后运行
gradle wrapper --gradle-version 8.10.2
```

或者直接让 Android Studio 自动下载。

## 项目结构

```
app/          # Android 客户端 (Compose + Material 3)
server/       # Ktor 后端服务
shared/       # (未来) 共享代码
```

## 功能特性

### 已实现
- ✅ Material 3 主题 (动态色彩)
- ✅ 沉浸导航栏 (Edge-to-Edge)
- ✅ 双模式数据层 (本地 / Self-hosted)
- ✅ Room 数据库
- ✅ Ktor 后端 (JWT 认证)
- ✅ Hilt 依赖注入

### 待完成
- [ ] 任务新增/编辑对话框
- [ ] 项目管理页面
- [ ] 同步逻辑
- [ ] 服务器部署 (Docker)

## 开发建议

1. **Material Design Express**: 使用 `@OptIn(ExperimentalMaterial3Api::class)` 启用最新 M3 组件
2. **沉浸体验**: `enableEdgeToEdge()` + `WindowInsets` 处理
3. **性能**: 使用 `LazyColumn` 和 `remember` 优化列表
4. **测试**: 在多个屏幕尺寸和设备上测试

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
