package com.example.threatfixture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ThreatFixtureApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThreatFixtureApplication.class, args);
    }
}

