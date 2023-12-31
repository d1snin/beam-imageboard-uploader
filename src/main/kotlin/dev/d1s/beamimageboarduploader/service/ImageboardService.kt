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
import dev.d1s.exkt.common.pagination.Paginator
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.files.PhotoSize
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface ImageboardService {

    suspend fun addImage(photoSize: PhotoSize, chatId: IdChatIdentifier): Result<Block>

    suspend fun streamImageBlocks(process: suspend (List<Block>) -> Unit): Result<Unit>

    suspend fun initSpace()
}

class DefaultImageboardService : ImageboardService, KoinComponent {

    private val applicationContext by inject<ApplicationContext>()

    private val storageService by inject<StorageService>()

    private val config by inject<ApplicationConfig>()

    private lateinit var spaceContext: SpaceContext

    private val mutex = Mutex()

    private val log = logging()

    override suspend fun addImage(photoSize: PhotoSize, chatId: IdChatIdentifier): Result<Block> =
        runCatching {
            mutex.withLock(owner = chatId) {
                log.i {
                    "Adding photo ${photoSize.fileId} to your imageboard..."
                }

                val imageUrl = storageService.uploadFile(photoSize).getOrThrow().toString()

                val block = spaceContext.block {
                    setIndex {
                        config.beam.imageIndex
                    }

                    setSize {
                        BlockSize.MEDIUM
                    }

                    setEntities {
                        fullWidthImage(url = imageUrl)
                    }

                    setMetadata {
                        setBlockImageEntityFluid()
                    }
                }

                block
            }
        }

    override suspend fun streamImageBlocks(process: suspend (List<Block>) -> Unit): Result<Unit> =
        runCatching {
            val paginator = Paginator(STREAM_BATCH_SIZE, currentPage = 1)

            suspend fun getBlocks() =
                applicationContext.getBlocks(config.beam.space, limitAndOffset = paginator.limitAndOffset)
                    .getOrThrow()
                    .elements
                    .filter {
                        it.index >= config.beam.imageIndex
                    }

            var batch = getBlocks()

            while (batch.isNotEmpty()) {
                process(batch)

                paginator.currentPage++
                batch = getBlocks()
            }
        }

    override suspend fun initSpace() {
        applicationContext.space(config.beam.space, processBlocks = false) {
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

    private companion object {

        private const val STREAM_BATCH_SIZE = 50
    }
}