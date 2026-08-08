/** En dev, toujours passer par le proxy Vite (évite CORS). */
export function getApiBaseUrl(): string {
  if (import.meta.env.DEV) {
    return '';
  }
  return import.meta.env.VITE_API_URL || '';
}
