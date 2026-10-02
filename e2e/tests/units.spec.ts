import { expect, test, type Page } from "@playwright/test";
import { deleteById } from "../support/api";
import { USERNAME } from "../support/auth";
import {
  createThroughForm,
  expectCreatedNowBy,
  expectReadView,
  main,
  openFromMenu,
  readView,
  uniqueName,
} from "../support/pages";

// Seeded by db.changelog_1.1.xml. Gram and Deciliter are used by recipes; no test may delete a seed.
const SEEDED = ["Deciliter", "Gram", "Spiseske", "Teske"];
const GRAM_ID = "c5173731-3a7e-498c-84b1-b2d3abe68cef";

// Units a test created, removed after it whether it passed or not.
const created: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of created.splice(0)) {
    await deleteById(page, "units", id);
  }
});

async function createUnit(page: Page, name: string, label: string) {
  const id = await createThroughForm(page, "units", { Name: name, Label: label });
  created.push(id);
  return id;
}

test("the menu leads to the unit list, which shows the seeded units", async ({ page }) => {
  await page.goto("/");

  await openFromMenu(page, "Units", "/units");

  await expect(page.getByRole("heading", { name: "Units" })).toBeVisible();
  for (const name of SEEDED) {
    await expect(main(page).getByRole("link", { name, exact: true })).toBeVisible();
  }
  await expect(page.getByRole("link", { name: "Units" })).toHaveAttribute("aria-current", "page");
});

test("the unit list is in alphabetical order, whatever order the API returns", async ({ page }) => {
  await page.route("**/api/v1/units", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: (await response.json()).reverse() });
  });

  await page.goto("/units");
  await expect(main(page).getByRole("link", { name: "Teske", exact: true })).toBeVisible();

  const names = await main(page).locator("tbody tr td:first-child").allTextContents();
  expect(names).toEqual(names.toSorted((a, b) => a.localeCompare(b)));
  expect(names.filter((name) => SEEDED.includes(name))).toEqual(SEEDED);
});

test("opening a unit from the list shows it in read mode", async ({ page }) => {
  await page.goto("/units");

  await main(page).getByRole("link", { name: "Gram", exact: true }).click();

  await expect(page).toHaveURL(`/units/${GRAM_ID}`);
  await expectReadView(page, "Gram", "gr");
});

test("a unit that does not exist shows not found", async ({ page }) => {
  await page.goto("/units/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("heading", { name: "Unit not found" })).toBeVisible();
  await page.getByRole("link", { name: "Back to units" }).click();
  await expect(page).toHaveURL("/units");
});

test("a new unit lands on its read view, loaded fresh, and is still there after leaving and returning", async ({
  page,
}) => {
  const name = uniqueName();
  await page.goto("/units");
  await page.getByRole("link", { name: "New unit" }).click();
  await expect(page).toHaveURL("/units/new");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Label").fill("e2e");

  const readBack = page.waitForResponse(
    (response) => response.request().method() === "GET" && /\/api\/v1\/units\/[0-9a-f-]{36}$/.test(response.url()),
  );
  await page.getByRole("button", { name: "Save" }).click();
  await readBack;
  await expect(page).toHaveURL(readView("units"));
  created.push(page.url().split("/").pop()!);
  await expectReadView(page, name, "e2e");

  await openFromMenu(page, "Units", "/units");
  await main(page).getByRole("link", { name, exact: true }).click();
  await expectReadView(page, name, "e2e");

  await page.reload();
  await expectReadView(page, name, "e2e");
});

test("an edited unit shows the change, and keeps it after leaving and returning", async ({ page }) => {
  const name = uniqueName();
  const id = await createUnit(page, name, "before");

  await page.getByRole("link", { name: "Edit" }).click();
  await expect(page).toHaveURL(`/units/${id}/edit`);
  await expect(page.getByLabel("Name")).toHaveValue(name);
  await expect(page.getByLabel("Label")).toHaveValue("before");
  const renamed = uniqueName();
  await page.getByLabel("Name").fill(renamed);
  await page.getByLabel("Label").fill("after");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(`/units/${id}`);
  await expectReadView(page, renamed, "after");

  await openFromMenu(page, "Units", "/units");
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await main(page).getByRole("link", { name: renamed, exact: true }).click();
  await expectReadView(page, renamed, "after");
});

test("the unit edit form shows when, in UTC, and by whom the unit was created", async ({ page }) => {
  const id = await createUnit(page, uniqueName(), "audit");

  await page.goto(`/units/${id}/edit`);

  await expectCreatedNowBy(page, USERNAME);
});

test("a name another unit has shows the server's error on the name field", async ({ page }) => {
  await page.goto("/units/new");
  await page.getByLabel("Name").fill("Gram");
  await page.getByLabel("Label").fill("g");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("A unit named 'Gram' already exists");
  await expect(page).toHaveURL("/units/new");
});

test("an empty unit shows the validation message on each field", async ({ page }) => {
  await page.goto("/units/new");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("Unit name is required");
  await expect(page.getByLabel("Label")).toHaveAccessibleDescription("Unit label is required");
  await expect(page).toHaveURL("/units/new");
});

test("cancel leaves the new-unit form without saving", async ({ page }) => {
  const name = uniqueName();
  await page.goto("/units/new");
  await page.getByLabel("Name").fill(name);

  await page.getByRole("link", { name: "Cancel" }).click();

  await expect(page).toHaveURL("/units");
  await expect(main(page).getByRole("link", { name: "Teske", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
});

test("deleting a unit asks first, in the page, and after confirming it is gone", async ({ page }) => {
  const name = uniqueName();
  const id = await createUnit(page, name, "del");

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: `Delete unit "${name}"?` });
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "Delete" }).click();

  await expect(page).toHaveURL("/units");
  await expect(main(page).getByRole("link", { name: "Teske", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await page.goto(`/units/${id}`);
  await expect(page.getByRole("heading", { name: "Unit not found" })).toBeVisible();
});

test("cancelling the unit delete dialog keeps the unit", async ({ page }) => {
  const name = uniqueName();
  await createUnit(page, name, "kept");

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Cancel" }).click();

  await expect(dialog).toBeHidden();
  await page.reload();
  await expectReadView(page, name, "kept");
});

test("a unit recipes use cannot be deleted, and the dialog says why", async ({ page }) => {
  await page.goto(`/units/${GRAM_ID}`);

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: 'Delete unit "Gram"?' });
  await dialog.getByRole("button", { name: "Delete" }).click();

  // Gram is on eight recipe lines in two recipes: the count is of recipes.
  await expect(dialog.getByRole("alert")).toHaveText("Unit 'Gram' is used by 2 recipes and cannot be deleted");
  await expect(dialog.getByRole("button")).toHaveText(["OK"]);
  await dialog.getByRole("button", { name: "OK" }).click();
  await expect(dialog).toBeHidden();
  await openFromMenu(page, "Units", "/units");
  await expect(main(page).getByRole("link", { name: "Gram", exact: true })).toBeVisible();
});
