import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../core/auth/auth.service';
import { STAFF_ROLE_OPTIONS, StaffUser } from '../core/models/staff-user.model';
import { RefOption } from '../core/models/reference.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { FormModalComponent } from '../shared/form-modal.component';
import { ConfirmDialogComponent } from '../shared/confirm-dialog.component';
import { StaffUsersService } from './staff-users.service';

@Component({
  selector: 'assist-staff-users-list',
  standalone: true,
  imports: [ReactiveFormsModule, FormModalComponent, ConfirmDialogComponent],
  templateUrl: './staff-users-list.component.html',
  styleUrl: './staff-users-list.component.scss',
})
export class StaffUsersListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly staffUsers = inject(StaffUsersService);
  private readonly referenceData = inject(ReferenceDataService);
  private readonly auth = inject(AuthService);

  readonly staffRoleOptions = STAFF_ROLE_OPTIONS;
  readonly users = signal<StaffUser[]>([]);
  readonly branches = signal<RefOption[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly modalError = signal<string | null>(null);
  readonly rolesInvalid = signal(false);
  readonly modalOpen = signal(false);
  readonly editingUserId = signal<number | null>(null);
  readonly pendingToggleUser = signal<StaffUser | null>(null);
  readonly togglingActive = signal(false);

  readonly branchLabelById = computed(() => {
    const map = new Map<number, string>();
    for (const branch of this.branches()) {
      map.set(branch.id, branch.label);
    }
    return map;
  });

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    branchId: [null as number | null],
    active: [true],
    roles: this.fb.nonNullable.group(
      Object.fromEntries(STAFF_ROLE_OPTIONS.map((role) => [role, false])) as Record<
        (typeof STAFF_ROLE_OPTIONS)[number],
        boolean
      >,
    ),
  });

  get isAdmin(): boolean {
    return this.auth.hasRole('ADMIN');
  }

  toggleActivePrompt(user: StaffUser): void {
    this.error.set(null);
    this.message.set(null);
    this.pendingToggleUser.set(user);
  }

  cancelToggleActive(): void {
    this.pendingToggleUser.set(null);
  }

  confirmToggleActive(): void {
    const user = this.pendingToggleUser();
    if (!user) {
      return;
    }
    this.togglingActive.set(true);
    this.staffUsers.updateStaffUser(user.id, { active: !user.active }).subscribe({
      next: () => {
        this.togglingActive.set(false);
        this.pendingToggleUser.set(null);
        this.message.set(user.active ? `${user.username} deactivated.` : `${user.username} reactivated.`);
        this.loadUsers();
      },
      error: (err) => {
        this.togglingActive.set(false);
        this.error.set(this.httpErrorMessage(err, 'Failed to update staff user status.'));
      },
    });
  }

  canToggleActive(user: StaffUser): boolean {
    const session = this.auth.currentSession();
    return session?.username !== user.username;
  }

  toggleActiveLabel(user: StaffUser): string {
    return user.active ? 'Deactivate' : 'Reactivate';
  }

  toggleConfirmTitle(user: StaffUser): string {
    return user.active ? 'Deactivate staff user' : 'Reactivate staff user';
  }

  toggleConfirmMessage(user: StaffUser): string {
    if (user.active) {
      return `${user.username} will no longer be able to log in. You can reactivate the account later.`;
    }
    return `${user.username} will be able to log in again with their existing password.`;
  }

  constructor() {
    this.loadBranches();
    this.loadUsers();
  }

  openCreateModal(): void {
    this.editingUserId.set(null);
    this.modalError.set(null);
    this.rolesInvalid.set(false);
    this.form.reset({
      username: '',
      email: '',
      password: '',
      branchId: null,
      active: true,
    });
    this.resetRoleChecks([]);
    this.form.controls.username.enable();
    this.form.controls.password.setValidators([Validators.required]);
    this.form.controls.password.updateValueAndValidity();
    this.modalOpen.set(true);
  }

  openEditModal(user: StaffUser): void {
    this.editingUserId.set(user.id);
    this.modalError.set(null);
    this.rolesInvalid.set(false);
    this.form.patchValue({
      username: user.username,
      email: user.email,
      password: '',
      branchId: user.branchId,
      active: user.active,
    });
    this.resetRoleChecks(user.roles);
    this.form.controls.username.disable();
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.modalError.set(null);
    this.rolesInvalid.set(false);
  }

  onRolesChanged(): void {
    if (this.selectedRoles().length) {
      this.rolesInvalid.set(false);
      if (this.modalError() === 'Select at least one role.') {
        this.modalError.set(null);
      }
    }
  }

  saveUser(): void {
    this.modalError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.modalError.set(this.resolveModalValidationError());
      return;
    }
    const roles = this.selectedRoles();
    if (!roles.length) {
      this.rolesInvalid.set(true);
      this.modalError.set('Select at least one role.');
      return;
    }
    this.rolesInvalid.set(false);

    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.message.set(null);

    const editingId = this.editingUserId();
    if (editingId == null) {
      this.staffUsers
        .createStaffUser({
          username: raw.username.trim(),
          email: raw.email.trim(),
          password: raw.password,
          branchId: raw.branchId,
          roles,
          active: raw.active,
        })
        .subscribe({
          next: () => this.onSaveSuccess('Staff user created.'),
          error: (err) => this.onSaveError(err),
        });
      return;
    }

    const updateBody: {
      email: string;
      branchId: number | null;
      roles: string[];
      active: boolean;
      password?: string;
    } = {
      email: raw.email.trim(),
      branchId: raw.branchId,
      roles,
      active: raw.active,
    };
    if (raw.password.trim()) {
      updateBody.password = raw.password;
    }
    this.staffUsers.updateStaffUser(editingId, updateBody).subscribe({
      next: () => this.onSaveSuccess('Staff user updated.'),
      error: (err) => this.onSaveError(err),
    });
  }

  branchLabel(branchId: number | null): string {
    if (branchId == null) {
      return '—';
    }
    return this.branchLabelById().get(branchId) ?? String(branchId);
  }

  formatRoles(roles: string[]): string {
    return roles.join(', ');
  }

  formatDate(value: string | null): string {
    if (!value) {
      return '—';
    }
    return value.replace('T', ' ').slice(0, 16);
  }

  showFieldError(field: 'username' | 'email' | 'password'): boolean {
    const control = this.form.controls[field];
    return control.touched && control.invalid;
  }

  fieldError(field: 'username' | 'email' | 'password'): string {
    const control = this.form.controls[field];
    if (control.hasError('required')) {
      switch (field) {
        case 'username':
          return 'Username is required.';
        case 'email':
          return 'Email is required.';
        case 'password':
          return 'Password is required.';
      }
    }
    if (field === 'email' && control.hasError('email')) {
      return 'Enter a valid email address (e.g. name@example.com).';
    }
    return 'Invalid value.';
  }

  private resolveModalValidationError(): string {
    const { username, email, password } = this.form.controls;
    if (username.hasError('required')) {
      return 'Username is required.';
    }
    if (email.hasError('required')) {
      return 'Email is required.';
    }
    if (email.hasError('email')) {
      return 'Enter a valid email address (e.g. name@example.com).';
    }
    if (password.hasError('required')) {
      return 'Password is required.';
    }
    return 'Fill in all required fields.';
  }

  private loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);
    this.staffUsers.listStaffUsers().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.httpErrorMessage(err, 'Failed to load staff users.'));
      },
    });
  }

  private loadBranches(): void {
    this.referenceData.listBranches().subscribe({
      next: (items) => this.branches.set(items),
    });
  }

  private selectedRoles(): string[] {
    const roleGroup = this.form.controls.roles.getRawValue();
    return STAFF_ROLE_OPTIONS.filter((role) => roleGroup[role]);
  }

  private resetRoleChecks(selected: string[]): void {
    const patch = Object.fromEntries(STAFF_ROLE_OPTIONS.map((role) => [role, selected.includes(role)])) as Record<
      (typeof STAFF_ROLE_OPTIONS)[number],
      boolean
    >;
    this.form.controls.roles.patchValue(patch);
  }

  private onSaveSuccess(text: string): void {
    this.saving.set(false);
    this.modalError.set(null);
    this.modalOpen.set(false);
    this.message.set(text);
    this.loadUsers();
  }

  private onSaveError(err: unknown): void {
    this.saving.set(false);
    this.modalError.set(this.httpErrorMessage(err, 'Failed to save staff user.'));
  }

  private httpErrorMessage(err: unknown, fallback: string): string {
    const httpErr = err as {
      status?: number;
      error?: { message?: string; defaultUserMessage?: string } | string;
    };
    if (httpErr.status === 403) {
      return 'Admin role required.';
    }
    const body = httpErr.error;
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (body && typeof body === 'object') {
      return body.defaultUserMessage ?? body.message ?? fallback;
    }
    return fallback;
  }
}
