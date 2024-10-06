package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.repository.CycleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;

@Service
@Slf4j
public class BotService {

    private final AbsSender absSender;
    private final CycleRepository cycleRepository;

    public BotService(AbsSender absSender, CycleRepository cycleRepository) {
        this.absSender = absSender;
        this.cycleRepository = cycleRepository;
    }

    public void processUpdate(Update update) {
        Long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        if ("/start".equalsIgnoreCase(messageText)) {
            sendWelcomeMessageWithMenu(chatId);
        } else if ("Начать отслеживание".equalsIgnoreCase(messageText)) {
            sendTextMessage(chatId, "Пожалуйста, введите дату начала последней менструации в формате ГГГГ-ММ-ДД:");
        } else if ("Статистика".equalsIgnoreCase(messageText)) {
            List<MenstrualCycle> cycles = cycleRepository.getCycles(chatId);
            if (!cycles.isEmpty()) {
                MenstrualCycle lastCycle = cycles.get(cycles.size() - 1);
                LocalDate nextPeriodStart = lastCycle.getStartDate().plusDays(lastCycle.getCycleLength());
                sendTextMessage(chatId, String.format("Ваш следующий цикл ожидается: %s", nextPeriodStart));
            } else {
                sendTextMessage(chatId, "У вас еще нет сохраненных данных о цикле. Нажмите 'Начать отслеживание', чтобы ввести данные.");
            }
        } else if ("Помощь".equalsIgnoreCase(messageText)) {
            sendTextMessage(chatId, "Я могу помочь вам отслеживать менструальный цикл. Вы можете использовать кнопки меню для управления.");
        } else {
            handleCycleInput(chatId, messageText);
        }
    }

    private void handleCycleInput(Long chatId, String messageText) {
        try {
            LocalDate startDate = LocalDate.parse(messageText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<MenstrualCycle> previousCycles = cycleRepository.getCycles(chatId);

            // Рассчитать длину цикла на основе предыдущих данных
            int cycleLength = previousCycles.isEmpty() ? 28 : previousCycles.get(previousCycles.size() - 1).getCycleLength();
            int periodLength = 5;

            MenstrualCycle cycle = new MenstrualCycle();
            cycle.setUserId(chatId);
            cycle.setCycleId(System.currentTimeMillis()); // Используем текущее время как уникальный ID для цикла
            cycle.setStartDate(startDate);
            cycle.setCycleLength(cycleLength);
            cycle.setPeriodLength(periodLength);

            cycleRepository.saveCycle(cycle);
            sendTextMessage(chatId, "Данные о цикле сохранены! Теперь вы можете запросить статистику.");
        } catch (DateTimeParseException e) {
            sendTextMessage(chatId, "Пожалуйста, введите дату в правильном формате ГГГГ-ММ-ДД.");
        }
    }

    private void sendWelcomeMessageWithMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Привет! Я бот для отслеживания менструального цикла. Используйте кнопки ниже для взаимодействия со мной:");
        message.setReplyMarkup(createMenuKeyboard());

        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message with menu: {}", e.getMessage());
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }
}
