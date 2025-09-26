package com.vocata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VocataApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocataApplication.class, args);
    }
}
