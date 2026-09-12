# LocalLedger 开发规划

> 本文档持续更新：每完成一项就更新状态；随版本发布归档到 `VERSIONING.md` 后从这里移除。
> 下一版本：`2.6`（versionCode `20`，单词待定，登记于 `VERSIONING.md`）。

状态标记：`[ ]` 待办 · `[~]` 进行中 · `[x]` 已完成

## 2.6 目标：Settings 重构 + 数据层缺陷修复

### UI

- [ ] `SettingsScreen` 分组卡片化：按「自动记账 / 后台保活 / 账户与分类 / 商户规则 / 备份与隐私」分组；权限状态用 leading 状态点（绿=已授权 / 琥珀=建议开启），扫一眼可见哪里未开通。当前是一整面 `Text`+`Button` 墙，却承担最关键的权限引导。
- [ ] 手动记账表单：账户、分类从自由文本改为下拉选择（ExposedDropdownMenu）+ 允许新建。现状打错一个字就静默新建重复分类，直接污染统计。
- [ ] 待确认列表支持滑动操作：右滑确认 / 左滑忽略（SwipeToDismissBox）。待确认本质是队列，逐条点两个按钮太累。
- [ ] 首页"今日收入"金色 `0xFFF9A825` 残留核对（应统一走 `ledgerColors().income`）。

### 数据层（源码审查确认的真实缺陷）

- [ ] **AlipayParser 商户覆盖**：`AlipayParser.parse` 中 `parsed.copy(merchant = merchant)` 在支出场景把 GenericParser 已提取的商户覆盖为 null（置信度 90→70、分类退化）。应改为 `merchant ?: parsed.merchant`。文件第 10 行 `A || B && C` 也需加括号明确优先级。
- [ ] **分类 id 空间冲突（根因修复）**：`NotificationProcessor` 自动建分类用 `id = "category-$名称"`，用户手建分类是 UUID，同名分裂成两条。UI 已做按名聚合兜底（`categoryBreakdown`），根因要在数据层统一为按名查找复用。
- [ ] **WeChatParser 恒 85 分**：GenericParser 成功即 85 ≥ 70 阈值，微信非支付场景通知有误记风险；且 `autoConfirmThreshold = if (isAdapted) 70 else 80` 是死逻辑（`canAutoConfirm` 本身要求 isAdapted），应删除或让非适配来源真正参与阈值判断。
- [ ] **信号词表去重**：`NotificationProcessor.hasCompletedTransactionSignal` 与 `PaymentAccessibilityService.COMPLETED_TRANSACTION_SIGNALS` 内容完全相同，提取到共享常量。
- [ ] **清理死代码**：`Daos.kt` 中 `fillMerchantForRecent`（TransactionDao / CandidateDao 各一处）无调用。

### 测试与工程

- [ ] 单元测试运行环境问题：`testDebugUnitTest` 报 `ClassNotFoundException`（class 已编译到 `app/build/tmp/kotlin-classes/debugUnitTest/`，疑似中文路径 + argfile 字符集导致 worker classpath 乱码）。先试 `-Dfile.encoding=UTF-8`（在 test JVM args 里）或把 worker classpath 改相对路径；修好后恢复 CI 习惯。
- [ ] 为 `NotificationProcessor.persist()` 的去重/状态机补测试（当前只有解析层有测试，最复杂逻辑无覆盖）。
- [ ] 目录与包名不一致：源码在 `com/example/myapplication/` 而 package 声明是 `com.example.localledger`，择机移动目录消除 Lint `PackageDirectoryMismatch`。

## 2.7+ 候选（尚未排期）

- [ ] 抽 ViewModel + 拆分 `Screens.kt`（~1500 行五屏同居一文件）：每屏一个文件，派生数据（月度汇总/日均/分桶）挪进 ViewModel `flatMapLatest`；配置变更不再丢表单状态；消除 `initial = emptyList()` 的"首帧假空态"。纯结构重构、不改行为，适合单独一轮。
- [ ] 嵌套 Scaffold 收敛：MainActivity 与各屏各有一层，首页靠 `contentWindowInsets = WindowInsets(0,0,0,0)` 硬顶状态栏，两屏行为不一致。
- [ ] 转账（TRANSFER）账户余额联动：`toAccountId` 字段已存在但余额没有真正流转。
- [ ] 通知来源扩展：银行 App 短信解析（GenericParser 已具雏形，置信度阈值 80 已预留）。

## 已完成的近期方向（归档线索）

- 2.5-prism：语义色板与暗色模式、WaveProgress 去无限重绘、密码遮蔽、Decimal 键盘、删除撤销、近 7 天图表、空状态、Locale 金额格式。
- 2.4-breeze：三路径统一去重、微信/支付宝 70% 自动记账、实况通知忽略按钮。
