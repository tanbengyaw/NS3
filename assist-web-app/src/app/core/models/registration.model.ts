export interface CommandProcessingResult {
  resourceId: number | null;
  resourceIdentifier: string | null;
  officeId: number | null;
  changes: Record<string, unknown> | null;
}

export interface ContactLine {
  head: string;
  back: string;
}

export interface RegistrationCase {
  id: number;
  caseRefNo: string;
  appStatus: string;
  appStatusReason: string | null;
  sectionId: number | null;
  sectionCode: string | null;
  employerName: string;
  registrationNo: string;
  businessEntityTypeId: number | null;
  email: string | null;
  phone: string | null;
  contactPhones: string | null;
  contactFaxes: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  addressLine3: string | null;
  stateId: number | null;
  cityId: number | null;
  cityName: string | null;
  postCode: string | null;
  corrAddressLine1: string | null;
  corrAddressLine2: string | null;
  corrAddressLine3: string | null;
  corrPostCode: string | null;
  corrStateId: number | null;
  corrCityId: number | null;
  corrCityName: string | null;
  serviceTypeId: number | null;
  pksBranchId: number | null;
  branch: boolean;
  msicId: number | null;
  methodContributionPaymentId: number | null;
  employerId: number | null;
}

export interface RegistrationCaseSummary {
  id: number;
  caseRefNo: string;
  appStatus: string;
  sectionId: number | null;
  sectionCode: string | null;
  employerName: string;
  registrationNo: string;
  submissionDate: string | null;
  createdDate: string | null;
}

export interface TempEmployee {
  id: number;
  caseId: number;
  employeeName: string;
  identificationNo: string;
  employmentStartDate: string;
  nationalityId: number | null;
}

export interface TempDirectorOwner {
  id: number;
  caseId: number;
  name: string;
  identificationTypeId: number | null;
  identificationNo: string;
  email: string | null;
  designation: string | null;
}

export interface TempPremises {
  id: number;
  caseId: number;
  name: string;
  addressLine: string;
  addressLine2: string | null;
  addressLine3: string | null;
  postCode: string | null;
  cityName: string | null;
  stateName: string | null;
}

export interface TempSstTariffCode {
  id: number;
  tariffCodeSalesTypeId: number;
  tariffCode: string | null;
  tariffDescription: string | null;
  contractTypeId: number;
  finishedGoods: string | null;
}

export interface TempSstContactPerson {
  id: number;
  name: string;
  email: string;
}

export interface TempSstInfo {
  id: number | null;
  caseId: number;
  tradeName: string | null;
  tourTaxRegNo: string | null;
  inTaxRefNo: string | null;
  cusAudRefNo: string | null;
  preRegNo: string | null;
  preRegName: string | null;
  dateOfReplacement: string | null;
  manComDate: string | null;
  dateSaleValTaxGoods: string | null;
  finYrEndMon: number | null;
  anTotalTaxSalesVal: number | null;
  businessComDate: string | null;
  localSales: number | null;
  exportSales: number | null;
  salesToDesignArea: number | null;
  othersSales: number | null;
  subContractWork: boolean;
  declareTrue: boolean;
  declareDate: string | null;
  applicantName: string | null;
  identityCard: string | null;
  designation: string | null;
  applicantEmail: string | null;
  applicantTelNo: string | null;
  directors: TempDirectorOwner[];
  premises: TempPremises[];
  tariffCodes: TempSstTariffCode[];
  contactPersons: TempSstContactPerson[];
  supportingDocuments: TempSstSupportingDocument[];
}

export interface TempSstSupportingDocument {
  id: number;
  documentTypeId: number;
  documentTypeLabel: string | null;
  fileName: string;
  contentType: string | null;
  fileSize: number | null;
  uploadedDate: string | null;
}

export interface UpsertSstInfoRequest {
  tradeName?: string;
  tourTaxRegNo?: string;
  inTaxRefNo?: string;
  cusAudRefNo?: string;
  preRegNo?: string;
  preRegName?: string;
  dateOfReplacement?: string;
  manComDate?: string;
  dateSaleValTaxGoods?: string;
  finYrEndMon?: number;
  anTotalTaxSalesVal?: number;
  businessComDate?: string;
  localSales?: number;
  exportSales?: number;
  salesToDesignArea?: number;
  othersSales?: number;
  subContractWork?: boolean;
  declareTrue?: boolean;
  declareDate?: string | null;
  applicantName?: string;
  identityCard?: string;
  designation?: string;
  applicantEmail?: string;
  applicantTelNo?: string;
}

export interface CreateContactPersonRequest {
  name: string;
  email: string;
}

export interface CreateDirectorRequest {
  name: string;
  identificationTypeId?: number;
  identificationNo: string;
  email?: string;
  designation?: string;
}

export interface CreatePremisesRequest {
  name: string;
  addressLine: string;
  addressLine2?: string;
  addressLine3?: string;
  postCode?: string;
  cityName?: string;
  stateName?: string;
}

export interface CreateTariffCodeRequest {
  tariffCodeSalesTypeId: number;
  contractTypeId: number;
  finishedGoods?: string;
}

export interface TaxPayerDirectorProfile {
  name: string;
  identificationTypeId: number | null;
  identificationNo: string;
  email: string | null;
  designation: string | null;
}

export interface TaxPayerPremisesProfile {
  name: string;
  addressLine: string;
  addressLine2: string | null;
  addressLine3: string | null;
  postCode: string | null;
  cityName: string | null;
  stateName: string | null;
}

export interface TaxPayerRegistrationProfile {
  employerId: number;
  employerCode: string;
  employerName: string;
  registrationNo: string;
  businessEntityTypeId: number | null;
  msicId: number | null;
  serviceTypeId: number | null;
  pksBranchId: number | null;
  email: string | null;
  phone: string | null;
  directors: TaxPayerDirectorProfile[];
  premises: TaxPayerPremisesProfile[];
}

export interface CreateRegistrationCaseRequest {
  sectionId?: number;
  dataSourceId?: number;
  employerName: string;
  registrationNo: string;
  serviceTypeId: number;
  pksBranchId: number;
  postCode: string;
  businessEntityTypeId?: number;
  msicId?: number;
  isBranch?: boolean;
  methodContributionPaymentId?: number;
  email?: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  addressLine3?: string;
  stateId?: number;
  cityId?: number;
  cityName?: string;
}

export interface UpdateRegistrationCaseRequest {
  businessEntityTypeId?: number;
  email?: string;
  phone?: string;
  contactPhones?: string;
  contactFaxes?: string;
  addressLine1?: string;
  addressLine2?: string;
  addressLine3?: string;
  stateId?: number;
  cityId?: number;
  cityName?: string;
  employerName?: string;
  serviceTypeId?: number;
  pksBranchId?: number;
  postCode?: string;
  msicId?: number;
  isBranch?: boolean;
  methodContributionPaymentId?: number;
  corrAddressLine1?: string;
  corrAddressLine2?: string;
  corrAddressLine3?: string;
  corrPostCode?: string;
  corrStateId?: number;
  corrCityId?: number;
  corrCityName?: string;
}

export interface CreateEmployeeRequest {
  employeeName: string;
  identificationNo: string;
  employmentStartDate: string;
}
