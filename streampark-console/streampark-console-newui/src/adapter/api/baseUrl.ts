export function baseUrl() {
  if (import.meta.env.VITE_APP_ENV) {
    return `${location.protocol}//${location.host}`;
  }

  const baseApi = import.meta.env.VITE_APP_BASE_API;

  return baseApi ? baseApi : '';
}
