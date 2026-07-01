package com.example.demo.entities;

import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.MessageStatus;
import com.example.demo.entities.utils.Role;
import com.example.demo.entities.utils.SyncStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityBehaviorTest {

    @Test
    void userDetailsExposeEmailAuthoritiesAndEnabledFlags() {
        User user = User.builder()
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertEquals("admin@example.com", user.getUsername());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void videoRetryHelpersUseRetryCountStatusAndBackoff() {
        OffsetDateTime lastRetryAt = OffsetDateTime.parse("2026-07-01T13:00:00+03:00");
        Video retryable = Video.builder()
                .syncStatus(SyncStatus.FAILED)
                .retryCount(2)
                .maxRetries(3)
                .lastRetryAt(lastRetryAt)
                .build();
        Video exhausted = Video.builder()
                .syncStatus(SyncStatus.DEAD)
                .retryCount(3)
                .maxRetries(3)
                .lastRetryAt(lastRetryAt)
                .build();

        assertTrue(retryable.hasRetriesLeft());
        assertFalse(exhausted.hasRetriesLeft());
        assertFalse(retryable.isPermanentlyFailed());
        assertTrue(exhausted.isPermanentlyFailed());
        assertEquals(lastRetryAt.plusMinutes(4), retryable.nextRetryEligibleAt());
    }

    @Test
    void videoBuilderDefaultsRetryCounters() {
        Video video = Video.builder().build();

        assertEquals(0, video.getRetryCount());
        assertEquals(3, video.getMaxRetries());
    }

    @Test
    void chatSessionPrePersistSetsStartedAndLastMessageTimes() {
        ChatSession session = ChatSession.builder().build();

        session.onCreate();

        assertNotNull(session.getStartedAt());
        assertNotNull(session.getLastMessageAt());
    }

    @Test
    void chatSessionBuilderDefaultsStatusAndMessages() {
        ChatSession session = ChatSession.builder().build();

        assertEquals(ChatSessionStatus.ACTIVE, session.getStatus());
        assertNotNull(session.getMessages());
        assertTrue(session.getMessages().isEmpty());
    }

    @Test
    void messagePrePersistSetsSentAtAndDefaultsCollections() {
        Message message = Message.builder().build();

        message.onCreate();

        assertEquals(MessageStatus.SENT, message.getStatus());
        assertNotNull(message.getSentAt());
        assertNotNull(message.getSources());
        assertTrue(message.getSources().isEmpty());
    }

    @Test
    void chatbotAndMessageClassBuildersDefaultCollections() {
        Chatbot chatbot = Chatbot.builder()
                .status(ChatbotStatus.CONFIGURING)
                .build();
        MessageClass messageClass = MessageClass.builder()
                .name("General")
                .build();

        assertNotNull(chatbot.getMessageClasses());
        assertNotNull(chatbot.getTrainingSources());
        assertTrue(chatbot.getMessageClasses().isEmpty());
        assertTrue(chatbot.getTrainingSources().isEmpty());
        assertTrue(messageClass.isActive());
        assertNotNull(messageClass.getChatbots());
    }

    @Test
    void customOAuth2UserDelegatesOAuth2UserDataAndReturnsNullOidcPartsForPlainOAuthUser() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        User domainUser = User.builder().email("user@example.com").role(Role.USER).build();
        when(oauth2User.getAttributes()).thenReturn(Map.of("email", "user@example.com"));
        when(oauth2User.getAuthorities()).thenReturn(List.of());
        when(oauth2User.getName()).thenReturn("oauth-name");
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User, domainUser);

        assertEquals(domainUser, customUser.getUser());
        assertEquals(Map.of("email", "user@example.com"), customUser.getAttributes());
        assertEquals("oauth-name", customUser.getName());
        assertTrue(customUser.getAuthorities().isEmpty());
        assertNull(customUser.getClaims());
        assertNull(customUser.getIdToken());
        assertNull(customUser.getUserInfo());
    }
}
