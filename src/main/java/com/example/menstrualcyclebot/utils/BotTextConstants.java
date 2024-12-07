package com.example.menstrualcyclebot.utils;

public class BotTextConstants {

    // Button Names
    public static final String COMMAND_START = "/start";
    public static final String COMMAND_GET_RECOMMENDATION = "💡 Получить рекомендацию";
    public static final String COMMAND_CURRENT_CYCLE_DAY = "📅 Текущий день цикла";
    public static final String COMMAND_PROFILE_SETTINGS = "👤 Профиль";
    public static final String COMMAND_NOTIFICATIONS_SETTINGS = "🔔 Настроить уведомления";
    public static final String COMMAND_CALENDAR = "📆 Календарь";
    public static final String COMMAND_NEW_CYCLE = "🔄 Новый цикл";

    public static final String COMMAND_ENTER_DATA = "✍️ Ввести данные";
    public static final String ENTER_HISTORICAL_DATA = "✍️ Ввести исторические данные";
    public static final String FINISH_HISTORICAL_DATA = "Завершить ввод исторических данных";
    public static final String CURRENT_CYCLE_DATA = "Ввести данные актуального цикла";
    public static final String BACK = "Назад";
    public static final String ENTER_ANOTHER_CYCLE = "Ввести еще один цикл";
    public static final String FINISH_DATA_ENTRY = "Закончить ввод данных";
    public static final String DELETE_CYCLE = "Удалить один из введенных циклов";
    public static final String DELETE_CURRENT_CYCLE = "Удалить цикл";
    public static final String MAIN_MENU = "Главное меню";
    public static final String CONFIRM_DELETE_CYCLE = "Да, удалить текущий цикл";
    public static final String NO_ACTIVE_OR_DELAYED_CYCLE_FOUND = "Не найден активный или задержанный цикл для создания нового.";

    // Message Texts
    public static final String UNKNOWN_COMMAND = "Неизвестная команда. Пожалуйста, выберите опцию из меню.";
    public static final String START_MESSAGE = "Здравствуйте! Для того, чтобы сразу начать, мы просим Вас ввести актуальные данные!";
    public static final String HELP_MESSAGE = "Чем я могу вам помочь? Выберите одну из команд в меню.";
    public static final String CYCLE_DELETED = "Активный цикл был успешно удален.";
    public static final String NO_ACTIVE_CYCLE = "У вас нет активного цикла для удаления.";
    public static final String HISTORICAL_CYCLES_LIMIT = "У вас уже есть 6 завершённых циклов. Ввод дополнительных данных невозможен.";
    public static final String ENTER_HISTORICAL_DATA_PROMPT = "Пожалуйста, введите дату завершённого цикла:";
    public static final String NEW_CYCLE_CREATED = "Новый цикл успешно создан.";
    public static final String CYCLE_UPDATED = "Ваш цикл успешно обновлен.";
    public static final String RECOMMENDATION_REQUEST = "Выберите тип ввода данных:";
    public static final String PROFILE_SETTINGS_TEXT = "Настройки профиля: ";
    public static final String ERROR_TRY_AGAIN_LATER = "Произошла ошибка. Попробуйте позже.";
    public static final String NO_COMPLETED_CYCLES_FOR_DELETION = "У вас нет завершённых циклов для удаления.";
    public static final String PROMPT_CYCLE_DELETION = "Введите номер цикла, который вы хотите удалить:";
    public static final String ERROR_GENERATING_CALENDAR = "Произошла ошибка при генерации календаря. Попробуйте снова позже.";
    public static final String MESSAGE_BEFORE_CALENDAR = "Выберите день для получения информации о фазе цикла";
    public static final String ENTER_DATE_FORMAT_DD_MM_YYYY = "Введите дату в формате дд-мм-гггг):";
    public static final String ENTER_TIMEZONE_OFFSET = "Введите смещение часового пояса (например, +3 для Москвы):";
    public static final String HISTORICAL_CYCLE_DATA_SAVED = "Данные исторических циклов сохранены.";
    public static final String NO_COMPLETED_CYCLES = "У вас нет завершенных циклов.";
    public static final String SELECT_COMMAND = "Выберите команду:";
    public static final String CYCLE_SAVED_SUCCESSFULLY = "Ваш цикл успешно сохранен!";
    public static final String YOUR_COMPLETED_CYCLES_LIST = "Ваши завершённые циклы:";
    public static final String PROMPT_DELETE_CYCLE = "Хотите удалить этот цикл?";
    public static final String ENTER_LAST_CYCLE_START_DATE = "Пожалуйста, введите дату начала вашего последнего цикла:";


    // Notification Types (обновленные)
    public static final String NOTIFICATION_TYPE_GENERAL_RECOMMENDATIONS = "Общие рекомендации";
    public static final String NOTIFICATION_TYPE_PHYSICAL_ACTIVITY = "Физическая активность";
    public static final String NOTIFICATION_TYPE_NUTRITION = "Питание";
    public static final String NOTIFICATION_TYPE_WORK_PRODUCTIVITY = "Работа и продуктивность";
    public static final String NOTIFICATION_TYPE_RELATIONSHIPS_COMMUNICATION = "Отношения и коммуникации";
    public static final String NOTIFICATION_TYPE_CARE = "Забота и уход";
    public static final String NOTIFICATION_TYPE_EMOTIONAL_WELLBEING = "Ментальное здоровье";
    public static final String NOTIFICATION_TYPE_SEX = "Секс";
    public static final String TODAY = "Сегодня: ";
    public static final String CYCLE_DAY_SUFFIX = " день цикла.";
    public static final String CYCLE_CANCELLED_SUCCESSFULLY = "Цикл успешно отменен.";
    public static final String ERROR_CYCLE_NOT_FOUND = "Ошибка: Цикл не найден.";

    // Buttons
    public static final String BACK_BUTTON = "Назад";

    // Recommendations Submenu

    public static final String SETTING_UP_GENERAL_RECOMMENDATIONS = "Общие рекомендации";
    public static final String SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS = "Окно фертильности";
    public static final String SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS = "Задержка цикла";
    public static final String BACK_TO_USER_SETTINGS_MENU = "Назад в настройки";
    public static final String BACK_TO_NOTIFICATION_SETTING = "Назад в настройки пользователя";

    public static final String OVULATION_EMOJI = "🌷";
    public static final String FERTILE_WINDOW_EMOJI = "\uD83C\uDF15";
    public static final String CYCLE_DELAY_EMOJI = "\uD83D\uDDD3";
    public static final String MENSTRUATION_EMOJI = "💧";
    public static final String FOLLICULAR_PHASE_EMOJI = "\uD83D\uDC44";
    public static final String LUTEAL_PHASE_EMOJI = "🌿";
    public static final String QUESTION_MARK_EMOJI = "❓";

}
