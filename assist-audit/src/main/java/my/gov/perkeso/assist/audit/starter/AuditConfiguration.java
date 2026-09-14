package my.gov.perkeso.assist.audit.starter;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "my.gov.perkeso.assist.audit")
@EntityScan(basePackages = "my.gov.perkeso.assist.audit.domain")
@EnableJpaRepositories(basePackages = "my.gov.perkeso.assist.audit.domain")
public class AuditConfiguration {
}
