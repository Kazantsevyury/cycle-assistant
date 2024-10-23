package com.example.menstrualcyclebot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class NotificationService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    public void notifyUser(Long chatId, String message) {
        String url = TELEGRAM_API_URL + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + message;

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Notification sent successfully to chat ID: " + chatId);
            } else {
                System.err.println("Failed to send notification to chat ID: " + chatId);
            }
        } catch (Exception e) {
            System.err.println("Error while sending notification: " + e.getMessage());
        }
    }
}
