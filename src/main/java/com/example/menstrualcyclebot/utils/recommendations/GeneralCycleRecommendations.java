package com.example.menstrualcyclebot.utils.recommendations;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс GeneralCycleRecommendations содержит рекомендации для каждой фазы менструального цикла,
 * а также общие рекомендации, зависящие от включенных параметров (например, физическая активность, питание и т.д.).
 *
 * Каждая рекомендация структурирована в виде Map, где ключ — это номер дня фазы или общий ключ (0),
 * а значение — текст рекомендации.
 *
 * Основные категории:
 * - Рекомендации по фазам цикла (менструальная, фолликулярная, овуляционная, лютеиновая)
 * - Общие рекомендации (физическая активность, питание, продуктивность, отношения, уход, эмоциональное состояние, сексуальная активность)
 *
 * Для редактирования:
 * - Найдите нужную фазу или категорию рекомендаций.
 * - Измените текст или добавьте новый день, используя соответствующий ключ.
 * - Убедитесь, что ключи последовательны и имеют логический порядок.
 */
public class GeneralCycleRecommendations {

    /**
     * Рекомендации по фазам цикла:
     * Менструальная фаза (1–5 день цикла)
     */
    public static final Map<Integer, String> MENSTRUATION_PHASE_RECOMMENDATIONS = new HashMap<>();

    /**
     * Фолликулярная фаза (6–13 день цикла)
     */
    public static final Map<Integer, String> FOLLICULAR_PHASE_RECOMMENDATIONS = new HashMap<>();

    /**
     * Овуляционная фаза (14–16 день цикла)
     */
    public static final Map<Integer, String> OVULATION_PHASE_RECOMMENDATIONS = new HashMap<>();

    /**
     * Лютеиновая фаза (17–28 день цикла)
     */
    public static final Map<Integer, String> LUTEAL_PHASE_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Физическая активность
     */
    public static final Map<Integer, String> PHYSICAL_ACTIVITY_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Питание
     */
    public static final Map<Integer, String> NUTRITION_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Продуктивность
     */
    public static final Map<Integer, String> PRODUCTIVITY_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Отношения и общение
     */
    public static final Map<Integer, String> RELATIONSHIPS_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Уход за собой
     */
    public static final Map<Integer, String> CARE_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Эмоциональное благополучие
     */
    public static final Map<Integer, String> EMOTIONAL_WELLBEING_RECOMMENDATIONS = new HashMap<>();

    /**
     * Общие рекомендации:
     * Сексуальная активность
     */
    public static final Map<Integer, String> SEXUAL_ACTIVITY_RECOMMENDATIONS = new HashMap<>();

    static {
        // --- Рекомендации по фазам цикла ---
        // Менструальная фаза
        MENSTRUATION_PHASE_RECOMMENDATIONS.put(1, "Первый день: отдыхайте больше, избегайте сильных нагрузок.");
        MENSTRUATION_PHASE_RECOMMENDATIONS.put(2, "Второй день: пейте много воды, можно легкую растяжку.");
        MENSTRUATION_PHASE_RECOMMENDATIONS.put(3, "Третий день: небольшая прогулка поможет улучшить настроение.");
        MENSTRUATION_PHASE_RECOMMENDATIONS.put(0, "В период менструации отдыхайте и следите за своим самочувствием.");

        // Фолликулярная фаза
        FOLLICULAR_PHASE_RECOMMENDATIONS.put(1, "Начало фолликулярной фазы: время для лёгких физических упражнений.");
        FOLLICULAR_PHASE_RECOMMENDATIONS.put(0, "Фолликулярная фаза: энергия на высоте, используйте её с умом.");

        // Овуляционная фаза
        OVULATION_PHASE_RECOMMENDATIONS.put(1, "День овуляции: используйте свою энергию для общения и важных дел.");
        OVULATION_PHASE_RECOMMENDATIONS.put(0, "Овуляторная фаза: время для уверенности и креативности.");

        // Лютеиновая фаза
        LUTEAL_PHASE_RECOMMENDATIONS.put(1, "Начало лютеиновой фазы: снизьте темп и добавьте отдых.");
        LUTEAL_PHASE_RECOMMENDATIONS.put(0, "Лютеиновая фаза: заботьтесь о своём эмоциональном благополучии.");

        // --- Общие рекомендации ---
        // Физическая активность
        PHYSICAL_ACTIVITY_RECOMMENDATIONS.put(1, "Начните день с лёгкой разминки или йоги.");
        PHYSICAL_ACTIVITY_RECOMMENDATIONS.put(0, "Умеренная физическая активность помогает поддерживать тонус.");

        // Питание
        NUTRITION_RECOMMENDATIONS.put(1, "Добавьте в рацион больше овощей и фруктов.");
        NUTRITION_RECOMMENDATIONS.put(0, "Сбалансированное питание — ключ к хорошему самочувствию.");

        // Продуктивность
        PRODUCTIVITY_RECOMMENDATIONS.put(1, "Запланируйте задачи с приоритетом.");
        PRODUCTIVITY_RECOMMENDATIONS.put(0, "Составьте список дел, чтобы улучшить концентрацию.");

        // Отношения и общение
        RELATIONSHIPS_RECOMMENDATIONS.put(1, "Уделите время разговору с близкими.");
        RELATIONSHIPS_RECOMMENDATIONS.put(0, "Открытое общение укрепляет отношения.");

        // Уход за собой
        CARE_RECOMMENDATIONS.put(1, "Попробуйте новую маску для лица или расслабляющую ванну.");
        CARE_RECOMMENDATIONS.put(0, "Регулярный уход за собой повышает настроение.");

        // Эмоциональное благополучие
        EMOTIONAL_WELLBEING_RECOMMENDATIONS.put(1, "Практикуйте медитацию или дыхательные упражнения.");
        EMOTIONAL_WELLBEING_RECOMMENDATIONS.put(0, "Уделите время себе, чтобы восстановить баланс.");

        // Сексуальная активность
        SEXUAL_ACTIVITY_RECOMMENDATIONS.put(1, "Обсудите с партнёром свои желания.");
        SEXUAL_ACTIVITY_RECOMMENDATIONS.put(0, "Интимная близость может улучшить настроение и сблизить вас.");
    }
}
