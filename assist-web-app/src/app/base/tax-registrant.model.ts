export interface TaxRegistrantSummary {
  taxpayerName: string;
  sstRegistrationNo: string;
}

export interface SstNotificationItem {
  id: number;
  details: string;
  createdDate: string;
}

export interface TaxRegistrantCompanyInfo {
  businessType: string;
  businessRegistrationNo: string;
  registeredBusinessName: string;
  tradeName: string;
  premiseAddressLine1: string;
  premiseAddressLine2: string | null;
  premiseAddressLine3: string | null;
  telNo: string;
}

export interface TaxRegistrantRegistrationTypeRow {
  taxType: string;
  sstRegistrationNo: string;
  registeredDate: string | null;
  status: string;
}

export interface TaxRegistrantRegistrationInfo {
  companyInfo: TaxRegistrantCompanyInfo;
  registrationTypes: TaxRegistrantRegistrationTypeRow[];
}

export type TaxRegistrantTab = 'notifications' | 'registration-info';
