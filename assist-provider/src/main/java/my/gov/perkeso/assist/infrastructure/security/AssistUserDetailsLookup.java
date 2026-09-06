package my.gov.perkeso.assist.infrastructure.security;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssistUserDetailsLookup {

    private final DbUserDetailsService userDetailsService;

    public Optional<AssistUserDetails> findByUsername(final String username) {
        try {
            return Optional.of((AssistUserDetails) userDetailsService.loadUserByUsername(username));
        } catch (UsernameNotFoundException ex) {
            return Optional.empty();
        }
    }
}
