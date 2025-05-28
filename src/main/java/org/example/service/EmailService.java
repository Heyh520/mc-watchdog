package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.config.ServerProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender sender;
    private final ServerProperties props;
    private final ScheduledExecutorService retryPool =
            Executors.newSingleThreadScheduledExecutor(
                    new CustomizableThreadFactory("Mail-Retry-"));

    public void sendWithRetry(String subject, String body, int maxRetries) {
        Runnable task = new Runnable() {
            int count = 0;
            @Override public void run() {
                try {
                    log.info("正在发送邮件....");
                    send(subject, body);
                    retryPool.shutdown();
                } catch (Exception e) {
                    if (++count >= maxRetries) {
                        log.error("Mail failed {} times, giving up", maxRetries);
                        retryPool.shutdown();
                    }
                }
            }
        };
        retryPool.scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
    }

    public void send(String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.getMail().getFrom());
        msg.setTo(props.getMail().getTo());
        msg.setSubject(subject);
        msg.setText(body);
        sender.send(msg);
        log.info("Mail sent: {}", subject);
    }
}
