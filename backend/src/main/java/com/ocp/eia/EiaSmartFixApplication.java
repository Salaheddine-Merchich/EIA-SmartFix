package com.ocp.eia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EiaSmartFixApplication {

    public static void main(String[] args) {
        SpringApplication.run(EiaSmartFixApplication.class, args);
    }
}
