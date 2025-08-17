package com.usmonie.compass.core

import kotlin.jvm.JvmInline
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class GraphId(public val id: String) : NavKey {
    override fun toString(): String = id
}
