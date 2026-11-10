# 番茄 · Tomato for Android

[![build](https://github.com/Maple2085/tomato-apk/actions/workflows/build.yml/badge.svg)](https://github.com/Maple2085/tomato-apk/actions/workflows/build.yml)

把 [Tomato-Novel-Downloader](https://github.com/zhongbai2333/Tomato-Novel-Downloader)
的官方 Android 原生程序，包进一个能点在桌面上、用完能关掉的 Android 应用。

- **不是**上游的官方客户端，也不修改上游代码；只是调用其 Release 里的 `Android_arm64` 可执行文件。
- 由 **DeepSeek**（AI 助手，负责设计、代码与 CI）与 **Maple2085**（需求、测试与设备验证）共同完成。

## 为什么做这个

上游程序是终端 TUI + Web UI 的形态，在手机上最顺手的用法是起一个本地 Web 服务再用浏览器操作。
本项目把这套流程收进一个图标里：

- 点图标 → 前台服务拉起下载器 → 内置 WebView 直接进 Web UI，不用开终端、不用敲命令。
- 不用时点通知栏「停止」或菜单「停止服务」，进程立刻退出，不常驻后台。
- 下载器自身带「检查程序更新」，本项目同样支持（见下文）。

## 功能

| 功能 | 说明 |
| --- | --- |
| 一键启动 | 点图标即启动服务并进入 Web UI（`127.0.0.1:18423`） |
| 一键停止 | 通知栏「停止」或右上角 ⋮ →「停止服务」 |
| 桌面 / 手机布局 | ⋮ →「切换为桌面版」，强制 1280 宽 viewport，支持双指缩放，选择会记住 |
| 保存路径自动修正 | 启动时若配置里的 `save_path` 为空，自动改为 `/sdcard/Download/TomatoNovel` |
| 程序自更新 | `targetSdk 28` 允许执行应用私有目录内的二进制，上游的 `--self-update` 可用 |
| 首次配置预置 | 内置一份默认 `config.yml`，开箱即用 |

## 安装

1. 打开 [Releases](https://github.com/Maple2085/tomato-apk/releases/latest)，下载 `app-debug.apk`。
2. 允许「安装未知来源应用」，安装。
3. 点桌面上的红色番茄图标。

> APK 是 debug 签名，自用足够，不要拿去上架。
> 仅支持 **arm64** 设备（目前绝大多数手机）。

首次启动会申请一次存储写入权限（Android 12 及以下），用于把小说写到 `Download` 目录；
拒绝也能用，只是文件会落在应用私有目录。

## 使用

1. 点图标，等 1~2 秒进入 Web UI。
2. 「搜索 / 下载」→ 输入书名 → 选择 → 开始下载。
3. 右上角 ⋮：
   - 「切换为桌面版 / 手机版」：页面窄的时候换布局
   - 「停止服务」：立刻结束进程
4. 生成的 EPUB 在 `/sdcard/Download/TomatoNovel/`。

## 构建

不需要本地 Android SDK：推送到 `main` 后由 GitHub Actions 构建。

- 产物：每次 run 的 Artifacts（`tomato-apk`），以及自动更新的
  [apk-latest](https://github.com/Maple2085/tomato-apk/releases/tag/apk-latest) Release。
- 升级上游下载器版本：改 `.github/workflows/build.yml` 里的 `VERSION` 后推送即可。

本地构建（可选，需要 JDK 17 + Android SDK）：

```bash
# 把上游 Android arm64 包放到 assets 下
mkdir -p app/src/main/assets
curl -fL -o app/src/main/assets/tnd \
  https://github.com/zhongbai2333/Tomato-Novel-Downloader/releases/download/v2.4.16/TomatoNovelDownloader-Android_arm64-v2.4.16
gradle assembleDebug
```

## 结构

```
app/src/main/
├── AndroidManifest.xml
├── assets/
│   ├── config.yml          # 默认配置（保存路径预置为 Download 目录）
│   └── tnd                 # 构建时下载，不入库
├── java/com/tomato/reader/
│   ├── MainActivity.java   # WebView、启动轮询、显示模式、菜单
│   └── TomatoService.java  # 前台服务、释放二进制、拉起/停止子进程
└── res/mipmap-xxxhdpi/ic_launcher.png
```

关键技术点：`targetSdk` 停在 **28**，因为 Android 10 起（`targetSdk >= 29`）
禁止执行应用数据目录里的文件；停 28 既能让 `exec` 成功，也让上游自更新能替换自身。

## 已知限制

- 只支持 arm64，且必须能联网访问上游 API。
- 程序本体是上游的，抓取规则、失败重试等都受上游制约；上游失效时本项目也无能为力。
- 没有做后台下载通知、断点状态同步之类的增强，只做了「开 / 关」。

## 免责声明

本项目仅用于学习 Android 与进程管理、WebView 集成等技术。请遵守上游项目的使用条款与
所在地法律法规；下载内容仅供个人阅读，请勿转载或用于商业用途。使用风险自负。

## 许可

本项目代码采用 [MIT](LICENSE)。打包进 APK 的上游程序同为 MIT，
完整声明见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。

## 致谢

- [zhongbai2333/Tomato-Novel-Downloader](https://github.com/zhongbai2333/Tomato-Novel-Downloader)：下载器本体
- [Termux](https://termux.dev/)：验证「Android 上跑原生二进制」的可行路径
