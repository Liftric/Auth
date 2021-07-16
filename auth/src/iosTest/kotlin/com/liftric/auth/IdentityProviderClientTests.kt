package com.liftric.auth

import kotlinx.coroutines.runBlocking

actual class IdentityProviderClientTests: AbstractIdentityProviderClientTests()

actual fun runTest(block: suspend () -> Unit) = runBlocking {
    block.invoke()
}