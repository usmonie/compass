package com.usmonie.compass.core.navigation

import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.GraphId
import com.usmonie.compass.core.NavigationResult

/**
 * Extension function to navigate with typed parameters
 */
public fun NavController.navigate(
    screenId: ScreenId,
    params: TypedParams = TypedParams.empty(),
    extras: Extra? = null,
    replace: Boolean = false,
    storeInBackStack: Boolean = true
): Boolean = navigate(
    screenId = screenId,
    options = NavOptions(
        storeInBackStack = storeInBackStack,
        params = params.toStringMap(),
        extras = extras,
        replace = replace
    )
)

/**
 * Extension function to navigate to graph with typed parameters
 */
public fun NavController.navigateToGraph(
    graphId: GraphId,
    params: TypedParams = TypedParams.empty(),
    extras: Extra? = null,
    replace: Boolean = false,
    storeInBackStack: Boolean = true
): Boolean = navigateToGraph(
    graphId = graphId,
    options = NavOptions(
        storeInBackStack = storeInBackStack,
        params = params.toStringMap(),
        extras = extras,
        replace = replace
    )
)

/**
 * Extension function to create ScreenId from string
 */
public fun String.toScreenId(): ScreenId = ScreenId(this)

/**
 * Extension function to create GraphId from string
 */
public fun String.toGraphId(): GraphId = GraphId(this)

/**
 * Extension function to get current screen state
 */
@Composable
public fun NavController.currentScreen(): ScreenDestination? {
    val destination by currentDestination.collectAsState()
    return destination
}

/**
 * Extension function to check if can go back
 */
@Composable
public fun NavController.canNavigateBack(): Boolean {
    val canGoBack by canGoBack.collectAsState()
    return canGoBack
}

/**
 * DSL function to build navigation graphs more easily
 */
public inline fun navController(
    initialGraphId: GraphId,
    extras: Extra? = null,
    params: TypedParams = TypedParams.empty(),
    noinline builder: NavGraphBuilder.() -> Unit
): NavController = NavController.create(
    initialGraphId = initialGraphId,
    extras = extras,
    params = params.toStringMap(),
    builder = builder
)

/**
 * DSL function to build navigation graphs with string IDs
 */
public inline fun navController(
    initialGraphId: String,
    extras: Extra? = null,
    params: TypedParams = TypedParams.empty(),
    noinline builder: NavGraphBuilder.() -> Unit
): NavController = navController(
    initialGraphId = initialGraphId.toGraphId(),
    extras = extras,
    params = params,
    builder = builder
)

/**
 * Extension function for easier graph creation in DSL
 */
public fun NavGraphBuilder.graph(
    id: String,
    builder: GraphBuilder.() -> Unit
) = graph(id.toGraphId(), builder)

/**
 * Extension function for easier screen creation in graph DSL
 */
public fun GraphBuilder.screen(
    id: String,
    factory: (Boolean, ScatterMap<String, String>?, Extra?) -> ScreenDestination
) = screen(id.toScreenId(), factory)

/**
 * Extension function for easier root screen creation in graph DSL
 */
public fun GraphBuilder.rootScreen(
    id: String,
    factory: (Boolean, ScatterMap<String, String>?, Extra?) -> ScreenDestination
) = rootScreen(id.toScreenId(), factory)

/**
 * Extension function to create simple screen factory
 */
public inline fun screenFactory(
    crossinline content: @Composable (ScreenId, TypedParams, Extra?) -> Unit
): (Boolean, ScatterMap<String, String>?, Extra?) -> ScreenDestination =
    { storeInBackStack, params, extra ->
        val typedParams = TypedParams.fromStringMap(params)
        object : ScreenDestination(ScreenId("temp"), storeInBackStack) {
            @Composable
            override fun Content() {
                content(id, typedParams, extra)
            }
        }
    }

/**
 * Extension function to create a simple screen destination
 */
public inline fun createScreen(
    id: ScreenId,
    storeInBackStack: Boolean = true,
    crossinline content: @Composable () -> Unit
): ScreenDestination = object : ScreenDestination(id, storeInBackStack) {
    @Composable
    override fun Content() {
        content()
    }
}

/**
 * Extension function to create a simple screen destination with string ID
 */
public inline fun createScreen(
    id: String,
    storeInBackStack: Boolean = true,
    crossinline content: @Composable () -> Unit
): ScreenDestination = createScreen(id.toScreenId(), storeInBackStack, content)

/**
 * Extension function for easier navigation with result
 */
public inline fun <reified T : NavigationResult> NavController.navigateForResult(
    screenId: ScreenId,
    params: TypedParams = TypedParams.empty(),
    extras: Extra? = null,
    storeInBackStack: Boolean = true,
    noinline onResult: suspend (T) -> Unit
): Boolean = navigateForResult(
    screenId = screenId,
    options = NavOptions(
        storeInBackStack = storeInBackStack,
        params = params.toStringMap(),
        extras = extras
    ),
    onResult = onResult
)

/**
 * Extension function for easier navigation to graph with result
 */
public inline fun <reified T : NavigationResult> NavController.navigateToGraphForResult(
    graphId: GraphId,
    params: TypedParams = TypedParams.empty(),
    extras: Extra? = null,
    storeInBackStack: Boolean = true,
    noinline onResult: suspend (T) -> Unit
): Boolean = navigateToGraphForResult(
    graphId = graphId,
    options = NavOptions(
        storeInBackStack = storeInBackStack,
        params = params.toStringMap(),
        extras = extras
    ),
    onResult = onResult
)

/**
 * Extension function to build TypedParams easily
 */
public inline fun buildParams(noinline init: TypedParams.Builder.() -> Unit): TypedParams =
    TypedParams.build(init)

/**
 * Extension function to add parameters to existing TypedParams
 */
public inline fun TypedParams.plus(noinline init: TypedParams.Builder.() -> Unit): TypedParams {
    val builder = TypedParams.Builder()

    // Add existing parameters
    keys().forEach { key ->
        when (val value = get<Any>(key)) {
            is String -> builder.putString(key, value)
            is Int -> builder.putInt(key, value)
            is Long -> builder.putLong(key, value)
            is Float -> builder.putFloat(key, value)
            is Double -> builder.putDouble(key, value)
            is Boolean -> builder.putBoolean(key, value)
            else -> value?.let { builder.put(key, it) }
        }
    }

    // Add new parameters
    builder.init()
    return builder.build()
}

/**
 * Extension function to create NavOptions more easily
 */
public fun navOptions(
    storeInBackStack: Boolean = true,
    params: TypedParams = TypedParams.empty(),
    extras: Extra? = null,
    replace: Boolean = false
): NavOptions = NavOptions(
    storeInBackStack = storeInBackStack,
    params = params.toStringMap(),
    extras = extras,
    replace = replace
)