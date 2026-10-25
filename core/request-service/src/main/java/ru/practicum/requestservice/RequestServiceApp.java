package ru.practicum.requestservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"ru.practicum.requestservice", "ru.practicum.common.exception", "ru.practicum.common.config"})
public class RequestServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApp.class, args);
    }
}