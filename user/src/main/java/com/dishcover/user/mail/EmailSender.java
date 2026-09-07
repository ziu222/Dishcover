package com.dishcover.user.mail;

import com.dishcover.user.exception.ApiExceptions.EmailDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Gửi email OTP xác thực — KHÁC {@code notification/mail/EmailSender} (best-effort, nuốt lỗi):
 * ở đây email là sản phẩm chính của luồng đăng ký, gửi lỗi phải ném ra để {@code register()} báo
 * lỗi thẳng cho người dùng thay vì báo "đã gửi" trong khi thực ra chưa (docs/specs/
 * email-otp-verification.md mục 1).
 */
@Component
public class EmailSender {

    private final JavaMailSender mailSender;

    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @param to  email người nhận
     * @param otp mã 6 số đã sinh qua {@code OtpStore.issue()}
     * @throws EmailDeliveryException nếu SMTP lỗi/timeout
     */
    public void send(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã xác thực Larder");
        message.setText("Mã xác thực của bạn là: " + otp + "\n\nMã có hiệu lực trong 10 phút.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailDeliveryException("Không gửi được email xác thực, thử lại sau");
        }
    }
}
