package com.energy.audit.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Energy Audit Platform Application
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.energy.audit")
public class EnergyAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyAuditApplication.class, args);
    }
}
