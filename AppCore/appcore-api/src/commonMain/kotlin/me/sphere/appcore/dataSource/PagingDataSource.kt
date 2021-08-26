package me.sphere.appcore.dataSource

import me.sphere.appcore.Projection

abstract class PagingDataSource<Item: Any> {
    abstract val state: Projection<PagingState<Item>>
    abstract fun next()
    abstract fun reload()
    abstract fun close()
}

data class PagingState<Item: Any>(
    val items: List<Item>,
    val status: PagingStatus
)

enum class PagingStatus {
    LOADING, FAILED, HAS_MORE, END_OF_COLLECTION;
}
