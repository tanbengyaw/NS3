const UPDATE_TAX_MIN = 1200;
const UPDATE_TAX_MAX = 1204;
const INCOMPLETE_TAX_MIN = 1205;
const INCOMPLETE_TAX_MAX = 1209;
const DISCONTINUE_TAX = 1103;

function effectiveSectionId(sectionId: number | null): number {
  return sectionId ?? 200;
}

export function isUpdateTaxSection(sectionId: number | null): boolean {
  const id = effectiveSectionId(sectionId);
  return id >= UPDATE_TAX_MIN && id <= UPDATE_TAX_MAX;
}

export function isIncompleteTaxSection(sectionId: number | null): boolean {
  const id = effectiveSectionId(sectionId);
  return id >= INCOMPLETE_TAX_MIN && id <= INCOMPLETE_TAX_MAX;
}

export function isUoWorkflowSection(sectionId: number | null): boolean {
  return isUpdateTaxSection(sectionId) || effectiveSectionId(sectionId) === DISCONTINUE_TAX;
}

export function canMarkIncompleteSubmit(roles: string[]): boolean {
  return roles.some((role) => ['OFFICER', 'PKR_BO', 'UO', 'ADMIN'].includes(role));
}

export function submitStatusHint(roles: string[], sectionId: number | null, incomplete: boolean): string {
  const isIncompleteTax = isIncompleteTaxSection(sectionId);
  const isUoWorkflow = isUoWorkflowSection(sectionId);

  if (roles.includes('EMPLOYER')) {
    return 'Your case will be sent to the processing branch queue (SUBMITTED).';
  }
  if (isIncompleteTax && !roles.includes('UO')) {
    return 'Incomplete tax registration — staff submit will auto-approve (APPROVED).';
  }
  if (roles.includes('RO')) {
    return isUoWorkflow
      ? 'Update/discontinue tax cases go to the UO queue (SUBMITTED).'
      : 'RO submit auto-approves new registration (APPROVED).';
  }
  if (roles.includes('PKR_BO') && (incomplete || isIncompleteTax)) {
    return 'Incomplete submission — PKR back-office will auto-approve (APPROVED).';
  }
  if (roles.includes('UO')) {
    if (isUoWorkflow) {
      return 'Case goes to your UO processing queue (SUBMITTED).';
    }
    return incomplete
      ? 'Incomplete OTC case — stays with counter (IN_PROGRESS).'
      : 'Case submitted to processing queue (SUBMITTED).';
  }
  if (roles.includes('OFFICER') || roles.includes('ADMIN')) {
    return incomplete ? 'Incomplete OTC case (IN_PROGRESS until complete).' : 'Case submitted (SUBMITTED).';
  }
  return 'Submit the case for processing.';
}
