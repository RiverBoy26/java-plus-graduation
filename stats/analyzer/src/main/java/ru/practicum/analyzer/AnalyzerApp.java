package ru.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.analyzer.similarity.kafka.EventSimilarityConsumer;
import ru.practicum.analyzer.useraction.kafka.UserActionConsumer;

@SpringBootApplication
public class AnalyzerApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(AnalyzerApp.class, args);

        UserActionConsumer userActionConsumer =
                context.getBean(UserActionConsumer.class);
        EventSimilarityConsumer eventSimilarityConsumer =
                context.getBean(EventSimilarityConsumer.class);

        new Thread(userActionConsumer::start).start();
        new Thread(eventSimilarityConsumer::start).start();
    }
}