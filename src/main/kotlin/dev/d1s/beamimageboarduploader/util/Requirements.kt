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

package dev.d1s.beamimageboarduploader.util

import dev.d1s.beamimageboarduploader.bot.command.START_COMMAND
import dev.d1s.beamimageboarduploader.bot.state.BotState
import dev.d1s.beamimageboarduploader.bot.state.modifyMessage
import dev.d1s.beamimageboarduploader.service.AdminService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.fromUserOrNull
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import org.koin.core.context.GlobalContext

private val adminService by GlobalContext.get().inject<AdminService>()

@OptIn(PreviewFeature::class)
suspend inline fun <R> BehaviourContextWithFSM<BotState>.requireUser(state: BotState, block: (User) -> R?): R? {
    val user = state.context.fromUserOrNull()?.user

    return if (user != null) {
        block(user)
    } else {
        notAvailable(state)
        null
    }
}

suspend inline fun <R> BehaviourContextWithFSM<BotState>.requirePrivateChat(
    state: BotState,
    block: (PrivateChat) -> R?
): R? {
    val chat = state.context.chat.privateChatOrNull()

    return if (chat != null) {
        block(chat)
    } else {
        val privateChatRequiredMessage =
            makeTitle(Emoji.CROSS_MARK, "To run this command you need to be in a private chat with bot.")

        modifyMessage(state, privateChatRequiredMessage)

        null
    }
}

suspend inline fun <R> BehaviourContextWithFSM<BotState>.requirePrivateChatAndUser(
    state: BotState,
    block: (PrivateChat, User) -> R?
): R? = requirePrivateChat(state) { privateChat ->
    requireUser(state) { user ->
        block(privateChat, user)
    }
}

suspend fun <R> BehaviourContextWithFSM<BotState>.requireAuthenticatedUser(
    state: BotState,
    block: suspend (User) -> R?
): R? = requirePrivateChatAndUser(state) { chat, user ->
    val authenticated = adminService.isAdminChat(chat)

    if (authenticated) {
        block(user)
    } else {
        val notAuthenticatedMessage = withTitle(Emoji.CROSS_MARK, "You are not authorized.") {
            +"Login with "
            botCommand(START_COMMAND)
        }

        modifyMessage(state, notAuthenticatedMessage)

        null
    }
}

suspend fun BehaviourContextWithFSM<BotState>.notAvailable(state: BotState) {
    val content = makeTitle(Emoji.CROSS_MARK, "Not available.")

    modifyMessage(state, content)
}