package org.spendoo.storage.service

import org.spendoo.storage.config.StorageProperties
import org.spendoo.storage.exception.InvalidImageException
import org.spendoo.storage.exception.UnknownErrorException
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.LocalDateTime

@Service
@EnableConfigurationProperties(StorageProperties::class)
class ImageStorageService(
    private val spendooS3Client: S3Client,
    private val identityStorageProperties: StorageProperties
) {
    fun uploadImage(
        file: MultipartFile,
        fileName: String,
        folderName: String
    ): String {
        val mimeType = file.contentType ?: throw InvalidImageException("null")
        val extension = allowedMimeTypes[mimeType] ?: throw InvalidImageException(mimeType)
        try {
            val fullFileName = "${fileName}.$extension"
            val randomParameter = LocalDateTime.now().toString()
            val key = "$folderName/$fullFileName"
            val putReq = createObjectRequest(key, mimeType)
            spendooS3Client.putObject(putReq, RequestBody.fromBytes(file.bytes))
            val imageUri = "$fullFileName?time=$randomParameter"
            return imageUri
        } catch (e: Exception) {
            throw UnknownErrorException(e.message ?: "Unknown error occurred", e)
        }
    }

    fun uploadImageFromBytes(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
        folderName: String
    ): String {
        val extension = allowedMimeTypes[contentType] ?: throw InvalidImageException(contentType)
        try {
            val fullFileName = "${fileName}.$extension"
            val randomParameter = LocalDateTime.now().toString()
            val key = "$folderName/$fullFileName"
            val putReq = createObjectRequest(key, contentType)
            spendooS3Client.putObject(putReq, RequestBody.fromBytes(bytes))
            val imageUri = "$fullFileName?time=$randomParameter"
            return imageUri
        } catch (e: Exception) {
            throw UnknownErrorException(e.message ?: "Unknown error occurred", e)
        }
    }

    fun deleteImage(folderName: String, fileName: String) {
        try {
            val deleteRequest = deleteObjectRequest("$folderName/$fileName")
            spendooS3Client.deleteObject(deleteRequest)
        } catch (e: Exception) {
            throw UnknownErrorException(e.message ?: "Unknown error occurred", e)
        }
    }

    private fun createObjectRequest(key: String, contentType: String): PutObjectRequest? {
        return PutObjectRequest.builder()
            .bucket(identityStorageProperties.bucket)
            .key(key)
            .contentType(contentType)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .build()
    }

    private fun deleteObjectRequest(key: String): DeleteObjectRequest {
        return DeleteObjectRequest.builder()
            .bucket(identityStorageProperties.bucket)
            .key(key)
            .build()
    }

    private companion object {
        val allowedMimeTypes = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
        )
    }
}