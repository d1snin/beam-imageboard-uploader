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

package dev.d1s.beamimageboarduploader

import dev.d1s.beam.client.app.ApplicationContext
import dev.d1s.beam.client.app.BeamClientApplication
import dev.d1s.beamimageboarduploader.bot.TelegramBot
import dev.d1s.beamimageboarduploader.database.RedisClientFactory
import dev.d1s.beamimageboarduploader.di.setupDi
import dev.d1s.beamimageboarduploader.s3.MinioClientFactory
import dev.d1s.beamimageboarduploader.service.ImageboardService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

class BeamImageboardUploaderApplication : BeamClientApplication(), KoinComponent {

    private val log = logging()

    private val redisClientFactory by inject<RedisClientFactory>()

    private val minioClientFactory by inject<MinioClientFactory>()

    private val imageboardService by inject<ImageboardService>()

    private val telegramBot by inject<TelegramBot>()

    override suspend fun ApplicationContext.run() {
        log.i {
            "Beam Imageboard Uploader bot is starting (${config.name})..."
        }

        setupDi(this)

        redisClientFactory.connect()
        minioClientFactory.connect()

        imageboardService.initSpace()

        val job = telegramBot.startTelegramBot()

        log.i {
            "Beam Imageboard Uploader bot is ready!"
        }

        job.join()
    }
}