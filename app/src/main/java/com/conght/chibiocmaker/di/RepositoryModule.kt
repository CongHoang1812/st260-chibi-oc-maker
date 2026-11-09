package com.conght.chibiocmaker.di

import com.conght.chibiocmaker.data.repository.CosplayRepository
import com.conght.chibiocmaker.data.repository.CosplayRepositoryImpl
import com.conght.chibiocmaker.data.repository.CreationRepository
import com.conght.chibiocmaker.data.repository.CreationRepositoryImpl
import com.conght.chibiocmaker.data.repository.DecorRepository
import com.conght.chibiocmaker.data.repository.DecorRepositoryImpl
import com.conght.chibiocmaker.data.repository.LuckyRollRepository
import com.conght.chibiocmaker.data.repository.LuckyRollRepositoryImpl
import com.conght.chibiocmaker.data.repository.MakerRepository
import com.conght.chibiocmaker.data.repository.MakerRepositoryImpl
import com.conght.chibiocmaker.data.repository.SettingsRepository
import com.conght.chibiocmaker.data.repository.SettingsRepositoryImpl
import com.conght.chibiocmaker.data.repository.StickerRepository
import com.conght.chibiocmaker.data.repository.StickerRepositoryImpl
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
    abstract fun bindCreationRepository(impl: CreationRepositoryImpl): CreationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindStickerRepository(impl: StickerRepositoryImpl): StickerRepository
}
