package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface UserStateHandler {
    void handleState(Bot bot, Update update, Cycle cycle);
}
