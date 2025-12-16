export function getCurrentLocalDateTimeInput(): string {
  const now = new Date();
  const offsetMs = now.getTimezoneOffset() * 60000;
  const local = new Date(now.getTime() - offsetMs);
  return local.toISOString().slice(0, 16);
}

export function normalizeDateTimeInput(value?: string | null): string | null {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const [datePart, timePartRaw] = trimmed.split('T');
  if (!datePart || !timePartRaw) {
    return null;
  }
  const [hour, minute, secondsRaw] = timePartRaw.split(':');
  if (hour == null || minute == null) {
    return null;
  }
  const seconds = (secondsRaw ?? '00').split('.')[0];

  const pad = (fragment: string): string => {
    if (!fragment) {
      return '00';
    }
    return fragment.length === 1 ? `0${fragment}` : fragment.slice(0, 2);
  };

  return `${datePart}T${pad(hour)}:${pad(minute)}:${pad(seconds)}`;
}
