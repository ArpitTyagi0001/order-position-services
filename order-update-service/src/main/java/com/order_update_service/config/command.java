package com.order_update_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class command {
    @Bean
    CommandLineRunner run(CsvProcessor csvProcessor) {
        return args -> csvProcessor.process();
    }
}
