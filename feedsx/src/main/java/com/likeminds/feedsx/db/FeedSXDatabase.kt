package com.likeminds.feedsx.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.likeminds.feedsx.db.dao.*
import com.likeminds.feedsx.db.models.*

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        AttachmentEntity::class,
        MemberRightsEntity::class,
        TopicEntity::class,
        ConfigurationEntity::class
    ], version = 5, exportSchema = false
)
abstract class FeedSXDatabase : RoomDatabase() {
    //User related queries
    abstract fun userDao(): UserWithRightsDao

    //Post related queries
    abstract fun postDao(): PostWithAttachmentsDao

    abstract fun configurationDao(): ConfigurationDao
}