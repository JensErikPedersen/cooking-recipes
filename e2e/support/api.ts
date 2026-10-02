import type { Page } from "@playwright/test";

// Housekeeping and setup only, never part of what a test proves: tests act through the UI. This
// removes what they created, so the stack's database does not fill up with test data, and creates
// what the UI cannot create yet.

/** The CSRF header for a write on the page's own session. */
async function csrfHeader(page: Page) {
  // Any API response sets the XSRF-TOKEN cookie a write has to send back.
  await page.request.get("/api/v1/auth/me");
  const token = (await page.context().cookies()).find((cookie) => cookie.name === "XSRF-TOKEN")!.value;
  return { "X-XSRF-TOKEN": decodeURIComponent(token) };
}

/** Deletes an entity through the API, on the page's own session. One already gone is fine. */
export async function deleteById(page: Page, collection: string, id: string) {
  const response = await page.request.delete(`/api/v1/${collection}/${id}`, { headers: await csrfHeader(page) });
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Cleanup could not delete ${collection}/${id}: ${response.status()}`);
  }
}

/** Creates an entity through the API, on the page's own session, and returns its id. */
export async function createById(page: Page, collection: string, body: unknown) {
  const response = await page.request.post(`/api/v1/${collection}`, { headers: await csrfHeader(page), data: body });
  if (!response.ok()) {
    throw new Error(`Setup could not create in ${collection}: ${response.status()} ${await response.text()}`);
  }
  return (await response.json()).id as string;
}
