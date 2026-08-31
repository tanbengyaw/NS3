package my.gov.perkeso.assist.core.infrastructure.data;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommandProcessingResult {

    private final Long resourceId;
    private final String resourceIdentifier;
    private final Long officeId;
    private final Map<String, Object> changes;

    public static CommandProcessingResult resourceResult(final Long resourceId) {
        return CommandProcessingResult.builder().resourceId(resourceId).build();
    }

    public static CommandProcessingResult resourceResult(final Long resourceId, final String resourceIdentifier) {
        return CommandProcessingResult.builder().resourceId(resourceId).resourceIdentifier(resourceIdentifier).build();
    }

    public static CommandProcessingResult withChanges(final Long resourceId, final String resourceIdentifier,
            final Map<String, Object> changes) {
        return CommandProcessingResult.builder().resourceId(resourceId).resourceIdentifier(resourceIdentifier)
                .changes(changes).build();
    }
}
