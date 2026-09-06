package my.gov.perkeso.assist.registration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.domain.base.BaseAddress;
import my.gov.perkeso.assist.registration.domain.base.BaseAddressRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseContact;
import my.gov.perkeso.assist.registration.domain.base.BaseContactRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseReferenceIds;
import my.gov.perkeso.assist.registration.domain.base.BaseTableNames;
import my.gov.perkeso.assist.registration.domain.base.TempAddress;
import my.gov.perkeso.assist.registration.domain.base.TempAddressRepository;
import my.gov.perkeso.assist.registration.domain.base.TempContact;
import my.gov.perkeso.assist.registration.domain.base.TempContactRepository;
import my.gov.perkeso.assist.registration.domain.base.TempUserEmployer;
import my.gov.perkeso.assist.registration.domain.base.TempUserEmployerRepository;
import my.gov.perkeso.assist.registration.domain.base.UserEmployer;
import my.gov.perkeso.assist.registration.domain.base.UserEmployerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BaseUserEmployerEnrollmentService {

    private static final long SELF_SERVICE_ACTOR_ID = 1L;

    private final TempUserEmployerRepository tempUserEmployerRepository;
    private final TempAddressRepository tempAddressRepository;
    private final TempContactRepository tempContactRepository;
    private final UserEmployerRepository userEmployerRepository;
    private final BaseAddressRepository baseAddressRepository;
    private final BaseContactRepository baseContactRepository;
    private final BaseDocumentEnrollmentService baseDocumentEnrollmentService;

    @Transactional
    public UserEmployer submitEnrollment(final BaseUserEmployerEnrollmentCommand command) {
        final LocalDateTime now = LocalDateTime.now();
        final Long actorId = command.portalUserId() != null ? command.portalUserId() : SELF_SERVICE_ACTOR_ID;

        final TempUserEmployer tempUserEmployer = buildTempUserEmployer(command, actorId, now);
        final TempUserEmployer savedTemp = tempUserEmployerRepository.save(tempUserEmployer);

        final TempAddress tempAddress = buildTempAddress(command, savedTemp.getId(), actorId, now);
        tempAddressRepository.save(tempAddress);

        tempContactRepository.save(buildTempPhoneContact(command, savedTemp.getId(), actorId, now));
        tempContactRepository.save(buildTempEmailContact(command, savedTemp.getId(), actorId, now));

        final UserEmployer userEmployer = promoteTempUserEmployer(savedTemp, actorId, now);
        final UserEmployer savedUserEmployer = userEmployerRepository.save(userEmployer);

        final BaseAddress address = promoteTempAddress(tempAddress, savedUserEmployer.getId(), actorId, now);
        baseAddressRepository.save(address);

        baseContactRepository.save(promoteTempPhoneContact(command, savedUserEmployer.getId(), actorId, now));
        baseContactRepository.save(promoteTempEmailContact(command, savedUserEmployer.getId(), actorId, now));

        markTempDeleted(savedTemp, tempAddress, actorId, now);

        baseDocumentEnrollmentService.promoteDraftDocuments(command.draftToken(), savedUserEmployer.getId(),
                command.registrationEmployerId(), actorId);

        return savedUserEmployer;
    }

    @Transactional
    public void updateEnrollmentProfile(final Long userEmployerId, final BaseUserEmployerEnrollmentCommand command) {
        final LocalDateTime now = LocalDateTime.now();
        final Long actorId = command.portalUserId() != null ? command.portalUserId() : SELF_SERVICE_ACTOR_ID;

        final UserEmployer userEmployer = userEmployerRepository.findById(userEmployerId)
                .filter(row -> !row.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Portal enrollment profile not found"));

        userEmployer.setEmployerCode(command.employerCode());
        userEmployer.setEmployerName(command.employerName());
        userEmployer.setRegistrationTypeId(command.registrationTypeId());
        userEmployer.setRegistrationNo(command.registrationNo());
        userEmployer.setFullName(command.fullName().trim());
        userEmployer.setIdentificationType(command.identificationTypeId());
        userEmployer.setIdentificationValue(command.identificationNo());
        userEmployer.setEmployerId(command.registrationEmployerId());
        userEmployer.setUpdateById(actorId);
        userEmployer.setUpdateDate(now);
        userEmployerRepository.save(userEmployer);

        final BaseAddress address = baseAddressRepository
                .findFirstByTableNameAndTablePkIdAndDeletedFalse(BaseTableNames.USER_EMPLOYER, userEmployerId)
                .orElseThrow(() -> new IllegalArgumentException("Portal enrollment address not found"));
        address.setAddressLine1(command.addressLine1());
        address.setAddressLine2(command.addressLine2());
        address.setAddressLine3(command.addressLine3());
        address.setStateId(command.stateId());
        if (command.cityId() != null && command.cityId() > 0) {
            address.setCityId(command.cityId());
        } else {
            address.setCityId(null);
        }
        address.setPostcode(command.postCode());
        address.setUpdateById(actorId);
        address.setUpdateDate(now);
        baseAddressRepository.save(address);

        updateContact(userEmployerId, BaseReferenceIds.CONTACT_TYPE_FIXED_LINE, command.phoneCallingCode(),
                command.phoneNumber(), actorId, now);
        updateContact(userEmployerId, BaseReferenceIds.CONTACT_TYPE_EMAIL, null,
                command.email().trim().toLowerCase(), actorId, now);

        baseDocumentEnrollmentService.promoteDraftDocumentsForResubmit(command.draftToken(), userEmployerId,
                command.registrationEmployerId(), actorId);
    }

    private void updateContact(final Long userEmployerId, final long contactTypeId, final String callingCode,
            final String contactValue, final Long actorId, final LocalDateTime now) {
        final List<BaseContact> contacts = baseContactRepository
                .findByTableNameAndTablePkIdAndDeletedFalse(BaseTableNames.USER_EMPLOYER, userEmployerId);
        final BaseContact contact = contacts.stream()
                .filter(row -> Objects.equals(row.getContactTypeId(), contactTypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Portal enrollment contact not found"));
        if (callingCode != null) {
            contact.setCallingCode(callingCode);
        }
        contact.setContactValue(contactValue);
        contact.setUpdateById(actorId);
        contact.setUpdateDate(now);
        baseContactRepository.save(contact);
    }

    private TempUserEmployer buildTempUserEmployer(final BaseUserEmployerEnrollmentCommand command,
            final Long actorId, final LocalDateTime now) {
        final TempUserEmployer temp = new TempUserEmployer();
        temp.setUserId(command.portalUserId());
        temp.setApplicationType(command.applicationType());
        temp.setEmployerCode(command.employerCode());
        temp.setEmployerName(command.employerName());
        temp.setRegistrationTypeId(command.registrationTypeId());
        temp.setRegistrationNo(command.registrationNo());
        temp.setFullName(command.fullName().trim());
        temp.setIdentificationType(command.identificationTypeId());
        temp.setIdentificationValue(command.identificationNo());
        temp.setSecurityPhrase(command.securityPhrase());
        temp.setGroupOfCompanies(false);
        temp.setEmployerId(command.registrationEmployerId());
        applyAudit(temp, actorId, now);
        return temp;
    }

    private TempAddress buildTempAddress(final BaseUserEmployerEnrollmentCommand command, final Long tempUserEmployerId,
            final Long actorId, final LocalDateTime now) {
        final TempAddress tempAddress = new TempAddress();
        tempAddress.setAddressLine1(command.addressLine1());
        tempAddress.setAddressLine2(command.addressLine2());
        tempAddress.setAddressLine3(command.addressLine3());
        tempAddress.setStateId(command.stateId());
        if (command.cityId() != null && command.cityId() > 0) {
            tempAddress.setCityId(command.cityId());
        }
        tempAddress.setPostcode(command.postCode());
        tempAddress.setAddressTypeId(BaseReferenceIds.ADDRESS_TYPE_POSTAL);
        tempAddress.setTableName(BaseTableNames.TEMP_USER_EMPLOYER);
        tempAddress.setTablePkId(tempUserEmployerId);
        applyAudit(tempAddress, actorId, now);
        return tempAddress;
    }

    private TempContact buildTempPhoneContact(final BaseUserEmployerEnrollmentCommand command,
            final Long tempUserEmployerId, final Long actorId, final LocalDateTime now) {
        final TempContact contact = new TempContact();
        contact.setContactTypeId(BaseReferenceIds.CONTACT_TYPE_FIXED_LINE);
        contact.setCallingCode(command.phoneCallingCode());
        contact.setContactValue(command.phoneNumber());
        contact.setTableName(BaseTableNames.TEMP_USER_EMPLOYER);
        contact.setTablePkId(tempUserEmployerId);
        applyAudit(contact, actorId, now);
        return contact;
    }

    private TempContact buildTempEmailContact(final BaseUserEmployerEnrollmentCommand command,
            final Long tempUserEmployerId, final Long actorId, final LocalDateTime now) {
        final TempContact contact = new TempContact();
        contact.setContactTypeId(BaseReferenceIds.CONTACT_TYPE_EMAIL);
        contact.setContactValue(command.email().trim().toLowerCase());
        contact.setTableName(BaseTableNames.TEMP_USER_EMPLOYER);
        contact.setTablePkId(tempUserEmployerId);
        applyAudit(contact, actorId, now);
        return contact;
    }

    private UserEmployer promoteTempUserEmployer(final TempUserEmployer temp, final Long actorId,
            final LocalDateTime now) {
        final UserEmployer userEmployer = new UserEmployer();
        userEmployer.setUserId(temp.getUserId());
        userEmployer.setApplicationType(temp.getApplicationType());
        userEmployer.setEmployerCode(temp.getEmployerCode());
        userEmployer.setEmployerName(temp.getEmployerName());
        userEmployer.setRegistrationTypeId(temp.getRegistrationTypeId());
        userEmployer.setRegistrationNo(temp.getRegistrationNo());
        userEmployer.setFullName(temp.getFullName());
        userEmployer.setIdentificationType(temp.getIdentificationType());
        userEmployer.setIdentificationValue(temp.getIdentificationValue());
        userEmployer.setGroupOfCompanies(temp.isGroupOfCompanies());
        userEmployer.setBranchId(temp.getBranchId());
        userEmployer.setEmployerId(temp.getEmployerId());
        applyAudit(userEmployer, actorId, now);
        return userEmployer;
    }

    private BaseAddress promoteTempAddress(final TempAddress tempAddress, final Long userEmployerId, final Long actorId,
            final LocalDateTime now) {
        final BaseAddress address = new BaseAddress();
        address.setAddressLine1(tempAddress.getAddressLine1());
        address.setAddressLine2(tempAddress.getAddressLine2());
        address.setAddressLine3(tempAddress.getAddressLine3());
        address.setCountryId(tempAddress.getCountryId());
        address.setStateId(tempAddress.getStateId());
        address.setCityId(tempAddress.getCityId());
        address.setPostcode(tempAddress.getPostcode());
        address.setPoBox(tempAddress.getPoBox());
        address.setLockedBag(tempAddress.getLockedBag());
        address.setWdt(tempAddress.getWdt());
        address.setAddressTypeId(BaseReferenceIds.ADDRESS_TYPE_POSTAL);
        address.setTableName(BaseTableNames.USER_EMPLOYER);
        address.setTablePkId(userEmployerId);
        applyAudit(address, actorId, now);
        return address;
    }

    private BaseContact promoteTempPhoneContact(final BaseUserEmployerEnrollmentCommand command,
            final Long userEmployerId, final Long actorId, final LocalDateTime now) {
        final BaseContact contact = new BaseContact();
        contact.setContactTypeId(BaseReferenceIds.CONTACT_TYPE_FIXED_LINE);
        contact.setCallingCode(command.phoneCallingCode());
        contact.setContactValue(command.phoneNumber());
        contact.setTableName(BaseTableNames.USER_EMPLOYER);
        contact.setTablePkId(userEmployerId);
        applyAudit(contact, actorId, now);
        return contact;
    }

    private BaseContact promoteTempEmailContact(final BaseUserEmployerEnrollmentCommand command,
            final Long userEmployerId, final Long actorId, final LocalDateTime now) {
        final BaseContact contact = new BaseContact();
        contact.setContactTypeId(BaseReferenceIds.CONTACT_TYPE_EMAIL);
        contact.setContactValue(command.email().trim().toLowerCase());
        contact.setTableName(BaseTableNames.USER_EMPLOYER);
        contact.setTablePkId(userEmployerId);
        applyAudit(contact, actorId, now);
        return contact;
    }

    private void markTempDeleted(final TempUserEmployer tempUserEmployer, final TempAddress tempAddress,
            final Long actorId, final LocalDateTime now) {
        tempUserEmployer.setDeleted(true);
        tempUserEmployer.setUpdateById(actorId);
        tempUserEmployer.setUpdateDate(now);
        tempUserEmployerRepository.save(tempUserEmployer);

        tempAddress.setDeleted(true);
        tempAddress.setUpdateById(actorId);
        tempAddress.setUpdateDate(now);
        tempAddressRepository.save(tempAddress);
    }

    private static void applyAudit(final my.gov.perkeso.assist.registration.domain.base.BaseAuditEntity entity,
            final Long actorId, final LocalDateTime now) {
        entity.setDeleted(false);
        entity.setCreateById(actorId);
        entity.setCreateDate(now);
    }
}
