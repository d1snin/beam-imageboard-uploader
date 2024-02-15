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

import dev.d1s.beamimageboarduploader.util.editMessageTextAndMarkup
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList

interface BotState : State {

    override val context: AccessibleMessage

    var botMessage: AccessibleMessage?
}

suspend fun BehaviourContext.modifyMessage(
    state: BotState,
    text: TextSourcesList,
    markup: InlineKeyboardMarkup? = null
) {
    val botMessage = state.botMessage

    if (botMessage != null) {
        editMessageTextAndMarkup(botMessage, text, markup)
    } else {
        sendMessage(state, text, markup)
    }
}

suspend fun BehaviourContext.sendMessage(
    state: BotState,
    text: TextSourcesList,
    markup: InlineKeyboardMarkup? = null
) {
    val message = sendMessage(state.context.chat, text, replyMarkup = markup)
    state.botMessage = message
}