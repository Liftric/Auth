package com.liftric.auth

import com.liftric.auth.base.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * AWS Cognito authentication client
 * Provides common request methods
 */
open class AuthHandler(private val configuration: Configuration) : Auth {
    enum class RequestType {
        signIn, signUp, confirmSignUp, signOut, getUser, changePassword,
        deleteUser, updateUserAttributes, forgotPassword, confirmForgotPassword,
        getUserAttributeVerificationCode, verifyUserAttribute
    }

    private val client = HttpClient() {
        defaultRequest {
            configuration.setupDefaultRequest(headers)
            contentType(ContentType.parse(Header.AmzJson))
        }
    }

    //----------
    // INTERFACE
    //----------

    override suspend fun signUp(
        username: String,
        password: String,
        attributes: List<UserAttribute>?
    ): Result<SignUpResponse> {
        return request(
            RequestType.signUp,
            serialize(
                SignUp.serializer(),
                SignUp(
                    ClientId = configuration.clientId,
                    Username = username,
                    Password = password,
                    UserAttributes = attributes ?: listOf()
                )
            )
        ).onResult {
            try {
                Result.success(parse(SignUpResponse.serializer(), it))
            } catch (e: SerializationException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun confirmSignUp(
        username: String,
        confirmationCode: String
    ): Result<Unit> {
        return request(
            RequestType.confirmSignUp,
            serialize(
                ConfirmSignUp.serializer(),
                ConfirmSignUp(
                    configuration.clientId,
                    username,
                    confirmationCode
                )
            )
        ).onResult {
            Result.success(Unit)
        }
    }

    override suspend fun signIn(
        username: String,
        password: String
    ): Result<SignInResponse> {
        return request(
            RequestType.signIn,
            serialize(
                Authentication.serializer(),
                Authentication(
                    AuthFlow.UserPasswordAuth,
                    configuration.clientId,
                    AuthParameters(username, password)
                )
            )
        ).onResult {
            try {
                Result.success(parse(SignInResponse.serializer(), it))
            } catch (e: SerializationException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getUser(accessToken: String): Result<GetUserResponse> {
        return request(
            RequestType.getUser,
            serialize(
                AccessToken.serializer(),
                AccessToken(accessToken)
            )
        ).onResult {
            try {
                Result.success(parse(GetUserResponse.serializer(), it))
            } catch (e: SerializationException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateUserAttributes(
        accessToken: String,
        attributes: List<UserAttribute>
    ): Result<UpdateUserAttributesResponse> {
        return request(
            RequestType.updateUserAttributes,
            serialize(
                UpdateUserAttributes.serializer(),
                UpdateUserAttributes(accessToken, attributes)
            )
        ).onResult {
            try {
                Result.success(parse(UpdateUserAttributesResponse.serializer(), it))
            } catch (e: SerializationException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun changePassword(
        accessToken: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return request(
            RequestType.changePassword,
            serialize(
                ChangePassword.serializer(),
                ChangePassword
                    (
                    accessToken,
                    currentPassword,
                    newPassword
                )
            )
        ).onResult {
            Result.success(Unit)
        }
    }

    override suspend fun forgotPassword(
        username: String
    ): Result<ForgotPasswordResponse> {
        return request(
            RequestType.forgotPassword,
            serialize(
                ForgotPassword.serializer(),
                ForgotPassword(
                    configuration.clientId,
                    username
                )
            )
        ).onResult {
            try {
                Result.success(parse(ForgotPasswordResponse.serializer(), it))
            } catch (e: SerializationException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun confirmForgotPassword(
        confirmationCode: String,
        username: String,
        password: String
    ): Result<Unit> {
        return request(
            RequestType.confirmForgotPassword,
            serialize(
                ConfirmForgotPassword.serializer(),
                ConfirmForgotPassword(
                    configuration.clientId,
                    confirmationCode,
                    username,
                    password
                )
            )
        ).onResult {
            Result.success(Unit)
        }
    }

    override suspend fun getUserAttributeVerificationCode(
        accessToken: String,
        attributeName: String,
        clientMetadata: Map<String, String>?
    ): Result<GetAttributeVerificationCodeResponse> {
        return request(
            RequestType.getUserAttributeVerificationCode,
            serialize(
                GetUserAttributeVerificationCode.serializer(),
                GetUserAttributeVerificationCode(
                    accessToken,
                    attributeName,
                    clientMetadata
                )
            )
        ).onResult {
            try {
                Result.success(parse(GetAttributeVerificationCodeResponse.serializer(), it))
            } catch (e: SerializationException) {
                Result.failure(e)
            }
        }
    }

    override suspend fun verifyUserAttribute(accessToken: String, attributeName: String, code: String): Result<Unit> {
        return request(
            RequestType.verifyUserAttribute,
            serialize(
                VerifyUserAttribute.serializer(),
                VerifyUserAttribute(
                    accessToken,
                    attributeName,
                    code
                )
            )
        ).onResult {
            Result.success(Unit)
        }
    }

    override suspend fun signOut(accessToken: String): Result<Unit> {
        return request(
            RequestType.signOut,
            serialize(
                AccessToken.serializer(),
                AccessToken(accessToken)
            )
        ).onResult {
            Result.success(Unit)
        }
    }

    override suspend fun deleteUser(accessToken: String): Result<Unit> {
        return request(
            RequestType.deleteUser,
            serialize(
                AccessToken.serializer(),
                AccessToken(accessToken)
            )
        ).onResult {
            Result.success(Unit)
        }
    }

    override fun getClaims(fromIdToken: String): Result<Claims> {
        return try {
            val component = fromIdToken.split(".")[1]
            Base64.decode(component)?.let { decoded64 ->
                val json = Json { encodeDefaults = true; isLenient = true }
                json.decodeFromString(Claims.serializer(), decoded64)?.let { claims ->
                    Result.success(claims)
                }
            }?: run {
                Result.failure(Error("Couldn't decode JWT token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //----------
    // REQUEST
    //----------

    private suspend fun request(type: RequestType, payload: String): Result<String> {
        try {
            val response = client.post<HttpResponse>(configuration.requestUrl) {
                header(
                    Header.AmzTarget,
                    when (type) {
                        RequestType.signUp -> IdentityProviderService.SignUp
                        RequestType.confirmSignUp -> IdentityProviderService.ConfirmSignUp
                        RequestType.signIn -> IdentityProviderService.InitiateAuth
                        RequestType.signOut -> IdentityProviderService.GlobalSignOut
                        RequestType.getUser -> IdentityProviderService.GetUser
                        RequestType.changePassword -> IdentityProviderService.ChangePassword
                        RequestType.forgotPassword -> IdentityProviderService.ForgotPassword
                        RequestType.confirmForgotPassword -> IdentityProviderService.ConfirmForgotPassword
                        RequestType.deleteUser -> IdentityProviderService.DeleteUser
                        RequestType.updateUserAttributes -> IdentityProviderService.UpdateUserAttributes
                        RequestType.getUserAttributeVerificationCode -> IdentityProviderService.GetUserAttributeVerificationCode
                        RequestType.verifyUserAttribute -> IdentityProviderService.VerifyUserAttribute
                    }
                )
                body = payload
            }
            return if (response.status == HttpStatusCode.OK) {
                Result.success(String(response.readBytes()))
            } else {
                val error = parse(RequestError.serializer(), String(response.readBytes()))
                Result.failure(Error(error.message))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}