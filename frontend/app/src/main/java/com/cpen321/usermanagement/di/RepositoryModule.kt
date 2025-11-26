package com.cpen321.usermanagement.di

import com.cpen321.usermanagement.data.repository.AuthRepository
import com.cpen321.usermanagement.data.repository.AuthRepositoryImpl
import com.cpen321.usermanagement.data.repository.BlockRepository
import com.cpen321.usermanagement.data.repository.BlockRepositoryImpl
import com.cpen321.usermanagement.data.repository.BuddyRepository
import com.cpen321.usermanagement.data.repository.BuddyRepositoryImpl
import com.cpen321.usermanagement.data.repository.EventRepository
import com.cpen321.usermanagement.data.repository.EventRepositoryImpl
import com.cpen321.usermanagement.data.repository.ChatRepository
import com.cpen321.usermanagement.data.repository.ChatRepositoryImpl
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.repository.ProfileRepositoryImpl
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
    fun provideAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository {
        return authRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository {
        return profileRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideBuddyRepository(
        buddyRepositoryImpl: BuddyRepositoryImpl
    ): BuddyRepository {
        return buddyRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository {
        return eventRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository {
        return chatRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideBlockRepository(
        blockRepositoryImpl: BlockRepositoryImpl
    ): BlockRepository {
        return blockRepositoryImpl
    }
}
