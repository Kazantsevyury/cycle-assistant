package com.example.menstrualcyclebot.utils.recommendations;

import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserCycleManagementService;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import com.example.menstrualcyclebot.utils.recommendations.GeneralCycleRecommendations;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MessageGenerationService {

    private final UserService userService;
    private final UserCycleManagementService userCycleManagementService;

    public MessageGenerationService(UserService userService, UserCycleManagementService userCycleManagementService) {
        this.userService = userService;
        this.userCycleManagementService = userCycleManagementService;
    }

    /**
     * Формирует персонализированное сообщение для пользователя.
     *
     * @param user Пользователь, для которого формируется сообщение.
     * @return Сформированное сообщение.
     */
    public String generateGeneralRecommendationsMessage(User user) {
        StringBuilder message = new StringBuilder();

        // Получаем текущий день цикла
        int currentDay = userCycleManagementService.getCurrentDay(user.getChatId());
        message.append("Сегодня ").append(currentDay).append(" день вашего цикла.\n");

        // Общие рекомендации
        message.append("\nОбщие рекомендации:\n");
        message.append(GeneralCycleRecommendations.MENSTRUATION_PHASE_RECOMMENDATIONS.getOrDefault(currentDay, "Нет специфических рекомендаций.")).append("\n");

        // Физическая активность
        if (user.isPhysicalActivityEnabled()) {
            message.append("\nФизическая активность:\n");
            message.append(GeneralCycleRecommendations.PHYSICAL_ACTIVITY_RECOMMENDATIONS.getOrDefault(currentDay, "Оставайтесь активными, но не перегружайтесь."));
        }

        // Питание
        if (user.isNutritionEnabled()) {
            message.append("\n\nПитание:\n");
            message.append(GeneralCycleRecommendations.NUTRITION_RECOMMENDATIONS.getOrDefault(currentDay, "Старайтесь питаться сбалансировано."));
        }

        // Продуктивность
        if (user.isWorkProductivityNotification()) {
            message.append("\n\nПродуктивность:\n");
            message.append(GeneralCycleRecommendations.PRODUCTIVITY_RECOMMENDATIONS.getOrDefault(currentDay, "Ставьте цели и действуйте."));
        }

        // Отношения и общение
        if (user.isRelationshipsCommunicationNotification()) {
            message.append("\n\nОтношения и общение:\n");
            message.append(GeneralCycleRecommendations.RELATIONSHIPS_RECOMMENDATIONS.getOrDefault(currentDay, "Общение с близкими улучшит настроение."));
        }

        // Уход за собой
        if (user.isCareNotification()) {
            message.append("\n\nУход за собой:\n");
            message.append(GeneralCycleRecommendations.CARE_RECOMMENDATIONS.getOrDefault(currentDay, "Займитесь уходом за собой."));
        }

        // Эмоциональное благополучие
        if (user.isEmotionalWellbeingNotification()) {
            message.append("\n\nЭмоциональное благополучие:\n");
            message.append(GeneralCycleRecommendations.EMOTIONAL_WELLBEING_RECOMMENDATIONS.getOrDefault(currentDay, "Практикуйте медитацию или просто расслабьтесь."));
        }

        // Сексуальная активность
        if (user.isSexNotification()) {
            message.append("\n\nСексуальная активность:\n");
            message.append(GeneralCycleRecommendations.SEXUAL_ACTIVITY_RECOMMENDATIONS.getOrDefault(currentDay, "Интимность может улучшить настроение."));
        }

        return message.toString();
    }
}
