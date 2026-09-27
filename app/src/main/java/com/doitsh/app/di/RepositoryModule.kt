package com.doitsh.app.di

import com.doitsh.app.data.remote.DoitshApiClient
import com.doitsh.app.data.repository.*
import com.doitsh.app.data.settings.DataMode
import com.doitsh.app.data.settings.UserSettings
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        settings: UserSettings,
        localRepo: LocalTaskRepository,
        remoteRepo: RemoteTaskRepository,
        apiClient: DoitshApiClient
    ): TaskRepository {
        if (settings.dataMode == DataMode.SELF_HOSTED) {
            val url = settings.serverUrl
            if (!url.isNullOrBlank()) {
                apiClient.configure(url, settings.authToken)
            }
            return remoteRepo
        }
        return localRepo
    }

    @Provides
    @Singleton
    fun provideProjectRepository(
        settings: UserSettings,
        localRepo: LocalProjectRepository,
        remoteRepo: RemoteProjectRepository,
        apiClient: DoitshApiClient
    ): ProjectRepository {
        if (settings.dataMode == DataMode.SELF_HOSTED) {
            val url = settings.serverUrl
            if (!url.isNullOrBlank()) {
                apiClient.configure(url, settings.authToken)
            }
            return remoteRepo
        }
        return localRepo
    }
}
