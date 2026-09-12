package org.example.besmarthelpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeSmartHelpdeskApplication {

  public static void main(String[] args) {
    SpringApplication.run(BeSmartHelpdeskApplication.class, args);
  }

}
