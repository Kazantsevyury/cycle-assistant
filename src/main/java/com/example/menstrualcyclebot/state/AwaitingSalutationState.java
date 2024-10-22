package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AwaitingSalutationState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        long chatId = update.getMessage().getChatId();
        String newSalutation = update.getMessage().getText().trim();

     //   bot.updateSalutation(chatId, newSalutation);
    }
}
