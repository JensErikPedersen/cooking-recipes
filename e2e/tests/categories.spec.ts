import { expect, test, type Page } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { deleteCategory } from "../support/api";
import { USERNAME } from "../support/auth";

// Seeded by db.changelog_1.1.xml. Each one is used by a recipe, so no test may delete it.
const SEEDED = ["Brød", "Dessert", "Hovedret", "Kager"];
const DESSERT_ID = "913a5159-3717-4b9d-a290-0158d31ea8aa";
const READ_VIEW = /\/categories\/[0-9a-f-]{36}$/;

// Categories a test created, removed after it whether it passed or not.
const created: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of created.splice(0)) {
    await deleteCategory(page, id);
  }
});

const uniqueName = () => `E2E ${randomUUID().slice(0, 8)}`;

const main = (page: Page) => page.getByRole("main");

async function openFromMenu(page: Page) {
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Categories" }).click();
  await expect(page).toHaveURL("/categories");
}

/** Fills and saves the new-category form, and returns the id the read view landed on. */
async function createCategory(page: Page, name: string, description: string) {
  await page.goto("/categories/new");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Description").fill(description);
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(READ_VIEW);
  const id = page.url().split("/").pop()!;
  created.push(id);
  return id;
}

async function expectReadView(page: Page, name: string, description: string) {
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  await expect(main(page)).toContainText(description);
  await expect(page.getByRole("textbox")).toHaveCount(0);
}

test("the menu leads to the category list, which shows the seeded categories", async ({ page }) => {
  await page.goto("/");

  await openFromMenu(page);

  await expect(page.getByRole("heading", { name: "Categories" })).toBeVisible();
  for (const name of SEEDED) {
    await expect(main(page).getByRole("link", { name, exact: true })).toBeVisible();
  }
  await expect(page.getByRole("link", { name: "Categories" })).toHaveAttribute("aria-current", "page");
});

test("the list is in alphabetical order, whatever order the API returns", async ({ page }) => {
  // The API's order is the primary key's, which for the seed happens to be alphabetical. Reversing
  // it makes sure the page sorts rather than trusting it.
  await page.route("**/api/v1/categories", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: (await response.json()).reverse() });
  });

  await page.goto("/categories");
  await expect(main(page).getByRole("link", { name: "Kager", exact: true })).toBeVisible();

  const names = await main(page).locator("tbody tr td:first-child").allTextContents();
  expect(names).toEqual(names.toSorted((a, b) => a.localeCompare(b)));
  expect(names.filter((name) => SEEDED.includes(name))).toEqual(SEEDED);
});

test("opening a category from the list shows it in read mode", async ({ page }) => {
  await page.goto("/categories");

  await main(page).getByRole("link", { name: "Dessert", exact: true }).click();

  await expect(page).toHaveURL(`/categories/${DESSERT_ID}`);
  await expectReadView(page, "Dessert", "Den søde afrundning på en god middag");
});

test("a category that does not exist shows not found", async ({ page }) => {
  await page.goto("/categories/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("heading", { name: "Category not found" })).toBeVisible();
  await page.getByRole("link", { name: "Back to categories" }).click();
  await expect(page).toHaveURL("/categories");
});

test("a new category lands on its read view, loaded fresh, and is still there after leaving and returning", async ({
  page,
}) => {
  const name = uniqueName();
  await page.goto("/categories");
  await page.getByRole("link", { name: "New category" }).click();
  await expect(page).toHaveURL("/categories/new");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Description").fill("Made by Playwright");

  // The read view must fetch the category itself rather than show what the form just sent.
  const readBack = page.waitForResponse(
    (response) => response.request().method() === "GET" && /\/api\/v1\/categories\/[0-9a-f-]{36}$/.test(response.url()),
  );
  await page.getByRole("button", { name: "Save" }).click();
  await readBack;
  await expect(page).toHaveURL(READ_VIEW);
  created.push(page.url().split("/").pop()!);
  await expectReadView(page, name, "Made by Playwright");

  await openFromMenu(page);
  await main(page).getByRole("link", { name, exact: true }).click();
  await expectReadView(page, name, "Made by Playwright");

  await page.reload();
  await expectReadView(page, name, "Made by Playwright");
});

test("an edited category shows the change, and keeps it after leaving and returning", async ({ page }) => {
  const name = uniqueName();
  const id = await createCategory(page, name, "Before");

  await page.getByRole("link", { name: "Edit" }).click();
  await expect(page).toHaveURL(`/categories/${id}/edit`);
  await expect(page.getByLabel("Name")).toHaveValue(name);
  await expect(page.getByLabel("Description")).toHaveValue("Before");
  const renamed = uniqueName();
  await page.getByLabel("Name").fill(renamed);
  await page.getByLabel("Description").fill("After");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(`/categories/${id}`);
  await expectReadView(page, renamed, "After");

  await openFromMenu(page);
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await main(page).getByRole("link", { name: renamed, exact: true }).click();
  await expectReadView(page, renamed, "After");
});

test("the edit form shows when, in UTC, and by whom the category was created", async ({ page }) => {
  const id = await createCategory(page, uniqueName(), "Audited");

  await page.goto(`/categories/${id}/edit`);

  await expect(main(page).locator("dt:text-is('Created by') + dd")).toHaveText(USERNAME);
  const created = await main(page).locator("dt:text-is('Created') + dd").textContent();
  expect(created).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$/);
  // Read as UTC, it is now: the label is true, not merely present.
  const createdAt = Date.parse(created!.replace(" ", "T").replace(" UTC", ":00Z"));
  expect(Math.abs(Date.now() - createdAt)).toBeLessThan(2 * 60 * 1000);
});

test("a name another category has shows the server's error on the name field", async ({ page }) => {
  await page.goto("/categories/new");
  await page.getByLabel("Name").fill("Dessert");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("A category named 'Dessert' already exists");
  await expect(page).toHaveURL("/categories/new");
});

test("an empty name shows the validation message on the name field", async ({ page }) => {
  await page.goto("/categories/new");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("Category name is required");
  await expect(page).toHaveURL("/categories/new");
});

test("cancel leaves the new-category form without saving", async ({ page }) => {
  const name = uniqueName();
  await page.goto("/categories/new");
  await page.getByLabel("Name").fill(name);

  await page.getByRole("link", { name: "Cancel" }).click();

  await expect(page).toHaveURL("/categories");
  await expect(main(page).getByRole("link", { name: "Kager", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
});
