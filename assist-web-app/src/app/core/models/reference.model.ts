export interface RefOption {
  id: number;
  label: string;
}

export interface PostcodeOption {
  postcode: string;
  label: string;
  stateId: number;
  stateName: string;
  cityId: number;
  cityName: string;
}

export interface TariffCodeSalesTypeOption {
  id: number;
  code: string;
  description: string;
}

export interface SstServiceTypeOption {
  id: number;
  code: string;
  description: string;
  accommodation: boolean;
}

export interface SupportingDocumentTypeOption {
  id: number;
  code: string;
  label: string;
  requiredForSalesTax: boolean;
}

export interface PortalDocTypeOption {
  id: number;
  code: string;
  label: string;
  requiredForPortalId: boolean;
}
