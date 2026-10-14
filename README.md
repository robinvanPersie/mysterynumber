# 神秘数字（MarketDesktopWidget）

Kotlin Multiplatform + Compose Multiplatform Desktop 项目，目标平台为 macOS 和 Windows 10。

当前仓库是最小项目骨架，包含：

- `shared`：共享 Kotlin/Compose 代码，当前只配置 JVM target。
- `desktopApp`：Desktop JVM 启动入口和原生安装包配置。
- `docs/DEVELOPMENT_PLAN.md`：后续开发计划。

当前明确不包含 Android、iOS、Web target，也不包含 macOS Widget。

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
