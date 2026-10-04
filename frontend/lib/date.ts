const pad = (value: number) => String(value).padStart(2, "0");

/**
 * A date from the API - ISO-8601 with its offset - as "yyyy-MM-dd HH:mm" in the browser's own time
 * zone. Pages render in the browser, after their data has loaded, so this is the viewer's zone.
 */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}`
  );
}
