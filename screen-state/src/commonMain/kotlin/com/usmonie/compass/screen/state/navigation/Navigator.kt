package com.usmonie.compass.screen.state.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
public interface Navigator {
    public val size: Int
    public fun navigateTo(
        id: ScreenId,
        storeInBackstack: Boolean = true,
        clearBackStack: Boolean = false,
        mode: Mode = id.mode,
        replace: Boolean = false,
    )

    public fun pop()
    public fun hide()
    public fun show()
}

public val LocalNavigator: ProvidableCompositionLocal<Navigator> =
    staticCompositionLocalOf { throw RuntimeException("Missing Navigator") }
