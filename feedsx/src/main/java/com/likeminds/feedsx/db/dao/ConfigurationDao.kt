package com.likeminds.feedsx.db.dao

import androidx.room.*
import com.likeminds.feedsx.db.models.ConfigurationEntity
import com.likeminds.feedsx.db.utils.DbConstants

@Dao
interface ConfigurationDao {

    //add all configurations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigurations(configurations: List<ConfigurationEntity>)

    //get a particular configuration
    @Query("SELECT * FROM ${DbConstants.CONFIGURATION_TABLE} WHERE type = :type")
    suspend fun getConfiguration(type: String): ConfigurationEntity?

    //get all configurations
    @Query("SELECT * FROM ${DbConstants.CONFIGURATION_TABLE}")
    suspend fun getConfigurations(): List<ConfigurationEntity>
}