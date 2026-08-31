package my.gov.perkeso.assist.infrastructure.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/api/openapi.json", "/api/openapi.yaml")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(final AssistUserDetailsLookup assistUserDetailsLookup) {
        return new InMemoryUserDetailsManager(assistUserDetailsLookup.all().toArray(new AssistUserDetails[0]));
    }

    @Bean
    public AssistUserDetailsLookup assistUserDetailsLookup(final PasswordEncoder passwordEncoder) {
        return new AssistUserDetailsLookup(devUsers(passwordEncoder));
    }

    private static List<AssistUserDetails> devUsers(final PasswordEncoder passwordEncoder) {
        final String encodedPassword = passwordEncoder.encode("password");
        return List.of(
                new AssistUserDetails("admin", encodedPassword, List.of("ADMIN", "OFFICER"), 2L,
                        "officer@perkeso.example"),
                new AssistUserDetails("ro", encodedPassword, List.of("RO"), 2L, "ro@perkeso.example"),
                new AssistUserDetails("employer", encodedPassword, List.of("EMPLOYER"), null, "hr@acme.example"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
