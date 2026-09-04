import { CommandProcessingResult } from '../core/models/registration.model';

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
  extras?: { queryRemark?: string; appStatusReason?: string },
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
    typeof changes['salesTaxSmkRegNo'] === 'string' ? changes['salesTaxSmkRegNo'] : null;
  const queryRemark =
    extras?.queryRemark ??
    (typeof changes['queryRemark'] === 'string' ? changes['queryRemark'] : null);
  const appStatusReason = extras?.appStatusReason ?? null;

  const message =
    action === 'approve'
      ? `Approved. Employer code: ${employerCode ?? '—'}`
      : action === 'query'
        ? 'Case sent to query. The applicant can update and resubmit.'
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
