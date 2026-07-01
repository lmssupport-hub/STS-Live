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
        try {
            Map<String, Object> payload = Map.of(
                "from", "Vativa Hub <noreply@contact.vativahub.com>",
                "to", List.of(toEmail),
                "subject", "Vativa Hub - Password Reset OTP",
                "text", "Hello,\n\n" +
                        "Your OTP for resetting your Vativa Hub password is: " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n\n" +
                        "— Vativa Hub"
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
            throw new RuntimeException(e); // re-thrown so UserService returns 500
        }
    }
}