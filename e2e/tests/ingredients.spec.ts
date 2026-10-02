import { expect, test, type Page } from "@playwright/test";
import { deleteById } from "../support/api";
import { USERNAME } from "../support/auth";
import {
  createThroughForm,
  danish,
  expectCreatedNowBy,
  expectReadView,
  main,
  openFromMenu,
  readView,
  uniqueName,
} from "../support/pages";

// Seeded by db.changelog_1.1.xml, in alphabetical order. Most are used by recipes; no test may
// delete a seed.
const SEEDED = [
  "Fuldkorns hvedemel",
  "Gær",
  "Hvedemel",
  "Kyllingebryst",
  "Revet Mozarella",
  "Rugmel",
  "Salt",
  "Surdej",
  "Vand",
];
const HVEDEMEL_ID = "549ab6e6-f2d8-4ab3-8ba8-6bc7af82f2fb";

// Ingredients a test created, removed after it whether it passed or not.
const created: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of created.splice(0)) {
    await deleteById(page, "ingredients", id);
  }
});

async function createIngredient(page: Page, name: string, description: string) {
  const id = await createThroughForm(page, "ingredients", { Name: name, Description: description });
  created.push(id);
  return id;
}

test("the menu leads to the ingredient list, which shows the seeded ingredients", async ({ page }) => {
  await page.goto("/");

  await openFromMenu(page, "Ingredients", "/ingredients");

  await expect(page.getByRole("heading", { name: "Ingredients" })).toBeVisible();
  for (const name of SEEDED) {
    await expect(main(page).getByRole("link", { name, exact: true })).toBeVisible();
  }
  await expect(page.getByRole("link", { name: "Ingredients" })).toHaveAttribute("aria-current", "page");
});

test("the ingredient list is in alphabetical order, whatever order the API returns", async ({ page }) => {
  await page.route("**/api/v1/ingredients", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: (await response.json()).reverse() });
  });

  await page.goto("/ingredients");
  await expect(main(page).getByRole("link", { name: "Vand", exact: true })).toBeVisible();

  const names = await main(page).locator("tbody tr td:first-child").allTextContents();
  expect(names).toEqual(names.toSorted(danish));
  expect(names.filter((name) => SEEDED.includes(name))).toEqual(SEEDED);
});

test("opening an ingredient from the list shows it in read mode", async ({ page }) => {
  await page.goto("/ingredients");

  await main(page).getByRole("link", { name: "Hvedemel", exact: true }).click();

  await expect(page).toHaveURL(`/ingredients/${HVEDEMEL_ID}`);
  await expectReadView(page, "Hvedemel", "Sigtet hvedemel uden skaldele og kim");
});

test("an ingredient that does not exist shows not found", async ({ page }) => {
  await page.goto("/ingredients/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("heading", { name: "Ingredient not found" })).toBeVisible();
  await page.getByRole("link", { name: "Back to ingredients" }).click();
  await expect(page).toHaveURL("/ingredients");
});

test("a new ingredient lands on its read view, loaded fresh, and is still there after leaving and returning", async ({
  page,
}) => {
  const name = uniqueName();
  await page.goto("/ingredients");
  await page.getByRole("link", { name: "New ingredient" }).click();
  await expect(page).toHaveURL("/ingredients/new");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Description").fill("Made by Playwright");

  const readBack = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" && /\/api\/v1\/ingredients\/[0-9a-f-]{36}$/.test(response.url()),
  );
  await page.getByRole("button", { name: "Save" }).click();
  await readBack;
  await expect(page).toHaveURL(readView("ingredients"));
  created.push(page.url().split("/").pop()!);
  await expectReadView(page, name, "Made by Playwright");

  await openFromMenu(page, "Ingredients", "/ingredients");
  await main(page).getByRole("link", { name, exact: true }).click();
  await expectReadView(page, name, "Made by Playwright");

  await page.reload();
  await expectReadView(page, name, "Made by Playwright");
});

test("an edited ingredient shows the change, and keeps it after leaving and returning", async ({ page }) => {
  const name = uniqueName();
  const id = await createIngredient(page, name, "Before");

  await page.getByRole("link", { name: "Edit" }).click();
  await expect(page).toHaveURL(`/ingredients/${id}/edit`);
  await expect(page.getByLabel("Name")).toHaveValue(name);
  await expect(page.getByLabel("Description")).toHaveValue("Before");
  const renamed = uniqueName();
  await page.getByLabel("Name").fill(renamed);
  await page.getByLabel("Description").fill("After");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(`/ingredients/${id}`);
  await expectReadView(page, renamed, "After");

  await openFromMenu(page, "Ingredients", "/ingredients");
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await main(page).getByRole("link", { name: renamed, exact: true }).click();
  await expectReadView(page, renamed, "After");
});

test("a cleared description is gone after a reload, not stored as an empty value", async ({ page }) => {
  const name = uniqueName();
  const id = await createIngredient(page, name, "To be cleared");

  await page.goto(`/ingredients/${id}/edit`);
  await page.getByLabel("Description").fill("");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/ingredients/${id}`);

  await page.reload();
  await expect(main(page).locator("dt:text-is('Description') + dd")).toHaveText("None");
  await page.goto(`/ingredients/${id}/edit`);
  await expect(page.getByLabel("Description")).toHaveValue("");
});

test("the ingredient edit form shows when, in UTC, and by whom the ingredient was created", async ({ page }) => {
  const id = await createIngredient(page, uniqueName(), "Audited");

  await page.goto(`/ingredients/${id}/edit`);

  await expectCreatedNowBy(page, USERNAME);
});

test("a name another ingredient has shows the server's error on the name field", async ({ page }) => {
  await page.goto("/ingredients/new");
  await page.getByLabel("Name").fill("Hvedemel");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("An ingredient named 'Hvedemel' already exists");
  await expect(page).toHaveURL("/ingredients/new");
});

test("an empty ingredient name shows the validation message on the name field", async ({ page }) => {
  await page.goto("/ingredients/new");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("Ingredient name is required");
  await expect(page).toHaveURL("/ingredients/new");
});

test("cancel leaves the new-ingredient form without saving", async ({ page }) => {
  const name = uniqueName();
  await page.goto("/ingredients/new");
  await page.getByLabel("Name").fill(name);

  await page.getByRole("link", { name: "Cancel" }).click();

  await expect(page).toHaveURL("/ingredients");
  await expect(main(page).getByRole("link", { name: "Vand", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
});

test("deleting an ingredient asks first, in the page, and after confirming it is gone", async ({ page }) => {
  const name = uniqueName();
  const id = await createIngredient(page, name, "To be deleted");

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: `Delete ingredient "${name}"?` });
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "Delete" }).click();

  await expect(page).toHaveURL("/ingredients");
  await expect(main(page).getByRole("link", { name: "Vand", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await page.goto(`/ingredients/${id}`);
  await expect(page.getByRole("heading", { name: "Ingredient not found" })).toBeVisible();
});

test("cancelling the ingredient delete dialog keeps the ingredient", async ({ page }) => {
  const name = uniqueName();
  await createIngredient(page, name, "Kept");

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Cancel" }).click();

  await expect(dialog).toBeHidden();
  await page.reload();
  await expectReadView(page, name, "Kept");
});

test("an ingredient recipes use cannot be deleted, and the dialog says why", async ({ page }) => {
  await page.goto(`/ingredients/${HVEDEMEL_ID}`);

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: 'Delete ingredient "Hvedemel"?' });
  await dialog.getByRole("button", { name: "Delete" }).click();

  await expect(dialog.getByRole("alert")).toHaveText("Ingredient 'Hvedemel' is used by 2 recipes and cannot be deleted");
  await expect(dialog.getByRole("button")).toHaveText(["OK"]);
  await dialog.getByRole("button", { name: "OK" }).click();
  await expect(dialog).toBeHidden();
  await openFromMenu(page, "Ingredients", "/ingredients");
  await expect(main(page).getByRole("link", { name: "Hvedemel", exact: true })).toBeVisible();
});
