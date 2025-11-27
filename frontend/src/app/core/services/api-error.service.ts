import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
} from '@angular/forms';

import { MensajeService } from '../../services/mensaje.service';

type BackendErrors = Record<string, string[]>;

interface HandleErrorOptions {
  form?: FormGroup;
  contextMessage?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiErrorService {
  private mensajeService = inject(MensajeService);

  handle(error: unknown, options: HandleErrorOptions = {}): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.mensajeService.error(
        options.contextMessage ?? 'Ocurrió un error inesperado.'
      );
      return;
    }

    if (options.form) {
      this.clearFormErrors(options.form);
    }

    switch (error.status) {
      case 400:
        this.handleBadRequest(error, options);
        return;
      case 404:
        this.mensajeService.error(this.resolveMessage(error) ?? 'El recurso solicitado no existe.');
        return;
      default:
        if (error.status >= 500) {
          this.mensajeService.error(
            this.resolveMessage(error) ?? 'Se produjo un error en el sistema.'
          );
        } else {
          this.mensajeService.error(
            this.resolveMessage(error) ??
              options.contextMessage ??
              'Ocurrió un error inesperado.'
          );
        }
        return;
    }
  }

  clearFormErrors(control: AbstractControl): void {
    if (control instanceof FormGroup || control instanceof FormArray) {
      Object.values(control.controls).forEach((childControl) =>
        this.clearFormErrors(childControl)
      );
    }

    const errors = control.errors;
    if (!errors) {
      return;
    }

    if ('backend' in errors) {
      const { backend, ...rest } = errors;
      const nextErrors = Object.keys(rest).length > 0 ? rest : null;
      if (control instanceof FormControl) {
        control.setErrors(nextErrors);
      } else {
        control.setErrors(nextErrors);
      }
    }
  }

  private handleBadRequest(error: HttpErrorResponse, options: HandleErrorOptions): void {
    const payload = this.normalizePayload(error.error);
    const backendErrors = this.extractFieldErrors(payload);
    const generalMessages = this.extractGeneralMessages(payload);

    if (options.form) {
      if (Object.keys(backendErrors).length > 0) {
        this.applyFieldErrors(options.form, backendErrors);
      }

      if (generalMessages.length > 0) {
        options.form.setErrors({
          ...(options.form.errors ?? {}),
          backend: generalMessages.join(' '),
        });
      }

      if (Object.keys(backendErrors).length > 0 || generalMessages.length > 0) {
        return;
      }
    }

    const message =
      this.resolveMessage(error) ?? options.contextMessage ?? 'Solicitud inválida.';
    this.mensajeService.error(message);
  }

  private applyFieldErrors(form: FormGroup, errors: BackendErrors): void {
    Object.entries(errors).forEach(([path, messages]) => {
      const control = this.findControl(form, path);
      if (!control) {
        return;
      }

      const currentErrors = control.errors ?? {};
      control.setErrors({
        ...currentErrors,
        backend: messages.join(' '),
      });
      control.markAsTouched();
      control.updateValueAndValidity({ onlySelf: true, emitEvent: false });
    });
  }

  private findControl(form: FormGroup, path: string): AbstractControl | null {
    if (!path) {
      return form;
    }

    const segments = path
      .replace(/\[(\d+)\]/g, '.$1')
      .split('.')
      .map((segment) => segment.trim())
      .filter((segment) => segment.length > 0)
      .map((segment) =>
        segment.match(/^\d+$/) ? Number.parseInt(segment, 10) : segment
      );

    return form.get(segments as (string | number)[]) ?? null;
  }

  private extractFieldErrors(payload: unknown): BackendErrors {
    if (!payload || typeof payload !== 'object') {
      return {};
    }

    const data = payload as Record<string, unknown>;
    const fieldErrors: BackendErrors = {};

    const rawErrors = data['errors'];

    if (Array.isArray(rawErrors)) {
      rawErrors.forEach((errorEntry) => {
        if (errorEntry && typeof errorEntry === 'object') {
          const entry = errorEntry as Record<string, unknown>;
          const field = this.normalizeFieldName(
            (entry['field'] as string | undefined) ?? (entry['campo'] as string | undefined)
          );
          const message = this.normalizeMessage(
            (entry['message'] as string | undefined) ??
              (entry['defaultMessage'] as string | undefined) ??
              (entry['mensaje'] as string | undefined)
          );
          if (field && message) {
            fieldErrors[field] = [...(fieldErrors[field] ?? []), message];
          }
        } else if (typeof errorEntry === 'string') {
          fieldErrors[''] = [...(fieldErrors[''] ?? []), errorEntry];
        }
      });
    } else if (rawErrors && typeof rawErrors === 'object') {
      const errorsObj = rawErrors as Record<string, unknown>;
      Object.entries(errorsObj).forEach(([key, value]) => {
        const field = this.normalizeFieldName(key);
        const messages = Array.isArray(value) ? value : [value];
        const normalizedMessages = messages
          .map((msg) => this.normalizeMessage(msg))
          .filter((msg): msg is string => Boolean(msg));

        if (field && normalizedMessages.length > 0) {
          fieldErrors[field] = normalizedMessages;
        }
      });
    }

    return fieldErrors;
  }

  private extractGeneralMessages(payload: unknown): string[] {
    if (!payload || typeof payload !== 'object') {
      return [];
    }

    const data = payload as Record<string, unknown>;
    const messages: string[] = [];

    const rawErrors = data['errors'];

    if (Array.isArray(rawErrors)) {
      rawErrors.forEach((entry) => {
        if (entry && typeof entry === 'object') {
          const message = this.normalizeMessage(
            (entry as Record<string, unknown>)['message'] ??
              (entry as Record<string, unknown>)['mensaje']
          );
          if (!('field' in (entry as Record<string, unknown>)) && message) {
            messages.push(message);
          }
        }
      });
    }

    const message =
      this.normalizeMessage(
        (data['message'] as string | undefined) ??
          (data['mensaje'] as string | undefined) ??
          (data['error'] as string | undefined)
      ) ?? null;
    if (message) {
      messages.push(message);
    }

    return messages;
  }

  private resolveMessage(error: HttpErrorResponse): string | null {
    const payload = this.normalizePayload(error.error);

    if (payload && typeof payload === 'object') {
      const data = payload as Record<string, unknown>;
      const candidate =
        this.normalizeMessage(data['message']) ??
        this.normalizeMessage(data['mensaje']) ??
        this.normalizeMessage(data['error']);
      if (candidate) {
        return candidate;
      }
    }

    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }

    return null;
  }

  private normalizePayload(payload: unknown): unknown {
    if (typeof payload === 'string') {
      try {
        return JSON.parse(payload);
      } catch {
        return { message: payload };
      }
    }
    return payload;
  }

  private normalizeFieldName(field: unknown): string {
    if (typeof field !== 'string') {
      return '';
    }

    return field.trim();
  }

  private normalizeMessage(message: unknown): string | null {
    if (typeof message === 'string') {
      const trimmed = message.trim();
      return trimmed.length > 0 ? trimmed : null;
    }

    if (message != null) {
      return String(message);
    }

    return null;
  }
}
