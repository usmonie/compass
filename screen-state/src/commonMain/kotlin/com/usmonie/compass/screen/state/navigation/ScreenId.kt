package com.usmonie.compass.screen.state.navigation

import androidx.navigation3.runtime.NavKey
import com.usmonie.compass.screen.state.Extra
import kotlinx.serialization.Serializable

@Serializable
public open class ScreenId(
    public val id: String,
    public open val mode: Mode = Mode.STANDARD,
) : Extra, NavKey

@Serializable
public enum class Mode {
    SINGLE_TOP,
    SINGLE_CONTENT_TOP,
    SINGLE_INSTANCE,
    STANDARD,
}