package com.guesswhosesong.app.di

import com.guesswhosesong.app.data.network.WebSocketManager
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
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base URL of the Ktor backend.
     * Change this to your Render / Cloud Run URL in production.
     */
    const val BASE_URL = "https://acquaint-capital-bling.ngrok-free.dev"
    const val WS_BASE_URL = "wss://acquaint-capital-bling.ngrok-free.dev"

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(GWSJson)
            }
            install(Logging) {
                level = if (com.guesswhosesong.app.BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            }
            install(WebSockets)
        }
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(httpClient: HttpClient, identityManager: PlayerIdentityManager): WebSocketManager {
        return WebSocketManager(httpClient, WS_BASE_URL, identityManager)
    }
}
