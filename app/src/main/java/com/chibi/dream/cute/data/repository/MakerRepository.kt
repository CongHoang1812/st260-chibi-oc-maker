package com.chibi.dream.cute.data.repository

import com.chibi.dream.cute.core.common.DispatcherProvider
import com.chibi.dream.cute.core.common.Resource
import com.chibi.dream.cute.core.common.resourceFlow
import com.chibi.dream.cute.data.model.CharacterConfig
import com.chibi.dream.cute.data.model.CharacterTemplate
import com.chibi.dream.cute.data.model.PartSelection
import com.chibi.dream.cute.data.model.TemplateDetail
import com.chibi.dream.cute.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface MakerRepository {
    fun templates(): Flow<Resource<List<CharacterTemplate>>>
    fun templateDetail(id: String): Flow<Resource<TemplateDetail>>

    /** null = không random được (UI không làm gì). */
    suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection?

    /** null = không random được (UI không làm gì). */
    suspend fun randomizeCharacter(templateId: String): CharacterConfig?
}

class MakerRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : MakerRepository {

    override fun templates() = resourceFlow { remote.getTemplates() }.flowOn(dispatchers.io)

    override fun templateDetail(id: String) = resourceFlow { remote.getTemplateDetail(id) }.flowOn(dispatchers.io)

    override suspend fun randomizeCategory(templateId: String, categoryId: String) =
        withContext(dispatchers.io) { remote.randomizeCategory(templateId, categoryId) }

    override suspend fun randomizeCharacter(templateId: String) =
        withContext(dispatchers.io) { remote.randomizeCharacter(templateId) }
}
