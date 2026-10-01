import { expect, test } from "@playwright/test";

// The browser only ever talks to the frontend origin; /api is forwarded to the backend.
// These run on the signed-in session saved by auth.setup.ts, except where stated.

test.describe("signed out", () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test("the API refuses a caller that has not signed in", async ({ request }) => {
    const response = await request.get("/api/v1/categories");

    expect(response.status()).toBe(401);
    expect(await response.json()).toMatchObject({ errorCode: 600 });
  });
});

test("the API is reachable through the frontend origin and returns the seeded categories", async ({ request }) => {
  const response = await request.get("/api/v1/categories");

  expect(response.status()).toBe(200);
  const names = (await response.json()).map((category: { name: string }) => category.name);
  expect(names).toEqual(expect.arrayContaining(["Brød", "Dessert", "Hovedret", "Kager"]));
});

test("an unknown API path is answered by the backend, not by Next.js", async ({ request }) => {
  const response = await request.get("/api/v1/no-such-resource");

  expect(response.status()).toBe(404);
  expect(await response.json()).toMatchObject({ errorCode: 10 });
});
