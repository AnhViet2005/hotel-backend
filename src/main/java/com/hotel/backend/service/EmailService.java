package com.hotel.backend.service;

import com.hotel.backend.model.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendBookingConfirmationEmail(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getGuestEmail());
            helper.setSubject("Xác nhận đặt phòng thành công - " + booking.getBookingCode());

            String content = buildBookingConfirmationContent(booking);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // Log error but don't fail the transaction
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    private String buildBookingConfirmationContent(Booking booking) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DecimalFormat currencyFormatter = new DecimalFormat("###,###,### VNĐ");

        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #007bff;'>Xác nhận đặt phòng thành công!</h2>" +
                "<p>Chào <strong>" + booking.getGuestName() + "</strong>,</p>" +
                "<p>Cảm ơn bạn đã lựa chọn khách sạn của chúng tôi. Đơn hàng của bạn đã được xác nhận (đã thanh toán cọc).</p>" +
                
                "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0;'>Thông tin đơn hàng:</h3>" +
                "<p><strong>Mã đặt phòng:</strong> " + booking.getBookingCode() + "</p>" +
                "<p><strong>Khách sạn:</strong> " + booking.getHotel().getHotelName() + "</p>" +
                "<p><strong>Ngày nhận phòng:</strong> " + booking.getCheckIn().format(formatter) + "</p>" +
                "<p><strong>Ngày trả phòng:</strong> " + booking.getCheckOut().format(formatter) + "</p>" +
                "<p><strong>Tổng tiền:</strong> " + currencyFormatter.format(booking.getTotalAmount()) + "</p>" +
                "<p><strong>Số tiền đã cọc:</strong> " + currencyFormatter.format(booking.getAdminRevenue()) + "</p>" +
                "</div>" +

                "<p>Chúng tôi rất hân hạnh được phục vụ bạn.</p>" +
                "<p>Trân trọng,<br/>Đội ngũ Cybertron Hotel</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu khôi phục mật khẩu - Cybertron Hotel");

            String content = "<html>" +
                    "<body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                    "<h2 style='color: #007bff;'>Khôi phục mật khẩu</h2>" +
                    "<p>Chào bạn,</p>" +
                    "<p>Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn. Vui lòng nhấn vào nút bên dưới để đặt lại mật khẩu:</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + resetLink + "' style='background-color: #007bff; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Đặt lại mật khẩu</a>" +
                    "</div>" +
                    "<p>Đường dẫn này sẽ hết hạn sau 15 phút. Nếu bạn không yêu cầu việc này, vui lòng bỏ qua email này.</p>" +
                    "<p>Trân trọng,<br/>Đội ngũ Cybertron Hotel</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending reset email: " + e.getMessage());
        }
    }
}
