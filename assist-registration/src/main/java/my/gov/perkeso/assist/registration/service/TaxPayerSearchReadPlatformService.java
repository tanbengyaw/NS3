package my.gov.perkeso.assist.registration.service;

import java.util.List;
import java.util.Optional;
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
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxPayerSearchReadPlatformService {

    private final EmployerRepository employerRepository;
    private final DirectorOwnerRepository directorOwnerRepository;
    private final PremisesRepository premisesRepository;
    private final SstInfoRepository sstInfoRepository;
    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempSstInfoRepository tempSstInfoRepository;

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

        final TaxPayerRegistrationProfileData.TaxPayerRegistrationProfileDataBuilder builder =
                TaxPayerRegistrationProfileData.builder()
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
                        .premises(premises);

        enrichFromRegistrationCase(builder, employer.getId());
        return builder.build();
    }

    private void enrichFromRegistrationCase(
            final TaxPayerRegistrationProfileData.TaxPayerRegistrationProfileDataBuilder builder,
            final Long employerId) {
        findRegistrationTempEmployer(employerId).ifPresent(temp -> {
            builder.addressLine1(temp.getAddressLine1())
                    .addressLine2(temp.getAddressLine2())
                    .addressLine3(temp.getAddressLine3())
                    .stateId(temp.getStateId())
                    .cityId(temp.getCityId())
                    .cityName(temp.getCityName())
                    .postCode(temp.getPostCode())
                    .corrAddressLine1(temp.getCorrAddressLine1())
                    .corrAddressLine2(temp.getCorrAddressLine2())
                    .corrAddressLine3(temp.getCorrAddressLine3())
                    .corrPostCode(temp.getCorrPostCode())
                    .corrStateId(temp.getCorrStateId())
                    .corrCityId(temp.getCorrCityId())
                    .corrCityName(temp.getCorrCityName())
                    .contactPhones(temp.getContactPhones())
                    .contactFaxes(temp.getContactFaxes())
                    .methodContributionPaymentId(temp.getMethodContributionPaymentId());
            if (temp.getEmail() != null && !temp.getEmail().isBlank()) {
                builder.email(temp.getEmail());
            }
            if (temp.getPhone() != null && !temp.getPhone().isBlank()) {
                builder.phone(temp.getPhone());
            }
            if (temp.getServiceTypeId() != null) {
                builder.serviceTypeId(temp.getServiceTypeId());
            }
            if (temp.getPksBranchId() != null) {
                builder.pksBranchId(temp.getPksBranchId());
            }
            if (temp.getMsicId() != null) {
                builder.msicId(temp.getMsicId());
            }
            tempSstInfoRepository.findByTempEmployerId(temp.getId()).ifPresent(sst -> builder.tradeName(sst.getTradeName())
                    .tourTaxRegNo(sst.getTourTaxRegNo())
                    .inTaxRefNo(sst.getInTaxRefNo())
                    .cusAudRefNo(sst.getCusAudRefNo()));
        });
    }

    private Optional<TempEmployer> findRegistrationTempEmployer(final Long employerId) {
        final Optional<SstInfo> latestSst = sstInfoRepository.findFirstByEmployerIdAndDeletedFalseOrderByIdDesc(employerId);
        if (latestSst.isPresent() && latestSst.get().getRegGeneralInfoId() != null) {
            final Optional<TempEmployer> fromSst = regGeneralInfoRepository.findById(latestSst.get().getRegGeneralInfoId())
                    .map(RegGeneralInfo::getTempEmployer);
            if (fromSst.isPresent()) {
                return fromSst;
            }
        }
        return regGeneralInfoRepository.findFirstByEmployerIdOrderByUpdatedDateDesc(employerId)
                .map(RegGeneralInfo::getTempEmployer);
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
