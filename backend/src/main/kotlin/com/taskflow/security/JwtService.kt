package com.taskflow.security

import com.taskflow.config.AppProperties
import com.taskflow.user.User
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

data class AuthenticatedUser(val id: Long, val email: String)
data class IssuedToken(val token: String, val expiresAt: Instant)

@Service
class JwtService(props: AppProperties, private val clock: Clock) {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.jwt.secret.toByteArray(Charsets.UTF_8))
    private val ttl: Duration = Duration.ofMinutes(props.jwt.expirationMinutes)

    fun generate(user: User): IssuedToken {
        val now = clock.instant()
        val expiresAt = now.plus(ttl)
        val token = Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact()
        return IssuedToken(token, expiresAt)
    }

    /** Returns null for any invalid, tampered or expired token. */
    fun parse(token: String): AuthenticatedUser? = try {
        val claims = Jwts.parser()
            .verifyWith(key)
            .clock { Date.from(clock.instant()) }
            .build()
            .parseSignedClaims(token)
            .payload
        AuthenticatedUser(claims.subject.toLong(), claims["email"] as String)
    } catch (e: JwtException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
