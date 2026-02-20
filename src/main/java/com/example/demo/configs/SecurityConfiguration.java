package com.example.demo.configs;

import com.example.demo.entities.CustomOAuth2User;
import com.example.demo.entities.User;
import com.example.demo.services.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
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
                .cors(cors -> {})

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/api/test/**",
                                "/actuator/**",
                                "/api/review/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // OAuth2 Login
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService)   // Handles OIDC (e.g. Google)
                        )
                        .successHandler((request, response, authentication) -> {

                            CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
                            User user = customUser.getUser();

                            String token = jwtService.generateToken(user);

                            // Redirect to your frontend with the JWT
                            //TODO: change this to the page we need the user to be redirected to
                            response.sendRedirect(
                                    "http://localhost:3000/oauth-success?token=" + token
                            );
                        })
                )

                // Stateless session for JWT architecture
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Custom authentication provider (for local login)
                .authenticationProvider(authenticationProvider)

                // JWT filter for incoming API requests
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}