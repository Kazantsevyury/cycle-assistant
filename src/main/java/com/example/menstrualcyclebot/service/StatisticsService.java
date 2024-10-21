package com.example.menstrualcyclebot.service;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class StatisticsService {

    public String createLineChart(long chatId) throws IOException {
        // Создаем набор данных
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(28, "Цикл", "Январь");
        dataset.addValue(30, "Цикл", "Февраль");
        dataset.addValue(26, "Цикл", "Март");
        dataset.addValue(27, "Цикл", "Апрель");

        // Создаем график
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Длительность цикла",
                "Месяц",
                "Дней",
                dataset
        );

        // Указываем путь для сохранения графика
        String filePath = "charts/chart_" + chatId + ".png";

        // Сохраняем график в файл
        ChartUtils.saveChartAsPNG(new File(filePath), lineChart, 800, 600);

        return filePath; // Возвращаем путь к файлу
    }
}

