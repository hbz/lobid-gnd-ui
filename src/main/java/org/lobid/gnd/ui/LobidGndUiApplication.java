package org.lobid.gnd.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LobidGndUiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(LobidGndUiApplication.class, args);
    }
}
