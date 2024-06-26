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

import dev.d1s.beam.client.app.ApplicationContext
import dev.d1s.beam.client.app.state.SpaceContext
import dev.d1s.beam.client.app.state.block
import dev.d1s.beam.client.app.state.space
import dev.d1s.beam.client.fullWidthImage
import dev.d1s.beam.client.setBlockImageEntityFluid
import dev.d1s.beam.client.setRowStretch
import dev.d1s.beam.commons.Block
import dev.d1s.beam.commons.BlockSize
import dev.d1s.beam.commons.RowAlign
import dev.d1s.beamimageboarduploader.config.ApplicationConfig
import dev.d1s.beamimageboarduploader.util.MetadataKeys
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface ImageboardService {

    suspend fun addImage(file: TelegramMediaFile, chatId: IdChatIdentifier): Result<Block>

    suspend fun streamImageBlocks(process: suspend (Block) -> Unit): Result<Unit>

    suspend fun syncImages(): Result<Unit>

    suspend fun initSpace()
}

class DefaultImageboardService : ImageboardService, KoinComponent {

    private val applicationContext by inject<ApplicationContext>()

    private val storageService by inject<StorageService>()

    private val appConfig by inject<ApplicationConfig>()

    private lateinit var spaceContext: SpaceContext

    private val mutex = Mutex()

    private val log = logging()

    override suspend fun addImage(file: TelegramMediaFile, chatId: IdChatIdentifier): Result<Block> =
        runCatching {
            mutex.withLock(owner = chatId) {
                log.i {
                    "Adding photo ${file.fileId} to your imageboard..."
                }

                val imageUrl = storageService.uploadFile(file).getOrThrow().toString()
                addImageBlock(imageUrl)
            }
        }

    override suspend fun streamImageBlocks(process: suspend (Block) -> Unit): Result<Unit> =
        runCatching {
            applicationContext.iterateBlocks(appConfig.beam.space) { block ->
                if (block.metadata[MetadataKeys.IMAGEBOARD_BLOCK_MANAGED] == "true") {
                    process(block)
                }
            }.getOrThrow()
        }

    override suspend fun syncImages(): Result<Unit> =
        runCatching {
            log.i {
                "Syncing images..."
            }

            streamImageBlocks { block ->
                log.d {
                    "Removing block ${block.id}..."
                }

                applicationContext.deleteBlock(block.id).getOrThrow()
            }.getOrThrow()

            storageService.streamImageUrls { url ->
                log.d {
                    "Adding block with image '$url'..."
                }

                addImageBlock(url)
            }.getOrThrow()
        }

    override suspend fun initSpace() {
        applicationContext.space(appConfig.beam.space) {
            configureRow()

            spaceContext = this
        }
    }

    private suspend fun SpaceContext.configureRow() {
        setRow {
            align = RowAlign.CENTER

            metadata {
                setRowStretch(stretch = false)
            }
        }
    }

    private suspend fun addImageBlock(imageUrl: String) =
        spaceContext.block(manage = false) {
            setIndex {
                appConfig.beam.imageIndex
            }

            setSize {
                BlockSize.MEDIUM
            }

            setEntities {
                fullWidthImage(url = imageUrl)
            }

            setMetadata {
                setBlockImageEntityFluid()
                metadata(MetadataKeys.IMAGEBOARD_BLOCK_MANAGED, "true")
            }
        }
}