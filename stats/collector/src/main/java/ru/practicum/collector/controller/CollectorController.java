package ru.practicum.collector.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.practicum.collector.kafka.CollectorKafkaProducer;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.stats.avro.UserActionAvro;
import stats.service.collector.UserActionProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorController implements CollectorControllerApi {

    private final CollectorKafkaProducer producer;

    @Override
    public void sendMessage(UserActionProto proto) {
        UserActionAvro avro = UserActionMapper.mapToUserActionAvro(proto);

        log.info("Получен proto request={}", avro);

        producer.send(avro);
    }
}
