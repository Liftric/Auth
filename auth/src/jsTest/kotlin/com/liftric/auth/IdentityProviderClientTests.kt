package com.liftric.auth

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise

actual class IdentityProviderClientTests : AbstractIdentityProviderClientTests()

actual fun runTest(block: suspend () -> Unit): dynamic = MainScope().promise {
    block.invoke()
}
