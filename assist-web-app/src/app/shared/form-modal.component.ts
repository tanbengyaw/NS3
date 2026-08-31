import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'assist-form-modal',
  standalone: true,
  template: `
    @if (open) {
      <div class="modal-root" role="presentation">
        <div class="modal-backdrop" (click)="closed.emit()"></div>
        <div class="modal-dialog" role="dialog" [attr.aria-label]="title">
          <div class="modal-content">
            <div class="modal-header">
              <button type="button" class="close" aria-label="Close" (click)="closed.emit()">&times;</button>
              <h4 class="modal-title">{{ title }}</h4>
            </div>
            <div class="modal-body">
              <ng-content />
            </div>
            @if (hasFooter) {
              <div class="modal-footer">
                <ng-content select="[modal-footer]" />
              </div>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .modal-root {
        position: fixed;
        inset: 0;
        z-index: 1050;
        display: flex;
        align-items: flex-start;
        justify-content: center;
        padding: 2rem 1rem;
        overflow-y: auto;
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.45);
      }

      .modal-dialog {
        position: relative;
        width: min(920px, 100%);
        margin: 0 auto;
        z-index: 1;
      }

      .modal-content {
        background: #fff;
        border-radius: 6px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
        max-height: calc(100vh - 4rem);
        display: flex;
        flex-direction: column;
      }

      .modal-header {
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
        padding: 1rem 2.5rem;
        border-bottom: 1px solid #e0e0e0;
      }

      .modal-title {
        margin: 0;
        font-size: 1.125rem;
        font-weight: 700;
        text-transform: uppercase;
        text-align: center;
        color: #1a237e;
      }

      .close {
        position: absolute;
        right: 0.75rem;
        top: 0.35rem;
        border: none;
        background: none;
        font-size: 1.75rem;
        line-height: 1;
        color: #5f6368;
        cursor: pointer;
      }

      .modal-body {
        padding: 1.25rem 1.5rem;
        overflow-y: auto;
      }

      .modal-footer {
        display: flex;
        justify-content: center;
        gap: 0.75rem;
        padding: 1rem 1.5rem 1.25rem;
        border-top: 1px solid #e0e0e0;
      }
    `,
  ],
})
export class FormModalComponent {
  @Input({ required: true }) open = false;
  @Input({ required: true }) title = '';
  @Input() hasFooter = true;
  @Output() closed = new EventEmitter<void>();
}
