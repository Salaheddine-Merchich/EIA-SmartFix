export function formatNumber(value: number): string {
  return new Intl.NumberFormat('fr-FR').format(value);
}

export function formatDurationMinutes(minutes?: number | null): string {
  if (minutes == null || Number.isNaN(minutes)) return 'N/A';
  return `${Math.round(minutes)} min`;
}

export function formatDurationHours(hours?: number | null): string {
  if (hours == null || Number.isNaN(hours)) return 'N/A';
  return `${hours.toFixed(1)} h`;
}

export function formatDateTime(value: string | Date): string {
  return new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(typeof value === 'string' ? new Date(value) : value);
}

export function formatTime(value: Date = new Date()): string {
  return new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(value);
}

export function formatRelativeTime(value: string): string {
  const date = new Date(value);
  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);

  if (diffMinutes < 1) return 'À l\'instant';
  if (diffMinutes < 60) return `Il y a ${diffMinutes} min`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `Il y a ${diffHours} h`;

  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function greetingName(fullName?: string): string {
  if (!fullName?.trim()) return 'Bonjour';
  const first = fullName.trim().split(/\s+/)[0];
  return `Bonjour ${first}`;
}
