package com.example.demo.configs;

import com.example.demo.entities.User;
import com.example.demo.entities.utils.Role;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtAuthFilterContinuesWithoutAuthenticationWhenHeaderMissing() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }

    @Test
    void jwtAuthFilterSetsAuthenticationForValidBearerToken() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService);
        User user = User.builder()
                .email("user@example.com")
                .role(Role.USER)
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);
        when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void jwtAuthFilterDoesNotAuthenticateInvalidToken() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService);
        User user = User.builder()
                .email("user@example.com")
                .role(Role.USER)
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(jwtService.extractUsername("invalid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);
        when(jwtService.isTokenValid("invalid-token", user)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void jwtAuthFilterSwallowsMalformedTokenExceptionAndContinuesChain() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.addHeader("Authorization", "Bearer TOKEN_NOT_FOUND");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(jwtService.extractUsername("TOKEN_NOT_FOUND"))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException(
                        "JWT strings must contain exactly 2 period characters. Found: 0"));

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }

    @Test
    void internalTokenFilterRejectsInternalEndpointWithoutExpectedToken() throws ServletException, IOException {
        InternalTokenFilter filter = new InternalTokenFilter();
        ReflectionTestUtils.setField(filter, "expectedInternalToken", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/internal/video-status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = mock(MockFilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Invalid or missing X-INTERNAL-TOKEN", response.getErrorMessage());
    }

    @Test
    void internalTokenFilterAllowsInternalEndpointWithExpectedToken() throws ServletException, IOException {
        InternalTokenFilter filter = new InternalTokenFilter();
        ReflectionTestUtils.setField(filter, "expectedInternalToken", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/internal/video-status");
        request.addHeader("X-INTERNAL-TOKEN", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = mock(MockFilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    @Test
    void internalTokenFilterAllowsNonInternalEndpointWithoutToken() throws ServletException, IOException {
        InternalTokenFilter filter = new InternalTokenFilter();
        ReflectionTestUtils.setField(filter, "expectedInternalToken", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/chatbots");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = mock(MockFilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }
}
