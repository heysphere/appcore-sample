package me.sphere.models

import com.squareup.sqldelight.ColumnAdapter
import kotlinx.datetime.Instant
import me.sphere.appcore.utils.*

object InstantColumnAdapter: ColumnAdapter<Instant, ByteArray> {
    override fun decode(databaseValue: ByteArray) = Instant.fromBlob(databaseValue)
    override fun encode(value: Instant) = value.toBlob()
}
