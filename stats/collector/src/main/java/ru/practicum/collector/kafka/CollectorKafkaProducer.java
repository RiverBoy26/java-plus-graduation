package ru.practicum.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorKafkaProducer {
    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private final KafkaTemplate<Long, UserActionAvro> kafka;

    public void send(UserActionAvro userAction) {
        kafka.send(USER_ACTIONS_TOPIC, userAction.getUserId(), userAction);

        log.debug(
                "User action sent to Kafka: topic={}, userId={}, eventId={}",
                USER_ACTIONS_TOPIC,
                userAction.getUserId(),
                userAction.getEventId()
        );
    }
}