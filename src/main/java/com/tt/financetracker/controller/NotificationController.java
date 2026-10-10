package com.tt.financetracker.controller;

import com.tt.financetracker.dto.response.NotificationResponse;
import com.tt.financetracker.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @GetMapping
    public List<NotificationResponse> list(Authentication authentication, HttpServletResponse response) {
        response.setHeader("X-Unread-Count", Long.toString(notificationService.unreadCount(authentication.getName())));
        return notificationService.list(authentication.getName());
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(Authentication authentication, @PathVariable Long id) {
        return notificationService.markRead(authentication.getName(), id);
    }
}
