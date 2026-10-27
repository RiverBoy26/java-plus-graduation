package ru.practicum.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EntityScan(basePackages = {"ru.practicum.eventservice.model", "ru.practicum.eventservice.compilation.entity", "ru.practicum.common.entity"})
@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"ru.practicum.eventservice", "ru.practicum.common.exception", "ru.practicum.common.config", "ru.practicum.statistics.client"})
public class EventServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApp.class, args);
    }
}
