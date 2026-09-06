const SECTION_LABELS: Record<number, string> = {
  200: 'SOCSO new registration',
  1100: 'Sales tax new registration',
  1101: 'Tourism tax registration',
  1102: 'Digital tax registration',
  1103: 'Discontinue tax',
  1104: 'DPSP tax registration',
  1200: 'Update service tax',
  1201: 'Update sales tax',
  1202: 'Update tourism tax',
  1203: 'Update digital tax',
  1204: 'Update DPSP tax',
  1206: 'Incomplete sales tax',
};

export function registrationSectionLabel(sectionId: number | null, sectionCode: string | null): string {
  if (sectionId != null && SECTION_LABELS[sectionId]) {
    return SECTION_LABELS[sectionId];
  }
  if (sectionCode) {
    return sectionCode.replace(/^REG_/, '').replaceAll('_', ' ').toLowerCase();
  }
  return 'Registration case';
}
