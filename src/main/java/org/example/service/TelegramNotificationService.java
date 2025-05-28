package org.example.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class TelegramNotificationService implements NotificationService {
    @Override public void notify(String msg) {
        // TODO 调用 Telegram Bot API
        log.info("[Telegram] {}", msg);
    }
}