package com.taskflow.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(val jwt: Jwt, val cors: Cors) {
    data class Jwt(val secret: String, val expirationMinutes: Long)
    data class Cors(val allowedOrigins: List<String>)
}
