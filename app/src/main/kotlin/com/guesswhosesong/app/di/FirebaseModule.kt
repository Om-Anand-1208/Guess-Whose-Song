package com.guesswhosesong.app.di

import com.google.firebase.auth.FirebaseAuth
import com.guesswhosesong.app.data.firebase.FirebaseAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuthManager(auth: FirebaseAuth): FirebaseAuthManager =
        FirebaseAuthManager(auth)
}
