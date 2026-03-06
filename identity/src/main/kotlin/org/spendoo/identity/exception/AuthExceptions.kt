package org.spendoo.identity.exception

import org.springframework.security.core.AuthenticationException

class InvalidCredentialsException(
    message: String = "Invalid email or password") : RuntimeException(message)


class TokenExpiredException (
    message: String = "Token has expired. Please login again.") : RuntimeException(message)


class UnauthorizedException (
    message: String = "Unauthorized access") : RuntimeException(message)

class UserAlreadyExistsException(
    message: String = "User already exists") : RuntimeException(message)

class UserNotFoundException(message: String) : AuthenticationException(message)

