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

package dev.d1s.beamimageboarduploader.bot.command

import dev.d1s.beamimageboarduploader.bot.state.BotState
import dev.d1s.beamimageboarduploader.service.AdminService
import dev.d1s.beamimageboarduploader.service.AuthenticationService
import dev.d1s.beamimageboarduploader.util.Emoji
import dev.d1s.beamimageboarduploader.util.makeTitle
import dev.d1s.beamimageboarduploader.util.withTitle
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.regular
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

const val START_COMMAND = "start"

class StartCommand : Command, KoinComponent {

    override val name = START_COMMAND

    override val description = "Start authentication process"

    override val hidden = false

    private val authenticationService by inject<AuthenticationService>()

    private val adminService by inject<AdminService>()

    private val log = logging()

    @OptIn(RiskFeature::class)
    override suspend fun BehaviourContextWithFSM<BotState>.onCommand(message: TextMessage) {
        val waitTokenMessage = makeTitle(Emoji.LOCK, "Please send me admin token.")

        val chat = message.chat

        sendMessage(chat, waitTokenMessage)

        val token = waitTextMessage().filter {
            it.from == message.from
        }.first()

        val tokenString = token.content.text

        log.d {
            "Waited for admin token: $tokenString"
        }

        val authenticated = authenticationService.authenticate(tokenString).authenticated

        if (authenticated) {
            val added = adminService.addAdminChat(chat)

            if (added) {
                val success = withTitle(Emoji.CHECK_MARK, "Fine! This chat is now authorized.") {
                    regular("You can now send me photos and I will process them right into your imageboard.")
                }

                reply(token, success)
            } else {
                val failure = makeTitle(Emoji.CROSS_MARK, "This chat is authorized.")
                reply(token, failure)
            }
        } else {
            val failure = makeTitle(Emoji.CROSS_MARK, "Wrong token.")
            reply(token, failure)
        }
    }
}