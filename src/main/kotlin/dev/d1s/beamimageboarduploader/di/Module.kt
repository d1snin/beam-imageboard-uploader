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

package dev.d1s.beamimageboarduploader.di

import dev.d1s.beam.client.app.ApplicationContext
import dev.d1s.beamimageboarduploader.BeamImageboardUploaderApplication
import dev.d1s.beamimageboarduploader.bot.DefaultTelegramBot
import dev.d1s.beamimageboarduploader.bot.TelegramBot
import dev.d1s.beamimageboarduploader.bot.command.*
import dev.d1s.beamimageboarduploader.bot.state.PhotoStateHandler
import dev.d1s.beamimageboarduploader.bot.state.StateHandler
import dev.d1s.beamimageboarduploader.bot.state.StreamStateHandler
import dev.d1s.beamimageboarduploader.bot.state.SyncStateHandler
import dev.d1s.beamimageboarduploader.config.ApplicationConfigFactory
import dev.d1s.beamimageboarduploader.config.DefaultApplicationConfigFactory
import dev.d1s.beamimageboarduploader.database.DefaultRedisClientFactory
import dev.d1s.beamimageboarduploader.database.RedisClientFactory
import dev.d1s.beamimageboarduploader.s3.DefaultMinioClientFactory
import dev.d1s.beamimageboarduploader.s3.MinioClientFactory
import dev.d1s.beamimageboarduploader.service.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger

fun setupDi(context: ApplicationContext) {
    startKoin {
        logger(SLF4JLogger())

        val mainModule = module {
            single {
                context
            }

            application()
            telegramBot()
            commandHolder()
            commands()
            stateHandlers()
            applicationConfig()
            applicationConfigFactory()
            redisClientFactory()
            minioClientFactory()
            services()
        }

        modules(mainModule)
    }
}

fun Module.application() {
    singleOf(::BeamImageboardUploaderApplication)
}

fun Module.telegramBot() {
    singleOf<TelegramBot>(::DefaultTelegramBot)
}

fun Module.commandHolder() {
    singleOf<CommandHolder>(::DefaultCommandHolder)
}

fun Module.commands() {
    singleOf<Command>(::StartCommand) {
        qualifier = Qualifier.StartCommand
    }

    singleOf<Command>(::StreamCommand) {
        qualifier = Qualifier.StreamCommand
    }

    singleOf<Command>(::SyncCommand) {
        qualifier = Qualifier.SyncCommand
    }
}

fun Module.stateHandlers() {
    singleOf<StateHandler>(::PhotoStateHandler) {
        qualifier = Qualifier.PhotoStateHandler
    }

    singleOf<StateHandler>(::StreamStateHandler) {
        qualifier = Qualifier.StreamStateHandler
    }

    singleOf<StateHandler>(::SyncStateHandler) {
        qualifier = Qualifier.SyncStateHandler
    }
}

fun Module.applicationConfig() {
    single {
        get<ApplicationConfigFactory>().config
    }
}

fun Module.applicationConfigFactory() {
    singleOf<ApplicationConfigFactory>(::DefaultApplicationConfigFactory)
}

fun Module.redisClientFactory() {
    singleOf<RedisClientFactory>(::DefaultRedisClientFactory)
}

fun Module.minioClientFactory() {
    singleOf<MinioClientFactory>(::DefaultMinioClientFactory)
}

fun Module.services() {
    singleOf<AuthenticationService>(::DefaultAuthenticationService)
    singleOf<AdminService>(::DefaultAdminService)
    singleOf<StorageService>(::DefaultStorageService)
    singleOf<ImageboardService>(::DefaultImageboardService)
}