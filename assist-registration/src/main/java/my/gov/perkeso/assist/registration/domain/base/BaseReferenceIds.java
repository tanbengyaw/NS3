package my.gov.perkeso.assist.registration.domain.base;

/**
 * Legacy BASE reference IDs. Values align with {@code base.ref_address_type},
 * {@code base.ref_contact_type}, and application type codes on {@code base.user_employer}.
 */
public final class BaseReferenceIds {

    public static final long ADDRESS_TYPE_POSTAL = 2L;
    public static final long CONTACT_TYPE_FIXED_LINE = 2L;
    public static final long CONTACT_TYPE_EMAIL = 3L;

    public static final int APPLICATION_TYPE_NEW_EMPLOYER = 1;
    public static final int APPLICATION_TYPE_EXISTING_EMPLOYER = 2;

    private BaseReferenceIds() {
    }
}
