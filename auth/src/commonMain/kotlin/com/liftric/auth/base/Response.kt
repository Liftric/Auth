@file:OptIn(ExperimentalJsExport::class) // redundant because IDEA ignores configured freeCompilerArgs

/**
 * Don't forget to check [ResponseJS.kt] when doing changes here :)
 */
package com.liftric.auth.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@Serializable
data class RequestError(
    @SerialName("__type")
    val type: String,
    val message: String
)

@Serializable
data class SignInResponse(
    val AuthenticationResult: AuthenticationResult,
    val ChallengeParameters: Map<String, String> = mapOf()
)

@JsExport
@Serializable
data class AuthenticationResult(
    val AccessToken: String = "",
    val ExpiresIn: Int = 0,
    val IdToken: String = "",
    val RefreshToken: String = "",
    val TokenType: String = ""
)

@JsExport
@Serializable
data class SignUpResponse(
    val CodeDeliveryDetails: CodeDeliveryDetails = CodeDeliveryDetails(),
    val UserConfirmed: Boolean = false,
    val UserSub: String = ""
)

@JsExport
@Serializable
data class CodeDeliveryDetails(
    val AttributeName: String = "",
    val DeliveryMedium: String = "",
    val Destination: String = ""
)

@Serializable
data class GetUserResponse(
    val MFAOptions: MFAOptions = MFAOptions(),
    val PreferredMfaSetting: String = "",
    val UserAttributes: List<UserAttribute> = listOf(),
    val UserMFASettingList: List<String> = listOf(),
    val Username: String
)

@JsExport
@Serializable
data class MFAOptions(
    val AttributeName: String = "",
    val DeliveryMedium: String = ""
)

@Serializable
data class UpdateUserAttributesResponse(
    val CodeDeliveryDetailsList: List<CodeDeliveryDetails> = listOf()
)

@JsExport
@Serializable
data class GetAttributeVerificationCodeResponse(
    val CodeDeliveryDetails: CodeDeliveryDetails = CodeDeliveryDetails()
)

@JsExport
@Serializable
data class ForgotPasswordResponse(
    val CodeDeliveryDetails: CodeDeliveryDetails = CodeDeliveryDetails()
)
