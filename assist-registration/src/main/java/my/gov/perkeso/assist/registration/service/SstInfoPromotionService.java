package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.constant.SstStatus;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.DirectorOwner;
import my.gov.perkeso.assist.registration.domain.DirectorOwnerRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.Premises;
import my.gov.perkeso.assist.registration.domain.PremisesRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstStatusInfo;
import my.gov.perkeso.assist.registration.domain.SstStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstTariffCode;
import my.gov.perkeso.assist.registration.domain.SstTariffCodeRepository;
import my.gov.perkeso.assist.registration.domain.TempDirectorOwner;
import my.gov.perkeso.assist.registration.domain.TempPremises;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCode;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeContext;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SstInfoPromotionService {

    private final TempSstInfoWritePlatformService tempSstInfoWritePlatformService;
    private final SstInfoRepository sstInfoRepository;
    private final SstStatusInfoRepository sstStatusInfoRepository;
    private final SstTariffCodeRepository sstTariffCodeRepository;
    private final DirectorOwnerRepository directorOwnerRepository;
    private final PremisesRepository premisesRepository;
    private final EmployerCodeGeneratorFactory employerCodeGeneratorFactory;

    @Transactional
    public SstInfo promoteSalesTaxOnApprove(final RegGeneralInfo regCase, final Employer employer) {
        if (regCase.getSectionId() != RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId()) {
            return null;
        }

        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        final List<TempDirectorOwner> directors = tempSstInfoWritePlatformService
                .listDirectorsForCase(regCase.getId());
        final List<TempPremises> premises = tempSstInfoWritePlatformService.listPremisesForCase(regCase.getId());
        final List<TempSstTariffCode> tariffCodes = tempSstInfoWritePlatformService
                .listTariffCodesForCase(tempSstInfo);

        final EmployerCodeContext context = EmployerCodeContext.builder()
                .branchId(regCase.getTempEmployer().getPksBranchId())
                .postCode(regCase.getTempEmployer().getPostCode())
                .build();
        final String smkNo = employerCodeGeneratorFactory.generateSmkNo(RegistrationSection.REG_NEW_REG_SST_SALES_TAX,
                context);

        final SstInfo sstInfo = mapSstInfo(tempSstInfo, employer.getId(), regCase.getId(), smkNo);
        final SstInfo savedSstInfo = sstInfoRepository.save(sstInfo);

        promoteDirectors(employer.getId(), directors);
        promotePremises(employer.getId(), premises);
        promoteTariffCodes(employer.getId(), savedSstInfo.getId(), tariffCodes);
        createActiveStatus(savedSstInfo.getId());

        return savedSstInfo;
    }

    private static SstInfo mapSstInfo(final TempSstInfo temp, final Long employerId, final Long caseId,
            final String smkNo) {
        final SstInfo sstInfo = new SstInfo();
        sstInfo.setEmployerId(employerId);
        sstInfo.setRegGeneralInfoId(caseId);
        sstInfo.setSmkRegNo(smkNo);
        sstInfo.setSalesTaxSmkRegNo(smkNo);
        sstInfo.setTradeName(temp.getTradeName());
        sstInfo.setTourTaxRegNo(temp.getTourTaxRegNo());
        sstInfo.setInTaxRefNo(temp.getInTaxRefNo());
        sstInfo.setCusAudRefNo(temp.getCusAudRefNo());
        sstInfo.setPreRegNo(temp.getPreRegNo());
        sstInfo.setPreRegName(temp.getPreRegName());
        sstInfo.setDateOfReplacement(temp.getDateOfReplacement());
        sstInfo.setManComDate(temp.getManComDate());
        sstInfo.setDateSaleValTaxGoods(temp.getDateSaleValTaxGoods());
        sstInfo.setFinYrEndMon(temp.getFinYrEndMon());
        sstInfo.setAnTotalTaxSalesVal(temp.getAnTotalTaxSalesVal());
        sstInfo.setBusinessComDate(temp.getBusinessComDate());
        sstInfo.setLocalSales(temp.getLocalSales());
        sstInfo.setExportSales(temp.getExportSales());
        sstInfo.setSalesToDesignArea(temp.getSalesToDesignArea());
        sstInfo.setOthersSales(temp.getOthersSales());
        sstInfo.setSubContractWork(temp.isSubContractWork());
        sstInfo.setAutoRegistration(false);
        sstInfo.setDeleted(false);
        sstInfo.setCreatedDate(LocalDateTime.now());
        return sstInfo;
    }

    private void promoteDirectors(final Long employerId, final List<TempDirectorOwner> directors) {
        for (final TempDirectorOwner temp : directors) {
            final DirectorOwner director = new DirectorOwner();
            director.setEmployerId(employerId);
            director.setName(temp.getName());
            director.setIdentificationTypeId(temp.getIdentificationTypeId());
            director.setIdentificationNo(temp.getIdentificationNo());
            director.setEmail(temp.getEmail());
            director.setDesignation(temp.getDesignation());
            director.setDeleted(false);
            director.setCreatedDate(LocalDateTime.now());
            directorOwnerRepository.save(director);
        }
    }

    private void promotePremises(final Long employerId, final List<TempPremises> premises) {
        for (final TempPremises temp : premises) {
            final Premises row = new Premises();
            row.setEmployerId(employerId);
            row.setName(temp.getName());
            row.setAddressLine(temp.getAddressLine());
            row.setAddressLine2(temp.getAddressLine2());
            row.setAddressLine3(temp.getAddressLine3());
            row.setPostCode(temp.getPostCode());
            row.setCityName(temp.getCityName());
            row.setStateName(temp.getStateName());
            row.setDeleted(false);
            row.setCreatedDate(LocalDateTime.now());
            premisesRepository.save(row);
        }
    }

    private void promoteTariffCodes(final Long employerId, final Long sstInfoId,
            final List<TempSstTariffCode> tariffCodes) {
        for (final TempSstTariffCode temp : tariffCodes) {
            final SstTariffCode tariff = new SstTariffCode();
            tariff.setSstInfoId(sstInfoId);
            tariff.setEmployerId(employerId);
            tariff.setTariffCodeSalesTypeId(temp.getTariffCodeSalesTypeId());
            tariff.setContractTypeId(temp.getContractTypeId());
            tariff.setFinishedGoods(temp.getFinishedGoods());
            tariff.setDeleted(false);
            tariff.setCreatedDate(LocalDateTime.now());
            sstTariffCodeRepository.save(tariff);
        }
    }

    private void createActiveStatus(final Long sstInfoId) {
        final SstStatusInfo statusInfo = new SstStatusInfo();
        statusInfo.setSstInfoId(sstInfoId);
        statusInfo.setTaxTypeId(TaxType.SALES_TAX.getAssistId());
        statusInfo.setSstStatusId(SstStatus.ACTIVE.getAssistId());
        statusInfo.setStartDate(LocalDate.now());
        statusInfo.setCurrent(true);
        statusInfo.setDeleted(false);
        statusInfo.setCreatedDate(LocalDateTime.now());
        sstStatusInfoRepository.save(statusInfo);
    }

    static boolean hasMainContractTariff(final List<TempSstTariffCode> tariffCodes) {
        return tariffCodes.stream()
                .anyMatch(t -> t.getContractTypeId() == SstContractType.MAIN_CONTRACT.getAssistId());
    }

    static boolean hasSubContractTariff(final List<TempSstTariffCode> tariffCodes) {
        return tariffCodes.stream()
                .anyMatch(t -> t.getContractTypeId() == SstContractType.SUB_CONTRACT.getAssistId());
    }
}
