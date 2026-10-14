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

/**
 * The base URL the shared API client must be configured with, or `null` when the
 * local implementation should be used instead.
 *
 * Self-hosted mode is only usable once a server URL has been entered: without one
 * [DoitshApiClient.configure] is never called, and every remote call would throw from
 * [DoitshApiClient.getClient] with "API client not configured". Handing out the remote
 * repository in that state is exactly the crash this guards against, so a blank URL
 * falls back to local rather than selecting a repository that cannot work.
 *
 * Deliberately *not* [UserSettings.isConfigured]: that property needs a Context, and it
 * is also `true` in [DataMode.LOCAL], which must not select remote.
 *
 * Pure and Context-free so it can be unit-tested without an Android runtime.
 */
internal fun remoteBaseUrl(mode: DataMode, serverUrl: String?): String? =
    serverUrl?.takeIf { mode == DataMode.SELF_HOSTED && it.isNotBlank() }

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
    ): TaskRepository = resolve(settings, localRepo, remoteRepo, apiClient)

    @Provides
    @Singleton
    fun provideProjectRepository(
        settings: UserSettings,
        localRepo: LocalProjectRepository,
        remoteRepo: RemoteProjectRepository,
        apiClient: DoitshApiClient
    ): ProjectRepository = resolve(settings, localRepo, remoteRepo, apiClient)

    /**
     * Both repositories make the same choice from the same settings, so the rule lives
     * here once.
     *
     * When both providers resolve to remote they each call [DoitshApiClient.configure]
     * with the same arguments; the second call closes the first client and builds an
     * identical one. That is wasteful but harmless — nothing observes the first client
     * in between — so it is left alone rather than adding reconfiguration state to the
     * client for no user-visible gain.
     */
    private fun <T> resolve(
        settings: UserSettings,
        local: T,
        remote: T,
        apiClient: DoitshApiClient
    ): T {
        val baseUrl = remoteBaseUrl(settings.dataMode, settings.serverUrl) ?: return local
        apiClient.configure(baseUrl, settings.authToken)
        return remote
    }
}
