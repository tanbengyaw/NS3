package my.gov.perkeso.assist.registration.data;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxPayerRegistrationProfileData {

    private final Long employerId;
    private final String employerCode;
    private final String employerName;
    private final String registrationNo;
    private final Long businessEntityTypeId;
    private final Long msicId;
    private final Long serviceTypeId;
    private final Long pksBranchId;
    private final String email;
    private final String phone;
    private final List<TaxPayerDirectorData> directors;
    private final List<TaxPayerPremisesData> premises;
}
