package com.chibi.dream.cute.data.remote

import com.chibi.dream.cute.data.model.CharacterConfig
import com.chibi.dream.cute.data.model.CharacterTemplate
import com.chibi.dream.cute.data.model.CosplayChallenge
import com.chibi.dream.cute.data.model.DecorPack
import com.chibi.dream.cute.data.model.DecorType
import com.chibi.dream.cute.data.model.LuckyRollResult
import com.chibi.dream.cute.data.model.PartSelection
import com.chibi.dream.cute.data.model.TemplateDetail

interface MakerRemoteDataSource {
    suspend fun getTemplates(): List<CharacterTemplate>
    suspend fun getTemplateDetail(templateId: String): TemplateDetail
    suspend fun getDecorPacks(type: DecorType): List<DecorPack>
    suspend fun getRandomCosplayChallenge(): CosplayChallenge
    suspend fun getLuckyRoll(): LuckyRollResult
    suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection?
    suspend fun randomizeCharacter(templateId: String): CharacterConfig?
}
