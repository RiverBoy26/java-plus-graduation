package ru.practicum.requestservice.feign;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Every Feign operation in this module only reads data (GET or batch read POST). */
@Configuration
public class FeignRetryConfiguration {
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(150L, 300L, 2);
    }
}
