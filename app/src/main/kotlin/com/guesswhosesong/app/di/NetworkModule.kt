package com.guesswhosesong.app.di

import com.guesswhosesong.app.BuildConfig
import com.guesswhosesong.app.data.network.RoomPollingClient
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.shared.dto.GWSJson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base URL of the Ktor backend.
     * Change this to your Render / Cloud Run URL in production.
     */
    val BASE_URL = BuildConfig.GWS_API_BASE_URL

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 35_000
                socketTimeoutMillis = 35_000
            }
            install(ContentNegotiation) {
                json(GWSJson)
            }
            install(Logging) {
                level = if (com.guesswhosesong.app.BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideRoomPollingClient(httpClient: HttpClient, identityManager: PlayerIdentityManager): RoomPollingClient {
        return RoomPollingClient(httpClient, BASE_URL, identityManager)
    }
}
