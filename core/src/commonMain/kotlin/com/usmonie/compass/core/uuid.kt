package com.usmonie.compass.core

import kotlin.random.Random

/**
 * Generates a random UUID-like string.
 * This is a simple implementation and not a true UUID.
 * Platform-specific implementations should be provided.
 */
internal expect fun randomUUID(): String

/**
 * Default implementation for generating random UUIDs
 */
internal fun generateSimpleUUID(): String {
    val characters = ('a'..'z') + ('0'..'9') + ('A'..'Z')
    val sections = listOf(8, 4, 4, 4, 12) // Similar to UUID format

    return sections.joinToString("-") { length ->
        (1..length).map { characters.random() }.joinToString("")
    }
}