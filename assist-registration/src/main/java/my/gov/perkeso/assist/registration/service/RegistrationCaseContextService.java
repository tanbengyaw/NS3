package my.gov.perkeso.assist.registration.service;

import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.PksBranchConstants;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import org.springframework.stereotype.Service;

/**
 * Mirrors ASSIST {@code AbstractRegCounter} draft branch and data-source assignment.
 */
@Service
@RequiredArgsConstructor
public class RegistrationCaseContextService {

    public RegistrationCaseContext resolveForCreate(final JsonCommand command, final PlatformUser user) {
        final RegistrationSection section = resolveSection(command);
        if (user.isEmployer()) {
            return RegistrationCaseContext.builder().section(section).dataSource(DataSource.PORTAL)
                    .pksBranchId(PksBranchConstants.HQ).processingPksBranchId(PksBranchConstants.HQ)
                    .receivingPksBranchId(PksBranchConstants.HQ).build();
        }
        final DataSource dataSource = resolveDataSource(command);
        final Long branchId = user.officeId();
        if (branchId == null) {
            throw new IllegalArgumentException("officeId is required for officer users");
        }
        return RegistrationCaseContext.builder().section(section).dataSource(dataSource).pksBranchId(branchId)
                .processingPksBranchId(branchId).receivingPksBranchId(branchId).build();
    }

    private static RegistrationSection resolveSection(final JsonCommand command) {
        final Long sectionId = command.longValueOfParameterNamed("sectionId");
        if (sectionId != null) {
            return RegistrationSection.fromAssistSectionId(sectionId);
        }
        return RegistrationSection.REG_NEW_REG;
    }

    private static DataSource resolveDataSource(final JsonCommand command) {
        final Long dataSourceId = command.longValueOfParameterNamed("dataSourceId");
        if (dataSourceId != null) {
            return DataSource.fromAssistId(dataSourceId);
        }
        return DataSource.OTC;
    }
}
