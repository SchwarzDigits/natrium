/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the EUPL v. 1.2 only.
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.cli.commands

import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.devices.DeviceLimitResolver
import schwarz.digits.natrium.devices.ListDevicesResult
import schwarz.digits.natrium.devices.RemoveDeviceResult
import schwarz.digits.natrium.session.LoginError
import schwarz.digits.natrium.session.LoginResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking

class LoginCommand : CliktCommand(name = "login") {

    private val email: String by option(
        "-e", "--email",
        help = "Account email"
    ).prompt("Email")

    private val password: String by option(
        "-p", "--password",
        help = "Account password"
    ).prompt("Password", hideInput = true)

    override fun run() = runBlocking {
        echo("Connecting ...")

        var result = Natrium.login(email, password)

        // 2FA handling
        if (result is LoginResult.Failure.Error && result.reason == LoginError.SECOND_FA_CODE_REQUIRED) {
            echo("2FA required")
            echo("2FA code: ", trailingNewline = false)
            val code = readln().trim()
            result = Natrium.login(email, password, secondFactorVerificationCode = code)
        }

        result = handleLoginResult(result)

        if (result is LoginResult.Success) {
            val info = result.session.sessionInfo()
            echo("Login successful")
            echo("User: ${info?.user?.name ?: "unknown"}")
            echo("Device: ${info?.device?.id ?: "unknown"}")
        }
    }

    private suspend fun handleLoginResult(result: LoginResult): LoginResult {
        when (result) {
            is LoginResult.Success -> return result
            is LoginResult.Failure.TooManyDevices -> return handleTooManyDevices(result.resolver)
            is LoginResult.Failure.Error -> {
                when (result.reason) {
                    LoginError.EMAIL_OR_PASSWORD_WRONG -> echo("Login failed: invalid credentials")
                    LoginError.SECOND_FA_CODE_REQUIRED -> echo("2FA failed")
                    LoginError.INVALID_2FA_CODE -> echo("Invalid 2FA code")
                    LoginError.CONNECTION_ERROR -> echo("Network error")
                    LoginError.LOGIN_FAILED -> echo("Login failed")
                    LoginError.ACCOUNT_LOCKED -> echo("Account locked")
                    LoginError.ACCOUNT_NOT_ACTIVATED -> echo("Account not activated")
                    LoginError.CLIENT_REGISTRATION_FAILED -> echo("Client registration failed")
                    LoginError.SESSION_COULD_NOT_BE_SAVED -> echo("Session could not be saved")
                    LoginError.SERVER_VERSION_NOT_SUPPORTED -> echo("Server version not supported")
                    LoginError.APP_UPDATE_REQUIRED -> echo("App update required")
                }
                return result
            }
        }
    }

    private suspend fun handleTooManyDevices(resolver: DeviceLimitResolver): LoginResult {
        echo("Too many devices registered. You need to remove one to continue.")

        when (val listResult = resolver.listDevices()) {
            is ListDevicesResult.Success -> {
                echo("\nRegistered devices:")
                listResult.devices.forEachIndexed { index, device ->
                    val label = device.label ?: device.model ?: "Unknown device"
                    echo("  ${index + 1}. $label (ID: ${device.id})")
                }

                echo("\nSelect a device to remove (number): ", trailingNewline = false)
                val selection = readln().trim().toIntOrNull()
                if (selection == null || selection < 1 || selection > listResult.devices.size) {
                    echo("Invalid selection. Login aborted.")
                    return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                }

                val deviceToRemove = listResult.devices[selection - 1]
                echo("Removing device: ${deviceToRemove.label ?: deviceToRemove.id}")

                when (val removeResult = resolver.removeDevice(deviceToRemove.id, password)) {
                    is RemoveDeviceResult.Success -> {
                        echo("Device removed. Retrying client registration...")
                        return handleLoginResult(resolver.retry())
                    }
                    is RemoveDeviceResult.Failure.PasswordRequired -> {
                        echo("Password required to remove device.")
                        return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                    }
                    is RemoveDeviceResult.Failure.InvalidCredentials -> {
                        echo("Invalid credentials for device removal.")
                        return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                    }
                    is RemoveDeviceResult.Failure -> {
                        echo("Failed to remove device.")
                        return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
                    }
                }
            }
            is ListDevicesResult.Failure -> {
                echo("Failed to load device list.")
                return LoginResult.Failure.Error(LoginError.CLIENT_REGISTRATION_FAILED)
            }
        }
    }
}
