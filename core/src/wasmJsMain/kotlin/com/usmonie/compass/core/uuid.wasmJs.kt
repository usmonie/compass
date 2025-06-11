package com.usmonie.compass.core

/**
 * WASM-JS implementation of randomUUID
 * Using simple UUID generation as crypto APIs might not be fully available in WASM
 */
internal actual fun randomUUID(): String = generateSimpleUUID()