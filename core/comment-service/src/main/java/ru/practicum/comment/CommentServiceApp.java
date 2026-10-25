package ru.practicum.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "ru.practicum.comment",
        "ru.practicum.common.exception",
        "ru.practicum.common.config"
})
@EnableFeignClients(basePackages = "ru.practicum.comment.feign")
public class CommentServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApp.class, args);
    }
}
