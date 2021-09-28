package me.sphere.unicorn.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.spjere.appcore.android.network.NetworkObserver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun networkObserver(@ApplicationContext context: Context) = NetworkObserver(context)

    @Provides
    @Singleton
    fun OkHttpClient() = okhttp3.OkHttpClient.Builder().build()
}