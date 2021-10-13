package me.sphere.unicorn.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.sphere.unicorn.BuildConfig
import me.spjere.appcore.android.network.NetworkObserver
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun networkObserver(@ApplicationContext context: Context) = NetworkObserver(context)

    @Provides
    @Singleton
    fun OkHttpClient(): OkHttpClient {
        val okhttpBuilder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            okhttpBuilder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        return okhttpBuilder.build()
    }
}