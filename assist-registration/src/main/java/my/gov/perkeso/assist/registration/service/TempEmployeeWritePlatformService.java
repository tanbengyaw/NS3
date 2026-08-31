package my.gov.perkeso.assist.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.TempEmployeeData;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployee;
import my.gov.perkeso.assist.registration.domain.TempEmployeeRepository;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseInvalidStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempEmployeeWritePlatformService {

    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempEmployeeRepository tempEmployeeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<TempEmployeeData> listEmployees(final Long caseId) {
        final RegGeneralInfo regCase = loadCase(caseId);
        return tempEmployeeRepository.findByTempEmployerIdAndDeletedFalseOrderByIdAsc(regCase.getTempEmployer().getId())
                .stream()
                .map(employee -> toData(caseId, employee))
                .toList();
    }

    @Transactional
    public TempEmployeeData createEmployee(final Long caseId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        final JsonNode node = parseJson(json);

        final TempEmployee employee = new TempEmployee();
        employee.setTempEmployerId(regCase.getTempEmployer().getId());
        employee.setEmployeeName(requireText(node, "employeeName"));
        employee.setIdentificationNo(requireText(node, "identificationNo"));
        employee.setEmploymentStartDate(LocalDate.parse(requireText(node, "employmentStartDate")));
        employee.setNationalityId(textLong(node, "nationalityId"));
        employee.setDeleted(false);
        employee.setCreatedDate(LocalDateTime.now());

        return toData(caseId, tempEmployeeRepository.save(employee));
    }

    @Transactional
    public TempEmployeeData updateEmployee(final Long caseId, final Long employeeId, final String json) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        final TempEmployee employee = loadEmployee(employeeId);
        assertSameTempEmployer(regCase, employee);
        final JsonNode node = parseJson(json);

        if (node.hasNonNull("employeeName")) {
            employee.setEmployeeName(node.get("employeeName").asText());
        }
        if (node.hasNonNull("identificationNo")) {
            employee.setIdentificationNo(node.get("identificationNo").asText());
        }
        if (node.hasNonNull("employmentStartDate")) {
            employee.setEmploymentStartDate(LocalDate.parse(node.get("employmentStartDate").asText()));
        }
        if (node.has("nationalityId")) {
            employee.setNationalityId(textLong(node, "nationalityId"));
        }

        return toData(caseId, tempEmployeeRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(final Long caseId, final Long employeeId) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        final TempEmployee employee = loadEmployee(employeeId);
        assertSameTempEmployer(regCase, employee);
        employee.setDeleted(true);
        tempEmployeeRepository.save(employee);
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

    private TempEmployee loadEmployee(final Long employeeId) {
        return tempEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Temp employee not found: " + employeeId));
    }

    private static void assertSameTempEmployer(final RegGeneralInfo regCase, final TempEmployee employee) {
        if (!regCase.getTempEmployer().getId().equals(employee.getTempEmployerId())) {
            throw new IllegalArgumentException("Employee does not belong to this registration case");
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

    private static Long textLong(final JsonNode node, final String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asLong();
    }

    private static TempEmployeeData toData(final Long caseId, final TempEmployee employee) {
        return TempEmployeeData.builder().id(employee.getId()).caseId(caseId).employeeName(employee.getEmployeeName())
                .identificationNo(employee.getIdentificationNo())
                .employmentStartDate(employee.getEmploymentStartDate()).nationalityId(employee.getNationalityId())
                .build();
    }
}
