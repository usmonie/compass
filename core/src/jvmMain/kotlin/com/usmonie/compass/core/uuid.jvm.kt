package com.usmonie.compass.core

import java.util.UUID

/**
 * JVM implementation of randomUUID using Java's UUID class
 */
internal actual fun randomUUID(): String = UUID.randomUUID().toString()