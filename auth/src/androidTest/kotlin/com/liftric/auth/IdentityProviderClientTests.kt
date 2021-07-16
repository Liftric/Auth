package com.liftric.auth

import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
actual class IdentityProviderClientTests: AbstractIdentityProviderClientTests()

actual fun runTest(block: suspend () -> Unit) = runBlocking {
    block.invoke()
}