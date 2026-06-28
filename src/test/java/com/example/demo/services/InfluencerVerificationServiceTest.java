package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ResponseVerificationDTO;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.Role;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.exceptions.VerificationException;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Coverage criteria: service unit coverage for influencer verification request/confirm flows,
// verifying duplicate/pending/verified/missing-channel branches, token refresh, promotion, and chatbot creation.
class InfluencerVerificationServiceTest {

    private static final String TOKEN = "Bearer token";
    private static final String CHANNEL_URL = "https://youtube.com/@creator";

    @Test
    void requestVerificationCreatesPendingVerificationForNewChannel() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        User user = User.builder().id(1L).email("creator@example.com").role(Role.USER).build();
        AtomicReference<InfluencerVerification> savedVerification = new AtomicReference<>();
        TestYoutubeClientService youtubeClientService = new TestYoutubeClientService(channelData(
                "UC123",
                "@creator",
                "https://img.youtube.com/avatar.jpg",
                1200));
        TestInfluencerVerificationRepo repoFake = new TestInfluencerVerificationRepo(
                Optional.empty(),
                null,
                savedVerification);
        injectDependencies(service, user, youtubeClientService, repo(repoFake), new TestChatbotService());

        ResponseVerificationDTO response = service.requestVerification(CHANNEL_URL, TOKEN);

        assertEquals("Verification Requested Successfully!", response.getMessage());
        assertNotNull(response.getVerificationToken());
        assertFalse(response.getVerificationToken().isBlank());
        assertTrue(response.getExpirationDateAt().isAfter(LocalDateTime.now()));

        InfluencerVerification saved = savedVerification.get();
        assertSame(user, saved.getUser());
        assertEquals("UC123", saved.getChannelId());
        assertEquals("@creator", saved.getHandle());
        assertEquals(CHANNEL_URL, saved.getChannelUrl());
        assertEquals("https://img.youtube.com/avatar.jpg", saved.getAvatarUrl());
        assertEquals(1200, saved.getSubscriberCount());
        assertEquals(response.getVerificationToken(), saved.getVerificationToken());
        assertEquals(response.getExpirationDateAt(), saved.getTokenExpirationAt());
        assertEquals(VerificationStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getRequestedAt());
        assertEquals("UC123", repoFake.channelIdLookup.get());
    }

    @Test
    void requestVerificationRefreshesTokenForPendingRequestOnSameChannel() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        InfluencerVerification existing = InfluencerVerification.builder()
                .channelId("UC123")
                .channelUrl(CHANNEL_URL)
                .avatarUrl("old-avatar")
                .verificationToken("old-token")
                .tokenExpirationAt(LocalDateTime.now().plusMinutes(10))
                .status(VerificationStatus.PENDING)
                .build();
        AtomicReference<InfluencerVerification> savedVerification = new AtomicReference<>();
        TestYoutubeClientService youtubeClientService = new TestYoutubeClientService(channelData(
                "UC123",
                "@creator",
                "unused-avatar",
                1200));
        youtubeClientService.profilePictureUrl = "new-avatar";
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.USER).build(),
                youtubeClientService,
                repo(new TestInfluencerVerificationRepo(Optional.of(existing), null, savedVerification)),
                new TestChatbotService());

        ResponseVerificationDTO response = service.requestVerification(CHANNEL_URL, TOKEN);

        assertSame(existing, savedVerification.get());
        assertEquals(response.getVerificationToken(), existing.getVerificationToken());
        assertNotEquals("old-token", existing.getVerificationToken());
        assertEquals(response.getExpirationDateAt(), existing.getTokenExpirationAt());
        assertEquals("new-avatar", existing.getAvatarUrl());
        assertEquals(1, youtubeClientService.profilePictureCalls.get());
    }

    @Test
    void requestVerificationThrowsWhenPendingRequestUsesDifferentChannel() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        InfluencerVerification existing = InfluencerVerification.builder()
                .channelUrl("https://youtube.com/@other")
                .status(VerificationStatus.PENDING)
                .build();
        AtomicReference<InfluencerVerification> savedVerification = new AtomicReference<>();
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.USER).build(),
                new TestYoutubeClientService(channelData("UC123", "@creator", "avatar", 1200)),
                repo(new TestInfluencerVerificationRepo(Optional.of(existing), null, savedVerification)),
                new TestChatbotService());

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.requestVerification(CHANNEL_URL, TOKEN));

        assertEquals("You have already requested verification for different channel.", exception.getMessage());
        assertNull(savedVerification.get());
    }

    @Test
    void requestVerificationThrowsWhenUserIsAlreadyVerified() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        InfluencerVerification existing = InfluencerVerification.builder()
                .channelUrl(CHANNEL_URL)
                .status(VerificationStatus.VERIFIED)
                .build();
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.INFLUENCER).build(),
                new TestYoutubeClientService(channelData("UC123", "@creator", "avatar", 1200)),
                repo(new TestInfluencerVerificationRepo(Optional.of(existing), null, new AtomicReference<>())),
                new TestChatbotService());

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.requestVerification(CHANNEL_URL, TOKEN));

        assertEquals("You are already verified.", exception.getMessage());
    }

    @Test
    void requestVerificationThrowsWhenChannelDoesNotExist() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.USER).build(),
                new TestYoutubeClientService(emptyChannelData()),
                repo(new TestInfluencerVerificationRepo(Optional.empty(), null, new AtomicReference<>())),
                new TestChatbotService());

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.requestVerification(CHANNEL_URL, TOKEN));

        assertEquals("Channel not found", exception.getMessage());
    }

    @Test
    void requestVerificationThrowsWhenChannelWasRequestedBefore() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        InfluencerVerification duplicateChannel = InfluencerVerification.builder()
                .channelId("UC123")
                .build();
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.USER).build(),
                new TestYoutubeClientService(channelData("UC123", "@creator", "avatar", 1200)),
                repo(new TestInfluencerVerificationRepo(Optional.empty(), duplicateChannel, new AtomicReference<>())),
                new TestChatbotService());

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.requestVerification(CHANNEL_URL, TOKEN));

        assertEquals("This channel requested verification before.", exception.getMessage());
    }

    @Test
    void confirmVerificationVerifiesPendingRequestAndPromotesUser() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        User user = User.builder().id(1L).email("creator@example.com").role(Role.USER).build();
        InfluencerVerification verification = InfluencerVerification.builder()
                .user(user)
                .status(VerificationStatus.PENDING)
                .tokenExpirationAt(LocalDateTime.now().plusMinutes(30))
                .build();
        TestChatbotService chatbotService = new TestChatbotService();
        injectDependencies(
                service,
                user,
                new TestYoutubeClientService(channelData("UC123", "@creator", "avatar", 1200)),
                repo(new TestInfluencerVerificationRepo(Optional.of(verification), null, new AtomicReference<>())),
                chatbotService);

        ResponseVerificationDTO response = service.confirmVerification(TOKEN);

        assertEquals("Influencer Verification Confirmed", response.getMessage());
        assertEquals(VerificationStatus.VERIFIED, verification.getStatus());
        assertNotNull(verification.getVerifiedAt());
        assertEquals(Role.INFLUENCER, user.getRole());
        assertSame(user, chatbotService.createdForUser.get());
    }

    @Test
    void confirmVerificationGeneratesNewTokenWhenExistingTokenExpired() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        InfluencerVerification verification = InfluencerVerification.builder()
                .verificationToken("old-token")
                .status(VerificationStatus.PENDING)
                .tokenExpirationAt(LocalDateTime.now().minusMinutes(1))
                .requestedAt(LocalDateTime.now().minusHours(2))
                .build();
        AtomicReference<InfluencerVerification> savedVerification = new AtomicReference<>();
        TestChatbotService chatbotService = new TestChatbotService();
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.USER).build(),
                new TestYoutubeClientService(channelData("UC123", "@creator", "avatar", 1200)),
                repo(new TestInfluencerVerificationRepo(Optional.of(verification), null, savedVerification)),
                chatbotService);

        ResponseVerificationDTO response = service.confirmVerification(TOKEN);

        assertEquals("Previous token expired. New token generated.", response.getMessage());
        assertNotEquals("old-token", response.getVerificationToken());
        assertEquals(response.getVerificationToken(), verification.getVerificationToken());
        assertEquals(response.getExpirationDateAt(), verification.getTokenExpirationAt());
        assertEquals(VerificationStatus.PENDING, verification.getStatus());
        assertSame(verification, savedVerification.get());
        assertNull(chatbotService.createdForUser.get());
    }

    @Test
    void confirmVerificationThrowsWhenNoPendingVerificationExists() throws Exception {
        InfluencerVerificationService service = new InfluencerVerificationService();
        injectDependencies(
                service,
                User.builder().id(1L).role(Role.USER).build(),
                new TestYoutubeClientService(channelData("UC123", "@creator", "avatar", 1200)),
                repo(new TestInfluencerVerificationRepo(Optional.empty(), null, new AtomicReference<>())),
                new TestChatbotService());

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.confirmVerification(TOKEN));

        assertEquals("No pending verification found.", exception.getMessage());
    }

    private static void injectDependencies(
            InfluencerVerificationService service,
            User user,
            YoutubeClientService youtubeClientService,
            InfluencerVerificationRepo repo,
            ChatbotService chatbotService
    ) throws Exception {
        injectField(service, "jwtService", new TestJwtService(user));
        injectField(service, "youtubeClientService", youtubeClientService);
        injectField(service, "influencerVerificationRepo", repo);
        injectField(service, "chatbotService", chatbotService);
    }

    private static void injectField(InfluencerVerificationService service, String name, Object value) throws Exception {
        Field field = InfluencerVerificationService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static JsonNode channelData(
            String channelId,
            String handle,
            String avatarUrl,
            long subscriberCount
    ) throws Exception {
        String json = """
                {
                  "items": [
                    {
                      "id": "%s",
                      "snippet": {
                        "customUrl": "%s",
                        "thumbnails": {
                          "high": {
                            "url": "%s"
                          }
                        }
                      },
                      "statistics": {
                        "subscriberCount": %d
                      }
                    }
                  ]
                }
                """.formatted(channelId, handle, avatarUrl, subscriberCount);
        return new ObjectMapper().readTree(json);
    }

    private static JsonNode emptyChannelData() throws Exception {
        return new ObjectMapper().readTree("""
                {
                  "items": []
                }
                """);
    }

    private static InfluencerVerificationRepo repo(TestInfluencerVerificationRepo fake) {
        return (InfluencerVerificationRepo) Proxy.newProxyInstance(
                InfluencerVerificationRepo.class.getClassLoader(),
                new Class[]{InfluencerVerificationRepo.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByUserAndStatusIn" -> {
                        fake.userLookup.set((User) args[0]);
                        fake.statusLookup.set((List<VerificationStatus>) args[1]);
                        yield fake.verificationForUser;
                    }
                    case "findByChannelId" -> {
                        fake.channelIdLookup.set((String) args[0]);
                        yield fake.verificationForChannel;
                    }
                    case "save" -> {
                        InfluencerVerification value = (InfluencerVerification) args[0];
                        fake.savedVerification.set(value);
                        yield value;
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "InfluencerVerificationRepoProxy";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        return null;
    }

    private static class TestJwtService extends JwtService {
        private final User user;
        private final AtomicReference<String> token = new AtomicReference<>();

        private TestJwtService(User user) {
            this.user = user;
        }

        @Override
        public User extractUser(String token) {
            this.token.set(token);
            return user;
        }
    }

    private static class TestYoutubeClientService extends YoutubeClientService {
        private final JsonNode channelData;
        private final AtomicReference<String> extractedUrl = new AtomicReference<>();
        private final AtomicReference<String> channelDataIdentifier = new AtomicReference<>();
        private final AtomicInteger profilePictureCalls = new AtomicInteger();
        private String profilePictureUrl;

        private TestYoutubeClientService(JsonNode channelData) {
            this.channelData = channelData;
        }

        @Override
        public String extractChannelId(String url) {
            extractedUrl.set(url);
            return "UC123";
        }

        @Override
        public JsonNode getChannelData(String identifier) {
            channelDataIdentifier.set(identifier);
            return channelData;
        }

        @Override
        public String getChannelProfilePictureUrl(String identifier) {
            profilePictureCalls.incrementAndGet();
            return profilePictureUrl;
        }
    }

    private static class TestInfluencerVerificationRepo {
        private final Optional<InfluencerVerification> verificationForUser;
        private final InfluencerVerification verificationForChannel;
        private final AtomicReference<InfluencerVerification> savedVerification;
        private final AtomicReference<User> userLookup = new AtomicReference<>();
        private final AtomicReference<List<VerificationStatus>> statusLookup = new AtomicReference<>();
        private final AtomicReference<String> channelIdLookup = new AtomicReference<>();

        private TestInfluencerVerificationRepo(
                Optional<InfluencerVerification> verificationForUser,
                InfluencerVerification verificationForChannel,
                AtomicReference<InfluencerVerification> savedVerification
        ) {
            this.verificationForUser = verificationForUser;
            this.verificationForChannel = verificationForChannel;
            this.savedVerification = savedVerification;
        }
    }

    private static class TestChatbotService extends ChatbotService {
        private final AtomicReference<User> createdForUser = new AtomicReference<>();

        @Override
        public void createChatbot(User user) {
            createdForUser.set(user);
        }
    }

}
