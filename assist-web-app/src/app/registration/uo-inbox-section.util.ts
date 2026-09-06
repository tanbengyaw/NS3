import { registrationSectionLabel } from './registration-section.util';

/** ASSIST sections routed to UO on submit (update tax 1200–1204, discontinue 1103). */
export const UO_WORKFLOW_SECTION_IDS = [1103, 1200, 1201, 1202, 1203, 1204] as const;

export type InboxSectionFilter = 'ALL' | 'UO' | `${number}`;

export interface InboxSectionFilterOption {
  value: InboxSectionFilter;
  label: string;
}

export const UO_INBOX_SECTION_FILTERS: InboxSectionFilterOption[] = [
  { value: 'UO', label: 'All UO queue' },
  { value: '1103', label: registrationSectionLabel(1103, null) },
  { value: '1200', label: registrationSectionLabel(1200, null) },
  { value: '1201', label: registrationSectionLabel(1201, null) },
  { value: '1202', label: registrationSectionLabel(1202, null) },
  { value: '1203', label: registrationSectionLabel(1203, null) },
  { value: '1204', label: registrationSectionLabel(1204, null) },
];

export function inboxSectionFilterParams(filter: InboxSectionFilter): {
  sectionId?: number;
  sectionIds?: number[];
} {
  if (filter === 'ALL') {
    return {};
  }
  if (filter === 'UO') {
    return { sectionIds: [...UO_WORKFLOW_SECTION_IDS] };
  }
  return { sectionId: Number(filter) };
}
