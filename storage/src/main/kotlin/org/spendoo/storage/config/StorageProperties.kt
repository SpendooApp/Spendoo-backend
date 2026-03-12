package org.spendoo.storage.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage.spendoo")
data class StorageProperties(
    val key: String,
    val secret: String,
    val endpoint: String,
    val bucket: String,
    val cdnEndpoint: String,
)