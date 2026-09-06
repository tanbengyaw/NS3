export interface StaffUser {
  id: number;
  username: string;
  email: string;
  branchId: number | null;
  roles: string[];
  active: boolean;
  createdDate: string;
  updatedDate: string | null;
}

export interface CreateStaffUserRequest {
  username: string;
  email: string;
  password: string;
  branchId?: number | null;
  roles: string[];
  active?: boolean;
}

export interface UpdateStaffUserRequest {
  email?: string;
  password?: string;
  branchId?: number | null;
  roles?: string[];
  active?: boolean;
}

export const STAFF_ROLE_OPTIONS = ['ADMIN', 'OFFICER', 'RO', 'UO', 'PKR_BO', 'EMPLOYER'] as const;

export type StaffRoleOption = (typeof STAFF_ROLE_OPTIONS)[number];
