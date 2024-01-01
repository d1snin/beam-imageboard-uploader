/*
 * Copyright 2023-2024 Mikhail Titov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.d1s.beamimageboarduploader.service

import dev.d1s.beamimageboarduploader.database.Key
import dev.d1s.beamimageboarduploader.database.RedisClientFactory
import dev.d1s.beamimageboarduploader.entity.AdminChats
import dev.d1s.beamimageboarduploader.util.setAndPersist
import dev.inmo.tgbotapi.types.chat.Chat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface AdminService {

    suspend fun addAdminChat(chat: Chat): Boolean

    suspend fun isAdminChat(chat: Chat): Boolean
}

class DefaultAdminService : AdminService, KoinComponent {

    private val redisFactory by inject<RedisClientFactory>()

    private val redis by lazy {
        redisFactory.redis
    }

    private val mutex = Mutex()

    private val log = logging()

    override suspend fun addAdminChat(chat: Chat): Boolean {
        mutex.withLock {
            val chatId = chat.id

            log.i {
                "Registering admin chat $chatId"
            }

            val adminChats = getAllAdminChats()

            if (chatId in adminChats.chats) {
                return false
            }

            val modifiedChats = adminChats.chats + chatId
            val modifiedAdminChats = AdminChats(modifiedChats)
            val serializedAdminChats = modifiedAdminChats.serialize()

            setRawAdminChats(serializedAdminChats)

            return true
        }
    }

    override suspend fun isAdminChat(chat: Chat): Boolean {
        val chatId = chat.id
        val adminChats = getAllAdminChats().chats

        return chatId in adminChats
    }

    private suspend fun getAllAdminChats(): AdminChats {
        val rawChats = getRawAdminChats()

        return rawChats?.let {
            AdminChats.deserialize(rawChats)
        } ?: AdminChats(listOf())
    }

    private suspend fun getRawAdminChats() =
        redis.get(Key.ADMIN_CHATS)

    private suspend fun setRawAdminChats(rawAdminChats: String) =
        redis.setAndPersist(Key.ADMIN_CHATS, rawAdminChats)
}