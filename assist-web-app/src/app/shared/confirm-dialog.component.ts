import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'assist-confirm-dialog',
  standalone: true,
  template: `
    @if (open) {
      <div class="modal-root" role="presentation">
        <div class="modal-backdrop" (click)="cancelled.emit()"></div>
        <div class="modal-dialog" role="dialog" [attr.aria-label]="title">
          <div class="modal-content">
            <div class="modal-header">
              <h4 class="modal-title">{{ title }}</h4>
            </div>
            <div class="modal-body">{{ message }}</div>
            <div class="modal-footer">
              <button type="button" class="btn cancel" [disabled]="busy" (click)="cancelled.emit()">Cancel</button>
              <button type="button" class="btn confirm" [disabled]="busy" (click)="confirmed.emit()">Confirm</button>
            </div>
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
        z-index: 1060;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 1rem;
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.45);
      }

      .modal-dialog {
        position: relative;
        width: min(480px, 100%);
        z-index: 1;
      }

      .modal-content {
        background: #fff;
        border-radius: 6px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
        overflow: hidden;
      }

      .modal-header {
        padding: 1rem 1.5rem 0.5rem;
      }

      .modal-title {
        margin: 0;
        font-size: 1.125rem;
        font-weight: 700;
        text-transform: uppercase;
        text-align: center;
        color: #1a237e;
      }

      .modal-body {
        padding: 1rem 1.5rem 1.25rem;
        text-align: center;
        font-size: 1.125rem;
        color: #3c4043;
      }

      .modal-footer {
        display: flex;
        justify-content: center;
        gap: 0.75rem;
        padding: 0 1.5rem 1.25rem;
      }

      .btn {
        min-width: 7rem;
        padding: 0.55rem 1.25rem;
        border-radius: 4px;
        font: inherit;
        font-weight: 600;
        cursor: pointer;
      }

      .btn:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }

      .cancel {
        border: 1px solid #9aa0a6;
        background: #fff;
        color: #3c4043;
      }

      .confirm {
        border: 1px solid #1a237e;
        background: #1a237e;
        color: #fff;
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  @Input({ required: true }) open = false;
  @Input() title = 'Confirmation';
  @Input() message = 'Proceed to delete?';
  @Input() busy = false;
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
