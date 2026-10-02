import { expect, test, type Page } from "@playwright/test";
import { createById, deleteById } from "../support/api";
import { USERNAME } from "../support/auth";
import { danish, expectCreatedNowBy, expectReadView, main, openFromMenu, readView, uniqueName } from "../support/pages";

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
const KAGER_ID = "fc87bd60-1c8e-436d-9806-5768027f91da";
const HVEDEMEL_ID = "549ab6e6-f2d8-4ab3-8ba8-6bc7af82f2fb";
const SALT_ID = "e0aa2252-c5f1-4c87-b42c-9dd10486f366";
const GRAM_ID = "c5173731-3a7e-498c-84b1-b2d3abe68cef";
const TESKE_ID = "615ba803-966f-43e5-8d2a-d38b5198a421";

// Recipes a test created, removed after it whether it passed or not.
const created: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of created.splice(0)) {
    await deleteById(page, "recipes", id);
  }
});

const category = (page: Page) => main(page).locator("dt:text-is('Category') + dd");
const tagItems = (page: Page) => main(page).locator("dt:text-is('Tags') + dd").getByRole("listitem");

/** Fills and saves the new-recipe form, and returns the id the read view landed on. */
async function createRecipe(page: Page, name: string, categoryName: string, tagNames: string[]) {
  await page.goto("/recipes/new");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Category").selectOption({ label: categoryName });
  for (const tag of tagNames) {
    await page.getByRole("checkbox", { name: tag, exact: true }).check();
  }
  await page.getByLabel("Description").fill("Made by Playwright");
  await page.getByLabel("Instructions").fill("1. Mix\n2. Bake");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(readView("recipes"));
  const id = page.url().split("/").pop()!;
  created.push(id);
  return id;
}

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

test("a new recipe lands on its read view, loaded fresh, with its category and tags, and keeps them after leaving", async ({
  page,
}) => {
  const name = uniqueName();
  await page.goto("/recipes");
  await page.getByRole("link", { name: "New recipe" }).click();
  await expect(page).toHaveURL("/recipes/new");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Category").selectOption({ label: "Kager" });
  await page.getByRole("checkbox", { name: "Spicy", exact: true }).check();
  await page.getByRole("checkbox", { name: "Thai", exact: true }).check();
  await page.getByLabel("Instructions").fill("1. Mix\n2. Bake");

  const readBack = page.waitForResponse(
    (response) => response.request().method() === "GET" && /\/api\/v1\/recipes\/[0-9a-f-]{36}$/.test(response.url()),
  );
  await page.getByRole("button", { name: "Save" }).click();
  await readBack;
  await expect(page).toHaveURL(readView("recipes"));
  created.push(page.url().split("/").pop()!);

  for (const visit of ["saved", "returned", "reloaded"]) {
    if (visit === "returned") {
      await openFromMenu(page, "Recipes", "/recipes");
      await main(page).getByRole("link", { name, exact: true }).click();
    } else if (visit === "reloaded") {
      await page.reload();
    }
    await expectReadView(page, name, "1. Mix");
    await expect(category(page)).toHaveText("Kager");
    await expect(tagItems(page)).toHaveText(["Spicy", "Thai"]);
  }
});

test("an edited recipe's new category and tags survive leaving and returning", async ({ page }) => {
  const name = uniqueName();
  const id = await createRecipe(page, name, "Kager", ["Spicy", "Thai"]);

  await page.getByRole("link", { name: "Edit" }).click();
  await expect(page).toHaveURL(`/recipes/${id}/edit`);
  await expect(page.getByLabel("Name")).toHaveValue(name);
  await expect(page.getByLabel("Category")).toHaveValue(KAGER_ID);
  await expect(page.getByRole("checkbox", { name: "Spicy", exact: true })).toBeChecked();
  await expect(page.getByRole("checkbox", { name: "Thai", exact: true })).toBeChecked();
  await page.getByLabel("Category").selectOption({ label: "Brød" });
  await page.getByRole("checkbox", { name: "Spicy", exact: true }).uncheck();
  await page.getByLabel("Description").fill("Edited by Playwright");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/recipes/${id}`);

  await openFromMenu(page, "Recipes", "/recipes");
  await main(page).getByRole("link", { name, exact: true }).click();
  await expectReadView(page, name, "Edited by Playwright");
  await expect(category(page)).toHaveText("Brød");
  await expect(tagItems(page)).toHaveText(["Thai"]);
});

test("editing a recipe's text keeps its ingredient lines", async ({ page }) => {
  // The form has no ingredient lines until 7d; this recipe gets them through the API.
  const name = uniqueName();
  const id = await createById(page, "recipes", {
    name,
    category: { id: KAGER_ID },
    recipeIngredients: [
      { ingredientId: HVEDEMEL_ID, amount: 500, unitId: GRAM_ID },
      { ingredientId: SALT_ID, amount: 2, unitId: TESKE_ID },
    ],
  });
  created.push(id);

  await page.goto(`/recipes/${id}/edit`);
  await page.getByLabel("Description").fill("Only the text changed");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/recipes/${id}`);
  await page.reload();

  await expectReadView(page, name, "Only the text changed");
  await expect(main(page).getByRole("table").getByRole("row")).toHaveText(["500grHvedemel", "2tskSalt"]);
});

test("the recipe edit form shows when, in UTC, and by whom the recipe was created", async ({ page }) => {
  const id = await createRecipe(page, uniqueName(), "Kager", []);

  await page.goto(`/recipes/${id}/edit`);

  await expectCreatedNowBy(page, USERNAME);
});

test("saving a recipe never sends ratings, on create or on edit", async ({ page }) => {
  // A round trip cannot tell "not sent" from "sent and ignored", so this one looks at the request.
  const bodies: Record<string, unknown>[] = [];
  await page.route("**/api/v1/recipes**", async (route) => {
    if (["POST", "PUT"].includes(route.request().method())) {
      bodies.push(route.request().postDataJSON());
    }
    await route.continue();
  });

  const id = await createRecipe(page, uniqueName(), "Kager", ["Thai"]);
  await page.goto(`/recipes/${id}/edit`);
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/recipes/${id}`);

  expect(bodies).toHaveLength(2);
  for (const body of bodies) {
    expect(body).not.toHaveProperty("recipeRatings");
  }
});

test("a name another recipe has shows the server's error on the name field", async ({ page }) => {
  await page.goto("/recipes/new");
  await page.getByLabel("Name").fill("Brunkage");
  await page.getByLabel("Category").selectOption({ label: "Kager" });

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("A recipe named 'Brunkage' already exists");
  await expect(page).toHaveURL("/recipes/new");
});

test("a recipe without a name, then without a category, shows each message on its field", async ({ page }) => {
  await page.goto("/recipes/new");

  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("Recipe name is required");

  await page.getByLabel("Name").fill(uniqueName());
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByLabel("Category")).toHaveAccessibleDescription("A recipe requires a category");
  await expect(page).toHaveURL("/recipes/new");
});

test("cancel leaves the new-recipe form without saving", async ({ page }) => {
  const name = uniqueName();
  await page.goto("/recipes/new");
  await page.getByLabel("Name").fill(name);

  await page.getByRole("link", { name: "Cancel" }).click();

  await expect(page).toHaveURL("/recipes");
  await expect(main(page).getByRole("link", { name: "Brunkage", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
});

test("deleting a recipe asks first, in the page, and after confirming it is gone", async ({ page }) => {
  const name = uniqueName();
  const id = await createRecipe(page, name, "Kager", ["Thai"]);

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: `Delete recipe "${name}"?` });
  await dialog.getByRole("button", { name: "Delete" }).click();

  await expect(page).toHaveURL("/recipes");
  await expect(main(page).getByRole("link", { name: "Brunkage", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await page.goto(`/recipes/${id}`);
  await expect(page.getByRole("heading", { name: "Recipe not found" })).toBeVisible();
});
