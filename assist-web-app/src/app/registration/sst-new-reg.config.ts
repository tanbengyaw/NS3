export type SstNewRegKind = 'sales' | 'tourism' | 'dpsp' | 'service' | 'digital';

export interface SstNewRegConfig {
  kind: SstNewRegKind;
  sectionId: number;
  routeBase: string;
  pageTitle: string;
  pageSubtitle: string;
  partBTitle: string;
  smkLabel: string;
  premisesTitle: string;
  partATitle: string;
  correspondenceTitle: string;
  registeredAddressLabel: string;
  sameAsAddressLabel: string;
  businessComDateLabel: string;
  requiresDirectors: boolean;
  requiresTariffs: boolean;
  requiresSalesBreakdown: boolean;
  /** Show manComDate / dateSaleValTaxGoods / anTotalTaxSalesVal without the full sales breakdown (service tax). */
  showPartBDates: boolean;
  /** Require at least one service type code (service tax). */
  requiresServiceCodes: boolean;
  /** Show the "types of digital service" checkboxes + achieving-value date + total value (digital tax). */
  requiresDigitalServiceTypes: boolean;
  /** Label the director section "Authorised personnel" and capture a telephone number (digital tax). */
  requiresAuthorisedPersonnel: boolean;
  requiresPremises: boolean;
  premisesOnForm1: boolean;
  premisesOnForm2: boolean;
  showSalesPartAExtras: boolean;
  showTourismPartA: boolean;
  showBusinessEntityType: boolean;
  showWebsiteAddress: boolean;
  requireForm1Email: boolean;
  requireForm1ContactPerson: boolean;
}

export const SST_NEW_REG_CONFIGS: Record<SstNewRegKind, SstNewRegConfig> = {
  service: {
    kind: 'service',
    sectionId: 1105,
    routeBase: '/registration/service-tax',
    pageTitle: 'Service tax new registration',
    pageSubtitle: 'New service tax registration — Form 1 (Part A) → Form 2 (dates & documents) → Preview',
    partBTitle: 'Part B — service tax particulars',
    smkLabel: 'Service tax SMK',
    premisesTitle: 'Premises',
    partATitle: 'Business registration',
    correspondenceTitle: 'Premise address',
    registeredAddressLabel: 'Business correspondence address — line 1',
    sameAsAddressLabel: 'Premise Address Same As Business Correspondence Address',
    businessComDateLabel: 'Business commencement date',
    requiresDirectors: true,
    requiresTariffs: false,
    requiresSalesBreakdown: false,
    showPartBDates: true,
    requiresServiceCodes: true,
    requiresDigitalServiceTypes: false,
    requiresAuthorisedPersonnel: false,
    requiresPremises: false,
    premisesOnForm1: false,
    premisesOnForm2: false,
    showSalesPartAExtras: true,
    showTourismPartA: false,
    showBusinessEntityType: true,
    showWebsiteAddress: false,
    requireForm1Email: true,
    requireForm1ContactPerson: false,
  },
  sales: {
    kind: 'sales',
    sectionId: 1100,
    routeBase: '/registration/sales-tax',
    pageTitle: 'Sales tax new registration',
    pageSubtitle: 'New sales tax registration — mirrors ASSIST Form 1 → Form 2 → Preview',
    partBTitle: 'Part B — sales tax particulars',
    smkLabel: 'Sales tax SMK',
    premisesTitle: 'Other place of manufacturing address',
    partATitle: 'Business registration',
    correspondenceTitle: 'Premise address',
    registeredAddressLabel: 'Business correspondence address — line 1',
    sameAsAddressLabel: 'Premise Address Same As Business Correspondence Address',
    businessComDateLabel: 'Business commencement date',
    requiresDirectors: true,
    requiresTariffs: true,
    requiresSalesBreakdown: true,
    showPartBDates: true,
    requiresServiceCodes: false,
    requiresDigitalServiceTypes: false,
    requiresAuthorisedPersonnel: false,
    requiresPremises: false,
    premisesOnForm1: true,
    premisesOnForm2: false,
    showSalesPartAExtras: true,
    showTourismPartA: false,
    showBusinessEntityType: true,
    showWebsiteAddress: false,
    requireForm1Email: true,
    requireForm1ContactPerson: false,
  },
  tourism: {
    kind: 'tourism',
    sectionId: 1101,
    routeBase: '/registration/tourism-tax',
    pageTitle: 'Tourism tax new registration',
    pageSubtitle: 'New tourism tax registration — Form 1 (Part A) → Form 2 (dates & documents) → Preview',
    partBTitle: 'Part B — financial year and commencement',
    smkLabel: 'Tourism tax SMK',
    premisesTitle: 'Accommodation premises',
    partATitle: 'Part A — details of business',
    correspondenceTitle: 'Correspondence address',
    registeredAddressLabel: 'Registered business address — line 1',
    sameAsAddressLabel: 'Correspondence address same as registered business address',
    businessComDateLabel: 'Date begin operation in Malaysia',
    requiresDirectors: false,
    requiresTariffs: false,
    requiresSalesBreakdown: false,
    showPartBDates: false,
    requiresServiceCodes: false,
    requiresDigitalServiceTypes: false,
    requiresAuthorisedPersonnel: false,
    requiresPremises: false,
    premisesOnForm1: false,
    premisesOnForm2: false,
    showSalesPartAExtras: false,
    showTourismPartA: true,
    showBusinessEntityType: true,
    showWebsiteAddress: false,
    requireForm1Email: false,
    requireForm1ContactPerson: true,
  },
  dpsp: {
    kind: 'dpsp',
    sectionId: 1104,
    routeBase: '/registration/dpsp-tax',
    pageTitle: 'DPSP tax new registration',
    pageSubtitle: 'Digital Platform Service Provider — Form 1 (Part A) → Form 2 (dates & documents) → Preview',
    partBTitle: 'Part B — details of platform digital service provider',
    smkLabel: 'DPSP tax SMK',
    premisesTitle: 'Premises',
    partATitle: 'Part A — details of business',
    correspondenceTitle: 'Correspondence address',
    registeredAddressLabel: 'Registered business address — line 1',
    sameAsAddressLabel: 'Correspondence address same as registered business address',
    businessComDateLabel: 'Date begin operation in Malaysia',
    requiresDirectors: false,
    requiresTariffs: false,
    requiresSalesBreakdown: false,
    showPartBDates: false,
    requiresServiceCodes: false,
    requiresDigitalServiceTypes: false,
    requiresAuthorisedPersonnel: false,
    requiresPremises: false,
    premisesOnForm1: false,
    premisesOnForm2: false,
    showSalesPartAExtras: false,
    showTourismPartA: false,
    showBusinessEntityType: false,
    showWebsiteAddress: true,
    requireForm1Email: true,
    requireForm1ContactPerson: false,
  },
  digital: {
    kind: 'digital',
    sectionId: 1102,
    routeBase: '/registration/digital-tax',
    pageTitle: 'Digital tax new registration',
    pageSubtitle: 'Application for digital tax — Form 1 (Part A) → Form 2 (dates & documents) → Preview',
    partBTitle: 'Part B — digital service details',
    smkLabel: 'Digital tax SMK',
    premisesTitle: 'Premises',
    partATitle: 'Part A — business particulars',
    correspondenceTitle: 'Correspondence address',
    registeredAddressLabel: 'Registered business address — line 1',
    sameAsAddressLabel: 'Correspondence address same as registered business address',
    businessComDateLabel: 'Date begin operation in Malaysia',
    requiresDirectors: true,
    requiresTariffs: false,
    requiresSalesBreakdown: false,
    showPartBDates: false,
    requiresServiceCodes: false,
    requiresDigitalServiceTypes: true,
    requiresAuthorisedPersonnel: true,
    requiresPremises: false,
    premisesOnForm1: false,
    premisesOnForm2: false,
    showSalesPartAExtras: false,
    showTourismPartA: false,
    showBusinessEntityType: false,
    showWebsiteAddress: true,
    requireForm1Email: true,
    requireForm1ContactPerson: false,
  },
};

export function resolveSstNewRegConfig(kind: unknown): SstNewRegConfig {
  if (kind === 'tourism') {
    return SST_NEW_REG_CONFIGS.tourism;
  }
  if (kind === 'dpsp') {
    return SST_NEW_REG_CONFIGS.dpsp;
  }
  if (kind === 'service') {
    return SST_NEW_REG_CONFIGS.service;
  }
  if (kind === 'digital') {
    return SST_NEW_REG_CONFIGS.digital;
  }
  return SST_NEW_REG_CONFIGS.sales;
}

export function isSstNewRegSection(sectionId: number | null): boolean {
  return (
    sectionId === 1100 ||
    sectionId === 1101 ||
    sectionId === 1102 ||
    sectionId === 1104 ||
    sectionId === 1105
  );
}
