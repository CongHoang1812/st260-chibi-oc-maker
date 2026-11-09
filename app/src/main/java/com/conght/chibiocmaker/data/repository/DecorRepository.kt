package com.conght.chibiocmaker.data.repository

import com.conght.chibiocmaker.core.common.DispatcherProvider
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.core.common.resourceFlow
import com.conght.chibiocmaker.data.model.DecorPack
import com.conght.chibiocmaker.data.model.DecorType
import com.conght.chibiocmaker.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface DecorRepository {
    fun packs(type: DecorType): Flow<Resource<List<DecorPack>>>
}

class DecorRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : DecorRepository {

    override fun packs(type: DecorType) = resourceFlow { remote.getDecorPacks(type) }.flowOn(dispatchers.io)
}
