package org.spendoo.storage.exception

abstract class UploadImageException(message: String): Exception(message)

class InvalidImageException(
    val extension: String
): UploadImageException("uploading image with invalid extension:  $extension")

class UnknownErrorException(message: String): UploadImageException(message)