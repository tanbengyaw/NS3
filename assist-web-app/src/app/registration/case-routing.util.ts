export function isUoWorkflowSection(sectionId: number | null | undefined): boolean {
  if (sectionId == null) {
    return false;
  }
  return sectionId === 1103 || (sectionId >= 1200 && sectionId <= 1204);
}

export function resolveRoutedToLabel(input: {
  appStatus: string | null | undefined;
  sectionId?: number | null;
  processingPksBranchName?: string | null;
  processingBranchName?: string | null;
  submittedByUsername?: string | null;
  createdByUsername?: string | null;
  routedToLabel?: string | null;
}): string | null {
  if (input.routedToLabel) {
    return input.routedToLabel;
  }
  const status = input.appStatus;
  if (!status) {
    return null;
  }
  if (status === 'IN_QUERY') {
    const username = input.submittedByUsername || input.createdByUsername || null;
    const who = isUoWorkflowSection(input.sectionId) ? 'Submitting officer' : 'Applicant';
    return username ? `${who} (${username})` : who;
  }
  if (status === 'SUBMITTED' || status === 'IN_PROGRESS') {
    const queue = isUoWorkflowSection(input.sectionId) ? 'UO' : 'Officer';
    const branchName = input.processingPksBranchName ?? input.processingBranchName ?? null;
    return branchName ? `${queue} — ${branchName}` : queue;
  }
  return null;
}
