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

## 构建 macOS 包和 Windows 便携版

```bash
./gradlew :desktopApp:packageNativeMacDmg
```

macOS 必须使用 `packageNativeMacApp` 或 `packageNativeMacDmg`，这两个任务会编译 `macosApp` 下的原生 SwiftUI 菜单栏程序。不要使用 Compose 的 `createDistributable` 或 `packageDmg` 作为 macOS 最终包，它们生成的是 Compose Desktop 应用。

Windows 便携版会在 GitHub Actions 的 Windows runner 上构建：

```bash
./gradlew.bat :desktopApp:packagePortableZip
```

生成的 ZIP 内包含完整的 `神秘数字` 目录。解压后双击目录中的 `神秘数字.exe` 即可运行，不需要安装流程。macOS 安装包和 Windows 便携版都会通过 GitHub Actions 上传为构建产物。
