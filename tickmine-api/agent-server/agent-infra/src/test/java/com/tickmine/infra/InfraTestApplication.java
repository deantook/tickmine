package com.tickmine.infra;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.tickmine.infra")
@EntityScan("com.tickmine.infra.persistence.entity")
@EnableJpaRepositories("com.tickmine.infra.persistence.repository")
public class InfraTestApplication {
}
