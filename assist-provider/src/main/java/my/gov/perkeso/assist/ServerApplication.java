package my.gov.perkeso.assist;

import my.gov.perkeso.assist.identity.starter.IdentityConfiguration;
import my.gov.perkeso.assist.registration.starter.RegistrationConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "my.gov.perkeso.assist")
@Import({ RegistrationConfiguration.class, IdentityConfiguration.class })
@EnableTransactionManagement
public class ServerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
