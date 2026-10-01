import { expect, test } from "@playwright/test";

// Seeded by db.changelog_1.1.xml. Each one is used by a recipe, so no test may delete it.
const SEEDED = ["Brød", "Dessert", "Hovedret", "Kager"];
const DESSERT_ID = "913a5159-3717-4b9d-a290-0158d31ea8aa";

test("the menu leads to the category list, which shows the seeded categories", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Categories" }).click();

  await expect(page).toHaveURL("/categories");
  await expect(page.getByRole("heading", { name: "Categories" })).toBeVisible();
  for (const name of SEEDED) {
    await expect(page.getByRole("main").getByRole("link", { name, exact: true })).toBeVisible();
  }
  await expect(page.getByRole("link", { name: "Categories" })).toHaveAttribute("aria-current", "page");
});

test("opening a category from the list shows it in read mode", async ({ page }) => {
  await page.goto("/categories");

  await page.getByRole("main").getByRole("link", { name: "Dessert", exact: true }).click();

  await expect(page).toHaveURL(`/categories/${DESSERT_ID}`);
  await expect(page.getByRole("heading", { name: "Dessert" })).toBeVisible();
  await expect(page.getByRole("main")).toContainText("Den søde afrundning på en god middag");
  await expect(page.getByRole("textbox")).toHaveCount(0);
});

test("a category that does not exist shows not found", async ({ page }) => {
  await page.goto("/categories/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("heading", { name: "Category not found" })).toBeVisible();
  await page.getByRole("link", { name: "Back to categories" }).click();
  await expect(page).toHaveURL("/categories");
});
