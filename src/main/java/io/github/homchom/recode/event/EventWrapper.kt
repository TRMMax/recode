package io.github.homchom.recode.event

import io.github.homchom.recode.init.RModule
import net.fabricmc.fabric.api.event.Event

/**
 * Wraps an existing event into an [REvent], using [transform] to map recode listeners to its
 * specification.
 */
fun <C, R, L> wrapEvent(event: Event<L>, transform: (Listener<C, R>) -> L): REvent<C, R, L> =
    EventWrapper(event, transform)

private open class EventWrapper<C, R, L>(
    override val fabricEvent: Event<L>,
    private val transform: (Listener<C, R>) -> L
) : REvent<C, R, L> {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun listen(listener: Listener<C, R>) = fabricEvent.register(transform(listener))

    override fun listenFrom(module: RModule, listener: Listener<C, R>) =
        fabricEvent.register(transform { context, result ->
            if (module.isEnabled) listener(context, result) else result
        })
}

/**
 * Constructs a [CustomEvent].
 */
@Suppress("FunctionName")
fun <C, R : Any> CustomEvent(fabricEvent: Event<Listener<C, R>>): CustomEvent<C, R> {
    return CustomEventWrapper(fabricEvent)
}

private class CustomEventWrapper<C, R : Any>(
    fabricEvent: Event<Listener<C, R>>
) : CustomEvent<C, R>, EventWrapper<C, R, Listener<C, R>>(fabricEvent, { it }) {
    override val prevResult get() = _prevResult
    private var _prevResult: R? = null

    override fun invoke(context: C, initialValue: R) = invoker(context, initialValue)
        .also { _prevResult = it }
}