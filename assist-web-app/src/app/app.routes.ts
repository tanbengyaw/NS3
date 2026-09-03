import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

import { LoginComponent } from './core/auth/login.component';

import { ShellComponent } from './layout/shell.component';

import { EmployersListComponent } from './employers/employers-list.component';

import { RegistrationWizardComponent } from './registration/registration-wizard.component';
import { SalesTaxWizardComponent } from './registration/sales-tax-wizard.component';
import { RegistrationInboxComponent } from './registration/registration-inbox.component';



export const routes: Routes = [

  { path: 'login', component: LoginComponent },

  {

    path: '',

    component: ShellComponent,

    canActivate: [authGuard],

    children: [

      { path: '', pathMatch: 'full', redirectTo: 'employers' },

      { path: 'employers', component: EmployersListComponent },

      { path: 'registration', component: RegistrationWizardComponent },

      { path: 'registration/inbox', component: RegistrationInboxComponent },

      { path: 'registration/sales-tax', component: SalesTaxWizardComponent },

      { path: 'registration/sales-tax/:caseId', component: SalesTaxWizardComponent },

      { path: 'registration/:caseId', component: RegistrationWizardComponent },

    ],

  },

  { path: '**', redirectTo: 'employers' },

];


