import { expect, test } from "@playwright/test";

test("the start page welcomes, describes the application and leads to each part of it", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "Cooking Recipes" })).toBeVisible();
  await expect(page.getByRole("main")).toContainText("Keep your recipes in one place.");
  for (const section of ["Recipes", "Categories", "Ingredients", "Units", "Tags"]) {
    await expect(page.getByRole("main").getByRole("link", { name: section, exact: true })).toBeVisible();
  }

  await page.getByRole("main").getByRole("link", { name: "Recipes", exact: true }).click();
  await expect(page).toHaveURL("/recipes");
  await expect(page.getByRole("heading", { name: "Recipes" })).toBeVisible();
});
