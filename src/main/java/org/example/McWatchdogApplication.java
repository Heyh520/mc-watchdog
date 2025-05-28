package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class McWatchdogApplication {
    public static void main(String[] args) {
        SpringApplication.run(McWatchdogApplication.class, args);
    }
}
