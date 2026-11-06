package ru.practicum.analyzer.similarity.kafka;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.io.IOException;

public class EventSimilarityDeserializer implements Deserializer<EventSimilarityAvro> {

    private final DatumReader<EventSimilarityAvro> reader =
            new SpecificDatumReader<>(EventSimilarityAvro.getClassSchema());

    private final DecoderFactory decoderFactory =
            DecoderFactory.get();

    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            Decoder decoder = decoderFactory.binaryDecoder(bytes, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Avro-message deserialization mistake", e
            );
        }
    }
}