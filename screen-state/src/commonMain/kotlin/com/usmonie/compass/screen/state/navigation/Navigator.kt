package com.usmonie.compass.screen.state.navigation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public interface Navigator {
    public val size: Int
    public fun navigateTo(id: ScreenId)
    public fun pop()

    public fun hide()
    public fun show()
}

public val LocalNavigator: ProvidableCompositionLocal<Navigator> =
    staticCompositionLocalOf { throw RuntimeException("Missing Navigator") }