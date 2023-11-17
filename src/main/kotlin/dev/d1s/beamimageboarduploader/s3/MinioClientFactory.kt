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

package dev.d1s.beamimageboarduploader.s3

import dev.d1s.beamimageboarduploader.config.ApplicationConfig
import io.minio.MinioClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface MinioClientFactory {

    val minio: MinioClient

    suspend fun connect()
}

class DefaultMinioClientFactory : MinioClientFactory, KoinComponent {

    private val config by inject<ApplicationConfig>()

    private val log = logging()

    private var internalMinio: MinioClient? = null

    override val minio get() = internalMinio ?: error("Minio client is not initialized")

    override suspend fun connect() {
        log.d {
            "Configuring Minio client..."
        }

        val minioConfig = config.minio

        val endpoint = minioConfig.endpoint
        val accessKeyId = minioConfig.accessKeyId
        val secretAccessKey = minioConfig.secretAccessKey

        val minioClient = createMinioClient(endpoint, accessKeyId, secretAccessKey)

        internalMinio = minioClient
    }

    private fun createMinioClient(endpoint: String, accessKeyId: String, secretAccessKey: String) =
        with(MinioClient.builder()) {
            endpoint(endpoint)
            credentials(accessKeyId, secretAccessKey)

            build()
        }
}