package my.gov.perkeso.assist.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.registration.data.TaxPayerDirectorData;
import my.gov.perkeso.assist.registration.data.TaxPayerPremisesData;
import my.gov.perkeso.assist.registration.data.TaxPayerRegistrationProfileData;
import my.gov.perkeso.assist.registration.domain.DirectorOwner;
import my.gov.perkeso.assist.registration.domain.DirectorOwnerRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.Premises;
import my.gov.perkeso.assist.registration.domain.PremisesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxPayerSearchReadPlatformService {

    private final EmployerRepository employerRepository;
    private final DirectorOwnerRepository directorOwnerRepository;
    private final PremisesRepository premisesRepository;

    public TaxPayerRegistrationProfileData retrieveForRegistration(final String searchType, final String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            throw new IllegalArgumentException("searchValue is required");
        }
        final String normalized = searchValue.trim().toUpperCase();
        final Employer employer = findEmployer(searchType, normalized);
        return toProfile(employer);
    }

    private Employer findEmployer(final String searchType, final String normalizedValue) {
        final String type = searchType != null ? searchType.trim().toUpperCase() : "SST_REGISTRATION_NO";
        return switch (type) {
            case "BRN" -> employerRepository.findByBusinessInfo_RegistrationNoIgnoreCaseAndDeletedFalse(normalizedValue)
                    .orElseThrow(() -> new ResourceNotFoundException("Tax payer not found for BRN: " + normalizedValue));
            case "SST_REGISTRATION_NO", "CODE" -> employerRepository
                    .findByEmployerCodeIgnoreCaseAndDeletedFalse(normalizedValue)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tax payer not found for SST registration no: " + normalizedValue));
            default -> throw new IllegalArgumentException("Unsupported searchType: " + searchType);
        };
    }

    private TaxPayerRegistrationProfileData toProfile(final Employer employer) {
        final List<TaxPayerDirectorData> directors = directorOwnerRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employer.getId()).stream()
                .map(this::mapDirector)
                .toList();
        final List<TaxPayerPremisesData> premises = premisesRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employer.getId()).stream()
                .map(this::mapPremises)
                .toList();

        return TaxPayerRegistrationProfileData.builder()
                .employerId(employer.getId())
                .employerCode(employer.getEmployerCode())
                .employerName(employer.getEmployerName())
                .registrationNo(employer.getBusinessInfo().getRegistrationNo())
                .businessEntityTypeId(employer.getBusinessInfo().getBusinessEntityTypeId())
                .msicId(employer.getMsicId())
                .serviceTypeId(employer.getServiceTypeId())
                .pksBranchId(employer.getPksBranchId())
                .email(employer.getEmail())
                .phone(employer.getPhone())
                .directors(directors)
                .premises(premises)
                .build();
    }

    private TaxPayerDirectorData mapDirector(final DirectorOwner director) {
        return TaxPayerDirectorData.builder()
                .name(director.getName())
                .identificationTypeId(director.getIdentificationTypeId())
                .identificationNo(director.getIdentificationNo())
                .email(director.getEmail())
                .designation(director.getDesignation())
                .build();
    }

    private TaxPayerPremisesData mapPremises(final Premises premises) {
        return TaxPayerPremisesData.builder()
                .name(premises.getName())
                .addressLine(premises.getAddressLine())
                .addressLine2(premises.getAddressLine2())
                .addressLine3(premises.getAddressLine3())
                .postCode(premises.getPostCode())
                .cityName(premises.getCityName())
                .stateName(premises.getStateName())
                .build();
    }
}
