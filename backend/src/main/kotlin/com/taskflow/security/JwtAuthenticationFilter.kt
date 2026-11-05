package com.taskflow.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val header = req.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith(BEARER) && SecurityContextHolder.getContext().authentication == null) {
            jwtService.parse(header.removePrefix(BEARER))?.let { principal ->
                val auth = UsernamePasswordAuthenticationToken(principal, null, emptyList())
                auth.details = WebAuthenticationDetailsSource().buildDetails(req)
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        chain.doFilter(req, res)
    }

    companion object {
        private const val BEARER = "Bearer "
    }
}
