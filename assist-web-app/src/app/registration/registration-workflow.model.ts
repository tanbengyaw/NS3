import { CommandProcessingResult } from '../core/models/registration.model';
import { isUoWorkflowSection } from './submit-routing.util';

export type RegistrationWorkflowAction = 'approve' | 'query' | 'reject';

export interface RegistrationWorkflowResult {
  action: RegistrationWorkflowAction;
  appStatus: string;
  employerCode: string | null;
  queryRemark: string | null;
  appStatusReason: string | null;
  salesTaxSmkRegNo: string | null;
  message: string;
  commandResult: CommandProcessingResult;
}

export function buildRegistrationWorkflowResult(
  action: RegistrationWorkflowAction,
  commandResult: CommandProcessingResult,
  extras?: { queryRemark?: string; appStatusReason?: string; sectionId?: number | null },
): RegistrationWorkflowResult {
  const changes = commandResult.changes ?? {};
  const appStatus =
    action === 'approve'
      ? 'APPROVED'
      : action === 'query'
        ? String(changes['appStatus'] ?? 'IN_QUERY')
        : 'REJECTED';

  const employerCode = commandResult.resourceIdentifier ?? null;
  const salesTaxSmkRegNo =
    typeof changes['sstSmkRegNo'] === 'string'
      ? changes['sstSmkRegNo']
      : typeof changes['tourismTaxSmkRegNo'] === 'string'
        ? changes['tourismTaxSmkRegNo']
        : typeof changes['dpspTaxSmkRegNo'] === 'string'
          ? changes['dpspTaxSmkRegNo']
          : typeof changes['serviceTaxSmkRegNo'] === 'string'
            ? changes['serviceTaxSmkRegNo']
            : typeof changes['salesTaxSmkRegNo'] === 'string'
              ? changes['salesTaxSmkRegNo']
              : null;
  const queryRemark =
    extras?.queryRemark ??
    (typeof changes['queryRemark'] === 'string' ? changes['queryRemark'] : null);
  const appStatusReason = extras?.appStatusReason ?? null;

  // Update Tax Payer / Discontinue Tax cases are staff-initiated (no portal "applicant" — an
  // officer/RO/UO created and submitted it), so the query message shouldn't imply an applicant
  // exists to respond; new-reg cases (including portal employer submissions) do have one.
  const hasApplicant = !isUoWorkflowSection(extras?.sectionId ?? null);
  const message =
    action === 'approve'
      ? `Approved. Employer code: ${employerCode ?? '—'}`
      : action === 'query'
        ? hasApplicant
          ? 'Case sent to query. The applicant can update and resubmit.'
          : 'Case sent to query. The submitting officer can review the remark, update the case, and resubmit.'
        : 'Case rejected.';

  return {
    action,
    appStatus,
    employerCode,
    queryRemark,
    appStatusReason,
    salesTaxSmkRegNo,
    message,
    commandResult,
  };
}
