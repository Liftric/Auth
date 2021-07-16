package com.liftric.auth

import com.liftric.auth.core.*
import com.liftric.auth.jwt.*
import env
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.test.*

expect fun runTest(block: suspend () -> Unit)

expect class IdentityProviderClientTests : AbstractIdentityProviderClientTests
abstract class AbstractIdentityProviderClientTests {
    private val provider = IdentityProviderClient(
        env["region"] ?: error("region env missing"),
        env["clientId"] ?: error("clientId env missing")
    )

    //-------------------
    // INTEGRATION TESTS
    //-------------------

    data class Credentials(val username: String, val password: String)

    private fun randomUser(): Credentials {
        val random = (0..999999).random()
        return Credentials(
            username = "auth-lib-test-user-${random}",
            password = "auth-lib-test-user-${random}A1@"
        )
    }

    private suspend fun createUser(): Pair<AuthenticationResult, Credentials> {
        val credential = randomUser()
        val userAttributes: List<UserAttribute> = listOf(
            UserAttribute(
                Name = "custom:target_group",
                Value = "ROLE_PATIENT"
            )
        )
        val signUpResponse = provider.signUp(credential.username, credential.password, userAttributes)

        assertNull(signUpResponse.exceptionOrNull())
        assertNotNull(signUpResponse.getOrNull())

        val signInResponse = provider.signIn(credential.username, credential.password)
        assertNotNull(signInResponse.getOrNull())

        return Pair(signInResponse.getOrNull()!!.AuthenticationResult, credential)
    }

    private suspend fun deleteUser(token: String) {
        val deleteUserResponse = provider.deleteUser(token)
        assertNull(deleteUserResponse.exceptionOrNull())
    }

    @JsName("SignUpSignInDeleteUserTest")
    @JvmName("SignUpSignInDeleteUserTest")
    @Test
    fun `Sign up, sign in, delete user should succeed`() = runTest {
        val credentials = randomUser()
        val signUpResponse = provider.signUp(
            credentials.username, credentials.password,
            attributes = listOf(
                UserAttribute(Name = "custom:target_group", Value = "ROLE_USER")
            )
        )

        assertNull(signUpResponse.exceptionOrNull())
        assertNotNull(signUpResponse.getOrNull())

        val signInResponse = provider.signIn(credentials.username, credentials.password)
        assertNull(signInResponse.exceptionOrNull())
        assertNotNull(signInResponse.getOrNull())

        val deleteUserResponse = provider.deleteUser(signInResponse.getOrNull()!!.AuthenticationResult.AccessToken)
        assertNull(deleteUserResponse.exceptionOrNull())
    }

    @JsName("GetUserTest")
    @JvmName("GetUserTest")
    @Test
    fun `Should get user`() = runTest {
        val (result, _) = createUser()

        val getUserAttribute = provider.getUser(result.AccessToken)
        assertNull(getUserAttribute.exceptionOrNull())
        assertNotNull(getUserAttribute.getOrNull())

        deleteUser(result.AccessToken)
    }

    @JsName("ChangeAttributeTest")
    @JvmName("ChangeAttributeTest")
    @Test
    fun `Should change attribute`() = runTest {
        val (result, _) = createUser()

        val updateUserAttributesResponse = provider.updateUserAttributes(
            result.AccessToken,
            listOf(UserAttribute(Name = "custom:target_group", Value = "ROLE_USER"))
        )
        assertNull(updateUserAttributesResponse.exceptionOrNull())
        assertNotNull(updateUserAttributesResponse.getOrNull())

        val getUserResponse = provider.getUser(result.AccessToken)
        assertNull(getUserResponse.exceptionOrNull())
        assertNotNull(getUserResponse.getOrNull())

        getUserResponse.getOrNull()!!.UserAttributes.map { attribute ->
            if (attribute.Name == "email") {
                assertEquals("ROLE_USER", attribute.Value)
            }
        }

        deleteUser(result.AccessToken)
    }

    @JsName("ChangePasswordTest")
    @JvmName("ChangePasswordTest")
    @Test
    fun `Should change password`() = runTest {
        val (result, credentials) = createUser()

        val changePasswordResponse = provider.changePassword(result.AccessToken, credentials.password, credentials.password + "B")
        assertNull(changePasswordResponse.exceptionOrNull())

        val signOutResponse = provider.signOut(result.AccessToken)
        assertNull(signOutResponse.exceptionOrNull())

        // AWS is not revoking Tokens automatically so give it some time
        delay(1000)

        val signInResponse = provider.signIn(credentials.username, credentials.password + "B")
        assertNull(signInResponse.exceptionOrNull())
        assertNotNull(signInResponse.getOrNull())

        deleteUser(signInResponse.getOrNull()!!.AuthenticationResult.AccessToken)
    }

    @JsName("SignOutSignInTest")
    @JvmName("SignOutSignInTest")
    @Test
    fun `Sign out and sign in should succeed`() = runTest {
        val (result, credentials) = createUser()

        val signOutResponse = provider.signOut(result.AccessToken)
        assertNull(signOutResponse.exceptionOrNull())

        // AWS is not revoking Tokens instantly so give it some time
        delay(1000)

        val signInResponse = provider.signIn(credentials.username, credentials.password)
        assertNull(signInResponse.exceptionOrNull())
        assertNotNull(signInResponse.getOrNull())

        deleteUser(signInResponse.getOrNull()!!.AuthenticationResult.AccessToken)
    }

    @JsName("SignOutSignInRefreshTest")
    @JvmName("SignOutSignInRefreshTest")
    @Test
    fun `Sign out, sign in and refresh should succeed`() = runTest {
        val (result, credentials) = createUser()

        val signOutResponse = provider.signOut(result.AccessToken)
        assertNull(signOutResponse.exceptionOrNull())

        // AWS is not revoking Tokens instantly so give it some time
        delay(1000)

        val signInResponse = provider.signIn(credentials.username, credentials.password)
        assertNull(signInResponse.exceptionOrNull())
        assertNotNull(signInResponse.getOrNull())

        val refreshToken = signInResponse.getOrThrow().AuthenticationResult.RefreshToken

        val refreshResponse = provider.refresh(refreshToken!!)
        assertNull(refreshResponse.exceptionOrNull())
        assertNotNull(refreshResponse.getOrNull())

        deleteUser(refreshResponse.getOrNull()!!.AuthenticationResult.AccessToken)
    }

    @JsName("RevokeTokenAndValidateExpiration")
    @JvmName("RevokeTokenAndValidateExpiration")
    @Test
    fun `Sign in, revoke token, validate`() = runTest {
        val (result, credentials) = createUser()

        val revokeTokenResponse = provider.revokeToken(result.RefreshToken!!)
        assertNull(revokeTokenResponse.exceptionOrNull())

        // AWS is not revoking Tokens instantly so give it some time
        delay(1000)

        // This should fail since the token has been revoked
        val signOutResponse = provider.signOut(result.AccessToken)
        assertTrue(signOutResponse.exceptionOrNull() is IdentityProviderException.NotAuthorized)

        // We delete the user after we're done with the test
        val signInResponse = provider.signIn(credentials.username, credentials.password)
        assertNull(signInResponse.exceptionOrNull())
        assertNotNull(signInResponse.getOrNull())

        deleteUser(signInResponse.getOrNull()!!.AuthenticationResult.AccessToken)
    }

    @JsName("SignUpFailPasswordTooShortTest")
    @JvmName("SignUpFailPasswordTooShortTest")
    @Test
    fun `Sign up should fail because password too short`() = runTest {
        val signUpResponse = provider.signUp(
            "Username", "Short",
            attributes = listOf(
                UserAttribute(Name = "custom:target_group", Value = "ROLE_USER")
            )
        )
        assertNotNull(signUpResponse.exceptionOrNull())
        assertNull(signUpResponse.getOrNull())
        assertEquals(
            "Password did not conform with policy: Password not long enough",
            signUpResponse.exceptionOrNull()!!.message
        )
        assertEquals(HttpStatusCode.BadRequest, (signUpResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("SignUpFailPasswordTooLongTest")
    @JvmName("SignUpFailPasswordTooLongTest")
    @Test
    fun `Sign up should fail because password too long`() = runTest {
        val signUpResponse = provider.signUp(
            "Username", buildString { (1..260).forEach { _ -> append("A") } },
            attributes = listOf(
                UserAttribute(Name = "custom:target_group", Value = "ROLE_USER")
            )
        )
        assertNotNull(signUpResponse.exceptionOrNull())
        assertNull(signUpResponse.getOrNull())
        assertEquals(
            "1 validation error detected: Value at 'password' failed to satisfy constraint: Member must have length less than or equal to 256",
            signUpResponse.exceptionOrNull()!!.message
        )
        assertEquals(HttpStatusCode.BadRequest, (signUpResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("SignUpFailUsernameTooLongTest")
    @JvmName("SignUpFailUsernameTooLongTest")
    @Test
    fun `Sign up should fail because username too long`() = runTest {
        val signUpResponse = provider.signUp(
            buildString { (1..130).forEach { _ -> append("A") } }, "Password",
            attributes = listOf(
                UserAttribute(Name = "custom:target_group", Value = "ROLE_USER")
            )
        )
        assertNotNull(signUpResponse.exceptionOrNull())
        assertNull(signUpResponse.getOrNull())
        assertEquals(
            "1 validation error detected: Value at 'username' failed to satisfy constraint: Member must have length less than or equal to 128",
            signUpResponse.exceptionOrNull()!!.message
        )
        assertEquals(HttpStatusCode.BadRequest, (signUpResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("SignInTest")
    @JvmName("SignInTest")
    @Test
    fun `Sign in should fail because wrong credentials`() = runTest {
        val signInResponse = provider.signIn(
            randomUser().username, "WRONG_PASSWORD"
        )
        assertNotNull(signInResponse.exceptionOrNull())
        assertNull(signInResponse.getOrNull())
        assertEquals("Incorrect username or password.", signInResponse.exceptionOrNull()!!.message)
        assertEquals(HttpStatusCode.BadRequest, (signInResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("DeleteUserFailTest")
    @JvmName("DeleteUserFailTest")
    @Test
    fun `Get user should fail since access token wrong`() = runTest {
        val deleteUserResponse = provider.deleteUser("WRONG_TOKEN")
        assertNotNull(deleteUserResponse.exceptionOrNull())
        assertEquals("Invalid Access Token", deleteUserResponse.exceptionOrNull()!!.message)
        assertEquals(HttpStatusCode.BadRequest, (deleteUserResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("DeleteUserTest")
    @JvmName("DeleteUserTest")
    @Test
    fun `Delete user should fail since access token wrong`() = runTest {
        val deleteUserResponse = provider.deleteUser("WRONG_TOKEN")
        assertNotNull(deleteUserResponse.exceptionOrNull())
        assertEquals("Invalid Access Token", deleteUserResponse.exceptionOrNull()!!.message)
        assertEquals(HttpStatusCode.BadRequest, (deleteUserResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("SignOutTest")
    @JvmName("SignOutTest")
    @Test
    fun `Sign out should fail since access token wrong`() = runTest {
        val signOutResponse = provider.signOut("WRONG_TOKEN")
        assertNotNull(signOutResponse.exceptionOrNull())
        assertEquals("Invalid Access Token", signOutResponse.exceptionOrNull()!!.message)
        assertEquals(HttpStatusCode.BadRequest, (signOutResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("UpdateUserAttributesTest")
    @JvmName("UpdateUserAttributesTest")
    @Test
    fun `Update attributes should fail since access token wrong`() = runTest {
        val updateUserAttributesResponse = provider.updateUserAttributes(
            "WRONG_TOKEN",
            attributes = listOf(
                UserAttribute(Name = "email", Value = "test@test.test")
            )
        )
        assertNotNull(updateUserAttributesResponse.exceptionOrNull())
        assertNull(updateUserAttributesResponse.getOrNull())
        assertEquals("Invalid Access Token", updateUserAttributesResponse.exceptionOrNull()!!.message)
        assertEquals(HttpStatusCode.BadRequest, (updateUserAttributesResponse.exceptionOrNull() as IdentityProviderException).status)
    }

    @JsName("TestGetIdTokenClaimsWithEmailNullValue")
    @JvmName("TestGetIdTokenClaimsWithEmailNullValue")
    @Test
    fun `Test if get id token  claims works without email address`() {
        val token =
            "eyJraWQiOiJwREgwTUpqeWdoRk4wT2J1cFpUNzl1QytLZkpZQ3BtNnZTamVXb3NpZUlFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiIxZWEyMTg1Yi1iYTk5LTRlYjUtYmZkMS0yMDY0MjQ2Yzc0NWQiLCJhdWQiOiIzdjRzNm9lMmRobjZua2hydTU3OWc2bTZnMSIsImNvZ25pdG86Z3JvdXBzIjpbIlJPTEVfUEFUSUVOVCJdLCJldmVudF9pZCI6IjA3ODE5NGUzLTM5YzQtNDBhYS04MzkwLTkzMmI2MjY2MjE3YSIsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNTk5NTY1OTU4LCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAuZXUtY2VudHJhbC0xLmFtYXpvbmF3cy5jb21cL2V1LWNlbnRyYWwtMV9DMUduN0hiWU4iLCJjb2duaXRvOnVzZXJuYW1lIjoiNDMzMGI4ZjctYmRjMy00MTI2LTllYWUtZWVkZTc0ZTI0MTEyIiwiZXhwIjoxNTk5NTY5NTU4LCJpYXQiOjE1OTk1NjU5NTh9.hLzDOItbHkUWYyI9hTFIKkoC50_UrRnPFoIcyrsiCzP5zQFlhTboe9TZ6BE0o21IEk8tcdBkFRHbuM8zPru-qopB9tC7pkhvY1FoPMlNlRSmqj8YZjJ8InnHForkdJ4n9keM8PcdwW6KWlAjLViwSmOl3k-ptQq1DnmnmGmcmfzssDzON0R__jsyEQs_EZWQ3c86qodbIU4peN9Dm26TMSQCzJhZwvCuGRmRgplsqOD4UfDVh5ya-bXUogJuirlE8KFUH1my13AJOxAJLBgyOjBZceFMnC4ZqZBbSND-iiMvQcpn4O6gd5p5Je367LO56w_ypo6eMffEs39fikIP4g"
        val idToken = CognitoIdToken(token)
        assertEquals(idToken.claims.sub, "1ea2185b-ba99-4eb5-bfd1-2064246c745d")
        assertEquals(idToken.claims.email, null)
        assertEquals(idToken.claims.emailVerified, null)
    }

    @JsName("TestGetIdTokenClaimsWithEmail")
    @JvmName("TestGetIdTokenClaimsWithEmail")
    @Test
    fun `Test if get id token claims works with email address`() {
        val token =
            "eyJraWQiOiJwREgwTUpqeWdoRk4wT2J1cFpUNzl1QytLZkpZQ3BtNnZTamVXb3NpZUlFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI3NTUzZGRmOC1hMTAzLTRjYjItOWVkZi0yNDcwMTBmNGNjNGQiLCJhdWQiOiIzdjRzNm9lMmRobjZua2hydTU3OWc2bTZnMSIsImNvZ25pdG86Z3JvdXBzIjpbIlJPTEVfUEFUSUVOVCJdLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImV2ZW50X2lkIjoiZmMxNTM3NTQtNDY5ZS00YzZiLTlhMzktODVhM2M3MDAxZTMwIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE1OTk1NjY5MjMsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC5ldS1jZW50cmFsLTEuYW1hem9uYXdzLmNvbVwvZXUtY2VudHJhbC0xX0MxR243SGJZTiIsImNvZ25pdG86dXNlcm5hbWUiOiI2YTg0MzYzNS1kZWM2LTQxMmYtYjI0MS1iNGRmYmI2NTVkM2YiLCJleHAiOjE1OTk1NzA1MjMsImlhdCI6MTU5OTU2NjkyMywiZW1haWwiOiJnYWViZWxAbGlmdHJpYy5jb20ifQ.ka1nCmT-ACwbvQ3uy3qsuZII6PQzdfJHA7UY3Wkt_7GU2fxBxcDdRjzdDdCmh4IE0e0uwfoddMXTXWaijo6yKvrv0VHtfsIkfFJb09TNtCNrxTy1PX-bJNeVT752N85pdNpkms6GefylP2iAZec520ISI1ZrHz0jlKfUq6iGpq3GKxIXJZ_dQGVPa2oTQDqG_CmOsr9sTRl8EoMoEIjxJdOAFeYltlPDcuhWZVUWsfwUq290UdOTBJhGruIre-cdfe03FEo9NG67mewldRYdsjNBgGQU_Jyp68hg1UQHrhKC-eUDmrWiyYGzKwbkUCCm1puwcy_wpu5HRQfjAjVW4A"
        val idToken = CognitoIdToken(token)
        assertEquals(idToken.claims.sub, "7553ddf8-a103-4cb2-9edf-247010f4cc4d")
        assertNotEquals(idToken.claims.email, null)
        assertEquals(idToken.claims.emailVerified, false)
    }

    @JsName("GetCustomAttributes")
    @JvmName("GetCustomAttributes")
    @Test
    fun `Test if custom attributes get mapped correctly`() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3NTUzZGRmOC1hMTAzLTRjYjItOWVkZi0yNDcwMTBmNGNjNGQiLCJhdWQiOiIzdjRzNm9lMmRobjZua2hydTU3OWc2bTZnMSIsImNvZ25pdG86Z3JvdXBzIjpbIlJPTEVfUEFUSUVOVCJdLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImV2ZW50X2lkIjoiZmMxNTM3NTQtNDY5ZS00YzZiLTlhMzktODVhM2M3MDAxZTMwIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE1OTk1NjY5MjMsImlzcyI6Imh0dHBzOi8vY29nbml0by1pZHAuZXUtY2VudHJhbC0xLmFtYXpvbmF3cy5jb20vZXUtY2VudHJhbC0xX0MxR243SGJZTiIsImNvZ25pdG86dXNlcm5hbWUiOiI2YTg0MzYzNS1kZWM2LTQxMmYtYjI0MS1iNGRmYmI2NTVkM2YiLCJleHAiOjE2MDEzMDE1ODYsImlhdCI6MTU5OTU2NjkyMywiZW1haWwiOiJ0ZXN0QHRlc3QuY29tIiwiY3VzdG9tOnR3aXR0ZXIiOiJ0ZXN0IiwiY3VzdG9tOmFnZSI6MTgsImp0aSI6ImI0NjE2MzZmLWUxOGMtNDhjZi04Mjk5LTUzYjZmMWIxNWZmMyJ9.Mzh2RGW1VWd1oxE89xW05Ce_JRs1Y2HifL3brBkf7NE"
        val idToken = CognitoIdToken(token)
        assertEquals(idToken.claims.customAttributes?.get("age")?.toLong(), 18)
        assertEquals(idToken.claims.customAttributes?.get("twitter"), "test")
    }

    @JsName("TestGetAccessTokenClaims")
    @JvmName("TestGetAccessTokenClaims")
    @Test
    fun `Test if get access token claims`() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6MTYwMTI5ODY0MiwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.AYrQOiqkjy6XyF33jAYje-hVfX5OnuiYUhh8pS1wxBk"
        val accessToken = CognitoAccessToken(token)
        assertEquals(accessToken.claims.sub, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertEquals(accessToken.claims.deviceKey, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertEquals(accessToken.claims.cognitoGroups[0], "admin")
        assertEquals(accessToken.claims.tokenUse, "access")
        assertEquals(accessToken.claims.scope, "aws.cognito.signin.user.admin")
        assertEquals(accessToken.claims.authTime, 1562190524)
        assertEquals(accessToken.claims.iss, "https://cognito-idp.us-west-2.amazonaws.com/us-west-2_example")
        assertEquals(accessToken.claims.exp, 1601298642)
        assertEquals(accessToken.claims.iat, 1562190524)
        assertEquals(accessToken.claims.jti, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertEquals(accessToken.claims.clientId, "57cbishk4j24pabc1234567890")
        assertEquals(accessToken.claims.username, "janedoe@example.com")
    }

    @JsName("ShouldThrowMissingComponentsException")
    @JvmName("ShouldThrowMissingComponentsException")
    @Test
    fun `Test should throw MissingComponentsException since it has no components`() {
        assertFailsWith(MissingComponentsException::class) {
            val token = "missingcomponents"
            val idToken = CognitoIdToken(token)
            idToken.claims
        }
    }

    @JsName("ShouldThrowInvalidBase64Exception")
    @JvmName("ShouldThrowInvalidBase64Exception")
    @Test
    fun `Test should throw InvalidBase64Exception since it is not a base 64 encoded string`() {
        assertFailsWith(InvalidBase64Exception::class) {
            val token =
                "component.EOKF36syRBtB11VgyChkNjc1HxRrajT7XXaxZfnVzPkV57K3b9yqkS284Ovb9uWzXgGeY2bxA3IySGfdOHiPAQ==F/v6hcTiU1sd975XHfDsz8o0rboujM77n7KwRMidobOLbo5ghUT/IFcxElUc8CirdZxaCaS3zs/CfRKRsXwbFNYd.component"
            val idToken = CognitoIdToken(token)
            idToken.getPayload()
        }
    }

    @JsName("ShouldThrowInvalidCognitoIdTokenException")
    @JvmName("ShouldThrowInvalidCognitoIdTokenException")
    @Test
    fun `Test should throw InvalidCognitoIdTokenException since this is not a valid Cognito Id token`() {
        assertFailsWith(InvalidCognitoIdTokenException::class) {
            val token =
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIzdjRzNm9lMmRobjZua2hydTU3OWc2bTZnMSIsImNvZ25pdG86Z3JvdXBzIjpbIlJPTEVfUEFUSUVOVCJdLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImV2ZW50X2lkIjoiZmMxNTM3NTQtNDY5ZS00YzZiLTlhMzktODVhM2M3MDAxZTMwIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE1OTk1NjY5MjMsImNvZ25pdG86dXNlcm5hbWUiOiI2YTg0MzYzNS1kZWM2LTQxMmYtYjI0MS1iNGRmYmI2NTVkM2YiLCJleHAiOjE2MDEyOTQ1NDQsImlhdCI6MTU5OTU2NjkyMywiZW1haWxfd2l0aF90eXBvIjoiZ2FlYmVsQGxpZnRyaWMuY29tIiwianRpIjoiYWNkNjg1MTUtZmExZi00ZTNmLWI3ZmUtODEwYzY2NmRhODYwIn0.zuqwEPXiLzbmxSdNQGjr3m4X5cXqdQf4aw_-7BUbvZk"
            val idToken = CognitoIdToken(token)
            idToken.claims
        }
    }

    @JsName("ShouldThrowInvalidCognitoAccessTokenException")
    @JvmName("ShouldThrowInvalidCognitoAccessTokenException")
    @Test
    fun `Test should throw InvalidCognitoAccessTokenException since this is not a valid Cognito Access token`() {
        assertFailsWith(InvalidCognitoAccessTokenException::class) {
            val token =
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIzdjRzNm9lMmRobjZua2hydTU3OWc2bTZnMSIsImNvZ25pdG86Z3JvdXBzIjpbIlJPTEVfUEFUSUVOVCJdLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImV2ZW50X2lkIjoiZmMxNTM3NTQtNDY5ZS00YzZiLTlhMzktODVhM2M3MDAxZTMwIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE1OTk1NjY5MjMsImNvZ25pdG86dXNlcm5hbWUiOiI2YTg0MzYzNS1kZWM2LTQxMmYtYjI0MS1iNGRmYmI2NTVkM2YiLCJleHAiOjE2MDEyOTQ1NDQsImlhdCI6MTU5OTU2NjkyMywiZW1haWxfd2l0aF90eXBvIjoiZ2FlYmVsQGxpZnRyaWMuY29tIiwianRpIjoiYWNkNjg1MTUtZmExZi00ZTNmLWI3ZmUtODEwYzY2NmRhODYwIn0.zuqwEPXiLzbmxSdNQGjr3m4X5cXqdQf4aw_-7BUbvZk"
            val accessToken = CognitoAccessToken(token)
            accessToken.claims
        }
    }
}
