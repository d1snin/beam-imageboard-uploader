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
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocumentsGroupMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPhotoGallery
import dev.inmo.tgbotapi.types.files.DocumentFile
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import javax.activation.MimeType

data class ImageState(
    override val context: AccessibleMessage,
    val file: TelegramMediaFile
) : BotState {

    override var botMessage: AccessibleMessage? = null
}

class ImageStateHandler : StateHandler, KoinComponent {

    private val imageboardService by inject<ImageboardService>()

    override suspend fun BehaviourContextWithFSM<BotState>.handle() {
        handlePhoto()
        handleDocument()

        strictlyOn<ImageState, BotState> { state ->
            requireAuthenticatedUser<BotState?>(state) { user ->
                modifyMessage(state, uploadingText)

                imageboardService.addImage(state.file, user.id).getOrElse {
                    it.printStackTrace()

                    modifyMessage(state, failureText)

                    return@requireAuthenticatedUser null
                }

                modifyMessage(state, successText)

                null
            }
        }
    }

    private suspend fun BehaviourContextWithFSM<BotState>.handlePhoto() {
        onPhoto { message ->
            startChainWithState(message, message.content.media)
        }

        onPhotoGallery { message ->
            message.group.forEach {
                startChainWithState(it.sourceMessage, it.content.media)
            }
        }
    }

    private suspend fun BehaviourContextWithFSM<BotState>.handleDocument() {
        suspend fun processDocument(message: AccessibleMessage, media: DocumentFile) {
            val requiredMime = MimeType("image/*")

            if (media.mimeType?.match(requiredMime) == true) {
                startChainWithState(message, media)
            }
        }

        onDocument { message ->
            processDocument(message, message.content.media)
        }

        onDocumentsGroupMessages { message ->
            message.content.group.forEach {
                processDocument(message, it.content.media)
            }
        }
    }

    private suspend fun BehaviourContextWithFSM<BotState>.startChainWithState(
        message: AccessibleMessage,
        file: TelegramMediaFile
    ) {
        val state = ImageState(
            context = message,
            file
        )

        startChain(state)
    }

    private companion object {

        private val uploadingText = makeTitle(Emoji.UP, "Uploading your photo to imageboard...")

        private val failureText = makeTitle(Emoji.CROSS_MARK, "Failed to upload your photo.")

        private val successText = makeTitle(Emoji.CHECK_MARK, "Your image was successfully uploaded!")
    }
}