package com.example.demo.services;

import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SyncStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YoutubeClientService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode getChannelData(String identifier) {

        String url;

        // If identifier starts with UC → it is a channelId
        if (identifier.startsWith("UC")) {
            url = "https://www.googleapis.com/youtube/v3/channels" +
                    "?part=snippet,statistics" +
                    "&id=" + identifier +
                    "&key=" + apiKey;
        }
        // Otherwise assume it's a handle
        else {
            url = "https://www.googleapis.com/youtube/v3/channels" +
                    "?part=snippet,statistics" +
                    "&forHandle=" + identifier +
                    "&key=" + apiKey;
        }

        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YouTube response");
        }
    }

    public String extractChannelId(String url) {

        url = url.trim();

        Pattern channelPattern = Pattern.compile("/channel/(UC[\\w-]+)");
        Matcher channelMatcher = channelPattern.matcher(url);

        if (channelMatcher.find()) {
            return channelMatcher.group(1);
        }

        Pattern handlePattern = Pattern.compile("/@([\\w-]+)");
        Matcher handleMatcher = handlePattern.matcher(url);

        if (handleMatcher.find()) {
            return "@" + handleMatcher.group(1);
        }

        throw new RuntimeException("Invalid YouTube channel URL");
    }

    public String extractVideoId(String url) {
        url = url.trim();
        Pattern pattern = Pattern.compile("(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String extractPlaylistId(String url) {
        url = url.trim();
        Pattern pattern = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String getChannelIdFromPlaylistUrl(String playlistUrl) {
        String playlistId = extractPlaylistId(playlistUrl);
        System.out.println("extracted playlist id: " + playlistId);

        if (playlistId == null) {
            return null;
        }

        String url = "https://www.googleapis.com/youtube/v3/playlists" +
                "?part=snippet" +
                "&id=" + playlistId +
                "&key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            if (items.isArray() && !items.isEmpty()) {
                return items.get(0).path("snippet").path("channelId").asText();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get playlist details from YouTube API");
        }
        return null;
    }

    public boolean isPlaylistUrlFromChannel(String playlistUrl, String expectedChannelId) {
        String channelId = getChannelIdFromPlaylistUrl(playlistUrl);
        return expectedChannelId != null && expectedChannelId.equals(channelId);
    }

    public String getChannelIdFromVideoUrl(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        System.out.println("extracted video id: " + videoId);
        if (videoId == null) {
            return null;
        }

        String url = "https://www.googleapis.com/youtube/v3/videos" +
                "?part=snippet" +
                "&id=" + videoId +
                "&key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            if (items.isArray() && items.size() > 0) {
                System.out.println("channel id: " + items.get(0).path("snippet").path("channelId").asText());
                return items.get(0).path("snippet").path("channelId").asText();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get video details from YouTube API");
        }
        return null;
    }

    public boolean isVideoUrlFromChannel(String videoUrl, String expectedChannelId) {
        String channelId = getChannelIdFromVideoUrl(videoUrl);
        return expectedChannelId != null && expectedChannelId.equals(channelId);
    }

    public Video getVideoDetailsForEntity(String videoId) {
        String url = "https://www.googleapis.com/youtube/v3/videos" +
                "?part=snippet,contentDetails" +
                "&id=" + videoId +
                "&key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            if (items.isArray() && !items.isEmpty()) {
                JsonNode snippet = items.get(0).path("snippet");
                String title = snippet.path("title").asText();
                String thumbnail = "";
                if (snippet.path("thumbnails").has("high")) {
                    thumbnail = snippet.path("thumbnails").path("high").path("url").asText();
                } else if (snippet.path("thumbnails").has("default")) {
                    thumbnail = snippet.path("thumbnails").path("default").path("url").asText();
                }

                Video video = new Video();
                video.setYoutubeVideoId(videoId);
                video.setTitle(title);
                video.setThumbnailUrl(thumbnail);
                video.setSyncStatus(SyncStatus.PROCESSING);

                return video;
            }
        } catch (Exception e) {
            System.err.println("Failed to get video details for entity: " + e.getMessage());
        }
        return null;
    }

    public List<Video> getVideosFromPlaylist(String playlistId) {
        List<Video> videos = new ArrayList<>();
        String url = "https://www.googleapis.com/youtube/v3/playlistItems" +
                "?part=snippet,contentDetails" +
                "&playlistId=" + playlistId +
                "&maxResults=50" + // TODO: make maxResult value as variable
                "&key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode snippet = item.path("snippet");
                    String videoId = item.path("contentDetails").path("videoId").asText();
                    String title = snippet.path("title").asText();
                    String thumbnail = "";
                    if (snippet.path("thumbnails").has("high")) {
                        thumbnail = snippet.path("thumbnails").path("high").path("url").asText();
                    } else if (snippet.path("thumbnails").has("default")) {
                        thumbnail = snippet.path("thumbnails").path("default").path("url").asText();
                    }

                    Video video = new Video();
                    video.setYoutubeVideoId(videoId);
                    video.setTitle(title);
                    video.setThumbnailUrl(thumbnail);
                    video.setSyncStatus(SyncStatus.PROCESSING);
                    videos.add(video);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get videos from playlist API: " + e.getMessage());
        }
        return videos;
    }

    public List<Video> getLatestVideosFromChannel(String channelId, int maxResults) {
        List<Video> videos = new ArrayList<>();
        if (maxResults <= 0) return videos;

        int limit = Math.min(maxResults, 50); // TODO: make maxResult value as variable

        String url = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet,id" +
                "&channelId=" + channelId +
                "&maxResults=" + limit +
                "&order=date" +
                "&type=video" +
                "&key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode snippet = item.path("snippet");
                    String videoId = item.path("id").path("videoId").asText();
                    String title = snippet.path("title").asText();
                    String thumbnail = "";
                    if (snippet.path("thumbnails").has("high")) {
                        thumbnail = snippet.path("thumbnails").path("high").path("url").asText();
                    } else if (snippet.path("thumbnails").has("default")) {
                        thumbnail = snippet.path("thumbnails").path("default").path("url").asText();
                    }

                    Video video = new Video();
                    video.setYoutubeVideoId(videoId);
                    video.setTitle(title);
                    video.setThumbnailUrl(thumbnail);
                    video.setSyncStatus(SyncStatus.PROCESSING);
                    videos.add(video);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get latest videos from channel API: " + e.getMessage());
        }
        return videos;
    }

    public List<Video> getAllVideosFromChannel(String channelId) {
        List<Video> videos = new ArrayList<>();
        String nextPageToken = "";
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            do {
                String url = "https://www.googleapis.com/youtube/v3/search" +
                        "?part=snippet,id" +
                        "&channelId=" + channelId +
                        "&type=video" +
                        "&maxResults=50" +
                        "&key=" + apiKey;
                
                if (!nextPageToken.isEmpty()) {
                    url += "&pageToken=" + nextPageToken;
                }

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode items = root.path("items");
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        JsonNode snippet = item.path("snippet");
                        String videoId = item.path("id").path("videoId").asText();
                        String title = snippet.path("title").asText();
                        String thumbnail = "";
                        if (snippet.path("thumbnails").has("high")) {
                            thumbnail = snippet.path("thumbnails").path("high").path("url").asText();
                        } else if (snippet.path("thumbnails").has("default")) {
                            thumbnail = snippet.path("thumbnails").path("default").path("url").asText();
                        }

                        Video video = new Video();
                        video.setYoutubeVideoId(videoId);
                        video.setTitle(title);
                        video.setThumbnailUrl(thumbnail);
                        video.setSyncStatus(SyncStatus.PROCESSING);
                        videos.add(video);
                    }
                }
                nextPageToken = root.path("nextPageToken").asText("");
            } while (!nextPageToken.isEmpty());
        } catch (Exception e) {
            System.err.println("Failed to get all videos from channel using search API: " + e.getMessage());
        }
        return videos;
    }


}