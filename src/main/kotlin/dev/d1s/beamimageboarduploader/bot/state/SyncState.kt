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

package dev.d1s.beamimageboarduploader.bot.state

import dev.d1s.beamimageboarduploader.service.ImageboardService
import dev.d1s.beamimageboarduploader.util.Emoji
import dev.d1s.beamimageboarduploader.util.makeTitle
import dev.d1s.beamimageboarduploader.util.requireAuthenticatedUser
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.strictlyOn
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class SyncState(
    override val context: AccessibleMessage
) : BotState {

    override var botMessage: AccessibleMessage? = null
}

class SyncStateHandler : StateHandler, KoinComponent {

    private val imageboardService by inject<ImageboardService>()

    override suspend fun BehaviourContextWithFSM<BotState>.handle() {
        strictlyOn<SyncState, BotState> { state ->
            requireAuthenticatedUser<BotState?>(state) { _ ->
                modifyMessage(state, startText)

                imageboardService.syncImages().getOrElse {
                    it.printStackTrace()

                    modifyMessage(state, failureText)

                    return@requireAuthenticatedUser null
                }

                modifyMessage(state, successText)

                null
            }
        }
    }

    private companion object {

        private val startText = makeTitle(Emoji.UP, "Syncing...")
        private val successText = makeTitle(Emoji.CHECK_MARK, "Synced!")

        private val failureText = makeTitle(Emoji.CROSS_MARK, "Failed to sync your images.")
    }
}