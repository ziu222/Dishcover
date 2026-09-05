package com.dishcover.notification.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    JavaMailSender mailSender;

    @Test
    void sendSucceedsCallsMailSenderOnce() {
        EmailSender sender = new EmailSender(mailSender);

        sender.send("user@test.com", "Cà chua hết hạn", "Hạn dùng: 2026-09-03", "/goi-y?ingredient=ca-chua");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    /** Best-effort: lỗi SMTP chỉ log warn, KHÔNG được throw ra ngoài — in-app notification đã lưu, đủ dùng. */
    @Test
    void mailSendExceptionIsSwallowedNotThrown() {
        doThrow(new MailSendException("SMTP auth failed")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailSender sender = new EmailSender(mailSender);

        assertDoesNotThrow(() ->
                sender.send("user@test.com", "Cà chua hết hạn", "Hạn dùng: 2026-09-03", "/goi-y?ingredient=ca-chua"));
    }
}
