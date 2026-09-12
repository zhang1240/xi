package com.example.localledger.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LedgerBlue = Color(0xFF1769AA)
val LedgerBlueLight = Color(0xFF4D9DE0)
val LedgerNavy = Color(0xFF183B56)
val LedgerRed = Color(0xFFC62828)
val LedgerGreen = Color(0xFF2E7D5B)
val LedgerOrange = Color(0xFFE08A22)
val LedgerWarm = Color(0xFFF4F8FC)

/**
 * 应用内语义色。屏幕层只允许引用这里的名字，不允许出现 Color(0xFF...) 字面量，
 * 否则深色模式下必然出现半深半浅的错配。
 */
@Immutable
data class LedgerColors(
    /** 卡片与列表的主标题文字。 */
    val ink: Color,
    /** 支出色：账本里最需要被看见的颜色。 */
    val expense: Color,
    /** 收入 / 退款色。 */
    val income: Color,
    /** 图表、进度与强调用蓝。 */
    val accent: Color,
    /** 页面渐变背景（自上而下）。 */
    val pageTop: Color,
    val pageBottom: Color,
    /** 悬浮按钮配色。 */
    val fabContainer: Color,
    val fabContent: Color,
    /** 分类图标圆底。 */
    val iconShell: Color,
    val onIconShell: Color,
    /** 预算波形的容器色（正常 / 超支两套）。 */
    val progressContainer: Color,
    val onProgressContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
) {
    val pageBackground: Brush
        get() = Brush.verticalGradient(listOf(pageTop, pageBottom))
}

val LightLedgerColors = LedgerColors(
    ink = Color(0xFF1B2A38),
    expense = Color(0xFFC62828),
    income = Color(0xFF2E7D5B),
    accent = Color(0xFF1E88E5),
    pageTop = Color(0xFFE3F0FD),
    pageBottom = Color(0xFFF4F6F7),
    fabContainer = Color.White,
    fabContent = Color(0xFF2B3A4A),
    iconShell = Color(0xFFE4EEF7),
    onIconShell = Color(0xFF44586B),
    progressContainer = Color(0xFFDCEBFA),
    onProgressContainer = Color(0xFF14568C),
    warningContainer = Color(0xFFFDE3E1),
    onWarningContainer = Color(0xFF9A211C)
)

val DarkLedgerColors = LedgerColors(
    ink = Color(0xFFEAF3FA),
    expense = Color(0xFFFF8A80),
    income = Color(0xFF7FD8AE),
    accent = Color(0xFF6FB6FF),
    pageTop = Color(0xFF101820),
    pageBottom = Color(0xFF16212C),
    fabContainer = Color(0xFF263746),
    fabContent = Color(0xFFEAF3FA),
    iconShell = Color(0xFF263746),
    onIconShell = Color(0xFFB9CCDC),
    progressContainer = Color(0xFF16324A),
    onProgressContainer = Color(0xFFB9DCFF),
    warningContainer = Color(0xFF4A1F1D),
    onWarningContainer = Color(0xFFFFC6C1)
)

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }

/** 语义色访问入口：`ledgerColors().expense`。 */
@Composable
fun ledgerColors(): LedgerColors = LocalLedgerColors.current
