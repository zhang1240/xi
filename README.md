# LocalLedger

本地优先的 Android 自动记账应用。通过通知监听与无障碍读屏捕获微信、支付宝的支付事件，自动解析入账；全部数据保存在本机，无任何网络权限。

## 特性

- **自动记账**：通知监听 + 无障碍双通道，微信/支付宝高置信交易直接入账，低置信进入待确认队列，通知栏一键确认/忽略
- **统一去重**：同一笔交易的多种捕获路径（通知/读屏/确认后重读）只记一次
- **实况通知**：记账结果以实况通知呈现，支持"忽略"操作
- **手动记账**：支出/收入/转账/退款/调整五类，账户与分类自由管理，商户规则自动归类
- **预算与统计**：月度预算波形进度、近 7 天支出图、分类占比
- **隐私**：无网络权限；应用锁（生物识别）；AES-GCM + PBKDF2 加密备份/恢复

## 技术栈

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · Navigation Compose · Biometric · minSdk 31 / targetSdk 37

## 构建

```bash
git clone https://github.com/zhang1240/xi.git
cd xi
./gradlew assembleDebug          # 产物：app/build/outputs/apk/debug/app-debug.apk
```

或用 Android Studio 打开工程直接 Run。发布版使用 Build → Generate Signed App Bundle/APK 签名构建。

## 版本

版本号规则与单词登记表见 [VERSIONING.md](VERSIONING.md)；进行中的工作见 [NEXT_DEVELOPMENT_PLAN.md](NEXT_DEVELOPMENT_PLAN.md)。

当前版本：`2.5-prism`（versionCode 19）

## 许可

[MIT](LICENSE)
