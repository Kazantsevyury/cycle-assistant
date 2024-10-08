package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.CycleInfo;
import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.repository.CycleRepository;
import com.example.menstrualcyclebot.repository.UserRepository;
import com.example.menstrualcyclebot.service.CycleService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.bots.AbsSender;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;

@Service
@Slf4j
public class BotService {

    private final AbsSender absSender;
    private final CycleRepository cycleRepository;
    private final UserRepository userRepository;
    private final CycleService cycleService;

    public BotService(AbsSender absSender, CycleRepository cycleRepository, UserRepository userRepository, CycleService cycleService) {
        this.absSender = absSender;
        this.cycleRepository = cycleRepository;
        this.userRepository = userRepository;
        this.cycleService = cycleService;
    }

    public void processUpdate(Update update) {
        Long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        Optional<User> userOptional = userRepository.findById(chatId);
        User user = userOptional.orElseGet(() -> {
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setUsername(update.getMessage().getFrom().getUserName());
            return userRepository.save(newUser);
        });

        if (!user.isSetupComplete()) {
            handleInitialSetup(user, messageText);
            return;
        }

        if ("/start".equalsIgnoreCase(messageText)) {
            sendWelcomeMessageWithMenu(chatId);
        } else if ("Начать отслеживание".equalsIgnoreCase(messageText)) {
            sendTextMessage(chatId, "Пожалуйста, введите данные в формате: ГГГГ-ММ-ДД <длительность менструации>");
        } else if ("Статистика".equalsIgnoreCase(messageText)) {
            sendCycleStatistics(chatId);
        } else {
            handleCycleInput(chatId, messageText);
        }
    }

    private void handleInitialSetup(User user, String messageText) {
        Long chatId = user.getChatId();

        if (user.getMenstrualCycles() == null || user.getMenstrualCycles().isEmpty()) {
            if (messageText.equalsIgnoreCase("/start")) {
                sendTextMessage(chatId, "Добро пожаловать! Давайте начнем настройку. Пожалуйста, введите длительность вашей менструации (в днях):");
            } else if (messageText.matches("\\d+")) {
                int periodLength = Integer.parseInt(messageText);
                MenstrualCycle cycle = new MenstrualCycle();
                cycle.setCycleLength(28);  // Длительность цикла фиксированная — 28 дней
                cycle.setPeriodLength(periodLength);
                user.setMenstrualCycles(List.of(cycle));  // Создаем запись цикла
                userRepository.save(user);
                sendTextMessage(chatId, "Пожалуйста, введите дату начала последнего цикла в формате ГГГГ-ММ-ДД:");
            } else {
                sendTextMessage(chatId, "Пожалуйста, введите корректное число для длительности менструации.");
            }
        } else if (user.getMenstrualCycles().get(0).getStartDate() == null) {
            try {
                LocalDate startDate = LocalDate.parse(messageText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                MenstrualCycle cycle = user.getMenstrualCycles().get(0);
                cycle.setStartDate(startDate);
                userRepository.save(user);
                sendTextMessage(chatId, "Настройка завершена! Теперь вы можете начать отслеживание менструального цикла.");
                user.setSetupComplete(true);
                userRepository.save(user);
                sendWelcomeMessageWithMenu(chatId);
            } catch (DateTimeParseException e) {
                sendTextMessage(chatId, "Пожалуйста, введите дату в правильном формате ГГГГ-ММ-ДД.");
            }
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    private void sendWelcomeMessageWithMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Привет! Я бот для отслеживания менструального цикла. Используйте кнопки ниже для взаимодействия со мной:");
        message.setReplyMarkup(createMenuKeyboard());  // Используем метод createMenuKeyboard() для добавления кнопок

        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке приветственного сообщения с меню: {}", e.getMessage());
        }
    }

    private void sendCycleStatistics(Long chatId) {
        Optional<User> userOptional = userRepository.findById(chatId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<MenstrualCycle> cycles = cycleRepository.findByUser(user);
            if (!cycles.isEmpty()) {
                MenstrualCycle lastCycle = cycles.get(cycles.size() - 1);
                StringBuilder stats = new StringBuilder();
                stats.append("Ваши данные о цикле:\n");
                stats.append("Дата начала: ").append(lastCycle.getStartDate()).append("\n");
                stats.append("Длительность цикла: ").append(lastCycle.getCycleLength()).append(" дней\n");
                stats.append("Длительность менструации: ").append(lastCycle.getPeriodLength()).append(" дней\n");
                stats.append("Овуляция: ").append(lastCycle.getOvulationDate()).append(" (день ").append(lastCycle.getOvulationDay()).append(")\n");
                stats.append("Овуляторный период: с ").append(lastCycle.getOvulationStartDay()).append("-го по ").append(lastCycle.getOvulationEndDay()).append("-й день\n");
                stats.append("Фолликулярная фаза: с ").append(lastCycle.getFollicularPhaseStart()).append(" по ").append(lastCycle.getFollicularPhaseEnd()).append("\n");
                stats.append("Лютеиновая фаза: с ").append(lastCycle.getLutealPhaseStart()).append(" по ").append(lastCycle.getLutealPhaseEnd()).append("\n");

                sendTextMessage(chatId, stats.toString());
            } else {
                sendTextMessage(chatId, "У вас еще нет сохраненных данных о цикле. Нажмите 'Начать отслеживание', чтобы ввести данные.");
            }
        } else {
            sendTextMessage(chatId, "Пользователь не найден. Пожалуйста, начните с команды /start.");
        }
    }

    private void handleCycleInput(Long chatId, String messageText) {
        try {
            Optional<User> userOptional = userRepository.findById(chatId);
            if (userOptional.isEmpty()) {
                sendTextMessage(chatId, "Пользователь не найден. Пожалуйста, начните с команды /start.");
                return;
            }

            User user = userOptional.get();

            String[] inputs = messageText.split(" ");
            if (inputs.length != 2) {
                sendTextMessage(chatId, "Пожалуйста, введите данные в формате: ГГГГ-ММ-ДД <длительность менструации>");
                return;
            }

            LocalDate startDate = LocalDate.parse(inputs[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int periodLength = Integer.parseInt(inputs[1]);

            // Используем CycleService для расчета данных о текущем цикле
            CycleInfo cycleInfo = cycleService.calculateCurrentCycle(28, periodLength, startDate);
            MenstrualCycle cycle = new MenstrualCycle();
            cycle.setUser(user);
            cycle.setCycleLength(28);  // Длительность цикла фиксированная — 28 дней
            cycle.setPeriodLength(periodLength);
            cycle.setStartDate(startDate);
            cycle.setOvulationDay(cycleInfo.getOvulationDay());
            cycle.setOvulationStartDay(cycleInfo.getOvulationStartDay());
            cycle.setOvulationEndDay(cycleInfo.getOvulationEndDay());
            cycle.setOvulationDate(cycleInfo.getOvulationDate());
            cycle.setFollicularPhaseStart(cycleInfo.getFollicularPhaseStart());
            cycle.setFollicularPhaseEnd(cycleInfo.getFollicularPhaseEnd());
            cycle.setLutealPhaseStart(cycleInfo.getLutealPhaseStart());
            cycle.setLutealPhaseEnd(cycleInfo.getLutealPhaseEnd());

            cycleRepository.save(cycle);

            sendTextMessage(chatId, "Данные о цикле сохранены! Теперь вы можете запросить статистику.");
        } catch (DateTimeParseException e) {
            sendTextMessage(chatId, "Пожалуйста, введите дату в правильном формате ГГГГ-ММ-ДД.");
        } catch (NumberFormatException e) {
            sendTextMessage(chatId, "Пожалуйста, введите корректные числа для длительности менструации.");
        }
    }
}