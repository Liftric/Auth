package com.liftric.auth

import IdentityProviderClientJS
import com.liftric.auth.core.UserAttribute
import env
import kotlinx.coroutines.await
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class IdentityProviderClientJSTests {
    private val provider = IdentityProviderClientJS(
        env["region"] ?: error("missing region"),
        env["clientId"] ?: error("missing clientId")
    )

    // Randomize temp user account name to not exceed aws try threshold
    private val random = (0..999999).random()
    private val username = "auth-lib-test-user-${random}"
    private val password = "auth-lib-test-user-${random}A1@"

    data class Credentials(val username: String, val password: String)

    @JsName("SignUpSignInDeleteUserTest")
    @Test
    fun `Sign up, sign in, delete user should succeed`() = runTest {
        println("Sign up, sign in, delete user should succeed")
        provider.signUp(
            username, password,
            attributes = arrayOf(
                UserAttribute(Name = "custom:target_group", Value = "ROLE_USER")
            )
        ).await().also {
            println("signUpResponse=$it")
            assertNotNull(it)
        }

        provider.signIn(username, password).await().also {
            println("signInResponse=$it")
            assertNotNull(it)

            provider.deleteUser(it.AuthenticationResult.AccessToken).await().also {
                println("deleteUser=$it")
                assertNotNull(it)
            }
        }
    }

    @JsName("SignUpFailPasswordTooLongTest")
    @Test
    fun `Sign up should fail because password too long`() = runTest {
        provider.signUp(
            "Username", buildString { (1..260).forEach { _ -> append("A") } },
            attributes = arrayOf(
                UserAttribute(Name = "custom:target_group", Value = "ROLE_USER")
            )
        ).then {
            fail("signUp must fail")
        }.catch {
            println(it.message)
        }
    }
}
