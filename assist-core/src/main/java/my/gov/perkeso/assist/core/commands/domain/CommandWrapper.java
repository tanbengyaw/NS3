package my.gov.perkeso.assist.core.commands.domain;

import lombok.Getter;

@Getter
public final class CommandWrapper {

    private final String entityName;
    private final String actionName;
    private final Long entityId;
    private final String href;
    private final String json;

    private CommandWrapper(final String entityName, final String actionName, final Long entityId, final String href,
            final String json) {
        this.entityName = entityName;
        this.actionName = actionName;
        this.entityId = entityId;
        this.href = href;
        this.json = json;
    }

    public static CommandWrapper fromJson(final String entityName, final String actionName, final String href,
            final String json) {
        return new CommandWrapper(entityName, actionName, null, href, json);
    }

    public static CommandWrapper fromJson(final String entityName, final String actionName, final Long entityId,
            final String href, final String json) {
        return new CommandWrapper(entityName, actionName, entityId, href, json);
    }
}
