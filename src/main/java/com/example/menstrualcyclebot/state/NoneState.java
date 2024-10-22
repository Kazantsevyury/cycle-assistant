package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;


public class NoneState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        bot.sendMessage(update.getMessage().getChatId(), "Нет активного процесса. Выберите команду из меню.");
    }
}
