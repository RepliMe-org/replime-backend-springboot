package com.example.demo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Coverage criteria: service unit coverage for YouTube URL parsing and thumbnail extraction helpers,
// verifying channel ids, handles, video ids, playlist ids, thumbnail priority, missing thumbnails, and invalid URLs.
class YoutubeClientServiceTest {

    private final YoutubeClientService service = new YoutubeClientService();

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
}
