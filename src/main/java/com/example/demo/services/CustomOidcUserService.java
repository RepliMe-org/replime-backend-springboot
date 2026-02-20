package com.example.demo.services;

import com.example.demo.entities.AuthProvider;
import com.example.demo.entities.CustomOAuth2User;
import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import com.example.demo.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService {

    private final UserRepo userRepo;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String providerId = oidcUser.getSubject();

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> userRepo.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .provider(AuthProvider.GOOGLE)
                                .providerId(providerId)
                                .role(Role.USER)
                                .build()
                ));

        return new CustomOAuth2User(oidcUser, user);
    }
}