package top.lixetron.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.ZonedDateTime;
import java.util.Optional;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableJpaRepositories(basePackages = "top.lixetron.server.repository")
@ComponentScan({"top.lixetron.server.service", "top.lixetron.server.listeners"})
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class ServerServiceConfig {

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(ZonedDateTime.now());
    }

}
