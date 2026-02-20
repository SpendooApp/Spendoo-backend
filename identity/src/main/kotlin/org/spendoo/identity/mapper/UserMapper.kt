package org.spendoo.identity.mapper
import org.spendoo.identity.dto.RegisterRequest
import org.spendoo.identity.entity.User

fun RegisterRequest.toEntity(hashedPassword: String): User {
    return User(
        fullName = this.fullName,
        email = this.email,
        passwordHash = hashedPassword,
        gender = this.gender,
        age = this.age )
}