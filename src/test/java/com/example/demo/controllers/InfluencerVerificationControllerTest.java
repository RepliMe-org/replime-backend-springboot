package com.example.demo.controllers;

import com.example.demo.dtos.RequestVerificationDTO;
import com.example.demo.dtos.ResponseVerificationDTO;
import com.example.demo.services.InfluencerVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InfluencerVerificationControllerTest {

    @Test
    void requestVerificationPassesChannelUrlAndTokenToService() throws Exception {
        InfluencerVerificationController controller = new InfluencerVerificationController();
        ResponseVerificationDTO serviceResponse = ResponseVerificationDTO.builder()
                .message("Verification Requested Successfully!")
                .verificationToken("verification-token")
                .expirationDateAt(LocalDateTime.now().plusHours(1))
                .build();
        TestInfluencerVerificationService service = new TestInfluencerVerificationService(serviceResponse);
        injectService(controller, service);

        RequestVerificationDTO request = new RequestVerificationDTO();
        request.setChannelUrl("https://youtube.com/@creator");

        ResponseVerificationDTO response = controller.requestVerification(request, "Bearer token");

        assertSame(serviceResponse, response);
        assertEquals("https://youtube.com/@creator", service.channelUrl.get());
        assertEquals("Bearer token", service.requestToken.get());
    }

    @Test
    void confirmVerificationReturnsOkResponseFromService() throws Exception {
        InfluencerVerificationController controller = new InfluencerVerificationController();
        ResponseVerificationDTO serviceResponse = ResponseVerificationDTO.builder()
                .message("Influencer Verification Confirmed")
                .build();
        TestInfluencerVerificationService service = new TestInfluencerVerificationService(serviceResponse);
        injectService(controller, service);

        ResponseEntity<ResponseVerificationDTO> response = controller.confirmVerification("Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertEquals("Bearer token", service.confirmToken.get());
    }

    private static void injectService(
            InfluencerVerificationController controller,
            InfluencerVerificationService service
    ) throws Exception {
        Field field = InfluencerVerificationController.class.getDeclaredField("influencerVerificationService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestInfluencerVerificationService extends InfluencerVerificationService {
        private final ResponseVerificationDTO response;
        private final AtomicReference<String> channelUrl = new AtomicReference<>();
        private final AtomicReference<String> requestToken = new AtomicReference<>();
        private final AtomicReference<String> confirmToken = new AtomicReference<>();

        private TestInfluencerVerificationService(ResponseVerificationDTO response) {
            this.response = response;
        }

        @Override
        public ResponseVerificationDTO requestVerification(String channelUrl, String token) {
            this.channelUrl.set(channelUrl);
            this.requestToken.set(token);
            return response;
        }

        @Override
        public ResponseVerificationDTO confirmVerification(String token) {
            this.confirmToken.set(token);
            return response;
        }
    }
}
