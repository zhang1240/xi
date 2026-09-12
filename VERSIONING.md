# LocalLedger 版本管理

当前版本：`2.4-breeze`（versionCode `18`）

下一版本：待定

当前版本（2.4）已完成：通知去重与无感记录。统一通知监听/无障碍/确认后再识别三条路径的去重，避免重复入库与重复通知；微信与支付宝达到 70% 置信度即自动记账，无需手动确认；实况通知新增“忽略”按钮；首页汇总卡与二级菜单（账单/统计/设置/待确认）布局与对齐优化。

版本值集中在根目录 `gradle.properties`：

```properties
appVersionCode=18
appVersionBase=2.4
appVersionWord=breeze
nextAppVersionCode=19
nextAppVersionBase=2.5
nextAppVersionWord=_
```

`versionCode` 只能递增，最终 `versionName` 格式为：

```text
基础版本号-英文单词
```

例如：`1.2-aurora`、`1.3-forge`。

英文单词规则：

- 使用常见英文词典中的单词；
- 使用小写形式；
- 每个版本使用未重复的单词；
- 单词登记在下表中。
- 从当前版本 `1.6-orbit` 开始，正式版本后缀不得重复；历史阶段标记 `a` 的重复记录保留作为版本历史，不再继续使用。

## 版本单词登记

| 版本    | 单词       | 含义      |
| ----- | -------- | ------- |
| 1.2   | `aurora` | 极光      |
| 1.3   | `forge`  | 锻造、构建   |
| 1.3.a | `a`      | 阶段测试标记  |
| 1.4   | `harbor` | 港湾      |
| 1.4a  | `a`      | 阶段测试标记  |
| 1.5   | `meadow` | 草地      |
| 1.6   | `orbit`  | 轨道      |
| 1.7   | `lumen`  | 光通量单位、光 |
| 1.8   | `nova`   | 新星      |
| 1.9   | `pulse`  | 脉冲      |
| 2.0   | `axis`   | 轴、方向    |
| 2.1   | `flux`   | 通量、流动   |
| 2.2   | `gale`   | 大风      |
| 2.3   | `hush`   | 安静、悄无声息  |
| 2.4   | `breeze`  | 微风、轻快无感  |
