package me.sphere.unicorn.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.sphere.appcore.SphereStore
import me.sphere.appcore.SphereStoreBuilder
import me.sphere.appcore.SqlDatabaseProvider
import me.sphere.appcore.makeStore
import me.sphere.appcore.rest.Backend0StoreActorsBuilder
import me.sphere.logging.Logger
import me.sphere.network.AgentHTTPClient
import me.sphere.unicorn.BuildConfig
import me.sphere.unicorn.environment.Environment
import me.spjere.appcore.android.logging.AppCoreLoggingBackend
import me.spjere.appcore.android.network.AgentHTTPClientImpl
import me.spjere.appcore.android.network.NetworkObserver
import me.spjere.appcore.android.preference.PreferenceStoreImpl
import me.spjere.appcore.android.sql.SqlDatabaseProviderImpl
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppCoreModule {

    @Provides
    @Singleton
    fun environment() = Environment(
        backendUrl = BuildConfig.GITHUB_API_URL,
        githubToken =BuildConfig.GITHUB_TOKEN
    )

    @Provides
    @Singleton
    fun agentHttpClient(
        okHttpClient: OkHttpClient,
        environment: Environment,
        networkObserver: NetworkObserver
    ): AgentHTTPClient =
        AgentHTTPClientImpl(okHttpClient, networkObserver, environment.backendUrl)

    @Provides
    @Singleton
    fun logger(): Logger = Logger(true, AppCoreLoggingBackend())

    @Provides
    @Singleton
    fun backend0StoreActorsBuilder(
        httpClient: AgentHTTPClient,
        logger: Logger
    ): Backend0StoreActorsBuilder = Backend0StoreActorsBuilder(
        httpClient,
        logger,
    )

    @Provides
    fun sqlDatabaseProvider(@ApplicationContext context: Context): SqlDatabaseProvider {
        return SqlDatabaseProviderImpl(context)
    }

    @Provides
    @Singleton
    fun preferenceStoreImpl(@ApplicationContext context: Context) = PreferenceStoreImpl(context)

    @Provides
    @Singleton
    fun sphereStoreOwner(
        sqlDatabaseProvider: SqlDatabaseProvider,
        backend0StoreActorsBuilder: Backend0StoreActorsBuilder,
        preferenceStore: PreferenceStoreImpl,
        httpClient: AgentHTTPClient,
        logger: Logger,
        environment: Environment
    ): SphereStore {
        return SphereStoreBuilder(
            listOf(backend0StoreActorsBuilder),
            sqlDatabaseProvider,
            preferenceStore,
            httpClient,
            logger,
        ).makeStore(environment.githubToken)
    }

    @Provides
    fun notificationListUseCase(sphereStore: SphereStore) = sphereStore.notificationListUseCase
}