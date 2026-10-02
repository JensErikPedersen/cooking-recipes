import { expect, test } from "@playwright/test";
import { main } from "../support/pages";

// What a page shows when there is nothing yet, while it waits, at an address nothing answers,
// and once the session behind it has ended.

test("a list with nothing in it says so instead of showing an empty table", async ({ page }) => {
  await page.route("**/api/v1/units", (route) => route.fulfill({ json: [] }));

  await page.goto("/units");

  await expect(main(page)).toContainText("No units yet.");
  await expect(main(page).getByRole("table")).toHaveCount(0);
});

test("a page says it is loading while its data is on the way", async ({ page }) => {
  let release!: () => void;
  const held = new Promise<void>((resolve) => (release = resolve));
  await page.route("**/api/v1/categories", async (route) => {
    await held;
    await route.continue();
  });

  await page.goto("/categories");
  await expect(main(page).getByRole("status")).toHaveText("Loading…");

  release();
  await expect(main(page).getByRole("link", { name: "Brød", exact: true })).toBeVisible();
  await expect(main(page).getByRole("status")).toHaveCount(0);
});

test("an address no page answers shows not found, with the way back", async ({ page }) => {
  await page.goto("/no-such-page");

  await expect(page.getByRole("heading", { name: "Page not found" })).toBeVisible();
  await page.getByRole("link", { name: "Go to the start page" }).click();
  await expect(page).toHaveURL("/");
});

test("when the session ends while a page is open, the next page goes to sign in", async ({ page }) => {
  await page.goto("/recipes");
  await expect(main(page).getByRole("link", { name: "Brunkage", exact: true })).toBeVisible();

  // As after a backend restart: the browser still holds a session cookie, the backend no longer
  // knows it. proxy.ts only checks the cookie is there, so the page loads and its API call is a 401.
  await page.context().addCookies([{ name: "JSESSIONID", value: "ended", domain: "localhost", path: "/" }]);
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Categories" }).click();

  await expect(page).toHaveURL("/login");
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
});
