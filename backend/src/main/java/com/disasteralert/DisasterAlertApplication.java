package com.disasteralert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DisasterAlertApplication {
    public static void main(String[] args) {
        SpringApplication.run(DisasterAlertApplication.class, args);
    }
}
