package ru.practicum.aggregator;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.service.SimilarityService;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationStarter {
    private final Producer<String, SpecificRecord> producer;
    private final Consumer<Long, UserActionAvro> consumer;
    private final SimilarityService service;

    @Value("${kafka.consumer.topic}")
    private String KAFKA_USER_ACTION_TOPIC;
    @Value("${kafka.producer.topic}")
    private String KAFKA_EVENTS_SIMILARITY_TOPIC;

    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {

            consumer.subscribe(List.of(KAFKA_USER_ACTION_TOPIC));


            while (true) {

                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                if (records.count() > 0) {
                    log.debug("Получено {} сообщений за один poll", records.count());
                }
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.debug("Event with: topic - {}, offset - {}, value - {}", record.topic(), record.offset(), record.value());
                    List<EventSimilarityAvro> recalculatedEventsSimilarities = service.process(record.value());
                    if (!recalculatedEventsSimilarities.isEmpty()) {
                        log.debug("Коэффициенты схожести пересчитаны");
                        for (EventSimilarityAvro similarity : recalculatedEventsSimilarities) {
                            ProducerRecord<String, SpecificRecord> producerRecord = new ProducerRecord<>(KAFKA_EVENTS_SIMILARITY_TOPIC,
                                    String.valueOf(similarity.getEventA()) + "_"+ String.valueOf(similarity.getEventB())
                                    ,similarity);
                            producer.send(producerRecord);
                            log.debug("Коэффициент схожести {} между событиями {} и {} отправлен в кафку",
                                    similarity.getScore(), similarity.getEventA(), similarity.getEventB());
                        }
                    }
                }
                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий после действий пользователей", e);
        } finally {

            try {
                producer.flush();
                log.debug("Produces flushes...");
                consumer.commitSync();
                log.debug("Consumer make commit Sync method");

            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }
}