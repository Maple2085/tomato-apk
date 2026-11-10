# 第三方组件与许可

本项目的 APK 会在构建时从上游项目下载并打包一个可执行文件，其版权与许可如下。

## Tomato-Novel-Downloader

- 项目：https://github.com/zhongbai2333/Tomato-Novel-Downloader
- 作者：zhongbai2333 及贡献者
- 许可：MIT License，Copyright (c) 2025 zhongbai2333
- 用法：构建时下载其 Release 中的 `TomatoNovelDownloader-Android_arm64-<version>`
  预编译文件，原样作为 `assets/tnd` 打包；本项目不修改其代码。

```
MIT License

Copyright (c) 2025 zhongbai2333

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Android / WebView

应用本身只使用 Android 平台自带的 `WebView`、`Service`、`Notification` 等框架组件，
不引入任何第三方 Java 依赖（无 AndroidX、无 Kotlin）。
