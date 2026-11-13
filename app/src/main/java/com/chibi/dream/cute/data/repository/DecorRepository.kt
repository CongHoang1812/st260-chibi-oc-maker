package com.chibi.dream.cute.data.repository

import com.chibi.dream.cute.core.common.DispatcherProvider
import com.chibi.dream.cute.core.common.Resource
import com.chibi.dream.cute.core.common.resourceFlow
import com.chibi.dream.cute.data.model.DecorPack
import com.chibi.dream.cute.data.model.DecorType
import com.chibi.dream.cute.data.remote.MakerRemoteDataSource
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
