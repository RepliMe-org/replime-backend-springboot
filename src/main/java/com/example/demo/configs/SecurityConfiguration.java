package com.example.demo.configs;

import com.example.demo.entities.CustomOAuth2User;
import com.example.demo.entities.User;
import com.example.demo.services.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthFilter jwtAuthFilter;
    private final CustomOidcUserService oidcUserService; // Inject the OIDC version
    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // Stateless session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/chatbots/**",
                                "/chatbot/categories/**",
                                "/test-fastapi",

                                // --- Swagger exact paths and wildcards ---
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api-docs/**",

                                // --- Swagger paths with your /api/v1 prefix ---
                                "/api/v1/v3/api-docs",
                                "/api/v1/v3/api-docs/**",
                                "/api/v1/swagger-ui/**",
                                "/api/v1/swagger-ui.html"
                        ).permitAll()
                        .anyRequest()
                        .authenticated())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                        )
                )

                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService)
                        )
                        .successHandler((request, response, authentication) -> {

                            CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
                            User user = customUser.getUser();

                            String token = jwtService.generateToken(user);

                            String username = user.getName();
                            String role = user.getRole().name();

                            // IMPORTANT: encode values to avoid URL issues
                            String redirectUrl = String.format(
                                    "http://localhost:4200/auth/callback?token=%s&username=%s&role=%s",
                                    URLEncoder.encode(token, StandardCharsets.UTF_8),
                                    URLEncoder.encode(username, StandardCharsets.UTF_8),
                                    URLEncoder.encode(role, StandardCharsets.UTF_8)
                            );

                            response.sendRedirect(redirectUrl);
                        })
                )

                // Disable default form login to prevent redirects
                .formLogin(form -> form.disable())


                // Custom authentication provider (for local login)
                .authenticationProvider(authenticationProvider)

                // JWT filter for incoming API requests
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}