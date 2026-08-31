package my.gov.perkeso.assist.registration.starter;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "my.gov.perkeso.assist.registration")
@EntityScan(basePackages = "my.gov.perkeso.assist.registration.domain")
@EnableJpaRepositories(basePackages = "my.gov.perkeso.assist.registration.domain")
public class RegistrationConfiguration {
}
