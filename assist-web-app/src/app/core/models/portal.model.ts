export type PortalApplicationType = 'NEW_EMPLOYER' | 'EXISTING_EMPLOYER';

export interface PortalEnrollmentRequest {
  username: string;
  email: string;
  applicationType: PortalApplicationType;
  employerCode?: string | null;
  employerName: string;
  registrationTypeId: number;
  registrationNo: string;
  addressLine1: string;
  addressLine2?: string | null;
  addressLine3?: string | null;
  stateId: number;
  cityId?: number | null;
  cityName?: string | null;
  postCode: string;
  fullName: string;
  identificationTypeId: number;
  identificationNo: string;
  phoneCallingCode?: string;
  phoneNumber: string;
  securityPhrase: string;
  draftToken?: string | null;
}

export interface PortalUser {
  id: number;
  username: string;
  email: string;
  applicationType: PortalApplicationType;
  employerId: number | null;
  employerCode: string | null;
  employerName: string | null;
  registrationTypeId: number | null;
  registrationNo: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  addressLine3: string | null;
  stateId: number | null;
  cityId: number | null;
  cityName: string | null;
  postCode: string | null;
  fullName: string | null;
  identificationTypeId: number | null;
  identificationNo: string | null;
  phoneCallingCode: string | null;
  phoneNumber: string | null;
  securityPhrase: string | null;
  enrollmentStatus: string | null;
  queryRemark: string | null;
  enrolledDate: string | null;
  linkedDate: string | null;
  loginActive: boolean;
  approvedDate: string | null;
}

export type PortalPageMode = 'new' | 'manage';

export type PortalEnrollmentResubmitRequest = Omit<PortalEnrollmentRequest, 'username'>;

export interface PortalDraftDocument {
  id: number;
  documentTypeId: number;
  documentTypeLabel: string | null;
  fileName: string;
  contentType: string | null;
  fileSize: number | null;
}
