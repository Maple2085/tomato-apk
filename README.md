# 番茄小说下载器 Android 壳

一个极小的 Android 外壳：WebView + 前台服务，内置
[Tomato-Novel-Downloader](https://github.com/zhongbai2333/Tomato-Novel-Downloader)
官方 `Android_arm64` 可执行文件。

- 点图标 = 启动服务并打开 Web UI（`127.0.0.1:18423`）
- 通知栏「停止」按钮 / 右上角菜单「停止服务」= 结束进程
- `targetSdk 28`：允许执行应用私有目录里的二进制（W^X 限制），因此程序自带的
  「检查程序更新」也能用
- 二进制不入库，由 GitHub Actions 构建时从上游 Release 拉取

## 构建

Actions 会自动构建，产物在每次 run 的 Artifacts 里（`tomato-apk`）。
升级下载器版本：改 `.github/workflows/build.yml` 里的 `VERSION`。
