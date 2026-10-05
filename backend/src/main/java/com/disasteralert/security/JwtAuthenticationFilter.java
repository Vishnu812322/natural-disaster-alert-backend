package com.disasteralert.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey key;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${app.jwt.secret}") String secret) {

        this.jwtService = jwtService;

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET must be at least 32 characters"
            );
        }

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        String path = request.getServletPath();

        return path.equals("/")
                || path.equals("/error")
                || path.equals("/api/health")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/public/")
                || path.equals("/api/devices")
                || path.startsWith("/api/dev/fcm/")
                || path.matches("/api/notifications/alert/.*/prepare")
                || path.equals("/api/cap/sync")
                || path.matches("/api/users/.*/location");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String header =
                request.getHeader("Authorization");

        if (header != null
                && header.startsWith("Bearer ")) {

            String token =
                    header.substring(7);

            if (jwtService.valid(token)) {

                try {

                    Claims claims =
                            Jwts.parser()
                                    .verifyWith(key)
                                    .build()
                                    .parseSignedClaims(token)
                                    .getPayload();

                    String email =
                            claims.getSubject();

                    String role =
                            String.valueOf(
                                    claims.getOrDefault(
                                            "role",
                                            "USER"
                                    )
                            );

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + role
                                            )
                                    )
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);

                } catch (Exception ignored) {

                    SecurityContextHolder
                            .clearContext();
                }
            }
        }

        chain.doFilter(request, response);
    }
}