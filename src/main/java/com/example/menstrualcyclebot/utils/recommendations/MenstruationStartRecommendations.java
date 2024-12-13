package com.example.menstrualcyclebot.utils.recommendations;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс MenstruationStartRecommendations хранит рекомендации, связанные с началом менструации.
 * Рекомендации разделены на дни до начала менструации и сам день начала.
 *
 * Для редактирования:
 * - Найдите нужный день перед началом менструации или сам день начала.
 * - Добавьте или измените текст рекомендации.
 * - Убедитесь, что ключи последовательны (-1, -2... для дней до начала, 1 для первого дня).
 */
public class MenstruationStartRecommendations {

    /**
     * Рекомендации по дням до начала менструации.
     * Ключ: отрицательное значение (-1, -2...) для дней до начала.
     * Значение: текст рекомендации.
     */
    public static final Map<Integer, String> DAYS_BEFORE_MENSTRUATION = new HashMap<>();

    /**
     * Рекомендации для самого начала менструации.
     * Ключ: положительное значение (1, 2...) для дней в менструации.
     */
    public static final Map<Integer, String> MENSTRUATION_START_DAYS = new HashMap<>();

    static {
        // --- Рекомендации перед началом менструации ---
        DAYS_BEFORE_MENSTRUATION.put(-3, "Три дня до начала: начинайте уменьшать интенсивность тренировок.");
        DAYS_BEFORE_MENSTRUATION.put(-2, "Два дня до начала: добавьте больше железа в рацион.");
        DAYS_BEFORE_MENSTRUATION.put(-1, "День до начала: уделите внимание отдыху и медитации.");

        // --- Рекомендации в начале менструации ---
        MENSTRUATION_START_DAYS.put(1, "Первый день: отдыхайте больше и избегайте тяжёлых нагрузок.");
        MENSTRUATION_START_DAYS.put(2, "Второй день: поддерживайте водный баланс и употребляйте лёгкую пищу.");
        MENSTRUATION_START_DAYS.put(0, "Начало менструации: время заботиться о себе и отдыхать.");
    }
}
