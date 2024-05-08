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

package dev.d1s.beamimageboarduploader.bot.command

import dev.d1s.beamimageboarduploader.bot.state.BotState
import dev.d1s.beamimageboarduploader.bot.state.SyncState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.types.message.content.TextMessage
import org.koin.core.component.KoinComponent

const val SYNC_COMMAND = "sync"

class SyncCommand : Command, KoinComponent {

    override val name = SYNC_COMMAND

    override val description = "Sync all pictures with storage"

    override val hidden = false

    override suspend fun BehaviourContextWithFSM<BotState>.onCommand(message: TextMessage) {
        val state = SyncState(context = message)

        startChain(state)
    }
}