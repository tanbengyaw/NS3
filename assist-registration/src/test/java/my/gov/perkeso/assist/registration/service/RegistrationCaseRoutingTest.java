package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistrationCaseRoutingTest {

    @Test
    void submittedDiscontinueTaxRoutesToUoOfProcessingBranch() {
        final var target = RegistrationCaseRouting.resolve("SUBMITTED", 1103L, "Pejabat PERKESO Shah Alam",
                "admin", "admin");
        assertThat(target).isNotNull();
        assertThat(target.role()).isEqualTo("UO");
        assertThat(target.label()).isEqualTo("UO — Pejabat PERKESO Shah Alam");
    }

    @Test
    void submittedSalesTaxRoutesToOfficerOfProcessingBranch() {
        final var target = RegistrationCaseRouting.resolve("SUBMITTED", 1100L, "Pejabat PERKESO Shah Alam",
                "employer", "employer");
        assertThat(target).isNotNull();
        assertThat(target.role()).isEqualTo("OFFICER");
        assertThat(target.label()).isEqualTo("Officer — Pejabat PERKESO Shah Alam");
    }

    @Test
    void inQueryStaffCaseReturnsToSubmittingOfficer() {
        final var target = RegistrationCaseRouting.resolve("IN_QUERY", 1103L, "Pejabat PERKESO Shah Alam",
                "admin", "ro");
        assertThat(target).isNotNull();
        assertThat(target.role()).isEqualTo("SUBMITTER");
        assertThat(target.username()).isEqualTo("admin");
        assertThat(target.label()).isEqualTo("Submitting officer (admin)");
    }

    @Test
    void inQueryNewRegReturnsToApplicant() {
        final var target = RegistrationCaseRouting.resolve("IN_QUERY", 1100L, "Pejabat PERKESO Shah Alam",
                "employer", "employer");
        assertThat(target).isNotNull();
        assertThat(target.role()).isEqualTo("APPLICANT");
        assertThat(target.label()).isEqualTo("Applicant (employer)");
    }

    @Test
    void approvedHasNoCurrentOwner() {
        assertThat(RegistrationCaseRouting.resolve("APPROVED", 1103L, "Pejabat PERKESO Shah Alam", "admin", "admin"))
                .isNull();
    }
}
