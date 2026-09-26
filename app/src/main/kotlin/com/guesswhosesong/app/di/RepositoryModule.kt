package com.guesswhosesong.app.di

import com.guesswhosesong.app.data.network.WebSocketManager
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.repository.RoomRepository
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRoomRepository(httpClient: HttpClient, identityManager: PlayerIdentityManager): RoomRepository =
        RoomRepository(httpClient, NetworkModule.BASE_URL, identityManager)

    @Provides
    @Singleton
    fun provideGameRepository(wsManager: WebSocketManager): GameRepository =
        GameRepository(wsManager)
}
