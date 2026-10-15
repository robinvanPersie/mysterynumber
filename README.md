# 神秘数字（MarketDesktopWidget）

Kotlin Multiplatform + Compose Multiplatform Desktop 项目，目标平台为 macOS 和 Windows 10。

当前仓库已进入第一轮功能实现，包含：

- `shared`：共享 Kotlin/Compose 代码，当前只配置 JVM target。
- `desktopApp`：Desktop JVM 启动入口和原生安装包配置。
- 共享行情解析、UTF-8/GB18030 解码、30 秒刷新和 Compose 行情界面。
- macOS 菜单栏/系统托盘入口，以及 Windows 10 普通窗口加系统托盘入口。
- `docs/DEVELOPMENT_PLAN.md`：后续开发计划。

当前明确不包含 Android、iOS、Web target，也不包含 macOS Widget。正式应用图标已复用原 SwiftUI 版本，真实平台安装验证仍待完成。

## 本地运行

```bash
./gradlew :desktopApp:run
```

## 构建安装包

```bash
./gradlew :desktopApp:packageDmg
./gradlew :desktopApp:packageMsi
```

Windows 安装包将在 GitHub Actions 的 Windows runner 上构建，macOS 安装包将在 macOS runner 上构建。
