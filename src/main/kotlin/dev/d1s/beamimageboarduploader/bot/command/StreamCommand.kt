package dev.d1s.beamimageboarduploader.bot.command

import dev.d1s.beamimageboarduploader.bot.state.BotState
import dev.d1s.beamimageboarduploader.bot.state.StreamState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.types.message.content.TextMessage
import org.koin.core.component.KoinComponent

const val STREAM_COMMAND = "stream"

class StreamCommand : Command, KoinComponent {

    override val name = STREAM_COMMAND

    override val description = "Stream all pictures into chat"

    override val hidden = false

    override suspend fun BehaviourContextWithFSM<BotState>.onCommand(message: TextMessage) {
        val state = StreamState(context = message)

        startChain(state)
    }
}