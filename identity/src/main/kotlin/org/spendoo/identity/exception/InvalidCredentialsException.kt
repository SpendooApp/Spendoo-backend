package org.spendoo.identity.exception

class InvalidCredentialsException(
    message: String = "Invalid email or password") : RuntimeException(message)