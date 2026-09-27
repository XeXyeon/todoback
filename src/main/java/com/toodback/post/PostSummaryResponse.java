package com.toodback.post;

import java.time.LocalDateTime;

public record PostSummaryResponse(Long id, String title, String authorNickname, long commentCount,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
}
