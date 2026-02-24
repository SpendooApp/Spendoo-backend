package org.spendoo.identity.exception

class UnauthorizedException (
    message: String = "Unauthorized access") : RuntimeException(message)