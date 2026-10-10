package com.tt.financetracker.service;

import com.tt.financetracker.dto.response.NotificationResponse;
import com.tt.financetracker.entity.User;
import com.tt.financetracker.exception.ResourceNotFoundException;
import com.tt.financetracker.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final AccountService accountService;

    public NotificationService(NotificationRepository notificationRepository, AccountService accountService) {
        this.notificationRepository = notificationRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {
        User user = accountService.requireByEmail(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByUserIdAndReadFalse(accountService.requireByEmail(email).getId());
    }

    @Transactional
    public NotificationResponse markRead(String email, Long id) {
        User user = accountService.requireByEmail(email);
        var notification = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) throw new AccessDeniedException("This notification belongs to another user");
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
