package com.peertopeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
public class PeerToPeerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeerToPeerApplication.class, args);
    }

}
