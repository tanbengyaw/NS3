package my.gov.perkeso.assist.infrastructure.security;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AssistUserDetailsLookup {

    private final Map<String, AssistUserDetails> usersByUsername;

    public AssistUserDetailsLookup(final Iterable<AssistUserDetails> users) {
        this.usersByUsername = java.util.stream.StreamSupport.stream(users.spliterator(), false)
                .collect(Collectors.toUnmodifiableMap(AssistUserDetails::getUsername, Function.identity()));
    }

    public Optional<AssistUserDetails> findByUsername(final String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    public java.util.Collection<AssistUserDetails> all() {
        return usersByUsername.values();
    }
}
