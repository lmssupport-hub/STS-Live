package com.example.nexus.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
@Service
public class EmailService {
    @Value("${resend.api.key}")
    private String resendApiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendOtpEmail(String toEmail, String otp) {
        send(
            toEmail,
            "Vativa Hub - Password Reset OTP",
            "Hello,\n\n" +
            "Your OTP for resetting your Vativa Hub password is: " + otp + "\n\n" +
            "This OTP is valid for 5 minutes.\n\n" +
            "— Vativa Hub"
        );
    }

    // NEW — invite-to-register email. registerLink carries the invite token so the
    // register page (Angular) can validate it, lock the email field, and on submit
    // stamp the new user with the inviting admin's id + the picked role.
    public void sendInviteEmail(String toEmail, String registerLink, String roleName) {
        String roleLine = roleName != null
            ? "You've been invited with the role: " + roleName + "\n\n"
            : "";

        send(
            toEmail,
            "You're invited to Vativa Hub",
            "Hello,\n\n" +
            "You have been invited to join Vativa Hub.\n\n" +
            roleLine +
            "Click the link below to complete your registration:\n" + registerLink + "\n\n" +
            "This link is valid for 72 hours.\n\n" +
            "— Vativa Hub"
        );
    }

    private void send(String toEmail, String subject, String text) {
        try {
            Map<String, Object> payload = Map.of(
                "from", "Vativa Hub <noreply@contact.vativahub.com>",
                "to", List.of(toEmail),
                "subject", subject,
                "text", text
            );
            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Email sent successfully to: " + toEmail);
            } else {
                System.err.println("❌ Email send failed: " + response.statusCode() + " - " + response.body());
                throw new RuntimeException("Resend API error: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.err.println("❌ Email send failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e); // re-thrown so callers return 500
        }
    }
}