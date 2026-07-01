package com.example.demo.dtos;

import com.example.demo.dtos.internal.BotQueryRequestDTO;
import com.example.demo.dtos.internal.BotQueryResponseDTO;
import com.example.demo.dtos.internal.VideoIndexRequestDTO;
import com.example.demo.dtos.utils.MessageSourceDto;
import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.utils.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoJsonAndBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void deleteVideoRequestSerializesSnakeCasePropertyNames() throws Exception {
        DeleteVideoRequestDTO request = DeleteVideoRequestDTO.builder()
                .youtubeVideoId("yt-123")
                .chatbotId("bot-123")
                .build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals("yt-123", json.get("youtube_video_id").asText());
        assertEquals("bot-123", json.get("chatbot_id").asText());
    }

    @Test
    void botQueryRequestSerializesFastApiPropertyNames() {
        BotQueryRequestDTO request = BotQueryRequestDTO.builder()
                .chatbotId("bot-123")
                .messageId(7L)
                .query("hello")
                .conversationHistory(List.of(BotQueryRequestDTO.ConversationHistoryDTO.builder()
                        .role("user")
                        .content("hi")
                        .build()))
                .config(BotQueryRequestDTO.ConfigDTO.builder()
                        .chatbotName("Creator Bot")
                        .description("desc")
                        .talkLikeMe(true)
                        .verbosity("BALANCED")
                        .build())
                .firstMessage(true)
                .build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals("bot-123", json.get("chatbot_id").asText());
        assertEquals(7L, json.get("message_id").asLong());
        assertTrue(json.has("conversation_history"));
        assertEquals("Creator Bot", json.get("config").get("chatbot_name").asText());
        assertTrue(json.get("config").get("talk_like_me").asBoolean());
        assertTrue(json.get("first_message").asBoolean());
    }

    @Test
    void botQueryResponseSourceSerializesSnakeCaseFields() {
        BotQueryResponseDTO response = BotQueryResponseDTO.builder()
                .answer("answer")
                .messageId(8L)
                .sessionTitle("Session")
                .sources(List.of(BotQueryResponseDTO.SourceDTO.builder()
                        .videoId("yt-1")
                        .videoTitle("Video")
                        .youtubeUrl("https://youtube.com/watch?v=yt-1")
                        .build()))
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals(8L, json.get("message_id").asLong());
        assertEquals("Session", json.get("session_title").asText());
        assertEquals("yt-1", json.get("sources").get(0).get("video_id").asText());
        assertEquals("Video", json.get("sources").get(0).get("video_title").asText());
    }

    @Test
    void videoIndexRequestSerializesVideoItemsForFastApi() {
        VideoIndexRequestDTO request = VideoIndexRequestDTO.builder()
                .chatbotId("bot-123")
                .videos(List.of(VideoIndexRequestDTO.VideoItem.builder()
                        .youtubeVideoId("yt-123")
                        .videoTitle("Video")
                        .build()))
                .build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals("bot-123", json.get("chatbot_id").asText());
        assertEquals("yt-123", json.get("videos").get(0).get("youtube_video_id").asText());
        assertEquals("Video", json.get("videos").get(0).get("video_title").asText());
    }

    @Test
    void nestedResponseBuildersKeepPaginationAndChatbotState() {
        UUID chatbotId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 13, 0);

        SessionListResponseDTO response = SessionListResponseDTO.builder()
                .data(List.of(SessionListResponseDTO.SessionItem.builder()
                        .id(1L)
                        .status(ChatSessionStatus.ACTIVE)
                        .startedAt(now)
                        .lastMessageAt(now)
                        .sessionTopic("topic")
                        .build()))
                .pagination(SessionListResponseDTO.PaginationInfo.builder()
                        .nextCursor("cursor")
                        .hasMore(true)
                        .limit(20)
                        .build())
                .build();
        PublicChatbotResponseDTO chatbot = PublicChatbotResponseDTO.builder()
                .id(chatbotId)
                .influencerUsername("creator@example.com")
                .chatbotName("Creator Bot")
                .status(ChatbotStatus.ACTIVE)
                .build();
        LoginResponseDTO login = LoginResponseDTO.builder()
                .token("token")
                .username("creator@example.com")
                .role(Role.INFLUENCER)
                .build();

        assertEquals("cursor", response.getPagination().getNextCursor());
        assertTrue(response.getPagination().isHasMore());
        assertEquals(chatbotId, chatbot.getId());
        assertEquals(Role.INFLUENCER, login.getRole());
    }

    @Test
    void utilityDtosKeepMessageSourceMetadata() {
        MessageSourceDto source = MessageSourceDto.builder()
                .videoId("yt-1")
                .videoTitle("Video")
                .thumbnailUrl("thumb.jpg")
                .youtubeUrl("url")
                .build();
        ChatSessionSearchResponseDTO.SearchMatch match = ChatSessionSearchResponseDTO.SearchMatch.builder()
                .sessionId(3L)
                .messageId(9L)
                .matchedMessage("matched")
                .sender(MessageSender.USER)
                .build();

        assertEquals("thumb.jpg", source.getThumbnailUrl());
        assertEquals("matched", match.getMatchedMessage());
        assertEquals(MessageSender.USER, match.getSender());
    }
}
