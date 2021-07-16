package com.liftric.auth.core

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
internal data class SignIn(
    val AuthFlow: String,
    val ClientId: String,
    val AuthParameters: Parameters
) {
    @Serializable
    internal data class Parameters(
        val USERNAME: String,
        val PASSWORD: String
    )
}

@Serializable
internal data class Refresh(
    val AuthFlow: String,
    val ClientId: String,
    val AuthParameters: Parameters
) {
    @Serializable
    internal data class Parameters(
        val REFRESH_TOKEN: String
    )
}

@Serializable
internal data class AccessToken(
    val AccessToken: String
)

@Serializable
internal data class RevokeToken(
    val ClientId: String,
    val Token: String,
)

@Serializable
internal data class ChangePassword(
    val AccessToken: String,
    val PreviousPassword: String,
    val ProposedPassword: String
)

@Serializable
internal data class SignUp(
    val ClientId: String,
    val Password: String,
    val Username: String,
    val UserAttributes: List<UserAttribute>
)

@Serializable
internal data class ConfirmSignUp(
    val ClientId: String,
    val Username: String,
    val ConfirmationCode: String
)

@Serializable
internal data class ForgotPassword(
    val ClientId: String,
    val Username: String
)

@Serializable
internal data class ConfirmForgotPassword(
    val ClientId: String,
    val ConfirmationCode: String,
    val Username: String,
    val Password: String
)

@JsExport
@Serializable
data class UserAttribute(
    val Name: String,
    val Value: String
)

@Serializable
internal data class UpdateUserAttributes(
    val AccessToken: String,
    val UserAttributes: List<UserAttribute>
)

@Serializable
internal data class GetUserAttributeVerificationCode(
    val AccessToken: String,
    val AttributeName: String,
    val ClientMetadata: Map<String, String>? = null
)

@Serializable
internal data class VerifyUserAttribute(
    val AccessToken: String,
    val AttributeName: String,
    val Code: String
)
