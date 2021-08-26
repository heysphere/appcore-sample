package me.sphere.sqldelight

abstract class StoreActorBase(val StoreScope: StoreScope): StoreActor {
    val actorName: String get() = this::class.simpleName ?: "<unknown>"

    abstract override fun attach()
}
