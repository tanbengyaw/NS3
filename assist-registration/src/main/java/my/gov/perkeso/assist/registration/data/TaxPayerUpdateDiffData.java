package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

/**
 * One changed field in the on-the-fly Update Tax Payer diff (live value vs the case's temp draft
 * value). Computed fresh on each request — not persisted.
 */
@Value
@Builder
public class TaxPayerUpdateDiffData {

    String fieldLabel;
    String oldValue;
    String newValue;
}
