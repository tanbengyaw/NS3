const SECTION_SOCSO = 200;
const SECTION_SALES_TAX = 1100;
const SECTION_TOURISM_TAX = 1101;
const SECTION_DIGITAL_TAX = 1102;
const SECTION_DPSP_TAX = 1104;
const SECTION_SERVICE_TAX = 1105;
const SECTION_UPDATE_SERVICE_TAX = 1200;
const SECTION_UPDATE_SALES_TAX = 1201;
const SECTION_UPDATE_TOURISM_TAX = 1202;
const SECTION_UPDATE_DIGITAL_TAX = 1203;
const SECTION_UPDATE_DPSP_TAX = 1204;

export function registrationCaseReviewLink(caseId: number): string[] {
  return ['/registration/review', String(caseId)];
}

export function registrationCaseWizardLink(sectionId: number | null, caseId: number): string[] | null {
  if (sectionId === SECTION_SALES_TAX) {
    return ['/registration/sales-tax', String(caseId)];
  }
  if (sectionId === SECTION_TOURISM_TAX) {
    return ['/registration/tourism-tax', String(caseId)];
  }
  if (sectionId === SECTION_DPSP_TAX) {
    return ['/registration/dpsp-tax', String(caseId)];
  }
  if (sectionId === SECTION_SERVICE_TAX) {
    return ['/registration/service-tax', String(caseId)];
  }
  if (sectionId === SECTION_DIGITAL_TAX) {
    return ['/registration/digital-tax', String(caseId)];
  }
  if (sectionId === SECTION_UPDATE_SALES_TAX) {
    return ['/registration/sales-tax', String(caseId)];
  }
  if (sectionId === SECTION_UPDATE_TOURISM_TAX) {
    return ['/registration/tourism-tax', String(caseId)];
  }
  if (sectionId === SECTION_UPDATE_DPSP_TAX) {
    return ['/registration/dpsp-tax', String(caseId)];
  }
  if (sectionId === SECTION_UPDATE_SERVICE_TAX) {
    return ['/registration/service-tax', String(caseId)];
  }
  if (sectionId === SECTION_UPDATE_DIGITAL_TAX) {
    return ['/registration/digital-tax', String(caseId)];
  }
  if (sectionId === SECTION_SOCSO || sectionId == null) {
    return ['/registration', String(caseId)];
  }
  return null;
}

export function registrationCaseWizardLabel(sectionId: number | null): string | null {
  if (sectionId === SECTION_SALES_TAX) {
    return 'Open sales tax wizard';
  }
  if (sectionId === SECTION_TOURISM_TAX) {
    return 'Open tourism tax wizard';
  }
  if (sectionId === SECTION_DPSP_TAX) {
    return 'Open DPSP tax wizard';
  }
  if (sectionId === SECTION_SERVICE_TAX) {
    return 'Open service tax wizard';
  }
  if (sectionId === SECTION_DIGITAL_TAX) {
    return 'Open digital tax wizard';
  }
  if (sectionId === SECTION_UPDATE_SALES_TAX) {
    return 'Open sales tax update wizard';
  }
  if (sectionId === SECTION_UPDATE_TOURISM_TAX) {
    return 'Open tourism tax update wizard';
  }
  if (sectionId === SECTION_UPDATE_DPSP_TAX) {
    return 'Open DPSP tax update wizard';
  }
  if (sectionId === SECTION_UPDATE_SERVICE_TAX) {
    return 'Open service tax update wizard';
  }
  if (sectionId === SECTION_UPDATE_DIGITAL_TAX) {
    return 'Open digital tax update wizard';
  }
  if (sectionId === SECTION_SOCSO || sectionId == null) {
    return 'Open SOCSO registration wizard';
  }
  return null;
}
