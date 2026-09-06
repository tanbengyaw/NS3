package my.gov.perkeso.assist.registration.constant;

/**
 * Section groupings for ASSIST-style submit routing (RO / UO / PKR_BO) and SST new-reg.
 */
public final class RegistrationSectionRouting {

    private RegistrationSectionRouting() {
    }

    public static long effectiveSectionId(final Long sectionId) {
        return sectionId != null ? sectionId : RegistrationSection.REG_NEW_REG.getAssistSectionId();
    }

    /** Sales (1100), tourism (1101), digital (1102), DPSP (1104), service (1105). */
    public static boolean isSstNewRegSection(final Long sectionId) {
        final long id = effectiveSectionId(sectionId);
        return id == RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId()
                || id == RegistrationSection.REG_SST_TOURISM_TAX.getAssistSectionId()
                || id == RegistrationSection.REG_SST_DIGITAL_TAX.getAssistSectionId()
                || id == RegistrationSection.REG_SST_DPSP_TAX.getAssistSectionId()
                || id == RegistrationSection.REG_SST_SERVICE_TAX.getAssistSectionId();
    }

    public static boolean isSstServiceTaxNewReg(final Long sectionId) {
        return effectiveSectionId(sectionId) == RegistrationSection.REG_SST_SERVICE_TAX.getAssistSectionId();
    }

    public static boolean isSstSalesTaxNewReg(final Long sectionId) {
        return effectiveSectionId(sectionId) == RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId();
    }

    public static boolean isSstTourismTaxNewReg(final Long sectionId) {
        return effectiveSectionId(sectionId) == RegistrationSection.REG_SST_TOURISM_TAX.getAssistSectionId();
    }

    public static boolean isSstDigitalTaxNewReg(final Long sectionId) {
        return effectiveSectionId(sectionId) == RegistrationSection.REG_SST_DIGITAL_TAX.getAssistSectionId();
    }

    public static boolean isSstDpspTaxNewReg(final Long sectionId) {
        return effectiveSectionId(sectionId) == RegistrationSection.REG_SST_DPSP_TAX.getAssistSectionId();
    }

    public static TaxType taxTypeForSstNewReg(final Long sectionId) {
        if (isSstTourismTaxNewReg(sectionId)) {
            return TaxType.TOURISM_TAX;
        }
        if (effectiveSectionId(sectionId) == RegistrationSection.REG_SST_DIGITAL_TAX.getAssistSectionId()) {
            return TaxType.DIGITAL_TAX;
        }
        if (effectiveSectionId(sectionId) == RegistrationSection.REG_SST_DPSP_TAX.getAssistSectionId()) {
            return TaxType.DPSP_TAX;
        }
        if (isSstServiceTaxNewReg(sectionId)) {
            return TaxType.SERVICE_TAX;
        }
        return TaxType.SALES_TAX;
    }

    public static boolean isUpdateTaxSection(final Long sectionId) {
        final long id = effectiveSectionId(sectionId);
        return id >= RegistrationSection.REG_UPDATE_TAX_PAYER_SERVICE_TAX.getAssistSectionId()
                && id <= RegistrationSection.REG_UPDATE_TAX_PAYER_DPSP_TAX.getAssistSectionId();
    }

    /**
     * SST new-reg (1100-1105 excl. 1103), SST update-tax-payer (1200-1204), or SST discontinue-tax
     * (1103) section — i.e. any case that carries a {@code TempSstInfo} draft and is therefore
     * eligible for the shared SST draft CRUD APIs (directors, premises, tariffs, service
     * categories, supporting documents, letters).
     */
    public static boolean isSstCaseSection(final Long sectionId) {
        return isSstNewRegSection(sectionId) || isUpdateTaxSection(sectionId) || isDiscontinueTaxSection(sectionId);
    }

    /** Update Tax Payer sections: 1200=service, 1201=sales, 1202=tourism, 1203=digital, 1204=dpsp. */
    public static TaxType taxTypeForUpdateSection(final Long sectionId) {
        final long id = effectiveSectionId(sectionId);
        if (id == RegistrationSection.REG_UPDATE_TAX_PAYER_SALES_TAX.getAssistSectionId()) {
            return TaxType.SALES_TAX;
        }
        if (id == RegistrationSection.REG_UPDATE_TAX_PAYER_TOURISM_TAX.getAssistSectionId()) {
            return TaxType.TOURISM_TAX;
        }
        if (id == RegistrationSection.REG_UPDATE_TAX_PAYER_DIGITAL_TAX.getAssistSectionId()) {
            return TaxType.DIGITAL_TAX;
        }
        if (id == RegistrationSection.REG_UPDATE_TAX_PAYER_DPSP_TAX.getAssistSectionId()) {
            return TaxType.DPSP_TAX;
        }
        return TaxType.SERVICE_TAX;
    }

    public static boolean isIncompleteTaxSection(final Long sectionId) {
        final long id = effectiveSectionId(sectionId);
        return id >= RegistrationSection.REG_INCOMPLETE_TAX_PAYER_SERVICE_TAX.getAssistSectionId()
                && id <= RegistrationSection.REG_INCOMPLETE_TAX_PAYER_DPSP_TAX.getAssistSectionId();
    }

    public static boolean isDiscontinueTaxSection(final Long sectionId) {
        return effectiveSectionId(sectionId) == RegistrationSection.REG_SST_DISCONTINUE_TAX.getAssistSectionId();
    }

    public static boolean isUoWorkflowSection(final Long sectionId) {
        return isUpdateTaxSection(sectionId) || isDiscontinueTaxSection(sectionId);
    }

    public static java.util.List<Long> uoWorkflowSectionIds() {
        return java.util.List.of(
                RegistrationSection.REG_SST_DISCONTINUE_TAX.getAssistSectionId(),
                RegistrationSection.REG_UPDATE_TAX_PAYER_SERVICE_TAX.getAssistSectionId(),
                RegistrationSection.REG_UPDATE_TAX_PAYER_SALES_TAX.getAssistSectionId(),
                RegistrationSection.REG_UPDATE_TAX_PAYER_TOURISM_TAX.getAssistSectionId(),
                RegistrationSection.REG_UPDATE_TAX_PAYER_DIGITAL_TAX.getAssistSectionId(),
                RegistrationSection.REG_UPDATE_TAX_PAYER_DPSP_TAX.getAssistSectionId());
    }
}
