package com.example.demo.services;

import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SyncStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Coverage criteria: service unit coverage for YouTube URL parsing and thumbnail extraction helpers,
// verifying channel ids, handles, video ids, playlist ids, thumbnail priority, missing thumbnails, and invalid URLs.
class YoutubeClientServiceTest {

    private final YoutubeClientService service = new YoutubeClientService();
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    void extractChannelIdReturnsChannelIdFromChannelUrl() {
        String channelId = service.extractChannelId("https://www.youtube.com/channel/UCabc_123-XYZ");

        assertEquals("UCabc_123-XYZ", channelId);
    }

    @Test
    void extractChannelIdReturnsHandleFromHandleUrl() {
        String handle = service.extractChannelId("https://www.youtube.com/@creator-name");

        assertEquals("@creator-name", handle);
    }

    @Test
    void extractChannelIdThrowsForInvalidUrl() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.extractChannelId("https://example.com/no-channel"));

        assertEquals("Invalid YouTube channel URL", exception.getMessage());
    }

    @Test
    void extractVideoIdSupportsWatchAndShortUrls() {
        assertEquals("abc123DEF45", service.extractVideoId("https://www.youtube.com/watch?v=abc123DEF45"));
        assertEquals("abc123DEF45", service.extractVideoId("https://youtu.be/abc123DEF45"));
    }

    @Test
    void extractVideoIdReturnsNullWhenNoVideoIdExists() {
        assertNull(service.extractVideoId("https://www.youtube.com/@creator"));
    }

    @Test
    void extractPlaylistIdReturnsListQueryParameter() {
        String playlistId = service.extractPlaylistId("https://www.youtube.com/playlist?list=PLabc_123-XYZ");

        assertEquals("PLabc_123-XYZ", playlistId);
    }

    @Test
    void extractPlaylistIdReturnsNullWhenMissing() {
        assertNull(service.extractPlaylistId("https://www.youtube.com/watch?v=abc123DEF45"));
    }

    @Test
    void extractChannelProfilePictureUrlPrefersHighThumbnail() throws Exception {
        JsonNode item = new ObjectMapper().readTree("""
                {
                  "snippet": {
                    "thumbnails": {
                      "default": {"url": "default.jpg"},
                      "medium": {"url": "medium.jpg"},
                      "high": {"url": "high.jpg"}
                    }
                  }
                }
                """);

        assertEquals("high.jpg", service.extractChannelProfilePictureUrl(item));
    }

    @Test
    void extractChannelProfilePictureUrlFallsBackToMediumThenDefault() throws Exception {
        JsonNode mediumItem = new ObjectMapper().readTree("""
                {"snippet": {"thumbnails": {"medium": {"url": "medium.jpg"}}}}
                """);
        JsonNode defaultItem = new ObjectMapper().readTree("""
                {"snippet": {"thumbnails": {"default": {"url": "default.jpg"}}}}
                """);

        assertEquals("medium.jpg", service.extractChannelProfilePictureUrl(mediumItem));
        assertEquals("default.jpg", service.extractChannelProfilePictureUrl(defaultItem));
    }

    @Test
    void extractChannelProfilePictureUrlReturnsNullWhenMissing() throws Exception {
        JsonNode item = new ObjectMapper().readTree("""
                {"snippet": {"thumbnails": {}}}
                """);

        assertNull(service.extractChannelProfilePictureUrl(item));
        assertNull(service.extractChannelProfilePictureUrl(null));
    }

    @Test
    void getChannelDataUsesChannelIdWhenIdentifierStartsWithUc() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&id=UCabc123&key=test-key"))
                .andRespond(withSuccess("{\"items\":[{\"id\":\"UCabc123\"}]}", MediaType.APPLICATION_JSON));

        JsonNode response = service.getChannelData("UCabc123");

        assertEquals("UCabc123", response.path("items").get(0).path("id").asText());
    }

    @Test
    void getChannelProfilePictureUrlReturnsNullWhenApiHasNoItems() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&forHandle=@creator&key=test-key"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertNull(service.getChannelProfilePictureUrl("@creator"));
    }

    @Test
    void getChannelIdFromPlaylistUrlReturnsPlaylistOwnerChannel() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/playlists?part=snippet&id=PLabc123&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[{"snippet":{"channelId":"UCowner"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/playlists?part=snippet&id=PLabc123&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[{"snippet":{"channelId":"UCowner"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("UCowner", service.getChannelIdFromPlaylistUrl("https://youtube.com/playlist?list=PLabc123"));
        assertTrue(service.isPlaylistUrlFromChannel("https://youtube.com/playlist?list=PLabc123", "UCowner"));
    }

    @Test
    void getChannelIdFromVideoUrlReturnsVideoOwnerChannel() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=abc123DEF45&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[{"snippet":{"channelId":"UCvideo"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=abc123DEF45&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[{"snippet":{"channelId":"UCvideo"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("UCvideo", service.getChannelIdFromVideoUrl("https://youtu.be/abc123DEF45"));
        assertTrue(service.isVideoUrlFromChannel("https://youtu.be/abc123DEF45", "UCvideo"));
    }

    @Test
    void fetchVideoDetailsMapsDurationThumbnailAndProcessingStatus() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=abc123DEF45&key=test-key"))
                .andRespond(withSuccess("""
                        {
                          "items": [{
                            "id": "abc123DEF45",
                            "snippet": {
                              "title": "Video title",
                              "thumbnails": {"high": {"url": "high.jpg"}}
                            },
                            "contentDetails": {"duration": "PT1H2M3S"}
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<Video> videos = service.fetchVideoDetails(List.of("abc123DEF45"));

        assertEquals(1, videos.size());
        assertEquals("abc123DEF45", videos.get(0).getYoutubeVideoId());
        assertEquals("Video title", videos.get(0).getTitle());
        assertEquals("1:02:03", videos.get(0).getDuration());
        assertEquals("high.jpg", videos.get(0).getThumbnailUrl());
        assertEquals(SyncStatus.PROCESSING, videos.get(0).getSyncStatus());
    }

    @Test
    void fetchVideoDetailsReturnsEmptyListWhenApiFails() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=abc123DEF45&key=test-key"))
                .andRespond(withServerError());

        assertTrue(service.fetchVideoDetails(List.of("abc123DEF45")).isEmpty());
    }

    @Test
    void getVideosFromPlaylistFetchesPlaylistItemsThenVideoDetails() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/playlistItems?part=contentDetails&playlistId=PLabc&maxResults=50&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[{"contentDetails":{"videoId":"abc123DEF45"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=abc123DEF45&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[{"id":"abc123DEF45","snippet":{"title":"Video","thumbnails":{"default":{"url":"default.jpg"}}},"contentDetails":{"duration":"PT2M5S"}}]}
                        """, MediaType.APPLICATION_JSON));

        List<Video> videos = service.getVideosFromPlaylist("PLabc");

        assertEquals(1, videos.size());
        assertEquals("02:05", videos.get(0).getDuration());
        assertEquals("default.jpg", videos.get(0).getThumbnailUrl());
    }

    @Test
    void getLatestVideosFromChannelReturnsEmptyForNonPositiveLimit() {
        assertTrue(service.getLatestVideosFromChannel("UCchannel", 0).isEmpty());
    }

    @Test
    void getAllVideosFromChannelFollowsNextPageTokens() {
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/search?part=id&channelId=UCchannel&type=video&maxResults=50&key=test-key"))
                .andRespond(withSuccess("""
                        {"nextPageToken":"next","items":[{"id":{"videoId":"abc123DEF45"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/search?part=id&channelId=UCchannel&type=video&maxResults=50&key=test-key&pageToken=next"))
                .andRespond(withSuccess("""
                        {"items":[{"id":{"videoId":"xyz123DEF45"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=abc123DEF45,xyz123DEF45&key=test-key"))
                .andRespond(withSuccess("""
                        {"items":[
                          {"id":"abc123DEF45","snippet":{"title":"One","thumbnails":{}},"contentDetails":{"duration":"PT1M"}},
                          {"id":"xyz123DEF45","snippet":{"title":"Two","thumbnails":{}},"contentDetails":{"duration":"PT2M"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Video> videos = service.getAllVideosFromChannel("UCchannel");

        assertEquals(2, videos.size());
        assertEquals("One", videos.get(0).getTitle());
        assertEquals("Two", videos.get(1).getTitle());
    }
}
