package com.akash.pooler_backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Akash Kumar
 */
@SpringBootApplication
@Slf4j
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class PoolerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PoolerBackendApplication.class, args);
	}

}
