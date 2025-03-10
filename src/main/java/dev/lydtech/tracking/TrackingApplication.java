package dev.lydtech.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class TrackingApplication {

    public static void main(String[] args) {
        log.info("Starting Spring 6 TrackingApplication...");
        SpringApplication.run(TrackingApplication.class, args);
    }

}
