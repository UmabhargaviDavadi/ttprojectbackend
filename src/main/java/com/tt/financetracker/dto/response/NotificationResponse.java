package com.tt.financetracker.dto.response;

import com.tt.financetracker.entity.Notification;

import java.time.Instant;
import java.util.Locale;

public record NotificationResponse(Long id, String title, String message, Instant createdAt,
                                   boolean read, String severity) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getCreatedAt(), notification.isRead(), notification.getType().name().toLowerCase(Locale.ROOT));
    }
}
