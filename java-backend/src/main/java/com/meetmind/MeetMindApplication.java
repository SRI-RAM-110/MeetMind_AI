package com.meetmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.meetmind.repository")
public class MeetMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetMindApplication.class, args);
    }
}
