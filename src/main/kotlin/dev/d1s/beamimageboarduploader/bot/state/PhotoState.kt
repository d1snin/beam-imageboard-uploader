/*
 * Copyright 2023 Mikhail Titov
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
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPhoto
import dev.inmo.tgbotapi.types.files.PhotoSize
import dev.inmo.tgbotapi.types.message.abstracts.Message
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class PhotoState(
    override val context: Message,
    val photoState: PhotoSize
) : BotState {

    override var botMessage: Message? = null
}

class PhotoStateHandler : StateHandler, KoinComponent {

    private val imageboardService by inject<ImageboardService>()

    override suspend fun BehaviourContextWithFSM<BotState>.handle() {
        onPhoto { message ->
            val state = PhotoState(
                context = message,
                message.content.media
            )

            startChain(state)
        }

        strictlyOn<PhotoState, BotState> { state ->
            requireAuthenticatedUser<BotState?>(state) { user ->
                modifyMessage(state, uploadingText)

                imageboardService.addImage(state.photoState, user.id).getOrElse {
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

        private val uploadingText = makeTitle(Emoji.UP, "Uploading your photo to imageboard...")

        private val failureText = makeTitle(Emoji.CROSS_MARK, "Failed to upload your photo.")

        private val successText = makeTitle(Emoji.CHECK_MARK, "Your image was successfully uploaded!")
    }
}