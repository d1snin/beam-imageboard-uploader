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

import dev.d1s.beam.commons.contententity.Image
import dev.d1s.beam.commons.contententity.get
import dev.d1s.beamimageboarduploader.service.ImageboardService
import dev.d1s.beamimageboarduploader.util.Emoji
import dev.d1s.beamimageboarduploader.util.makeTitle
import dev.d1s.beamimageboarduploader.util.requireAuthenticatedUser
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.strictlyOn
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class StreamState(
    override val context: AccessibleMessage
) : BotState {

    override var botMessage: AccessibleMessage? = null
}

class StreamStateHandler : StateHandler, KoinComponent {

    private val imageboardService by inject<ImageboardService>()

    override suspend fun BehaviourContextWithFSM<BotState>.handle() {
        strictlyOn<StreamState, BotState> { state ->
            requireAuthenticatedUser<BotState?>(state) { _ ->
                modifyMessage(state, startText)

                imageboardService.streamImageBlocks { blocks ->
                    blocks.forEach { imageBlock ->
                        val imageEntity = imageBlock.entities.find {
                            it.type == Image.name
                        }

                        imageEntity?.let { image ->
                            val url = image.parameters[Image.url]
                            requireNotNull(url)

                            sendPhoto(state.context.chat, InputFile.fromUrl(url))
                        }
                    }

                }.getOrElse {
                    it.printStackTrace()

                    modifyMessage(state, failureText)

                    return@requireAuthenticatedUser null
                }

                null
            }
        }
    }

    private companion object {

        private val startText = makeTitle(Emoji.UP, "Streaming your imageboard now...")

        private val failureText = makeTitle(Emoji.CROSS_MARK, "Failed to stream your images.")
    }
}