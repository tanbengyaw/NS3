package my.gov.perkeso.assist.core.commands.service;

import my.gov.perkeso.assist.core.commands.domain.CommandWrapper;

public class CommandWrapperBuilder {

    private String entityName;
    private String actionName;
    private Long entityId;
    private String href;
    private String json;

    public CommandWrapperBuilder withEntityName(final String entityName) {
        this.entityName = entityName;
        return this;
    }

    public CommandWrapperBuilder withActionName(final String actionName) {
        this.actionName = actionName;
        return this;
    }

    public CommandWrapperBuilder withEntityId(final Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public CommandWrapperBuilder withHref(final String href) {
        this.href = href;
        return this;
    }

    public CommandWrapperBuilder withJson(final String json) {
        this.json = json;
        return this;
    }

    public CommandWrapper build() {
        if (entityId == null) {
            return CommandWrapper.fromJson(entityName, actionName, href, json);
        }
        return CommandWrapper.fromJson(entityName, actionName, entityId, href, json);
    }

    public static CommandWrapperBuilder createRegistrationCase() {
        return new CommandWrapperBuilder().withEntityName("REGISTRATION_CASE").withActionName("CREATE")
                .withHref("/registration-cases");
    }

    public static CommandWrapperBuilder updateRegistrationCase(final Long caseId) {
        return new CommandWrapperBuilder().withEntityName("REGISTRATION_CASE").withActionName("UPDATE").withEntityId(caseId)
                .withHref("/registration-cases/" + caseId);
    }

    public static CommandWrapperBuilder submitRegistrationCase(final Long caseId) {
        return new CommandWrapperBuilder().withEntityName("REGISTRATION_CASE").withActionName("SUBMIT").withEntityId(caseId)
                .withHref("/registration-cases/" + caseId + "?command=submit");
    }

    public static CommandWrapperBuilder approveRegistrationCase(final Long caseId) {
        return new CommandWrapperBuilder().withEntityName("REGISTRATION_CASE").withActionName("APPROVE").withEntityId(caseId)
                .withHref("/registration-cases/" + caseId + "?command=approve");
    }

    public static CommandWrapperBuilder rejectRegistrationCase(final Long caseId) {
        return new CommandWrapperBuilder().withEntityName("REGISTRATION_CASE").withActionName("REJECT").withEntityId(caseId)
                .withHref("/registration-cases/" + caseId + "?command=reject");
    }

    public static CommandWrapperBuilder queryRegistrationCase(final Long caseId) {
        return new CommandWrapperBuilder().withEntityName("REGISTRATION_CASE").withActionName("QUERY").withEntityId(caseId)
                .withHref("/registration-cases/" + caseId + "?command=query");
    }
}
