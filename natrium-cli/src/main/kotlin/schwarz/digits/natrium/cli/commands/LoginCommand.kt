/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
