package com.homehealthcare.config;

import com.homehealthcare.shared.persistence.TenantAwareJpaRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = "com.homehealthcare",
        repositoryBaseClass = TenantAwareJpaRepository.class)
public class PersistenceConfig {
}
