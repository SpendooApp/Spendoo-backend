package org.spendoo.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.spendoo.identity.security.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

@Component
class ApiClient(
    private val jwtUtil: JwtUtil,
    @Value("\${internal.api.base-url:http://localhost:8080}") baseUrl: String,
    @Value("\${ai.service.base-url:https://localhost:8000}") private val aiBaseUrl: String
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .messageConverters { converters ->
            val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val jacksonConverter = MappingJackson2HttpMessageConverter(objectMapper)
            val index = converters.indexOfFirst { it is MappingJackson2HttpMessageConverter }
            if (index != -1) {
                converters[index] = jacksonConverter
            } else {
                converters.add(0, jacksonConverter)
            }
        }
        .build()

    fun <T : Any> call(responseType: Class<T>, configure: ApiRequest.() -> Unit): T? {
        val request = ApiRequest().apply(configure)
        val targetPath = if (request.callAIService) {
            val base = if (aiBaseUrl.endsWith("/")) aiBaseUrl else "$aiBaseUrl/"
            val path = if (request.path.startsWith("/")) request.path.substring(1) else request.path
            "$base$path"
        } else {
            request.path
        }
        val uriBuilder = UriComponentsBuilder.fromUriString(targetPath)
        request.queryParams.forEach { (key, value) ->
            uriBuilder.queryParam(key, value)
        }
        val uri = uriBuilder.build().toUriString()

        val requestSpec = restClient.method(request.method).uri(uri)
        request.headers.forEach { (key, value) ->
            requestSpec.header(key, value)
        }

        if (request.addToken) {
            val targetUserId = request.userId ?: (SecurityContextHolder.getContext().authentication?.principal as? UUID)
            if (targetUserId != null) {
                val token = jwtUtil.generateAccessToken(targetUserId)
                requestSpec.header("Authorization", "Bearer $token")
            }
        }

        if (request.body != null) {
            requestSpec.body(request.body!!)
        }

        return requestSpec.retrieve().body(responseType)
    }
}
