package com.usmonie.compass.screen.state.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import com.usmonie.compass.screen.state.Extra
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public open class ScreenId(
    public val id: String,
    public open val mode: Mode = Mode.STANDARD,
) : Extra, NavKey {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScreenId

        if (id != other.id) return false
        if (mode != other.mode) return false

        return true
    }

    override fun hashCode(): Int {
        Exception().printStackTrace()
        var result = id.hashCode()
        result = 31 * result + mode.hashCode()
        return result
    }
}

@Serializable
public enum class Mode {
    SINGLE_TOP,
    SINGLE_CONTENT_TOP,
    SINGLE_INSTANCE,
    STANDARD,
}
