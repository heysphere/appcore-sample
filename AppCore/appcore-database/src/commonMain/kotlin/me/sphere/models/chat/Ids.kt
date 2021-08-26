package me.sphere.models.chat

import com.squareup.sqldelight.ColumnAdapter
import me.sphere.appcore.utils.Id
import me.sphere.appcore.utils.freeze

class LongIdAdapter<T: Id<Long>>(private val initializer: (Long) -> T): ColumnAdapter<T, Long> {
    init { freeze() }
    override fun decode(databaseValue: Long): T = initializer(databaseValue)
    override fun encode(value: T): Long = value.rawValue
}

class StringIdAdapter<T: Id<String>>(private val initializer: (String) -> T): ColumnAdapter<T, String> {
    init { freeze() }
    override fun decode(databaseValue: String): T = initializer(databaseValue)
    override fun encode(value: T): String = value.rawValue
}
