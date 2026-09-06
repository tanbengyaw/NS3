package my.gov.perkeso.assist.identity.starter;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "my.gov.perkeso.assist.identity")
@EntityScan(basePackages = "my.gov.perkeso.assist.identity.domain")
@EnableJpaRepositories(basePackages = "my.gov.perkeso.assist.identity.domain")
public class IdentityConfiguration {
}
