package org.spendoo.identity.exception

class UserAlreadyExistsException(
    message: String = "User already exists") : RuntimeException(message)
