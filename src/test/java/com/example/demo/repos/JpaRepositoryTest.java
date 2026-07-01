package com.example.demo.repos;

import com.example.demo.entities.AnalyticsReport;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.Message;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.MessageSource;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.User;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.AuthProvider;
import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.Formality;
import com.example.demo.entities.utils.MessageClassType;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.utils.MessageStatus;
import com.example.demo.entities.utils.Role;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import com.example.demo.entities.utils.VerificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:repo-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaRepositoryTest {

    @Autowired
    private AnalyticsReportRepo analyticsReportRepo;
    @Autowired
    private ChatbotCategoryRepo chatbotCategoryRepo;
    @Autowired
    private ChatbotConfigRepo chatbotConfigRepo;
    @Autowired
    private ChatbotRepo chatbotRepo;
    @Autowired
    private ChatSessionRepo chatSessionRepo;
    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;
    @Autowired
    private MessageClassRepo messageClassRepo;
    @Autowired
    private MessageRepo messageRepo;
    @Autowired
    private MessageSourceRepo messageSourceRepo;
    @Autowired
    private TrainingSourceRepository trainingSourceRepository;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private VideoRepository videoRepository;

    @Test
    void userRepoFindsUsersByEmailProviderAndRole() {
        User admin = user("Admin", "admin@example.com", Role.ADMIN);
        admin.setProvider(com.example.demo.entities.utils.AuthProvider.GOOGLE);
        admin.setProviderId("google-1");
        userRepo.save(admin);
        userRepo.save(user("Viewer", "viewer@example.com", Role.USER));

        assertTrue(userRepo.findByEmail("admin@example.com").isPresent());
        assertTrue(userRepo.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-1").isPresent());
        assertTrue(userRepo.existsByRole(Role.ADMIN));
        assertFalse(userRepo.existsByRole(Role.INFLUENCER));
    }

    @Test
    void chatbotCategoryRepoFindsExistingAndNonDeletedCategories() {
        ChatbotCategory active = chatbotCategoryRepo.save(ChatbotCategory.builder()
                .name("Education")
                .isDeleted(false)
                .build());
        chatbotCategoryRepo.save(ChatbotCategory.builder()
                .name("Deleted")
                .isDeleted(true)
                .build());

        List<ChatbotCategory> nonDeleted = chatbotCategoryRepo.findByIsDeletedFalse();

        assertTrue(chatbotCategoryRepo.existsByName("Education"));
        assertEquals(List.of(active.getId()), nonDeleted.stream().map(ChatbotCategory::getId).toList());
    }

    @Test
    void chatbotRepoFindsPublicChatbotsInfluencerChatbotAndCategoryCounts() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Education"));
        User influencer = userRepo.save(user("Creator", "creator@example.com", Role.INFLUENCER));
        Chatbot publicChatbot = chatbotRepo.save(chatbot(influencer, category, true));
        chatbotRepo.save(chatbot(userRepo.save(user("Private", "private@example.com", Role.INFLUENCER)), category, false));

        assertEquals(List.of(publicChatbot.getId()), chatbotRepo.findAllByIsPublicTrue().stream().map(Chatbot::getId).toList());
        assertEquals(publicChatbot.getId(), chatbotRepo.findByInfluencerId(influencer.getId()).getId());
        assertTrue(chatbotRepo.existsByCategoryId(category.getId()));
        assertEquals(2, chatbotRepo.countByCategoryId(category.getId()));
    }

    @Test
    void chatbotConfigRepoPersistsConfig() {
        ChatbotConfig config = chatbotConfigRepo.save(ChatbotConfig.builder()
                .name("Creator Bot")
                .description("Helpful answers")
                .greetingMessage("Hi")
                .talkLikeMe(false)
                .tone(Tone.FRIENDLY)
                .verbosity(Verbosity.BALANCED)
                .formality(Formality.CASUAL)
                .build());

        assertEquals("Creator Bot", chatbotConfigRepo.findById(config.getId()).orElseThrow().getName());
    }

    @Test
    void influencerVerificationRepoFindsByUserStatusChannelAndUser() {
        User influencer = userRepo.save(user("Creator", "creator2@example.com", Role.INFLUENCER));
        InfluencerVerification verification = influencerVerificationRepo.save(InfluencerVerification.builder()
                .user(influencer)
                .channelId("channel-1")
                .status(VerificationStatus.VERIFIED)
                .handle("@creator")
                .build());

        assertEquals(verification.getId(), influencerVerificationRepo
                .findByUserAndStatusIn(influencer, List.of(VerificationStatus.VERIFIED))
                .orElseThrow()
                .getId());
        assertEquals(List.of(verification.getId()), influencerVerificationRepo.findAllByUser(influencer).stream()
                .map(InfluencerVerification::getId)
                .toList());
        assertEquals(verification.getId(), influencerVerificationRepo.findByChannelId("channel-1").getId());
        assertEquals(verification.getId(), influencerVerificationRepo.findByUser(influencer).getId());
    }

    @Test
    void messageClassRepoFindsByCategoryTypeActiveAndAssignedChatbot() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Support"));
        Chatbot chatbot = chatbotRepo.save(chatbot(
                userRepo.save(user("Creator", "creator3@example.com", Role.INFLUENCER)),
                category,
                true));
        MessageClass system = messageClassRepo.save(messageClass(category, "General", MessageClassType.SYSTEM, true));
        MessageClass inactive = messageClassRepo.save(messageClass(category, "Old", MessageClassType.SYSTEM, false));
        MessageClass custom = messageClassRepo.save(messageClass(category, "Custom", MessageClassType.CUSTOM, true));
        chatbot.getMessageClasses().add(system);
        chatbot.getMessageClasses().add(custom);
        chatbotRepo.saveAndFlush(chatbot);

        assertEquals(2, messageClassRepo.findByCategoryIdAndType(category.getId(), MessageClassType.SYSTEM).size());
        assertEquals(List.of(system.getId()), messageClassRepo
                .findByCategoryIdAndTypeAndIsActiveTrue(category.getId(), MessageClassType.SYSTEM)
                .stream()
                .map(MessageClass::getId)
                .toList());
        assertEquals(2, messageClassRepo.findByChatbotsContaining(chatbot).size());
        assertEquals(List.of(custom.getId()), messageClassRepo
                .findByChatbotsContainingAndType(chatbot, MessageClassType.CUSTOM)
                .stream()
                .map(MessageClass::getId)
                .toList());
        assertTrue(messageClassRepo.existsByCategoryIdAndName(category.getId(), "General"));
        assertEquals(3, messageClassRepo.findByCategoryId(category.getId()).size());
        assertEquals(system.getId(), messageClassRepo.getMessageClassById(system.getId()).getId());
        assertEquals(2, messageClassRepo.findByIdIn(List.of(system.getId(), inactive.getId())).size());
    }

    @Test
    void chatSessionRepoFindsCountsChatbotSessionsAndFirstPageOrdering() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Education"));
        User user = userRepo.save(user("Viewer", "viewer2@example.com", Role.USER));
        Chatbot chatbot = chatbotRepo.save(chatbot(
                userRepo.save(user("Creator", "creator4@example.com", Role.INFLUENCER)),
                category,
                true));
        ChatSession older = chatSessionRepo.save(session(chatbot, user, ChatSessionStatus.ACTIVE, LocalDateTime.now().minusHours(2)));
        ChatSession newer = chatSessionRepo.save(session(chatbot, user, ChatSessionStatus.ACTIVE, LocalDateTime.now().minusHours(1)));
        chatSessionRepo.save(session(chatbot, user, ChatSessionStatus.DELETED, LocalDateTime.now()));

        List<ChatSession> firstPage = chatSessionRepo.findFirstPage(user.getId(), chatbot.getId(), PageRequest.of(0, 10));

        assertEquals(2, chatSessionRepo.countByUserIdAndStatusNot(user.getId(), ChatSessionStatus.DELETED));
        assertEquals(3, chatSessionRepo.findByChatbotId(chatbot.getId()).size());
        assertEquals(List.of(newer.getId(), older.getId()), firstPage.stream().map(ChatSession::getId).toList());
    }

    @Test
    void messageRepoFindsByClassSessionOrderAndChatbotSenderIntent() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Education"));
        User viewer = userRepo.save(user("Viewer", "viewer3@example.com", Role.USER));
        Chatbot chatbot = chatbotRepo.save(chatbot(
                userRepo.save(user("Creator", "creator5@example.com", Role.INFLUENCER)),
                category,
                true));
        ChatSession session = chatSessionRepo.save(session(chatbot, viewer, ChatSessionStatus.ACTIVE, LocalDateTime.now()));
        MessageClass messageClass = messageClassRepo.save(messageClass(category, "Question", MessageClassType.SYSTEM, true));
        Message oldQuestion = messageRepo.save(message(session, messageClass, "old", MessageSender.USER, MessageIntent.CONTENT_QUESTION, LocalDateTime.now().minusDays(2)));
        Message recentQuestion = messageRepo.save(message(session, messageClass, "recent", MessageSender.USER, MessageIntent.CONTENT_QUESTION, LocalDateTime.now()));
        messageRepo.save(message(session, messageClass, "bot", MessageSender.BOT, MessageIntent.CONTENT_QUESTION, LocalDateTime.now()));

        assertEquals(3, messageRepo.findByMessageClassId(messageClass.getId()).size());
        assertEquals(List.of(oldQuestion.getId(), recentQuestion.getId()), messageRepo
                .findBySessionIdOrderBySentAtAscIdAsc(session.getId())
                .stream()
                .filter(message -> message.getSender() == MessageSender.USER)
                .map(Message::getId)
                .toList());
        assertEquals(2, messageRepo.findByChatbotAndSenderAndIntent(
                chatbot.getId(), MessageSender.USER, MessageIntent.CONTENT_QUESTION).size());
        assertEquals(2, messageRepo.findByChatbotAndSenderAndIntentSince(
                        chatbot.getId(),
                        MessageSender.USER,
                        MessageIntent.CONTENT_QUESTION,
                        LocalDateTime.now().minusHours(1))
                .size());
    }

    @Test
    void trainingSourceAndVideoReposFindActiveVideosForChatbotSources() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Education"));
        Chatbot chatbot = chatbotRepo.save(chatbot(
                userRepo.save(user("Creator", "creator6@example.com", Role.INFLUENCER)),
                category,
                true));
        TrainingSource source = trainingSourceRepository.save(trainingSource(chatbot));
        Video completed = videoRepository.save(video(source, "yt-1", SyncStatus.COMPLETED));
        videoRepository.save(video(source, "yt-deleted", SyncStatus.DELETED));

        assertEquals(List.of(source.getId()), trainingSourceRepository.findByChatbotId(chatbot.getId()).stream()
                .map(TrainingSource::getId)
                .toList());
        assertTrue(videoRepository.existsByYoutubeVideoIdAndSyncStatusNot("yt-1", SyncStatus.DELETED));
        assertEquals(List.of(completed.getId()), videoRepository
                .findByTrainingSourceAndSyncStatusNot(source, SyncStatus.DELETED)
                .stream()
                .map(Video::getId)
                .toList());
        assertTrue(videoRepository.findByYoutubeVideoIdAndSyncStatusNot("yt-1", SyncStatus.DELETED).isPresent());
        assertTrue(videoRepository.findByIdAndSyncStatusNot(completed.getId(), SyncStatus.DELETED).isPresent());
    }

    @Test
    void analyticsReportRepoFindsLatestReportsAndHistory() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Education"));
        Chatbot chatbot = chatbotRepo.save(chatbot(
                userRepo.save(user("Creator", "creator7@example.com", Role.INFLUENCER)),
                category,
                true));
        LocalDateTime olderTime = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime newerTime = LocalDateTime.of(2026, 7, 1, 11, 0);
        analyticsReportRepo.save(report(chatbot, olderTime, 2));
        AnalyticsReport newer = analyticsReportRepo.save(report(chatbot, newerTime, 5));

        assertEquals(newer.getId(), analyticsReportRepo.findFirstByChatbotIdOrderByGeneratedAtDesc(chatbot.getId()).orElseThrow().getId());
        assertEquals(2, analyticsReportRepo.findByChatbotId(chatbot.getId()).size());
        assertTrue(analyticsReportRepo.findByChatbotIdAndGeneratedAt(chatbot.getId(), newerTime).isPresent());
        assertEquals(List.of(5, 2), analyticsReportRepo.findHistoryByChatbotId(chatbot.getId()).stream()
                .map(row -> (Integer) row[1])
                .toList());
    }

    @Test
    void messageSourceRepoAggregatesCitedVideosByChatbot() {
        ChatbotCategory category = chatbotCategoryRepo.save(category("Education"));
        User viewer = userRepo.save(user("Viewer", "viewer4@example.com", Role.USER));
        Chatbot chatbot = chatbotRepo.save(chatbot(
                userRepo.save(user("Creator", "creator8@example.com", Role.INFLUENCER)),
                category,
                true));
        ChatSession session = chatSessionRepo.save(session(chatbot, viewer, ChatSessionStatus.ACTIVE, LocalDateTime.now()));
        Message messageOne = messageRepo.save(message(session, null, "answer 1", MessageSender.BOT, MessageIntent.CONTENT_QUESTION, LocalDateTime.now()));
        Message messageTwo = messageRepo.save(message(session, null, "answer 2", MessageSender.BOT, MessageIntent.CONTENT_QUESTION, LocalDateTime.now()));
        TrainingSource source = trainingSourceRepository.save(trainingSource(chatbot));
        Video video = videoRepository.save(video(source, "yt-1", SyncStatus.COMPLETED));
        messageSourceRepo.save(MessageSource.builder().message(messageOne).video(video).youtubeUrl("url-1").build());
        messageSourceRepo.save(MessageSource.builder().message(messageTwo).video(video).youtubeUrl("url-2").build());

        List<Object[]> rows = messageSourceRepo.aggregateCitedVideosByChatbot(chatbot.getId());

        assertEquals(1, rows.size());
        assertEquals("yt-1", rows.get(0)[0]);
        assertEquals("Video yt-1", rows.get(0)[1]);
        assertEquals(2L, rows.get(0)[2]);
    }

    private static User user(String name, String email, Role role) {
        return User.builder()
                .name(name)
                .email(email)
                .password("secret")
                .role(role)
                .build();
    }

    private static ChatbotCategory category(String name) {
        return ChatbotCategory.builder()
                .name(name)
                .isDeleted(false)
                .build();
    }

    private static Chatbot chatbot(User influencer, ChatbotCategory category, boolean isPublic) {
        return Chatbot.builder()
                .influencer(influencer)
                .category(category)
                .status(ChatbotStatus.ACTIVE)
                .isPublic(isPublic)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static MessageClass messageClass(
            ChatbotCategory category,
            String name,
            MessageClassType type,
            boolean isActive) {
        return MessageClass.builder()
                .category(category)
                .name(name)
                .type(type)
                .isActive(isActive)
                .build();
    }

    private static ChatSession session(
            Chatbot chatbot,
            User user,
            ChatSessionStatus status,
            LocalDateTime lastMessageAt) {
        ChatSession session = ChatSession.builder()
                .chatbot(chatbot)
                .user(user)
                .status(status)
                .build();
        session.setLastMessageAt(lastMessageAt);
        return session;
    }

    private static Message message(
            ChatSession session,
            MessageClass messageClass,
            String content,
            MessageSender sender,
            MessageIntent intent,
            LocalDateTime sentAt) {
        Message message = Message.builder()
                .session(session)
                .messageClass(messageClass)
                .content(content)
                .sender(sender)
                .intent(intent)
                .status(MessageStatus.SENT)
                .build();
        message.setSentAt(sentAt);
        return message;
    }

    private static TrainingSource trainingSource(Chatbot chatbot) {
        return TrainingSource.builder()
                .chatbot(chatbot)
                .sourceType(SourceType.VIDEO)
                .sourceValue("https://youtube.com/watch?v=yt-1")
                .syncStatus(SyncStatus.COMPLETED)
                .build();
    }

    private static Video video(TrainingSource source, String youtubeVideoId, SyncStatus status) {
        return Video.builder()
                .trainingSource(source)
                .youtubeVideoId(youtubeVideoId)
                .title("Video " + youtubeVideoId)
                .syncStatus(status)
                .build();
    }

    private static AnalyticsReport report(Chatbot chatbot, LocalDateTime generatedAt, int contentGapCount) {
        return AnalyticsReport.builder()
                .chatbot(chatbot)
                .generatedAt(generatedAt)
                .contentGapCount(contentGapCount)
                .executiveSummary("summary")
                .build();
    }
}
