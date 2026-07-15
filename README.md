# Terris Android

一个使用 Kotlin 和原生 Android Canvas 绘制的轻量俄罗斯方块游戏。

## 游戏内容

- 经典 10 x 20 棋盘和 7-bag 方块序列
- 左移、右移、软降、硬降、旋转和幽灵方块
- 消行计分、等级加速、暂停、重开
- 手机触控操作，也支持方向键、空格和 `P` 键

## 通过 GitHub Actions 构建 APK

项目包含 `.github/workflows/build-apk.yml`。推送到 `main` 分支或手动运行工作流后，GitHub Actions 会执行 `assembleDebug`，并把 `app-debug.apk` 上传为名为 `terris-debug-apk` 的 artifact。

## 本地构建

本地需要 JDK 17 和 Android SDK。准备好环境后执行：

```sh
./gradlew assembleDebug
```

APK 输出路径为 `app/build/outputs/apk/debug/app-debug.apk`。

## License

MIT
