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

package dev.d1s.beamimageboarduploader.service

import dev.d1s.beamimageboarduploader.bot.TelegramBot
import dev.d1s.beamimageboarduploader.config.ApplicationConfig
import dev.d1s.beamimageboarduploader.s3.MinioClientFactory
import dev.inmo.tgbotapi.extensions.api.files.downloadFileStream
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.types.files.PhotoSize
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import io.minio.ListObjectsArgs
import io.minio.PutObjectArgs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface StorageService {

    suspend fun uploadFile(photoSize: PhotoSize): Result<Url>

    suspend fun streamImageUrls(process: suspend (String) -> Unit): Result<Unit>
}

class DefaultStorageService : StorageService, KoinComponent {

    private val minioClientFactory by inject<MinioClientFactory>()

    private val minio by lazy {
        minioClientFactory.minio
    }

    private val bot by inject<TelegramBot>()

    private val requestExecutor by lazy {
        bot.requestExecutor
    }

    private val config by inject<ApplicationConfig>()

    private val mutex = Mutex()

    private val log = logging()

    private val bucket by lazy {
        config.minio.bucketName
    }

    override suspend fun uploadFile(photoSize: PhotoSize): Result<Url> =
        runCatching {
            mutex.withLock {
                val fileId = photoSize.fileId
                val uniqueFileId = photoSize.fileUniqueId
                val objectName = "$uniqueFileId.jpg"

                log.i {
                    "Uploading file '$objectName' to bucket '$bucket'"
                }

                val file = requestExecutor.downloadFileStream(fileId).toInputStream()
                val fileSize = requestExecutor.getFileAdditionalInfo(fileId).fileSize
                requireNotNull(fileSize) {
                    "File size is null"
                }

                log.d {
                    "File size: $fileSize"
                }

                val args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .stream(file, fileSize, DEFAULT_PART_SIZE)
                    .contentType(ContentType.Image.JPEG.toString())
                    .build()

                minio.putObject(args)

                val url = URLBuilder(config.minio.endpoint).apply {
                    path(bucket, objectName)
                }.build()

                url
            }
        }

    override suspend fun streamImageUrls(process: suspend (String) -> Unit): Result<Unit> =
        runCatching {
            val args = ListObjectsArgs.builder()
                .bucket(bucket)
                .build()

            val objects = minio.listObjects(args)

            objects.forEach {
                val objectName = it.get().objectName()
                val url = URLBuilder(config.minio.endpoint).apply {
                    path(config.minio.bucketName, objectName)
                }.buildString()

                process(url)
            }
        }

    private companion object {

        private const val DEFAULT_PART_SIZE = -1L
    }
}