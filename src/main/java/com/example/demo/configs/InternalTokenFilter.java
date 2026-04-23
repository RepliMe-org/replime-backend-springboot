package com.example.demo.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${fastapi.token}")
    private String expectedInternalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Context path is usually /api/v1. To match safely:
        if (path.contains("/internal/")) {
            String token = request.getHeader("X-INTERNAL-TOKEN");
            if (token == null || !token.equals(expectedInternalToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing X-INTERNAL-TOKEN");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

