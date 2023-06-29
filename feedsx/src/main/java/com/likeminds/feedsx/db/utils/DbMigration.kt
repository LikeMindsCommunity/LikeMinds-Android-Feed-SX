package com.likeminds.feedsx.db.utils

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.likeminds.feedsx.db.utils.DbConstants.MEMBER_RIGHTS_TABLE
import com.likeminds.feedsx.db.utils.DbConstants.USER_TABLE

// all db migrations are written here
object DbMigration {
    // migration from version-1 to version-2
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `${MEMBER_RIGHTS_TABLE}` (`id` INTEGER NOT NULL, `is_locked` INTEGER, `is_selected` INTEGER NOT NULL, `state` INTEGER NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT, `user_unique_id` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `$USER_TABLE` ADD COLUMN community INTEGER NOT NULL"
            )
            database.execSQL(
                "ALTER TABLE `$USER_TABLE` ADD COLUMN user INTEGER NOT NULL"
            )
            database.execSQL(
                "ALTER TABLE `$USER_TABLE` ADD COLUMN sdk_client_user_unique_id TEXT NOT NULL"
            )
            database.execSQL(
                "ALTER TABLE `$USER_TABLE` ADD COLUMN sdk_client_uuid TEXT NOT NULL"
            )
        }
    }
}