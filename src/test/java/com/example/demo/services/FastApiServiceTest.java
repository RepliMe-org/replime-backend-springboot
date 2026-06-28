package com.example.demo.services;

import com.example.demo.dtos.DeleteVideoRequestDTO;
import com.example.demo.dtos.internal.AnalyticsProcessRequestDTO;
import com.example.demo.dtos.internal.AnalyticsProcessResponseDTO;
import com.example.demo.dtos.internal.BotQueryRequestDTO;
import com.example.demo.dtos.internal.BotQueryResponseDTO;
import com.example.demo.dtos.internal.VideoIndexRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for each FastAPI WebClient endpoint,
// verifying configured base URL, HTTP method/URI, internal token header, request body, response type, and returned DTO/map values.
@SuppressWarnings({"rawtypes", "unchecked"})
class FastApiServiceTest {

    private static final String INTERNAL_TOKEN = "internal-token";

    private WebClient.Builder builder;
    private WebClient webClient;
    private FastApiService service;

    @BeforeEach
    void setUp() {
        builder = mock(WebClient.Builder.class);
        webClient = mock(WebClient.class);
        when(builder.baseUrl("http://localhost:8000/ai")).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        service = new FastApiService(builder);
        ReflectionTestUtils.setField(service, "X_TOKEN", INTERNAL_TOKEN);
    }

    @Test
    void constructorConfiguresFastApiBaseUrl() {
        verify(builder).baseUrl("http://localhost:8000/ai");
        verify(builder).build();
    }

    @Test
    void callFastApiGetsHealthEndpointAndReturnsMap() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        Map<String, Object> response = Map.of("status", "ok");
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v1/health/")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Map<String, Object> result = service.callFastApi();

        assertSame(response, result);
        verify(webClient).get();
        verify(uriSpec).uri("/api/v1/health/");
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void deleteVideoChunksSendsDeleteRequestWithTokenBodyAndReturnsMap() {
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        DeleteVideoRequestDTO request = DeleteVideoRequestDTO.builder()
                .chatbot_id("chatbot-1")
                .youtube_video_id("video-1")
                .build();
        Map<String, Object> response = Map.of("deleted", true);
        when(webClient.method(HttpMethod.DELETE)).thenReturn(uriSpec);
        when(uriSpec.uri("/delete/video", "video-1")).thenReturn(bodySpec);
        when(bodySpec.header("X-INTERNAL-TOKEN", INTERNAL_TOKEN)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(request)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Map<String, Object> result = service.deleteVideoChunks("video-1", request);

        assertSame(response, result);
        verify(webClient).method(HttpMethod.DELETE);
        verify(uriSpec).uri("/delete/video", "video-1");
        verify(bodySpec).header("X-INTERNAL-TOKEN", INTERNAL_TOKEN);
        verify(bodySpec).bodyValue(request);
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void indexVideosPostsTokenAndBodyAsVoidRequest() {
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        VideoIndexRequestDTO request = new VideoIndexRequestDTO();
        request.setChatbotId("chatbot-1");
        request.setVideos(List.of(new VideoIndexRequestDTO.VideoItem("video-1", "Video title")));
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/ingest/videos")).thenReturn(bodySpec);
        when(bodySpec.header("X-INTERNAL-TOKEN", INTERNAL_TOKEN)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(request)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        service.indexVideos(request);

        verify(webClient).post();
        verify(uriSpec).uri("/ingest/videos");
        verify(bodySpec).header("X-INTERNAL-TOKEN", INTERNAL_TOKEN);
        verify(bodySpec).bodyValue(request);
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void processChatPostsTokenAndBodyAndReturnsResponse() {
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        BotQueryRequestDTO request = new BotQueryRequestDTO();
        request.setChatbotId("chatbot-1");
        request.setQuery("hello");
        request.setFirstMessage(true);
        BotQueryResponseDTO response = new BotQueryResponseDTO();
        response.setAnswer("Hi");
        response.setSessionTitle("Greeting");
        response.setMessageId(5L);
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/chat/process")).thenReturn(bodySpec);
        when(bodySpec.header("X-INTERNAL-TOKEN", INTERNAL_TOKEN)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(request)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BotQueryResponseDTO.class)).thenReturn(Mono.just(response));

        BotQueryResponseDTO result = service.processChat(request);

        assertSame(response, result);
        assertEquals("Hi", result.getAnswer());
        verify(uriSpec).uri("/chat/process");
        verify(bodySpec).header("X-INTERNAL-TOKEN", INTERNAL_TOKEN);
        verify(bodySpec).bodyValue(request);
        verify(responseSpec).bodyToMono(BotQueryResponseDTO.class);
    }

    @Test
    void processAnalyticsPostsTokenAndBodyAndReturnsResponse() {
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        AnalyticsProcessRequestDTO request = new AnalyticsProcessRequestDTO();
        request.setChatbotId("chatbot-1");
        request.setDescription("Helpful creator bot");
        request.setQuestions(List.of(new AnalyticsProcessRequestDTO.QuestionDTO("What camera do you use?", true)));
        AnalyticsProcessResponseDTO response = new AnalyticsProcessResponseDTO();
        response.setExecutiveSummary("Mostly product questions");
        response.setMostAskedClusters(List.of(Map.of("topic", "gear")));
        response.setContentGaps(List.of(Map.of("topic", "lighting")));
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/analytics/process")).thenReturn(bodySpec);
        when(bodySpec.header("X-INTERNAL-TOKEN", INTERNAL_TOKEN)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(request)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AnalyticsProcessResponseDTO.class)).thenReturn(Mono.just(response));

        AnalyticsProcessResponseDTO result = service.processAnalytics(request);

        assertSame(response, result);
        assertEquals("Mostly product questions", result.getExecutiveSummary());
        verify(uriSpec).uri("/analytics/process");
        verify(bodySpec).header("X-INTERNAL-TOKEN", INTERNAL_TOKEN);
        verify(bodySpec).bodyValue(request);
        verify(responseSpec).bodyToMono(AnalyticsProcessResponseDTO.class);
    }

}
