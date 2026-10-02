import { expect, test } from "@playwright/test";
import { danish, expectReadView, main, openFromMenu } from "../support/pages";

// Seeded by db.changelog_1.1.xml, in Danish order. No test may change or delete them.
const SEEDED = [
  "Brunkage",
  "Fuldkorns hvedebrød",
  "Hvedebrød med Rugmel",
  "Kylling med bacon og lækker kålsalat",
  "Stegt flæsk med persillesovs og kogte kartofler",
  "Thai-ret med kylling og kokossauce",
  "Ølandshvedebrød",
];
const FULDKORN_ID = "5d22c394-b5ce-48c3-8199-72ccc92c737c";
const BRUNKAGE_ID = "ef61018c-98db-41ff-92d7-52bd950f7bfd";

test("the menu leads to the recipe list, which shows the seeded recipes with their category and tags", async ({
  page,
}) => {
  await page.goto("/");

  await openFromMenu(page, "Recipes", "/recipes");

  await expect(page.getByRole("heading", { name: "Recipes" })).toBeVisible();
  for (const name of SEEDED) {
    await expect(main(page).getByRole("link", { name, exact: true })).toBeVisible();
  }
  await expect(main(page).getByRole("row", { name: /Fuldkorns hvedebrød/ })).toContainText("Brød");
  await expect(main(page).getByRole("row", { name: /Fuldkorns hvedebrød/ })).toContainText("Godt til kaffen");
});

test("the recipe list is in alphabetical order, whatever order the API returns", async ({ page }) => {
  await page.route("**/api/v1/recipes", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: (await response.json()).reverse() });
  });

  await page.goto("/recipes");
  await expect(main(page).getByRole("link", { name: "Brunkage", exact: true })).toBeVisible();

  const names = await main(page).locator("tbody tr td:first-child").allTextContents();
  expect(names).toEqual(names.toSorted(danish));
  // Danish order: Ølandshvedebrød last, not beside the Os.
  expect(names.filter((name) => SEEDED.includes(name))).toEqual(SEEDED);
});

test("a recipe's read view shows its category, tags, every ingredient with amount and unit, and instructions", async ({
  page,
}) => {
  await page.goto("/recipes");

  await main(page).getByRole("link", { name: "Fuldkorns hvedebrød", exact: true }).click();

  await expect(page).toHaveURL(`/recipes/${FULDKORN_ID}`);
  await expectReadView(page, "Fuldkorns hvedebrød", "Et dejligt Fuldkorns brød som smager godt og mætter dejligt");
  await expect(main(page).locator("dt:text-is('Category') + dd")).toHaveText("Brød");
  await expect(main(page).locator("dt:text-is('Tags') + dd")).toHaveText("Godt til kaffen");

  const lines = main(page).getByRole("table").getByRole("row");
  await expect(lines).toHaveCount(6);
  // Sorted by ingredient name, each read as amount, unit, ingredient.
  await expect(lines.first()).toHaveText("170grFuldkorns hvedemel");
  await expect(lines.filter({ hasText: "Vand" })).toHaveText("5dlVand");
  await expect(lines.filter({ hasText: "Salt" })).toHaveText("16grSalt");

  await expect(main(page)).toContainText("1. Hæld vand og gær i en røremaskine...");
});

test("a recipe without tags or ingredients says so rather than leaving gaps", async ({ page }) => {
  await page.goto(`/recipes/${BRUNKAGE_ID}`);

  await expectReadView(page, "Brunkage");
  await expect(main(page).locator("dt:text-is('Tags') + dd")).toHaveText("None");
  await expect(main(page).getByRole("heading", { name: "Ingredients" })).toBeVisible();
  await expect(main(page).getByRole("table")).toHaveCount(0);
});

test("a recipe's category, tags and ingredients link to their own pages", async ({ page }) => {
  await page.goto(`/recipes/${FULDKORN_ID}`);
  await main(page).getByRole("link", { name: "Brød", exact: true }).click();
  await expectReadView(page, "Brød");

  await page.goto(`/recipes/${FULDKORN_ID}`);
  await main(page).getByRole("link", { name: "Godt til kaffen", exact: true }).click();
  await expectReadView(page, "Godt til kaffen");

  await page.goto(`/recipes/${FULDKORN_ID}`);
  await main(page).getByRole("link", { name: "Vand", exact: true }).click();
  await expectReadView(page, "Vand", "Almindelig postevand fra hanen");
});

test("a recipe that does not exist shows not found", async ({ page }) => {
  await page.goto("/recipes/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("heading", { name: "Recipe not found" })).toBeVisible();
  await page.getByRole("link", { name: "Back to recipes" }).click();
  await expect(page).toHaveURL("/recipes");
});
