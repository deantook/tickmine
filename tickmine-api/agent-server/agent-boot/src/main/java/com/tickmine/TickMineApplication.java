package com.tickmine;

import com.tickmine.infra.config.TickMineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.tickmine")
@EnableConfigurationProperties(TickMineProperties.class)
public class TickMineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TickMineApplication.class, args);
    }
}
