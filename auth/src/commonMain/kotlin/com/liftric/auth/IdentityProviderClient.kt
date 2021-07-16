package com.liftric.auth

import com.liftric.auth.core.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Don't forget [IdentityProviderClientJS] when doing changes here :) */

/**
 * AWS Cognito Identity Provider client.
 * Provides common request methods.
 */
open class IdentityProviderClient(region: String, clientId: String) : IdentityProvider {
    private val json = Json {
        allowStructuredMapKeys = true
    }
    private val configuration = Configuration(region, clientId)
    private val client = HttpClient {
        /**
         * When referencing members that are in the
         * IdentityProvider's scope, assign them to
         * a new variable in this scope. Needed to
         * avoid [InvalidMutabilityException] in iOS.
         */
        val configuration = configuration
        val json = json
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
            acceptContentTypes = listOf(
                ContentType.parse(Header.AmzJson),
                ContentType.Application.Json
            )
        }
        defaultRequest {
            configuration.setupDefaultRequest(headers)
            contentType(ContentType.parse(Header.AmzJson))
        }
    }

    override suspend fun signUp(
        username: String,
        password: String,
        attributes: List<UserAttribute>?
    ): Result<SignUpResponse> = request(
        Request.SignUp,
        SignUp(
            ClientId = configuration.clientId,
            Username = username,
            Password = password,
            UserAttributes = attributes?: listOf()
        )
    )

    override suspend fun confirmSignUp(
        username: String,
        confirmationCode: String
    ): Result<Unit> = request(
        Request.ConfirmSignUp,
        ConfirmSignUp(
            ClientId = configuration.clientId,
            Username = username,
            ConfirmationCode = confirmationCode
        )
    )

    override suspend fun signIn(
        username: String,
        password: String
    ): Result<SignInResponse> = request(
        Request.SignIn,
        SignIn(
            AuthFlow = Authentication.UserPasswordAuth.flow,
            ClientId = configuration.clientId,
            AuthParameters = SignIn.Parameters(username, password)
        )
    )

    override suspend fun refresh(refreshToken: String): Result<SignInResponse> = request(
        Request.SignIn,
        Refresh(
            AuthFlow = Authentication.RefreshTokenAuth.flow,
            ClientId = configuration.clientId,
            AuthParameters = Refresh.Parameters(refreshToken)
        )
    )

    override suspend fun getUser(accessToken: String): Result<GetUserResponse> = request(
        Request.GetUser,
        AccessToken(accessToken)
    )

    override suspend fun updateUserAttributes(
        accessToken: String,
        attributes: List<UserAttribute>
    ): Result<UpdateUserAttributesResponse> = request(
        Request.UpdateUserAttributes,
        UpdateUserAttributes(
            AccessToken = accessToken,
            UserAttributes = attributes
        )
    )

    override suspend fun changePassword(
        accessToken: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = request(
        Request.ChangePassword,
        ChangePassword(
            AccessToken = accessToken,
            PreviousPassword = currentPassword,
            ProposedPassword = newPassword
        )
    )

    override suspend fun forgotPassword(
        username: String
    ): Result<ForgotPasswordResponse> = request(
        Request.ForgotPassword,
        ForgotPassword(
            ClientId = configuration.clientId,
            Username = username
        )
    )

    override suspend fun confirmForgotPassword(
        confirmationCode: String,
        username: String,
        password: String
    ): Result<Unit> = request(
        Request.ConfirmForgotPassword,
        ConfirmForgotPassword(
            ClientId = configuration.clientId,
            ConfirmationCode = confirmationCode,
            Username = username,
            Password = password
        )
    )

    override suspend fun getUserAttributeVerificationCode(
        accessToken: String,
        attributeName: String,
        clientMetadata: Map<String, String>?
    ): Result<GetAttributeVerificationCodeResponse> = request(
        Request.GetUserAttributeVerificationCode,
        GetUserAttributeVerificationCode(
            AccessToken = accessToken,
            AttributeName = attributeName,
            ClientMetadata = clientMetadata
        )
    )

    override suspend fun verifyUserAttribute(
        accessToken: String,
        attributeName: String,
        code: String
    ): Result<Unit> = request(
        Request.VerifyUserAttribute,
        VerifyUserAttribute(
            AccessToken = accessToken,
            AttributeName = attributeName,
            Code = code
        )
    )

    override suspend fun signOut(accessToken: String): Result<Unit> = request(
        Request.SignOut,
        AccessToken(accessToken)
    )

    override suspend fun revokeToken(refreshToken: String): Result<Unit> = request(
        Request.RevokeToken,
        RevokeToken(
            ClientId = configuration.clientId,
            Token = refreshToken
        )
    )

    override suspend fun deleteUser(accessToken: String): Result<Unit> = request(
        Request.DeleteUser,
        AccessToken(accessToken)
    )

    private suspend inline fun <reified T> request(type: Request, payload: Any): Result<T> = try {
        // println("request: type=$type payload=$payload")
        client.post<HttpResponse>(configuration.requestUrl) {
            header(Header.AmzTarget, type.value)
            body = payload
        }.run {
            when(T::class) {
                Unit::class -> Result.success(Unit as T)
                else -> Result.success(json.decodeFromString(readText()))
            }
        }
    } catch (e: ResponseException) {
        e.toIdentityProviderException()
    } catch (t: Throwable) {
        Result.failure(t)
    }

    private suspend inline fun <reified T> ResponseException.toIdentityProviderException(): Result<T> = try {
        json.decodeFromString<RequestError>(response.readText()).run {
            Result.failure(
                when(type) {
                    AWSException.CodeMismatch -> IdentityProviderException.CodeMismatch(response.status, message)
                    AWSException.ExpiredCode -> IdentityProviderException.ExpiredCode(response.status, message)
                    AWSException.InternalError -> IdentityProviderException.InternalError(response.status, message)
                    AWSException.InvalidLambdaResponse -> IdentityProviderException.InvalidLambdaResponse(response.status, message)
                    AWSException.InvalidParameter -> IdentityProviderException.InvalidParameter(response.status, message)
                    AWSException.InvalidPassword -> IdentityProviderException.InvalidPassword(response.status, message)
                    AWSException.LimitExceeded -> IdentityProviderException.LimitExceeded(response.status, message)
                    AWSException.NotAuthorized -> IdentityProviderException.NotAuthorized(response.status, message)
                    AWSException.ResourceNotFound -> IdentityProviderException.ResourceNotFound(response.status, message)
                    AWSException.TooManyFailedAttempts -> IdentityProviderException.TooManyFailedAttempts(response.status, message)
                    AWSException.TooManyRequests -> IdentityProviderException.TooManyRequests(response.status, message)
                    AWSException.UnexpectedLambda -> IdentityProviderException.UnexpectedLambda(response.status, message)
                    AWSException.UserLambdaValidation -> IdentityProviderException.UserLambdaValidation(response.status, message)
                    AWSException.UserNotConfirmed -> IdentityProviderException.UserNotConfirmed(response.status, message)
                    AWSException.UserNotFound -> IdentityProviderException.UserNotFound(response.status, message)
                    else -> IdentityProviderException.Unknown(response.status, type, message)
                }
            )
        }
    } catch (e: SerializationException) {
        Result.failure(e)
    }
}
