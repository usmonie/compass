package com.usmonie.compass.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class ScreenId(public val id: String) : NavKey {
    override fun toString(): String = id
}