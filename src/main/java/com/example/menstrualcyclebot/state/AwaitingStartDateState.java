package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import com.example.menstrualcyclebot.utils.DateParserUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;

public class AwaitingStartDateState implements UserStateHandler {

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        // Используем DateParserUtils для обработки даты
        LocalDate startDate = DateParserUtils.parseDateWithMultipleFormats(messageText);

        if (startDate == null) {
            // Сообщаем пользователю, если дата не была распознана
            bot.sendMessage(chatId, "Пожалуйста, введите дату в формате: дд.мм.гггг");
            return;
        }

        if (startDate.isAfter(LocalDate.now())) {
            bot.sendMessage(chatId, "Дата не может быть в будущем. Пожалуйста, введите корректную дату:");
            return;
        }

        // Сохраняем дату начала цикла
        cycle.setStartDate(startDate);
        bot.changeUserState(chatId, new AwaitingPeriodLengthState());
        bot.sendMessage(chatId, "Пожалуйста, введите длину менструации (в днях):");
    }
}
