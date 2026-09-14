package my.gov.perkeso.assist.registration.service;

import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.SstInfo;

/**
 * Legacy {@code EmployerImpl#isAuditAutoRegSstDataCompleteForTaxType}: sales/service need
 * manufacturing commencement + date sale value taxable; tourism/digital/DPSP need applicant name.
 */
public final class IncompleteAutoRegCompleteness {

    private IncompleteAutoRegCompleteness() {
    }

    public static boolean isComplete(final TaxType taxType, final SstInfo sstInfo) {
        if (sstInfo == null || taxType == null) {
            return false;
        }
        return switch (taxType) {
            case SALES_TAX, SERVICE_TAX -> sstInfo.getManComDate() != null && sstInfo.getDateSaleValTaxGoods() != null;
            case TOURISM_TAX, DIGITAL_TAX, DPSP_TAX -> sstInfo.getApplicantName() != null
                    && !sstInfo.getApplicantName().isBlank();
        };
    }

    public static TaxType parseTaxType(final String taxTypeParam) {
        if (taxTypeParam == null || taxTypeParam.isBlank()) {
            throw new IllegalArgumentException("taxType is required");
        }
        try {
            return TaxType.valueOf(taxTypeParam.trim().toUpperCase());
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported taxType: " + taxTypeParam
                    + " (expected one of SERVICE_TAX, SALES_TAX, TOURISM_TAX, DIGITAL_TAX, DPSP_TAX)", ex);
        }
    }
}
