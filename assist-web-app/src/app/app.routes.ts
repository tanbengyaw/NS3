import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

import { LoginComponent } from './core/auth/login.component';

import { ShellComponent } from './layout/shell.component';

import { EmployerDetailComponent } from './employers/employer-detail.component';
import { EmployersListComponent } from './employers/employers-list.component';

import { RegistrationWizardComponent } from './registration/registration-wizard.component';
import { SalesTaxWizardComponent } from './registration/sales-tax-wizard.component';
import { RegistrationCaseReviewComponent } from './registration/registration-case-review.component';
import { RegistrationInboxComponent } from './registration/registration-inbox.component';
import { UpdateTaxPayerSearchComponent } from './registration/update-tax-payer-search.component';
import { DiscontinueTaxSearchComponent } from './registration/discontinue-tax-search.component';
import { DiscontinueTaxCaseComponent } from './registration/discontinue-tax-case.component';
import { PortalIdRegistrationComponent } from './portal/portal-id-registration.component';
import { StaffUsersListComponent } from './admin/staff-users-list.component';
import { adminGuard } from './core/auth/admin.guard';



export const routes: Routes = [

  { path: 'login', component: LoginComponent },

  {

    path: '',

    component: ShellComponent,

    canActivate: [authGuard],

    children: [

      { path: '', pathMatch: 'full', redirectTo: 'employers' },

      { path: 'employers', component: EmployersListComponent },
      { path: 'employers/:employerId', component: EmployerDetailComponent },

      { path: 'registration', component: RegistrationWizardComponent },

      { path: 'registration/inbox', component: RegistrationInboxComponent },
      { path: 'registration/review/:caseId', component: RegistrationCaseReviewComponent },

      { path: 'registration/sales-tax', component: SalesTaxWizardComponent, data: { sstTax: 'sales' } },

      { path: 'registration/sales-tax/:caseId', component: SalesTaxWizardComponent, data: { sstTax: 'sales' } },

      { path: 'registration/tourism-tax', component: SalesTaxWizardComponent, data: { sstTax: 'tourism' } },

      { path: 'registration/tourism-tax/:caseId', component: SalesTaxWizardComponent, data: { sstTax: 'tourism' } },

      { path: 'registration/dpsp-tax', component: SalesTaxWizardComponent, data: { sstTax: 'dpsp' } },

      { path: 'registration/dpsp-tax/:caseId', component: SalesTaxWizardComponent, data: { sstTax: 'dpsp' } },

      { path: 'registration/service-tax', component: SalesTaxWizardComponent, data: { sstTax: 'service' } },

      { path: 'registration/service-tax/:caseId', component: SalesTaxWizardComponent, data: { sstTax: 'service' } },

      { path: 'registration/digital-tax', component: SalesTaxWizardComponent, data: { sstTax: 'digital' } },

      { path: 'registration/digital-tax/:caseId', component: SalesTaxWizardComponent, data: { sstTax: 'digital' } },

      { path: 'registration/update-tax-payer', component: UpdateTaxPayerSearchComponent },

      { path: 'registration/discontinue-tax', component: DiscontinueTaxSearchComponent },

      { path: 'registration/discontinue-tax/:caseId', component: DiscontinueTaxCaseComponent },

      { path: 'portal-id-registration', component: PortalIdRegistrationComponent },

      { path: 'admin/staff-users', component: StaffUsersListComponent, canActivate: [adminGuard] },

      { path: 'registration/:caseId', component: RegistrationWizardComponent },

    ],

  },

  { path: '**', redirectTo: 'employers' },

];


