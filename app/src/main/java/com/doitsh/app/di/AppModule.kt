package com.doitsh.app.di

import android.content.Context
import com.doitsh.app.data.local.DoitshDatabase
import com.doitsh.app.data.local.ProjectDao
import com.doitsh.app.data.local.TaskDao
import com.doitsh.app.data.remote.DoitshApiClient
import com.doitsh.app.data.repository.*
import com.doitsh.app.data.settings.DataMode
import com.doitsh.app.data.settings.UserSettings
import com.doitsh.app.domain.repository.ProjectRepository
import com.doitsh.app.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoitshDatabase =
        DoitshDatabase.getDatabase(context)

    @Provides
    fun provideTaskDao(db: DoitshDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideProjectDao(db: DoitshDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideUserSettings(@ApplicationContext context: Context): UserSettings =
        UserSettings(context)

    @Provides
    @Singleton
    fun provideApiClient(): DoitshApiClient = DoitshApiClient()
}
