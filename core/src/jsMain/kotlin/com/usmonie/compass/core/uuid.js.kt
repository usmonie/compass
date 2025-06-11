package com.usmonie.compass.core

/**
 * JS implementation of randomUUID using crypto.randomUUID or fallback
 */
internal actual fun randomUUID(): String {
    return try {
        // Try to use crypto.randomUUID if available (modern browsers)
        js("crypto.randomUUID()") as String
    } catch (e: Throwable) {
        // Fallback to manual generation
        generateSimpleUUID()
    }
}