package com.likeminds.feedsx.db.models

import androidx.room.*
import com.likeminds.feedsx.db.utils.DbConstants

@Entity(tableName = DbConstants.CONFIGURATION_TABLE)
class ConfigurationEntity constructor(
    @PrimaryKey
    @ColumnInfo(name = "type")
    var type: String,
    @ColumnInfo(name = "description")
    var description: String,
    @ColumnInfo("value")
    var value: String
) {
    class Builder {
        private var type: String = ""
        private var description: String = ""
        private var value: String = ""

        fun type(type: String) = apply { this.type = type }
        fun description(description: String) = apply { this.description = description }
        fun value(value: String) = apply { this.value = value }
        fun build() = ConfigurationEntity(type, description, value)
    }

    fun toBuilder(): Builder {
        return Builder().type(type)
            .value(value)
            .description(description)
    }
}