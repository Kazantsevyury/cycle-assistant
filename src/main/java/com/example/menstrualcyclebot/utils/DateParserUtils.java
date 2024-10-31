package com.example.menstrualcyclebot.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateParserUtils {

    // Метод для парсинга даты с учётом нескольких форматов и дополнением текущего года/месяца
    public static LocalDate parseDateWithMultipleFormats(String dateString) {
        LocalDate currentDate = LocalDate.now();

        // Убираем пробелы и заменяем их на точки для единообразия
        dateString = dateString.replace(" ", ".");

        // Если введён только день, добавляем текущий месяц и год
        if (dateString.matches("\\d{1,2}")) {
            dateString = String.format("%02d.%02d.%d", Integer.parseInt(dateString), currentDate.getMonthValue(), currentDate.getYear());
        }

        // Если введён день и месяц, добавляем текущий год
        if (dateString.matches("\\d{1,2}[.-]\\d{1,2}")) {
            dateString = String.format("%02d.%02d.%d",
                    Integer.parseInt(dateString.split("[.-]")[0]),
                    Integer.parseInt(dateString.split("[.-]")[1]),
                    currentDate.getYear());
        }

        // Список поддерживаемых форматов
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("d.MM.yyyy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("dd.M.yyyy"),
                DateTimeFormatter.ofPattern("dd,MM,yyyy"),
                DateTimeFormatter.ofPattern("d,MM,yyyy"),
                DateTimeFormatter.ofPattern("d,M,yyyy"),
                DateTimeFormatter.ofPattern("dd,M,yyyy"),
                DateTimeFormatter.ofPattern("yyyy.MM.d"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                DateTimeFormatter.ofPattern("yyyy.M.d"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy-MM-d"),
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy")
        );

        // Пробуем каждый формат
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateString, formatter); // Возвращаем дату, если формат подошел
            } catch (DateTimeParseException ignored) {
                // Игнорируем ошибку и пробуем следующий формат
            }
        }

        // Если ни один формат не подошёл, возвращаем null
        return null;
    }
}
