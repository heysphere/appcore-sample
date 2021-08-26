package me.sphere.appcore.utils.loop

class LoopSnapshot<State, Event>(
    val state: State,
    val lastEvent: Event?,
    val isActive: Boolean
)
