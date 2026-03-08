package org.spendoo.identity.exception

abstract class AuthenticationException(message: String) : RuntimeException(message)

class InvalidCredentialsException(
    message: String = "Invalid email or password") : AuthenticationException(message)


class TokenExpiredException (
    message: String = "Token has expired. Please login again.") : AuthenticationException(message)


class UnauthorizedException (
    message: String = "Unauthorized access") : AuthenticationException(message)

class UserAlreadyExistsException(
    message: String = "User already exists") : AuthenticationException(message)

class UserNotFoundException(message: String) : AuthenticationException(message)

