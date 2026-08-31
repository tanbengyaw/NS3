package my.gov.perkeso.assist.registration.service;

import lombok.Builder;
import lombok.Getter;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;

@Getter
@Builder
public class RegistrationCaseContext {

    private final RegistrationSection section;
    private final DataSource dataSource;
    private final Long pksBranchId;
    private final Long processingPksBranchId;
    private final Long receivingPksBranchId;
}
