package com.conght.chibiocmaker.data.remote

import com.conght.chibiocmaker.data.model.CharacterConfig
import com.conght.chibiocmaker.data.model.CharacterTemplate
import com.conght.chibiocmaker.data.model.CosplayChallenge
import com.conght.chibiocmaker.data.model.DecorPack
import com.conght.chibiocmaker.data.model.DecorType
import com.conght.chibiocmaker.data.model.LuckyRollResult
import com.conght.chibiocmaker.data.model.PartSelection
import com.conght.chibiocmaker.data.model.TemplateDetail

interface MakerRemoteDataSource {
    suspend fun getTemplates(): List<CharacterTemplate>
    suspend fun getTemplateDetail(templateId: String): TemplateDetail
    suspend fun getDecorPacks(type: DecorType): List<DecorPack>
    suspend fun getRandomCosplayChallenge(): CosplayChallenge
    suspend fun getLuckyRoll(): LuckyRollResult
    suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection?
    suspend fun randomizeCharacter(templateId: String): CharacterConfig?
}
