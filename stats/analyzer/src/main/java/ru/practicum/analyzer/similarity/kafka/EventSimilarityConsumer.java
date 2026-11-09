package ru.practicum.analyzer.similarity.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.similarity.service.SimilarityServiceImpl;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class EventSimilarityConsumer {
    private static final String EVENT_SIMILARITY_TOPIC = "stats.events-similarity.v1";
    private final KafkaConsumer<Long, SpecificRecordBase> kafkaConsumer;
    private final SimilarityServiceImpl similarityService;

    public EventSimilarityConsumer(
            @Qualifier("similarityKafkaConsumer")
            KafkaConsumer<Long, SpecificRecordBase> kafkaConsumer,
            SimilarityServiceImpl similarityService
    ) {
        this.kafkaConsumer = kafkaConsumer;
        this.similarityService = similarityService;
    }

    public void start() {
        try {
            kafkaConsumer.subscribe(List.of(EVENT_SIMILARITY_TOPIC));

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records =
                        kafkaConsumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    EventSimilarityAvro similarity = (EventSimilarityAvro) record.value();

                    log.info("Received similarity: eventA={}, eventB={}, similarity={}, timestamp={}",
                            similarity.getEventA(),
                            similarity.getEventB(),
                            similarity.getScore(),
                            similarity.getTimestamp());

                    similarityService.saveSimilarity(similarity);
                }
                kafkaConsumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Handling event error", e);
        } finally {
            try {
                kafkaConsumer.commitSync();
            } finally {
                kafkaConsumer.close();
                log.info("Consumer closed");
            }
        }
    }


}