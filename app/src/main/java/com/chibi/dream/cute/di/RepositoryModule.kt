package com.chibi.dream.cute.di

import com.chibi.dream.cute.data.repository.CosplayRepository
import com.chibi.dream.cute.data.repository.CosplayRepositoryImpl
import com.chibi.dream.cute.data.repository.CreationRepository
import com.chibi.dream.cute.data.repository.CreationRepositoryImpl
import com.chibi.dream.cute.data.repository.DecorRepository
import com.chibi.dream.cute.data.repository.DecorRepositoryImpl
import com.chibi.dream.cute.data.repository.LuckyGiftRepository
import com.chibi.dream.cute.data.repository.LuckyGiftRepositoryImpl
import com.chibi.dream.cute.data.repository.LuckyRollRepository
import com.chibi.dream.cute.data.repository.LuckyRollRepositoryImpl
import com.chibi.dream.cute.data.repository.MakerRepository
import com.chibi.dream.cute.data.repository.MakerRepositoryImpl
import com.chibi.dream.cute.data.repository.SettingsRepository
import com.chibi.dream.cute.data.repository.SettingsRepositoryImpl
import com.chibi.dream.cute.data.repository.StickerRepository
import com.chibi.dream.cute.data.repository.StickerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindMakerRepository(impl: MakerRepositoryImpl): MakerRepository

    @Binds @Singleton
    abstract fun bindDecorRepository(impl: DecorRepositoryImpl): DecorRepository

    @Binds @Singleton
    abstract fun bindCosplayRepository(impl: CosplayRepositoryImpl): CosplayRepository

    @Binds @Singleton
    abstract fun bindLuckyRollRepository(impl: LuckyRollRepositoryImpl): LuckyRollRepository

    @Binds @Singleton
    abstract fun bindLuckyGiftRepository(impl: LuckyGiftRepositoryImpl): LuckyGiftRepository

    @Binds @Singleton
    abstract fun bindCreationRepository(impl: CreationRepositoryImpl): CreationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindStickerRepository(impl: StickerRepositoryImpl): StickerRepository
}
