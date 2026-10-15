# 神秘数字开发计划

## 1. 项目目标

创建一个名为“神秘数字”的跨平台桌面行情程序，复刻现有 macOS 主程序的核心能力：在桌面常驻入口中展示四个指数的实时行情。第一阶段支持 macOS 和 Windows 10，不实现 macOS Widget。

项目目录：`~/Documents/code/MarketDesktopWidget`

## 2. 范围边界

### 本项目包含

- macOS 菜单栏应用。
- Windows 10 普通窗口应用，并保留系统托盘入口作为辅助操作。
- macOS 通过菜单栏图标显示行情面板；Windows 启动后直接显示常规行情窗口，也可以通过托盘菜单重新打开窗口。
- macOS 使用独立原生 SwiftUI `MenuBarExtra(.window)` 目标，以保持与原 SwiftUI 项目一致的左键打开、点击外部收起、系统状态栏图标和面板 UI；Windows 继续使用 KMP + Compose Desktop。
- 上证指数、深证成指、创业板指、科创 50 四项数据。
- 手动刷新、自动刷新、最后更新时间和网络错误状态。
- macOS `.dmg` 与 Windows `.msi` 安装包。
- GitHub Actions 自动构建两个平台的安装包。

### 当前不包含

- macOS Widget。
- Android、iOS、Web target。
- 登录、账号同步、自选股和复杂图表。
- 交易功能或任何下单能力。

## 3. 技术选型

- Kotlin Multiplatform：共享数据模型、解析、状态和刷新逻辑。
- Compose Multiplatform Desktop：共享界面层。
- Desktop JVM：macOS 和 Windows 的运行时目标。
- Kotlin `2.4.10`。
- Compose Multiplatform `1.11.1`。
- Kotlin Coroutines `1.11.0`。
- Gradle `9.1.0`。

版本与 `MediaFactory` 保持一致，但不复制其 Android/iOS/Web 配置和 convention plugin。

## 4. 目标项目结构

```text
MarketDesktopWidget/
├── desktopApp/                         # Desktop 入口、macOS 菜单栏、Windows 托盘和打包
├── shared/                             # 共享 Kotlin/Compose 代码
│   └── src/
│       ├── commonMain/
│       └── commonTest/
├── docs/DEVELOPMENT_PLAN.md
└── .github/workflows/                   # 后续添加 CI
```

目前只配置 `:shared` 和 `:desktopApp`，没有 Android、iOS 或 Web module。

## 5. 共享代码与平台边界

共享层负责：

- 行情数据模型和 UI 状态。
- 腾讯行情接口请求抽象。
- 返回文本解码和字段解析。
- 刷新调度、手动刷新和错误状态。
- Compose UI 中与平台无关的行情展示。

Desktop 层负责：

- macOS 菜单栏入口和隐藏 Dock 行为。
- Windows 10 普通窗口启动、关闭和退出行为，以及系统托盘辅助入口。
- macOS 菜单栏面板的显示/隐藏和退出程序。
- macOS 菜单栏图标使用与原 SwiftUI `chart.line.uptrend.xyaxis` 对应的趋势图标；托盘菜单的“显示神秘数字”始终显示面板，菜单栏图标点击负责切换显示/隐藏。
- 原生图标、打包参数和平台差异处理。

### UI 一致性要求

- macOS 版本的窗口内容和交互视觉以现有 SwiftUI 主程序为基准。
- Windows 10 版本不做独立的 Windows 风格改版，窗口内部复用同一套 Compose UI、布局、颜色、字体层级、行情卡片和操作按钮。
- 两个平台只允许在宿主行为上存在差异：macOS 使用菜单栏入口并隐藏 Dock；Windows 使用普通窗口作为主入口，同时保留系统托盘作为辅助入口。
- Windows 系统托盘不替代主窗口；启动后默认直接打开行情窗口，托盘菜单提供显示窗口、刷新和退出等操作。

## 6. 行情接口与解析方案

接口：

```text
https://qt.gtimg.cn/q=s_sh000001,s_sz399001,s_sz399006,s_sh000688
```

对应代码：

- `s_sh000001`：上证指数。
- `s_sz399001`：深证成指。
- `s_sz399006`：创业板指。
- `s_sh000688`：科创 50。

每个响应变量按分号和波浪号拆分后，使用固定字段位置读取：

- `fields[1]`：名称。
- `fields[2]`：代码。
- `fields[3]`：当前价。
- `fields[4]`：涨跌额。
- `fields[5]`：涨跌幅。

解码必须采用：

1. 优先按 UTF-8 解码。
2. 检测乱码或不合理文本后使用 GB18030 兜底。
3. 清理 BOM、NUL、换行和响应前后的空白字符。
4. 对字段数量不足、数值无法转换和单项异常进行容错，不影响其他指数展示。

解析器需要使用固定输入样本和乱码编码样本进行单元测试。

## 7. 刷新与状态

- 默认自动刷新间隔：约 30 秒。
- 支持用户点击手动刷新。
- 请求期间显示刷新中状态，避免重复并发请求。
- 保留上一次成功数据，并在网络异常时展示错误状态。
- 记录最后一次成功更新时间。
- 后续应根据接口响应和实际网络环境验证 30 秒轮询的稳定性。

## 8. 桌面交互

### macOS

- 应用作为菜单栏程序运行。
- 不在 Dock 中保留常驻图标。
- 点击菜单栏图标显示或隐藏行情面板。
- 面板中提供刷新和退出按钮。

### Windows 10

- 应用作为普通窗口程序运行，启动后直接打开行情窗口。
- 窗口内部使用与 macOS 版本一致的行情 UI。
- 应用可以继续驻留在 Windows 10 系统托盘中，托盘菜单提供显示窗口、刷新和退出。
- 用户通过窗口右上角关闭按钮、窗口内退出操作或托盘菜单结束程序；关闭窗口时是否最小化到托盘需要在实现阶段明确并保持一致。
- 适配 Windows 10 的任务栏、通知区域和高 DPI 缩放行为。

## 9. UI 状态设计

至少覆盖以下状态：

- 首次加载。
- 加载成功。
- 自动刷新中。
- 手动刷新中。
- 网络不可用。
- 响应内容为空或格式异常。
- 部分指数解析失败。

UI 中不显示应用名作为行情卡片标题；应用名称和窗口/安装包标识统一使用“神秘数字”。

## 10. 图标与原生打包

- 创建“神秘数字”专用应用图标。
- macOS 使用 `.icns`，生成 `.dmg`。
- Windows 使用 `.ico`，生成 `.msi`。
- 验证安装包中的显示名称、卸载信息、图标和启动行为。
- 应用显示名统一为“神秘数字”，代码包名使用 `com.mysterynumbers`；两者分开管理，避免后续重命名影响数据目录。

## 11. GitHub Actions

计划添加两个构建任务：

- `macos-latest`：运行 `packageDmg`，上传 `.dmg` artifact。
- `windows-latest`：运行 `packageMsi`，上传 `.msi` artifact。

每个任务应固定 JDK、使用 Gradle wrapper，并缓存 Gradle 依赖。Windows 任务应明确验证 Windows 10 可安装性；GitHub runner 本身不等同于 Windows 10 用户环境，因此仍需后续在真实 Windows 10 环境验证。

## 12. 分阶段实施顺序

1. [x] 验证空项目的 Desktop JVM 配置。
2. [x] 抽取行情模型、HTTP 请求接口和腾讯响应解析器。
3. [x] 加入 UTF-8/GB18030 兼容和解析单元测试基础。
4. [x] 实现共享行情 UI、刷新状态和错误状态。
5. [x] 接入 macOS 菜单栏/系统菜单入口的基础生命周期和退出行为。
6. [x] 接入 Windows 10 普通窗口和系统托盘辅助行为。
7. [x] 添加正式应用图标和 `.dmg`/`.msi` 打包资源（复用原 SwiftUI 图标；安装包仍需在各目标平台完成最终验证）。
8. [x] 添加 GitHub Actions，分别构建并上传两个平台的安装包。
9. [ ] 在 macOS 与真实 Windows 10 环境完成安装、运行、刷新、退出和升级验证。

## 13. 风险与限制

- 腾讯行情接口是非正式接口，返回格式和编码可能变化，需要保留严格的容错和测试样本。
- 腾讯接口当前返回内容实测为 GB18030/GBK 兼容编码；JVM 端必须先严格尝试 UTF-8，遇到非法 UTF-8 字节后再回退 GB18030，不能仅根据响应结构判断 UTF-8 成功。
- Compose Desktop 对 macOS 菜单栏、Windows 10 普通窗口和系统托盘的原生体验需要分别验证。
- GitHub Actions 可以代为构建 Windows 包，但无法完全替代真实 Windows 10 的安装和行为测试。
- 未签名的 macOS 应用可能触发 Gatekeeper 提示；正式分发时需要 Developer ID 签名和公证。
- 当前骨架只验证项目结构和编译链路，不代表行情功能已经完成。
