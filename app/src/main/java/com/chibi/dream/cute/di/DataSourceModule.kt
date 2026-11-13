package com.chibi.dream.cute.di

import com.chibi.dream.cute.core.common.AppConfig
import com.chibi.dream.cute.data.remote.ApiRemoteDataSource
import com.chibi.dream.cute.data.remote.AssetProbe
import com.chibi.dream.cute.data.remote.CharacterImageComposer
import com.chibi.dream.cute.data.remote.GlideCharacterImageComposer
import com.chibi.dream.cute.data.remote.MakerRemoteDataSource
import com.chibi.dream.cute.data.remote.MockDataSource
import com.chibi.dream.cute.data.remote.OkHttpAssetProbe
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideMakerRemoteDataSource(
        mock: Provider<MockDataSource>,
        api: Provider<ApiRemoteDataSource>,
    ): MakerRemoteDataSource = if (AppConfig.USE_MOCK_DATA) mock.get() else api.get()

    @Provides
    fun provideCharacterImageComposer(impl: GlideCharacterImageComposer): CharacterImageComposer = impl

    @Provides
    fun provideAssetProbe(impl: OkHttpAssetProbe): AssetProbe = impl
}
