package com.example.nexus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@contact.vativahub.com");
            message.setTo(toEmail);
            message.setSubject("Vativa Hub - Password Reset OTP");
            message.setText(
                "Hello,\n\n" +
                "Your OTP for resetting your Vativa Hub password is: " + otp + "\n\n" +
                "This OTP is valid for 5 minutes.\n\n" +
                "— Vativa Hub"
            );
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Email send failed: " + e.getMessage());
            e.printStackTrace();
            throw e; // re-throw so UserService returns 500
        }
    }
}
