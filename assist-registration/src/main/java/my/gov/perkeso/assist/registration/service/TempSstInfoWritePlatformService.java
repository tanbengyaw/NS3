package my.gov.perkeso.assist.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.data.TariffCodeSalesTypeData;
import my.gov.perkeso.assist.registration.data.TempDirectorOwnerData;
import my.gov.perkeso.assist.registration.data.TempPremisesData;
import my.gov.perkeso.assist.registration.data.TempSstInfoData;
import my.gov.perkeso.assist.registration.data.TempSstTariffCodeData;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempDirectorOwner;
import my.gov.perkeso.assist.registration.domain.TempDirectorOwnerRepository;
import my.gov.perkeso.assist.registration.domain.TempPremises;
import my.gov.perkeso.assist.registration.domain.TempPremisesRepository;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCode;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCodeRepository;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseInvalidStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempSstInfoWritePlatformService {

    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempSstInfoRepository tempSstInfoRepository;
    private final TempDirectorOwnerRepository tempDirectorOwnerRepository;
    private final TempPremisesRepository tempPremisesRepository;
    private final TempSstTariffCodeRepository tempSstTariffCodeRepository;
    private final RegistrationReferenceReadPlatformService registrationReferenceReadPlatformService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TempSstInfoData getSstInfo(final Long caseId) {
        final RegGeneralInfo regCase = loadCase(caseId);
        assertSstSalesSection(regCase);
        final TempSstInfo tempSstInfo = findOrEmpty(regCase.getTempEmployer().getId());
        return toData(caseId, tempSstInfo, listDirectors(caseId), listPremises(caseId), listTariffCodes(tempSstInfo));
    }

    @Transactional
    public TempSstInfoData upsertSstInfo(final Long caseId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final JsonNode node = parseJson(json);

        TempSstInfo tempSstInfo = tempSstInfoRepository.findByTempEmployerId(regCase.getTempEmployer().getId())
                .orElseGet(() -> {
                    final TempSstInfo created = new TempSstInfo();
                    created.setTempEmployerId(regCase.getTempEmployer().getId());
                    created.setCreatedDate(LocalDateTime.now());
                    return created;
                });

        applySstFields(tempSstInfo, node);
        tempSstInfo.setUpdatedDate(LocalDateTime.now());
        tempSstInfo = tempSstInfoRepository.save(tempSstInfo);

        return toData(caseId, tempSstInfo, listDirectors(caseId), listPremises(caseId), listTariffCodes(tempSstInfo));
    }

    @Transactional
    public TempPremisesData createPremises(final Long caseId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final JsonNode node = parseJson(json);

        final TempPremises premises = new TempPremises();
        premises.setRegGeneralInfoId(caseId);
        premises.setName(requireText(node, "name"));
        premises.setAddressLine(requireText(node, "addressLine"));
        premises.setAddressLine2(text(node, "addressLine2"));
        premises.setAddressLine3(text(node, "addressLine3"));
        premises.setPostCode(text(node, "postCode"));
        premises.setCityName(text(node, "cityName"));
        premises.setStateName(text(node, "stateName"));
        premises.setDeleted(false);
        premises.setCreatedDate(LocalDateTime.now());

        return toPremisesData(caseId, tempPremisesRepository.save(premises));
    }

    @Transactional
    public TempPremisesData updatePremises(final Long caseId, final Long premisesId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempPremises premises = loadPremises(premisesId);
        if (!premises.getRegGeneralInfoId().equals(caseId)) {
            throw new IllegalArgumentException("Premises does not belong to this registration case");
        }
        final JsonNode node = parseJson(json);
        premises.setName(requireText(node, "name"));
        premises.setAddressLine(requireText(node, "addressLine"));
        premises.setAddressLine2(text(node, "addressLine2"));
        premises.setAddressLine3(text(node, "addressLine3"));
        premises.setPostCode(text(node, "postCode"));
        premises.setCityName(text(node, "cityName"));
        premises.setStateName(text(node, "stateName"));
        return toPremisesData(caseId, tempPremisesRepository.save(premises));
    }

    @Transactional
    public void deletePremises(final Long caseId, final Long premisesId) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempPremises premises = loadPremises(premisesId);
        if (!premises.getRegGeneralInfoId().equals(caseId)) {
            throw new IllegalArgumentException("Premises does not belong to this registration case");
        }
        premises.setDeleted(true);
        tempPremisesRepository.save(premises);
    }

    @Transactional
    public TempDirectorOwnerData createDirector(final Long caseId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final JsonNode node = parseJson(json);

        final TempDirectorOwner director = new TempDirectorOwner();
        director.setRegGeneralInfoId(caseId);
        director.setName(requireText(node, "name"));
        director.setIdentificationTypeId(textLong(node, "identificationTypeId"));
        director.setIdentificationNo(requireText(node, "identificationNo"));
        director.setEmail(text(node, "email"));
        director.setDesignation(text(node, "designation"));
        director.setDeleted(false);
        director.setCreatedDate(LocalDateTime.now());

        return toDirectorData(caseId, tempDirectorOwnerRepository.save(director));
    }

    @Transactional
    public TempDirectorOwnerData updateDirector(final Long caseId, final Long directorId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempDirectorOwner director = loadDirector(directorId);
        if (!director.getRegGeneralInfoId().equals(caseId)) {
            throw new IllegalArgumentException("Director does not belong to this registration case");
        }
        final JsonNode node = parseJson(json);
        director.setName(requireText(node, "name"));
        director.setIdentificationTypeId(textLong(node, "identificationTypeId"));
        director.setIdentificationNo(requireText(node, "identificationNo"));
        director.setEmail(text(node, "email"));
        director.setDesignation(text(node, "designation"));
        return toDirectorData(caseId, tempDirectorOwnerRepository.save(director));
    }

    @Transactional
    public void deleteDirector(final Long caseId, final Long directorId) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempDirectorOwner director = loadDirector(directorId);
        if (!director.getRegGeneralInfoId().equals(caseId)) {
            throw new IllegalArgumentException("Director does not belong to this registration case");
        }
        director.setDeleted(true);
        tempDirectorOwnerRepository.save(director);
    }

    @Transactional
    public TempSstTariffCodeData createTariffCode(final Long caseId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempSstInfo tempSstInfo = requireTempSstInfo(regCase);
        final JsonNode node = parseJson(json);

        final TempSstTariffCode tariff = new TempSstTariffCode();
        tariff.setTempSstInfoId(tempSstInfo.getId());
        tariff.setTariffCodeSalesTypeId(requireLong(node, "tariffCodeSalesTypeId"));
        tariff.setContractTypeId(requireLong(node, "contractTypeId"));
        tariff.setFinishedGoods(text(node, "finishedGoods"));
        tariff.setDeleted(false);
        tariff.setCreatedDate(LocalDateTime.now());

        return toTariffData(tempSstTariffCodeRepository.save(tariff),
                registrationReferenceReadPlatformService.retrieveTariffCodeSalesTypeMap(
                        List.of(tariff.getTariffCodeSalesTypeId())).get(tariff.getTariffCodeSalesTypeId()));
    }

    @Transactional
    public TempSstTariffCodeData updateTariffCode(final Long caseId, final Long tariffId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempSstInfo tempSstInfo = requireTempSstInfo(regCase);
        final TempSstTariffCode tariff = loadTariff(tariffId);
        if (!tariff.getTempSstInfoId().equals(tempSstInfo.getId())) {
            throw new IllegalArgumentException("Tariff code does not belong to this registration case");
        }
        final JsonNode node = parseJson(json);
        tariff.setTariffCodeSalesTypeId(requireLong(node, "tariffCodeSalesTypeId"));
        tariff.setContractTypeId(requireLong(node, "contractTypeId"));
        tariff.setFinishedGoods(text(node, "finishedGoods"));
        final TempSstTariffCode saved = tempSstTariffCodeRepository.save(tariff);
        return toTariffData(saved, registrationReferenceReadPlatformService.retrieveTariffCodeSalesTypeMap(
                List.of(saved.getTariffCodeSalesTypeId())).get(saved.getTariffCodeSalesTypeId()));
    }

    @Transactional
    public void deleteTariffCode(final Long caseId, final Long tariffId) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        assertSstSalesSection(regCase);
        final TempSstInfo tempSstInfo = requireTempSstInfo(regCase);
        final TempSstTariffCode tariff = loadTariff(tariffId);
        if (!tariff.getTempSstInfoId().equals(tempSstInfo.getId())) {
            throw new IllegalArgumentException("Tariff code does not belong to this registration case");
        }
        tariff.setDeleted(true);
        tempSstTariffCodeRepository.save(tariff);
    }

    TempSstInfo requireTempSstInfoForCase(final RegGeneralInfo regCase) {
        return tempSstInfoRepository.findByTempEmployerId(regCase.getTempEmployer().getId())
                .orElseThrow(() -> new IllegalArgumentException("SST draft data is required before submit"));
    }

    List<TempDirectorOwner> listDirectorsForCase(final Long caseId) {
        return tempDirectorOwnerRepository.findByRegGeneralInfoIdAndDeletedFalseOrderByIdAsc(caseId);
    }

    List<TempSstTariffCode> listTariffCodesForCase(final TempSstInfo tempSstInfo) {
        if (tempSstInfo.getId() == null) {
            return List.of();
        }
        return tempSstTariffCodeRepository.findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(tempSstInfo.getId());
    }

    List<TempPremises> listPremisesForCase(final Long caseId) {
        return tempPremisesRepository.findByRegGeneralInfoIdAndDeletedFalseOrderByIdAsc(caseId);
    }

    private TempPremises loadPremises(final Long premisesId) {
        return tempPremisesRepository.findById(premisesId)
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Temp premises not found: " + premisesId));
    }

    private List<TempPremisesData> listPremises(final Long caseId) {
        return listPremisesForCase(caseId).stream().map(p -> toPremisesData(caseId, p)).toList();
    }

    private TempSstInfo findOrEmpty(final Long tempEmployerId) {
        return tempSstInfoRepository.findByTempEmployerId(tempEmployerId).orElseGet(() -> {
            final TempSstInfo empty = new TempSstInfo();
            empty.setTempEmployerId(tempEmployerId);
            return empty;
        });
    }

    private TempSstInfo requireTempSstInfo(final RegGeneralInfo regCase) {
        return tempSstInfoRepository.findByTempEmployerId(regCase.getTempEmployer().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Save SST Form 2 data before adding tariff codes"));
    }

    private List<TempDirectorOwnerData> listDirectors(final Long caseId) {
        return listDirectorsForCase(caseId).stream().map(d -> toDirectorData(caseId, d)).toList();
    }

    private List<TempSstTariffCodeData> listTariffCodes(final TempSstInfo tempSstInfo) {
        final List<TempSstTariffCode> rows = listTariffCodesForCase(tempSstInfo);
        final Map<Long, TariffCodeSalesTypeData> lookup = registrationReferenceReadPlatformService
                .retrieveTariffCodeSalesTypeMap(rows.stream().map(TempSstTariffCode::getTariffCodeSalesTypeId).toList());
        return rows.stream().map(row -> toTariffData(row, lookup.get(row.getTariffCodeSalesTypeId()))).toList();
    }

    private static void applySstFields(final TempSstInfo tempSstInfo, final JsonNode node) {
        if (node.has("tradeName")) {
            tempSstInfo.setTradeName(text(node, "tradeName"));
        }
        if (node.has("tourTaxRegNo")) {
            tempSstInfo.setTourTaxRegNo(text(node, "tourTaxRegNo"));
        }
        if (node.has("inTaxRefNo")) {
            tempSstInfo.setInTaxRefNo(text(node, "inTaxRefNo"));
        }
        if (node.has("cusAudRefNo")) {
            tempSstInfo.setCusAudRefNo(text(node, "cusAudRefNo"));
        }
        if (node.has("preRegNo")) {
            tempSstInfo.setPreRegNo(text(node, "preRegNo"));
        }
        if (node.has("preRegName")) {
            tempSstInfo.setPreRegName(text(node, "preRegName"));
        }
        if (node.hasNonNull("dateOfReplacement")) {
            tempSstInfo.setDateOfReplacement(LocalDate.parse(node.get("dateOfReplacement").asText()));
        }
        if (node.hasNonNull("manComDate")) {
            tempSstInfo.setManComDate(LocalDate.parse(node.get("manComDate").asText()));
        }
        if (node.hasNonNull("dateSaleValTaxGoods")) {
            tempSstInfo.setDateSaleValTaxGoods(LocalDate.parse(node.get("dateSaleValTaxGoods").asText()));
        }
        if (node.has("finYrEndMon")) {
            tempSstInfo.setFinYrEndMon(node.get("finYrEndMon").isNull() ? null : node.get("finYrEndMon").asInt());
        }
        if (node.has("anTotalTaxSalesVal")) {
            tempSstInfo.setAnTotalTaxSalesVal(decimal(node, "anTotalTaxSalesVal"));
        }
        if (node.hasNonNull("businessComDate")) {
            tempSstInfo.setBusinessComDate(LocalDate.parse(node.get("businessComDate").asText()));
        }
        if (node.has("localSales")) {
            tempSstInfo.setLocalSales(decimal(node, "localSales"));
        }
        if (node.has("exportSales")) {
            tempSstInfo.setExportSales(decimal(node, "exportSales"));
        }
        if (node.has("salesToDesignArea")) {
            tempSstInfo.setSalesToDesignArea(decimal(node, "salesToDesignArea"));
        }
        if (node.has("othersSales")) {
            tempSstInfo.setOthersSales(decimal(node, "othersSales"));
        }
        if (node.has("subContractWork")) {
            tempSstInfo.setSubContractWork(node.get("subContractWork").asBoolean());
        }
    }

    private RegGeneralInfo loadCase(final Long caseId) {
        return regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Registration case not found: " + caseId));
    }

    private RegGeneralInfo loadEditableCase(final Long caseId) {
        final RegGeneralInfo regCase = loadCase(caseId);
        if (regCase.getAppStatus() != AppStatus.NEW && regCase.getAppStatus() != AppStatus.IN_QUERY
                && regCase.getAppStatus() != AppStatus.IN_PROGRESS) {
            throw new RegistrationCaseInvalidStatusException(regCase.getCaseRefNo(), regCase.getAppStatus().name(),
                    AppStatus.NEW.name());
        }
        return regCase;
    }

    private TempDirectorOwner loadDirector(final Long directorId) {
        return tempDirectorOwnerRepository.findById(directorId)
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Temp director not found: " + directorId));
    }

    private TempSstTariffCode loadTariff(final Long tariffId) {
        return tempSstTariffCodeRepository.findById(tariffId)
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Temp tariff code not found: " + tariffId));
    }

    private static void assertSstSalesSection(final RegGeneralInfo regCase) {
        if (regCase.getSectionId() != RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId()) {
            throw new IllegalArgumentException("SST info API is only available for sales tax new registration (1100)");
        }
    }

    private JsonNode parseJson(final String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }

    private static String requireText(final JsonNode node, final String field) {
        if (!node.hasNonNull(field) || node.get(field).asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.get(field).asText();
    }

    private static Long requireLong(final JsonNode node, final String field) {
        if (!node.hasNonNull(field)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.get(field).asLong();
    }

    private static Long textLong(final JsonNode node, final String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asLong();
    }

    private static String text(final JsonNode node, final String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static BigDecimal decimal(final JsonNode node, final String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).decimalValue();
    }

    private static TempSstInfoData toData(final Long caseId, final TempSstInfo tempSstInfo,
            final List<TempDirectorOwnerData> directors, final List<TempPremisesData> premises,
            final List<TempSstTariffCodeData> tariffCodes) {
        return TempSstInfoData.builder().id(tempSstInfo.getId()).caseId(caseId).tradeName(tempSstInfo.getTradeName())
                .tourTaxRegNo(tempSstInfo.getTourTaxRegNo()).inTaxRefNo(tempSstInfo.getInTaxRefNo())
                .cusAudRefNo(tempSstInfo.getCusAudRefNo()).preRegNo(tempSstInfo.getPreRegNo())
                .preRegName(tempSstInfo.getPreRegName()).dateOfReplacement(tempSstInfo.getDateOfReplacement())
                .manComDate(tempSstInfo.getManComDate()).dateSaleValTaxGoods(tempSstInfo.getDateSaleValTaxGoods())
                .finYrEndMon(tempSstInfo.getFinYrEndMon()).anTotalTaxSalesVal(tempSstInfo.getAnTotalTaxSalesVal())
                .businessComDate(tempSstInfo.getBusinessComDate()).localSales(tempSstInfo.getLocalSales())
                .exportSales(tempSstInfo.getExportSales()).salesToDesignArea(tempSstInfo.getSalesToDesignArea())
                .othersSales(tempSstInfo.getOthersSales()).subContractWork(tempSstInfo.isSubContractWork())
                .directors(directors).premises(premises).tariffCodes(tariffCodes).build();
    }

    private static TempPremisesData toPremisesData(final Long caseId, final TempPremises premises) {
        return TempPremisesData.builder().id(premises.getId()).caseId(caseId).name(premises.getName())
                .addressLine(premises.getAddressLine()).addressLine2(premises.getAddressLine2())
                .addressLine3(premises.getAddressLine3()).postCode(premises.getPostCode())
                .cityName(premises.getCityName()).stateName(premises.getStateName()).build();
    }

    private static TempDirectorOwnerData toDirectorData(final Long caseId, final TempDirectorOwner director) {
        return TempDirectorOwnerData.builder().id(director.getId()).caseId(caseId).name(director.getName())
                .identificationTypeId(director.getIdentificationTypeId())
                .identificationNo(director.getIdentificationNo()).email(director.getEmail())
                .designation(director.getDesignation()).build();
    }

    private static TempSstTariffCodeData toTariffData(final TempSstTariffCode tariff) {
        return toTariffData(tariff, null);
    }

    private static TempSstTariffCodeData toTariffData(final TempSstTariffCode tariff,
            final TariffCodeSalesTypeData reference) {
        return TempSstTariffCodeData.builder().id(tariff.getId())
                .tariffCodeSalesTypeId(tariff.getTariffCodeSalesTypeId())
                .tariffCode(reference != null ? reference.getCode() : null)
                .tariffDescription(reference != null ? reference.getDescription() : null)
                .contractTypeId(tariff.getContractTypeId())
                .finishedGoods(tariff.getFinishedGoods()).build();
    }
}
