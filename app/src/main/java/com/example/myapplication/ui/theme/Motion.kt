package com.example.localledger.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/**
 * 系统「移除动画」无障碍开关（Settings > Accessibility > Remove animations）。
 *
 * 用 Settings 而不是 LocalAccessibilityManager：后者在配置变更时不会失效，
 * 用户中途改开关就得重启应用才生效。
 */
@Composable
fun isReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
