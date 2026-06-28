package com.example.demo.controllers;

import com.example.demo.dtos.ApiResponseDTO;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigResponseDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.dtos.InfluencerChatbotResponseDTO;
import com.example.demo.dtos.InfluencerMessageClassesDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.VideoResponseDTO;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.services.ChatbotConfigService;
import com.example.demo.services.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Coverage criteria: direct controller unit coverage for influencer chatbot endpoints,
// verifying service/config delegation, path/body/header forwarding, accepted/ok statuses, and generated success DTOs.
class InfluencerChatbotControllerTest {

    @Test
    void getInfluencerChatbotReturnsServiceResponse() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        ResponseEntity<InfluencerChatbotResponseDTO> serviceResponse =
                ResponseEntity.ok(InfluencerChatbotResponseDTO.builder().build());
        chatbotService.influencerChatbotResponse = serviceResponse;

        ResponseEntity<InfluencerChatbotResponseDTO> response = controller.getInfluencerChatbot("Bearer token");

        assertSame(serviceResponse, response);
        assertEquals("Bearer token", chatbotService.influencerToken.get());
    }

    @Test
    void getChatbotStatusReturnsServiceResponse() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        ResponseEntity<ChatbotStatus> serviceResponse = ResponseEntity.ok(ChatbotStatus.ACTIVE);
        chatbotService.statusResponse = serviceResponse;

        ResponseEntity<ChatbotStatus> response = controller.getChatbotStatus("Bearer token");

        assertSame(serviceResponse, response);
        assertEquals("Bearer token", chatbotService.statusToken.get());
    }

    @Test
    void saveChatbotConfigDelegatesToConfigService() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotConfigService configService = injectConfigService(controller);
        ResponseEntity<String> serviceResponse = ResponseEntity.ok("saved");
        configService.saveResponse = serviceResponse;
        ChatbotConfigRequestDTO request = new ChatbotConfigRequestDTO();

        ResponseEntity<String> response = controller.saveChatbotConfig(request, "Bearer token");

        assertSame(serviceResponse, response);
        assertSame(request, configService.saveRequest.get());
        assertEquals("Bearer token", configService.saveToken.get());
    }

    @Test
    void updateChatbotConfigDelegatesToConfigService() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotConfigService configService = injectConfigService(controller);
        ResponseEntity<ChatbotConfigResponseDTO> serviceResponse =
                ResponseEntity.ok(ChatbotConfigResponseDTO.builder().name("Bot").build());
        configService.updateResponse = serviceResponse;
        ChatbotConfigUpdateDTO request = new ChatbotConfigUpdateDTO();

        ResponseEntity<ChatbotConfigResponseDTO> response = controller.updateChatbotConfig(request, "Bearer token");

        assertSame(serviceResponse, response);
        assertSame(request, configService.updateRequest.get());
        assertEquals("Bearer token", configService.updateToken.get());
    }

    @Test
    void assignCategoryPassesCategoryAndReturnsSuccessDto() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);

        ResponseEntity<ApiResponseDTO> response = controller.assignCategory(6L, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("category assigned successfully", response.getBody().getMessage());
        assertEquals(6L, chatbotService.assignedCategoryId.get());
        assertEquals("Bearer token", chatbotService.assignedCategoryToken.get());
    }

    @Test
    void getAllMessageClassesReturnsClassificationContext() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        chatbotService.messageClassesContext = InfluencerMessageClassesDTO.builder().build();

        ResponseEntity<InfluencerMessageClassesDTO> response = controller.getAllMessageClasses("Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(chatbotService.messageClassesContext, response.getBody());
        assertEquals("Bearer token", chatbotService.classificationToken.get());
    }

    @Test
    void chooseMessageClassesPassesIdsAndToken() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        chatbotService.chosenClasses = List.of(MessageClassResponseDTO.builder().id(1L).name("Pricing").build());
        List<Long> ids = List.of(1L, 2L);

        ResponseEntity<List<MessageClassResponseDTO>> response =
                controller.chooseMessageClasses(ids, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(chatbotService.chosenClasses, response.getBody());
        assertSame(ids, chatbotService.chosenClassIds.get());
        assertEquals("Bearer token", chatbotService.chosenClassesToken.get());
    }

    @Test
    void createCustomMessageClassesPassesNamesAndReturnsSuccessDto() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        List<String> names = List.of("Refunds");

        ResponseEntity<ApiResponseDTO> response = controller.createCustomMessageClasses(names, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Custom message classes created and assigned to chatbot successfully", response.getBody().getMessage());
        assertEquals("Bearer token", chatbotService.customClassesToken.get());
        assertSame(names, chatbotService.customClassNames.get());
    }

    @Test
    void removeMessageClassPassesIdAndReturnsSuccessDto() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);

        ResponseEntity<ApiResponseDTO> response = controller.removeMessageClass(8L, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Message class removed from chatbot successfully", response.getBody().getMessage());
        assertEquals(8L, chatbotService.removedMessageClassId.get());
        assertEquals("Bearer token", chatbotService.removedMessageClassToken.get());
    }

    @Test
    void addTrainingSourceReturnsAcceptedVideos() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        chatbotService.videos = List.of(VideoResponseDTO.builder().youtubeVideoId("abc").build());
        TrainingSourceRequestDTO request = new TrainingSourceRequestDTO();

        ResponseEntity<List<VideoResponseDTO>> response = controller.addTrainingSource(request, "Bearer token");

        assertEquals(202, response.getStatusCode().value());
        assertSame(chatbotService.videos, response.getBody());
        assertSame(request, chatbotService.trainingSourceRequest.get());
        assertEquals("Bearer token", chatbotService.trainingSourceToken.get());
    }

    @Test
    void removeVideoPassesVideoIdAndReturnsMessage() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);

        ResponseEntity<String> response = controller.removeVideo("yt-123", "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Video removed successfully", response.getBody());
        assertEquals("yt-123", chatbotService.deletedVideoId.get());
        assertEquals("Bearer token", chatbotService.deletedVideoToken.get());
    }

    @Test
    void retryVideoPassesVideoIdAndToken() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        chatbotService.retryVideoResponse = VideoResponseDTO.builder().videoId(7L).build();

        ResponseEntity<VideoResponseDTO> response = controller.retryVideo(7L, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(chatbotService.retryVideoResponse, response.getBody());
        assertEquals(7L, chatbotService.retryVideoId.get());
        assertEquals("Bearer token", chatbotService.retryVideoToken.get());
    }

    @Test
    void getAllVideosReturnsServiceVideos() throws Exception {
        InfluencerChatbotController controller = newController();
        TestChatbotService chatbotService = injectChatbotService(controller);
        chatbotService.videos = List.of(VideoResponseDTO.builder().youtubeVideoId("abc").build());

        ResponseEntity<List<VideoResponseDTO>> response = controller.getAllVideos("Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(chatbotService.videos, response.getBody());
        assertEquals("Bearer token", chatbotService.getVideosToken.get());
    }

    private static InfluencerChatbotController newController() {
        return new InfluencerChatbotController();
    }

    private static TestChatbotService injectChatbotService(InfluencerChatbotController controller) throws Exception {
        TestChatbotService service = new TestChatbotService();
        Field field = InfluencerChatbotController.class.getDeclaredField("chatbotService");
        field.setAccessible(true);
        field.set(controller, service);
        return service;
    }

    private static TestChatbotConfigService injectConfigService(InfluencerChatbotController controller) throws Exception {
        TestChatbotConfigService service = new TestChatbotConfigService();
        Field field = InfluencerChatbotController.class.getDeclaredField("chatbotConfigService");
        field.setAccessible(true);
        field.set(controller, service);
        return service;
    }

    private static class TestChatbotService extends ChatbotService {
        private ResponseEntity<InfluencerChatbotResponseDTO> influencerChatbotResponse;
        private ResponseEntity<ChatbotStatus> statusResponse;
        private InfluencerMessageClassesDTO messageClassesContext;
        private List<MessageClassResponseDTO> chosenClasses;
        private List<VideoResponseDTO> videos;
        private VideoResponseDTO retryVideoResponse;
        private final AtomicReference<String> influencerToken = new AtomicReference<>();
        private final AtomicReference<String> statusToken = new AtomicReference<>();
        private final AtomicReference<Long> assignedCategoryId = new AtomicReference<>();
        private final AtomicReference<String> assignedCategoryToken = new AtomicReference<>();
        private final AtomicReference<String> classificationToken = new AtomicReference<>();
        private final AtomicReference<List<Long>> chosenClassIds = new AtomicReference<>();
        private final AtomicReference<String> chosenClassesToken = new AtomicReference<>();
        private final AtomicReference<String> customClassesToken = new AtomicReference<>();
        private final AtomicReference<List<String>> customClassNames = new AtomicReference<>();
        private final AtomicReference<Long> removedMessageClassId = new AtomicReference<>();
        private final AtomicReference<String> removedMessageClassToken = new AtomicReference<>();
        private final AtomicReference<TrainingSourceRequestDTO> trainingSourceRequest = new AtomicReference<>();
        private final AtomicReference<String> trainingSourceToken = new AtomicReference<>();
        private final AtomicReference<String> deletedVideoId = new AtomicReference<>();
        private final AtomicReference<String> deletedVideoToken = new AtomicReference<>();
        private final AtomicReference<Long> retryVideoId = new AtomicReference<>();
        private final AtomicReference<String> retryVideoToken = new AtomicReference<>();
        private final AtomicReference<String> getVideosToken = new AtomicReference<>();

        @Override
        public ResponseEntity<InfluencerChatbotResponseDTO> getInfluencerChatbot(String token) {
            influencerToken.set(token);
            return influencerChatbotResponse;
        }

        @Override
        public ResponseEntity<ChatbotStatus> getChatbotStatus(String token) {
            statusToken.set(token);
            return statusResponse;
        }

        @Override
        public void assignCategory(Long categoryId, String token) {
            assignedCategoryId.set(categoryId);
            assignedCategoryToken.set(token);
        }

        @Override
        public InfluencerMessageClassesDTO getInfluencerClassificationContext(String token) {
            classificationToken.set(token);
            return messageClassesContext;
        }

        @Override
        public List<MessageClassResponseDTO> chooseMessageClassesForChatbot(List<Long> messageClassIds, String token) {
            chosenClassIds.set(messageClassIds);
            chosenClassesToken.set(token);
            return chosenClasses;
        }

        @Override
        public void createMessageClassesForSpecificChatbot(String token, List<String> messageClassesNames) {
            customClassesToken.set(token);
            customClassNames.set(messageClassesNames);
        }

        @Override
        public void removeMessageClassFromChatbot(Long messageClassId, String token) {
            removedMessageClassId.set(messageClassId);
            removedMessageClassToken.set(token);
        }

        @Override
        public List<VideoResponseDTO> addTrainingSourceToChatbot(TrainingSourceRequestDTO sourceRequest, String token) {
            trainingSourceRequest.set(sourceRequest);
            trainingSourceToken.set(token);
            return videos;
        }

        @Override
        public void deleteVideoFromChatbot(String youtubeVideoId, String token) {
            deletedVideoId.set(youtubeVideoId);
            deletedVideoToken.set(token);
        }

        @Override
        public VideoResponseDTO retryVideo(Long videoId, String token) {
            retryVideoId.set(videoId);
            retryVideoToken.set(token);
            return retryVideoResponse;
        }

        @Override
        public List<VideoResponseDTO> getAllVideosOfChatbot(String token) {
            getVideosToken.set(token);
            return videos;
        }
    }

    private static class TestChatbotConfigService extends ChatbotConfigService {
        private ResponseEntity<String> saveResponse;
        private ResponseEntity<ChatbotConfigResponseDTO> updateResponse;
        private final AtomicReference<ChatbotConfigRequestDTO> saveRequest = new AtomicReference<>();
        private final AtomicReference<String> saveToken = new AtomicReference<>();
        private final AtomicReference<ChatbotConfigUpdateDTO> updateRequest = new AtomicReference<>();
        private final AtomicReference<String> updateToken = new AtomicReference<>();

        @Override
        public ResponseEntity<String> saveChatbotConfig(ChatbotConfigRequestDTO requestDTO, String token) {
            saveRequest.set(requestDTO);
            saveToken.set(token);
            return saveResponse;
        }

        @Override
        public ResponseEntity<ChatbotConfigResponseDTO> updateChatbotConfig(ChatbotConfigUpdateDTO requestDTO, String token) {
            updateRequest.set(requestDTO);
            updateToken.set(token);
            return updateResponse;
        }
    }
}
