package com.example.menstrualcyclebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MenstrualcyclebotApplication {

	public static void main(String[] args) {
		SpringApplication.run(MenstrualcyclebotApplication.class, args);
	}

}