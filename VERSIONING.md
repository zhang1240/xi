# LocalLedger 版本管理

当前版本：`2.0-axis`（versionCode `14`）

下一版本：`2.1-flux`（versionCode `15`）

版本值集中在根目录 `gradle.properties`：

```properties
appVersionCode=14
appVersionBase=2.0
appVersionWord=axis
nextAppVersionCode=15
nextAppVersionBase=2.1
nextAppVersionWord=flux
```

发布 1.4 时，将版本基础号和单词改为：

```properties
appVersionCode=7
appVersionBase=1.4
appVersionWord=harbor
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

| 版本 | 单词 | 含义 |
| --- | --- | --- |
| 1.2 | `aurora` | 极光 |
| 1.3 | `forge` | 锻造、构建 |
| 1.3.a | `a` | 阶段测试标记 |
| 1.4 | `harbor` | 港湾 |
| 1.4a | `a` | 阶段测试标记 |
| 1.5 | `meadow` | 草地 |
| 1.6 | `orbit` | 轨道 |
| 1.7 | `lumen` | 光通量单位、光 |
| 1.8 | `nova` | 新星 |
| 1.9 | `pulse` | 脉冲 |
| 2.0 | `axis` | 轴、方向 |
| 2.1 | `flux` | 通量、流动 |
