package com.example.demo.controllers;

import com.example.demo.services.FastApiService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for the FastAPI health test endpoint,
// verifying the service map is returned with HTTP 200.
class TestFastApiControllerTest {

    @Test
    void testReturnsFastApiHealthResponse() throws Exception {
        TestFastApiController controller = new TestFastApiController();
        Map<String, Object> serviceResponse = Map.of("message", "ok");
        injectService(controller, new TestFastApiService(serviceResponse));

        ResponseEntity<Map<String, Object>> response = controller.test();

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
    }

    private static void injectService(TestFastApiController controller, FastApiService service) throws Exception {
        Field field = TestFastApiController.class.getDeclaredField("fastApiService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestFastApiService extends FastApiService {
        private final Map<String, Object> response;

        private TestFastApiService(Map<String, Object> response) {
            super(WebClient.builder());
            this.response = response;
        }

        @Override
        public Map<String, Object> callFastApi() {
            return response;
        }
    }
}
