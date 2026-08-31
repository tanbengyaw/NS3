export interface Employer {
  id: number;
  employerCode: string;
  employerName: string;
  registrationNo: string;
  serviceTypeId: number | null;
  pksBranchId: number;
  branch: boolean;
  msicId: number | null;
  contributionActive: boolean;
  operationalStatus: string | null;
  createdDate: string | null;
}

export type EmployerSearchType = 'NAME' | 'CODE' | 'BRN';
