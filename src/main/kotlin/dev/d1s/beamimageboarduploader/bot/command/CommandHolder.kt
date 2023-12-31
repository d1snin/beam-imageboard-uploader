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

import dev.inmo.tgbotapi.types.BotCommand
import org.koin.core.component.KoinComponent

interface CommandHolder {

    val commands: List<Command>

    val botCommands: List<BotCommand>

    val filteredBotCommands: List<BotCommand>
}

class DefaultCommandHolder : CommandHolder, KoinComponent {

    override val commands by lazy {
        getKoin().getAll<Command>()
    }

    override val botCommands by lazy {
        commands.toBotCommands()
    }

    override val filteredBotCommands by lazy {
        val filteredBotCommands = commands.filter {
            !it.hidden
        }

        filteredBotCommands.toBotCommands()
    }
}