package com.watchparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WatchPartyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchPartyBackendApplication.class, args);
        System.out.println("YouTube Watch Party Backend Started!");
        System.out.println("Server: http://localhost:8080");
        System.out.println("H2 Console: http://localhost:8080/h2-console");
        System.out.println("WebSocket: ws://localhost:8080/ws");
    }
}