package com.usmonie.compass.core.navigation

import androidx.collection.ScatterMap
import androidx.collection.mutableScatterMapOf
import kotlin.reflect.KClass

/**
 * Type-safe wrapper for navigation parameters
 * Provides a more robust alternative to string-based parameters
 */
public class TypedParams private constructor(
    private val params: ScatterMap<String, Any>
) {
    /**
     * Get a parameter value by key with specific type
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> get(key: String, type: KClass<T>): T? {
        val value = params[key] ?: return null
        return if (type.isInstance(value)) {
            value as T
        } else {
            null
        }
    }

    /**
     * Get a parameter value by key with inferred type
     */
    public inline fun <reified T : Any> get(key: String): T? {
        return get(key, T::class)
    }

    /**
     * Check if parameter exists
     */
    public fun contains(key: String): Boolean {
        return params.containsKey(key)
    }

    /**
     * Get all parameter keys
     */
    public fun keys(): Set<String> {
        val keySet = mutableSetOf<String>()
        params.forEach { key, _ -> keySet.add(key) }
        return keySet
    }

    /**
     * Convert to legacy ScatterMap<String, String> for backward compatibility
     */
    public fun toStringMap(): ScatterMap<String, String> {
        val stringMap = mutableScatterMapOf<String, String>()
        params.forEach { key, value ->
            stringMap[key] = value.toString()
        }
        return stringMap
    }

    /**
     * Builder for creating TypedParams
     */
    public class Builder {
        private val params = mutableScatterMapOf<String, Any>()

        public fun put(key: String, value: Any): Builder {
            params[key] = value
            return this
        }

        public fun putInt(key: String, value: Int): Builder = put(key, value)
        public fun putLong(key: String, value: Long): Builder = put(key, value)
        public fun putFloat(key: String, value: Float): Builder = put(key, value)
        public fun putDouble(key: String, value: Double): Builder = put(key, value)
        public fun putBoolean(key: String, value: Boolean): Builder = put(key, value)
        public fun putString(key: String, value: String): Builder = put(key, value)

        public fun build(): TypedParams {
            return TypedParams(params)
        }
    }

    public companion object {
        /**
         * Create empty parameters
         */
        public fun empty(): TypedParams {
            return TypedParams(mutableScatterMapOf())
        }

        /**
         * Create parameters with builder pattern
         */
        public fun build(init: Builder.() -> Unit): TypedParams {
            val builder = Builder()
            builder.init()
            return builder.build()
        }

        /**
         * Create from legacy string map
         */
        public fun fromStringMap(stringMap: ScatterMap<String, String>?): TypedParams {
            if (stringMap == null) return empty()

            val builder = Builder()
            stringMap.forEach { key, value ->
                // Try to parse common types
                when {
                    value.equals("true", ignoreCase = true) ||
                            value.equals("false", ignoreCase = true) -> {
                        builder.putBoolean(key, value.toBoolean())
                    }

                    value.toIntOrNull() != null -> {
                        builder.putInt(key, value.toInt())
                    }

                    value.toLongOrNull() != null -> {
                        builder.putLong(key, value.toLong())
                    }

                    value.toFloatOrNull() != null -> {
                        builder.putFloat(key, value.toFloat())
                    }

                    value.toDoubleOrNull() != null -> {
                        builder.putDouble(key, value.toDouble())
                    }

                    else -> {
                        builder.putString(key, value)
                    }
                }
            }
            return builder.build()
        }
    }
}