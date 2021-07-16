@file:JsExport

import com.liftric.auth.IdentityProviderClient
import com.liftric.auth.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * Typescript compatible [IdentityProviderClient] implementation.
 */
class IdentityProviderClientJS(region: String, clientId: String) {
    private val provider: IdentityProviderClient = IdentityProviderClient(region, clientId)

    fun signUp(username: String, password: String, attributes: Array<UserAttribute>? = null): Promise<SignUpResponse> =
        MainScope().promise {
            provider.signUp(username, password, attributes?.toList())
                .getOrThrow()
        }

    fun confirmSignUp(username: String, confirmationCode: String): Promise<Unit> =
        MainScope().promise {
            provider.confirmSignUp(username, confirmationCode)
                .getOrThrow()
        }

    fun signIn(username: String, password: String): Promise<SignInResponseJS> =
        MainScope().promise {
            provider.signIn(username, password)
                .getOrThrow().let {
                    SignInResponseJS(
                        it.AuthenticationResult,
                        it.ChallengeParameters.map { MapEntry(it.key, it.value) }.toTypedArray()
                    )
                }
        }

    fun refresh(refreshToken: String): Promise<SignInResponseJS> =
        MainScope().promise {
            provider.refresh(refreshToken)
                .getOrThrow().let {
                    SignInResponseJS(
                        it.AuthenticationResult,
                        it.ChallengeParameters.map { MapEntry(it.key, it.value) }.toTypedArray()
                    )
                }
        }

    fun getUser(accessToken: String): Promise<GetUserResponseJS> =
        MainScope().promise {
            provider.getUser(accessToken)
                .getOrThrow().let {
                    GetUserResponseJS(
                        it.MFAOptions,
                        it.PreferredMfaSetting,
                        it.UserAttributes.toTypedArray(),
                        it.UserMFASettingList.toTypedArray(),
                        it.Username
                    )
                }
        }

    fun updateUserAttributes(
        accessToken: String,
        attributes: Array<UserAttribute>
    ): Promise<UpdateUserAttributesResponseJS> =
        MainScope().promise {
            provider.updateUserAttributes(accessToken, attributes.toList())
                .getOrThrow().let {
                    UpdateUserAttributesResponseJS(it.CodeDeliveryDetailsList.toTypedArray())
                }
        }

    fun changePassword(accessToken: String, currentPassword: String, newPassword: String): Promise<Unit> =
        MainScope().promise {
            provider.changePassword(accessToken, currentPassword, newPassword)
                .getOrThrow()
        }

    fun forgotPassword(username: String): Promise<ForgotPasswordResponse> =
        MainScope().promise {
            provider.forgotPassword(username)
                .getOrThrow()
        }

    fun confirmForgotPassword(confirmationCode: String, username: String, password: String): Promise<Unit> =
        MainScope().promise {
            provider.confirmForgotPassword(confirmationCode, username, password)
                .getOrThrow()
        }

    fun getUserAttributeVerificationCode(
        accessToken: String,
        attributeName: String,
        clientMetadata: Array<MapEntry>? = null
    ): Promise<GetAttributeVerificationCodeResponse> =
        MainScope().promise {
            provider.getUserAttributeVerificationCode(
                accessToken,
                attributeName,
                clientMetadata?.associate { it.key to it.value })
                .getOrThrow()
        }

    fun verifyUserAttribute(accessToken: String, attributeName: String, code: String): Promise<Unit> =
        MainScope().promise {
            provider.verifyUserAttribute(accessToken, attributeName, code)
                .getOrThrow()
        }

    fun signOut(accessToken: String): Promise<Unit> =
        MainScope().promise {
            provider.signOut(accessToken)
                .getOrThrow()
        }

    fun revokeToken(refreshToken: String): Promise<Unit> =
        MainScope().promise {
            provider.revokeToken(refreshToken)
                .getOrThrow()
        }

    fun deleteUser(accessToken: String): Promise<Unit> =
        MainScope().promise {
            provider.deleteUser(accessToken)
                .getOrThrow()
        }
}

