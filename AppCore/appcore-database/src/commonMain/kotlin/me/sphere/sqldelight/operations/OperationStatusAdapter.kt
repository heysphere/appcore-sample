package me.sphere.sqldelight.operations

import com.squareup.sqldelight.ColumnAdapter
import me.sphere.models.operations.OperationStatus

object OperationStatusAdapter: ColumnAdapter<OperationStatus, String> {
    override fun decode(databaseValue: String) = OperationStatus.valueOf(databaseValue)
    override fun encode(value: OperationStatus) = value.name
}
