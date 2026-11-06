package ru.practicum.aggregator.kafka;


import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.stats.avro.UserActionAvro;

import java.util.Properties;

@Configuration
public class KafkaClientConfig {
    @Value("${kafka.bootstrap-servers}")
    private String KAFKA_BOOTSTRAP_SERVERS;
    @Value("${kafka.consumer.group-id}")
    private String KAFKA_CONSUMER_GROUP_ID;
    @Value("${kafka.consumer.auto-offset-reset}")
    private String KAFKA_AUTO_OFFSET_RESET_CONFIG;

    @Bean
    public Producer<String, SpecificRecord> producer() {
        return new KafkaProducer<>(getProducerConfig());
    }

    @Bean
    public Consumer<Long, UserActionAvro> consumer() {
        return new KafkaConsumer<>(getConsumerConfig());
    }

    private Properties getProducerConfig() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroCommonSerializer.class);

        return properties;
    }

    private Properties getConsumerConfig() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_CONSUMER_GROUP_ID);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KAFKA_AUTO_OFFSET_RESET_CONFIG);

        return properties;
    }
}