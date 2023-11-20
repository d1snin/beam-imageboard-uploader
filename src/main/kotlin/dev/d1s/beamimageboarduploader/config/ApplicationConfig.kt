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

package dev.d1s.beamimageboarduploader.config

import dev.d1s.beamimageboarduploader.entity.AdminToken

data class ApplicationConfig(
    val redis: RedisConfig,
    val minio: MinioConfig,
    val beam: BeamConfig,
    val bot: TelegramBotConfig,
)

data class RedisConfig(
    val endpoint: String
)

data class MinioConfig(
    val endpoint: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String
)

data class BeamConfig(
    val space: String,
    val imageIndex: Int
)

data class TelegramBotConfig(
    val token: String,
    val adminToken: AdminToken
)