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
import dev.inmo.tgbotapi.types.message.abstracts.Message
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class StreamState(
    override val context: Message
) : BotState {

    override var botMessage: Message? = null
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