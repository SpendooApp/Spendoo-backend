package org.spendoo.identity.exception

class TokenExpiredException (
    message: String = "Token has expired. Please login again.") : RuntimeException(message)