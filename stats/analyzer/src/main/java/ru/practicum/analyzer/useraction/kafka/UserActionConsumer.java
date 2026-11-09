package ru.practicum.analyzer.useraction.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.useraction.service.UserEventService;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class UserActionConsumer {
    private static final String USER_ACTION_TOPIC = "stats.user-actions.v1";

    private final KafkaConsumer<Long, SpecificRecordBase> kafkaConsumer;
    private final UserEventService service;

    public UserActionConsumer(
            @Qualifier("userActionKafkaConsumer")
            KafkaConsumer<Long, SpecificRecordBase> kafkaConsumer,
            UserEventService service
    ) {
        this.kafkaConsumer = kafkaConsumer;
        this.service = service;
    }

    public void start() {
        try {
            kafkaConsumer.subscribe(List.of(USER_ACTION_TOPIC));

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records =
                        kafkaConsumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    UserActionAvro action = (UserActionAvro) record.value();

                    log.info("Received similarity: userId={}, eventId={}, actionType={}, timestamp={}",
                            action.getUserId(),
                            action.getEventId(),
                            action.getActionType(),
                            action.getTimestamp());

                    service.saveUserAction(action);
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