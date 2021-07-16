package com.liftric.auth.jwt

import java.util.Base64

internal actual class Base64 {
    actual companion object {
        actual fun decode(input: String): String? {
            return try {
                String(Base64.getDecoder().decode(input), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
    }
}
