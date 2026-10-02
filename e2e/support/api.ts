import type { Page } from "@playwright/test";

// Housekeeping only, never part of what a test proves: tests act through the UI, and this removes
// what they created so the stack's database does not fill up with test data.

/** Deletes an entity through the API, on the page's own session. One already gone is fine. */
export async function deleteById(page: Page, collection: string, id: string) {
  // Any API response sets the XSRF-TOKEN cookie the delete has to send back.
  await page.request.get("/api/v1/auth/me");
  const token = (await page.context().cookies()).find((cookie) => cookie.name === "XSRF-TOKEN")!.value;
  const response = await page.request.delete(`/api/v1/${collection}/${id}`, {
    headers: { "X-XSRF-TOKEN": decodeURIComponent(token) },
  });
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`Cleanup could not delete ${collection}/${id}: ${response.status()}`);
  }
}
