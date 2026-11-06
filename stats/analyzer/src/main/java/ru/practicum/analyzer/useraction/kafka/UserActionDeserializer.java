package ru.practicum.analyzer.useraction.kafka;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.stats.avro.UserActionAvro;

import java.io.IOException;

public class UserActionDeserializer implements Deserializer<UserActionAvro> {

    private final DatumReader<UserActionAvro> reader =
            new SpecificDatumReader<>(UserActionAvro.getClassSchema());

    private final DecoderFactory decoderFactory =
            DecoderFactory.get();

    @Override
    public UserActionAvro deserialize(String topic, byte[] bytes) {
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