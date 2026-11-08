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

test("each part's jar on the start page leads there, the drawing as much as the name", async ({ page }) => {
  await page.goto("/");
  const units = page.getByRole("main").getByRole("listitem").filter({ hasText: "What ingredients are measured in" });
  const box = (await units.boundingBox())!;

  // The top of the tile: the jug, above the name and its sentence.
  await units.click({ position: { x: box.width / 2, y: 12 } });

  await expect(page).toHaveURL("/units");
});
