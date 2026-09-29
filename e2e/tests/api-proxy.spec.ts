import { expect, test } from "@playwright/test";

// The browser only ever talks to the frontend origin; /api is forwarded to the backend.

test("the API is reachable through the frontend origin", async ({ request }) => {
  const response = await request.get("/api/v1/categories");

  expect(response.status()).toBe(200);
  expect(Array.isArray(await response.json())).toBe(true);
});

test("an unknown API path is answered by the backend, not by Next.js", async ({ request }) => {
  const response = await request.get("/api/v1/no-such-resource");

  expect(response.status()).toBe(404);
  expect(await response.json()).toMatchObject({ errorCode: 10 });
});
