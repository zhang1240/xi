# LocalLedger 开发规划

> 本文档持续更新：每完成一项就更新状态；随版本发布归档到 `VERSIONING.md` 后从这里移除。
> 下一版本：`2.6`（versionCode `20`，单词待定，登记于 `VERSIONING.md`）。

状态标记：`[ ]` 待办 · `[~]` 进行中 · `[x]` 已完成

## 2.6 目标：首页 Dashboard 化 + Settings 重构 + 数据层缺陷修复

### 首页 Dashboard 化（2026-09-12 真机截图评审定稿，外部建议文档已审）

> 诊断：控件太密、层级不成立——四张同权重卡连排、卡内还有嵌套卡（波浪进度外面套容器）、
> 大数字有两个（¥644.10 与 ¥53.67 抢焦点）、FAB 与页面同色族"沉"在列表上。
> 方向：从"卡片驱动"转"层级驱动"，减密为四区——焦点 / 日程任务 / 列表 / 导航。
> 涉及文件：`Screens.kt`（HomeScreen / BillRow / WaveProgress）、`Color.kt`、`Theme.kt`。

- [ ] **Header 文案**："我的账单/80 条记账" → 两行"9月 / 本月概览"（recordCount 降级或移除）。不做月份切换（见否决清单）。
- [ ] **焦点区归并**：左"本月总支出"+右"日均+图表"两张并排卡 → 归并为单区——月支出 `displaySmall`(36sp) 全页唯一特大数字；其下次级行"今日 ¥35.50 · 日均 ¥53.67"（body 级，收入绿/支出红）；7 天柱状图限高 80–100dp 并入本区底部，不再独立成卡。
- [ ] **预算条换 Linear**：WaveProgress（截图中蛇形线+外围又套一层圆角容器，双重嵌套）→ M3 `LinearProgressIndicator`（trackThickness 8dp，圆角轨道），去外层容器；文案三态：`已使用 42.9%` / ≥80% 提醒态 / 超支 `已超出 ¥xx`（`colors.expense`）。WaveProgress 无它用则删除。
- [ ] **待确认升级任务入口**：count>0 时 warningContainer 底 + 整行 clickable + 尾部 chevron；count=0 隐藏整卡（不是显示"暂无待处理"占卡位）。
- [ ] **底部导航 Badge**：待确认 `NavigationBarItem` 套 `BadgedBox`，>99 显示 `99+`，0 隐藏。全文档性价比最高项。
- [ ] **首页列表截断**：现在全月账单全渲染（`grouped.forEach` 无上限，性能+定位双问题）→ 最近 7 笔 + "查看全部 →"（"全部"按钮文案同步改，`Screens.kt` TextButton）。
- [ ] **账单行减密**：同日内行间分隔线只保留组末（isLast 已有）；副标题 `09/12 17:51 | 通知 | 微信` → `微信 · 17:51`（"通知"来源仅手工/编辑等异常态才标注）；日期组头已有日收支小计，保留。
- [ ] **分类图标映射**：`categoryIconChar` 首字圆牌 → `name → (Material Icon, 分类色)` 映射表（餐饮 restaurant/购物 shopping_cart/交通 directions_subway/住房 home/通讯 smartphone/学习 school/医疗 medical_services/其他 receipt_long），未知名回退 receipt_long+中性色；色板进 `LedgerColors` 体系，禁 emoji。
- [ ] **卡片降噪（token 化）**：卡底与页面底色色差压到近乎不可辨（截图卡底 #EBE1E5 偏紫粉、白列表裸露，深浅交替碎）；统一 elevation 0、圆角 Card=16dp/Emphasis=20dp 两档、间距 4/8 网格全量梳理。FAB 提色为主题 accent（现与背景同色族沉底）。页面渐变两端色差压缩到接近纯色。
- [ ] **色板翻译**：外部文档方案 A 色值仅作基调参考，翻译进 `LightLedgerColors/DarkLedgerColors` 语义字段（含暗色等对比度变体，文档没给暗色方案需自配）；对比度修正：支出红白底 <4.5:1 的用 #B93A3A 级深色或限 ≥18sp bold；次要文字暗色变体单独调。屏幕层零 `Color(0x…)` 规矩不破。

**否决清单（勿复议）**：月份切换（全 App 隐式"当前月"参数化，牵动所有 DAO/统计/预算，留待 2.7 ViewModel 化后再议）；emoji 图标（跨设备渲染/主题色/暗色三雷）；直接抄十六进制色值到屏幕层；环比"较上月"降级 P1——只做"日均环比"口径，上月无数据或本月前 3 天隐藏整行。

**P1（随 2.7）**：FAB 滚动收起为 ＋；账单详情底部弹层（改分类/金额/账户/删除入口，先 sheet 后页面）；统计页同步重排（分类占比条形列表）。

### UI（其余）

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
