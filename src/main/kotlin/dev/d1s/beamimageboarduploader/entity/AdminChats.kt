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

package dev.d1s.beamimageboarduploader.entity

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.RawChatId

@JvmInline
value class AdminChats(
    val chats: List<IdChatIdentifier>
) {
    fun serialize() =
        chats.joinToString(separator = SEPARATOR) {
            it.chatId.toString()
        }

    companion object {

        private const val SEPARATOR = ";"

        fun deserialize(chats: String): AdminChats {
            val chatIds = chats.split(SEPARATOR).map {
                ChatId(RawChatId(it.toLong()))
            }

            return AdminChats(chatIds)
        }
    }
}