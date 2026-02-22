package com.example.demo.entities;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OidcUser {

    private final OAuth2User oauth2User;
    private final User user;

    public CustomOAuth2User(OAuth2User oauth2User, User user) {
        this.oauth2User = oauth2User;
        this.user = user;
    }

    public User getUser() { return user; }

    @Override
    public Map<String, Object> getAttributes() { return oauth2User.getAttributes(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return oauth2User.getAuthorities(); }

    @Override
    public String getName() { return oauth2User.getName(); }

    @Override
    public Map<String, Object> getClaims() {
        return (oauth2User instanceof OidcUser oidcUser) ? oidcUser.getClaims() : null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return (oauth2User instanceof OidcUser oidcUser) ? oidcUser.getIdToken() : null;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return (oauth2User instanceof OidcUser oidcUser) ? oidcUser.getUserInfo() : null;
    }
}