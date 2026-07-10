package com.hotel.backend.service;

import com.hotel.backend.model.Booking;
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
        String content = buildBookingConfirmationContent(booking);
        sendEmail(booking.getGuestEmail(), "Xác nhận đặt phòng thành công - " + booking.getBookingCode(), content);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
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
        sendEmail(toEmail, "Yêu cầu khôi phục mật khẩu - Cybertron Hotel", content);
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        String brevoApiKey = System.getenv("BREVO_API_KEY");
        String resendApiKey = System.getenv("RESEND_API_KEY");

        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            System.out.println(">>> Sending email via Brevo REST API to: " + toEmail);
            sendEmailViaBrevo(toEmail, subject, htmlContent, brevoApiKey);
            return;
        }

        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            System.out.println(">>> Sending email via Resend REST API to: " + toEmail);
            sendEmailViaResend(toEmail, subject, htmlContent, resendApiKey);
            return;
        }

        System.out.println(">>> Sending email via SMTP JavaMailSender to: " + toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println(">>> SMTP Email sent successfully.");
        } catch (Exception e) {
            System.err.println(">>> Error sending SMTP email: " + e.getMessage());
        }
    }

    private void sendEmailViaBrevo(String toEmail, String subject, String htmlContent, String apiKey) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            String jsonBody = "{"
                    + "\"sender\":{\"name\":\"Cybertron Hotel\",\"email\":\"lifangaming2@gmail.com\"},"
                    + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"htmlContent\":\"" + escapeJson(htmlContent) + "\""
                    + "}";
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            System.out.println(">>> Brevo Mail Response Status: " + response.statusCode());
            System.out.println(">>> Brevo Mail Response Body: " + response.body());
        } catch (Exception e) {
            System.err.println(">>> Failed to send mail via Brevo: " + e.getMessage());
        }
    }

    private void sendEmailViaResend(String toEmail, String subject, String htmlContent, String apiKey) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            String jsonBody = "{"
                    + "\"from\":\"Cybertron Hotel <onboarding@resend.dev>\","
                    + "\"to\":[\"" + toEmail + "\"],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"html\":\"" + escapeJson(htmlContent) + "\""
                    + "}";
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            System.out.println(">>> Resend Mail Response Status: " + response.statusCode());
            System.out.println(">>> Resend Mail Response Body: " + response.body());
        } catch (Exception e) {
            System.err.println(">>> Failed to send mail via Resend: " + e.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
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
}
