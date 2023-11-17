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
import dev.inmo.tgbotapi.types.files.PhotoSize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface ImageboardService {

    suspend fun addImage(photoSize: PhotoSize): Result<Block>

    suspend fun initSpace()
}

class DefaultImageboardService : ImageboardService, KoinComponent {

    private val applicationContext by inject<ApplicationContext>()

    private val storageService by inject<StorageService>()

    private val config by inject<ApplicationConfig>()

    private lateinit var spaceContext: SpaceContext

    private val log = logging()

    override suspend fun addImage(photoSize: PhotoSize): Result<Block> =
        runCatching {
            log.i {
                "Adding photo ${photoSize.fileId} to your imageboard..."
            }

            val imageUrl = storageService.uploadFile(photoSize).getOrThrow().toString()

            val block = spaceContext.block {
                setIndex { 0 }

                setSize {
                    BlockSize.HALF
                }

                setEntities {
                    fullWidthImage(url = imageUrl)
                }

                setMetadata {
                    setBlockImageEntityFluid()
                }
            }

            spaceContext.configureRow()

            block
        }

    override suspend fun initSpace() {
        applicationContext.space(config.beam.space, processBlocks = false) {
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
}