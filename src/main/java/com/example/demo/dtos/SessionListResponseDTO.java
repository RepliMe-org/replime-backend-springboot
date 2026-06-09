package com.example.demo.dtos;

import com.example.demo.entities.utils.ChatSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponseDTO {
    private List<SessionItem> data;
    private PaginationInfo pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionItem {
        private Long id;
        private ChatSessionStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime lastMessageAt;
        private String sessionTopic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private String nextCursor;
        private boolean hasMore;
        private int limit;
    }
}
