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

import dev.d1s.beamimageboarduploader.config.ApplicationConfig
import dev.d1s.beamimageboarduploader.entity.AdminToken
import dev.d1s.beamimageboarduploader.entity.AuthenticationResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lighthousegames.logging.logging

interface AuthenticationService {

    suspend fun authenticate(adminToken: AdminToken): AuthenticationResult
}

class DefaultAuthenticationService : AuthenticationService, KoinComponent {

    private val config by inject<ApplicationConfig>()

    private val log = logging()

    override suspend fun authenticate(adminToken: AdminToken): AuthenticationResult {
        val realToken = config.bot.adminToken

        log.i {
            "Trying to authenticate $adminToken against $realToken"
        }

        val result = AuthenticationResult(adminToken == realToken)

        log.i {
            "Authenticated: ${result.authenticated}"
        }

        return result
    }
}