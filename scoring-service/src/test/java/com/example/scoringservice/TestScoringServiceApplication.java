package com.example.scoringservice;

import org.springframework.boot.SpringApplication;

public class TestScoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(ScoringServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
