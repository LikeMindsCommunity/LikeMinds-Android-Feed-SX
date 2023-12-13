package com.likeminds.feedsx.feed

import com.likeminds.feedsx.db.dao.ConfigurationDao
import com.likeminds.feedsx.db.models.ConfigurationEntity
import javax.inject.Inject

class ConfigurationRepository @Inject constructor(
    private val configurationDao: ConfigurationDao
) {

    suspend fun insertConfigurations(configurations: List<ConfigurationEntity>) {
        configurationDao.insertConfigurations(configurations)
    }

    suspend fun getConfiguration(type: String): ConfigurationEntity? {
        return configurationDao.getConfiguration(type)
    }

    suspend fun getConfigurations(): List<ConfigurationEntity> {
        return configurationDao.getConfigurations()
    }
}