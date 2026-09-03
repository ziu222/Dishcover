package com.dishcover.notification.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Gửi email best-effort — lỗi SMTP chỉ log, KHÔNG throw lại (in-app notification đã lưu, đủ dùng). */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;

    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body, String actionUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body + "\n\nXem gợi ý công thức: " + actionUrl);
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Gửi email thất bại cho {}: {}", to, ex.getMessage());
        }
    }
}
