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

// Seeded by db.changelog_1.1.xml, in alphabetical order. "Godt til kaffen" and "Thai" are used by
// recipes; no test may delete a seed.
const SEEDED = ["Børnevenlig", "Godt til grillen", "Godt til kaffen", "Mexi", "Spicy", "Thai"];

// Tags a test created, removed after it whether it passed or not.
const created: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of created.splice(0)) {
    await deleteById(page, "tags", id);
  }
});

async function createTag(page: Page, name: string) {
  const id = await createThroughForm(page, "tags", { Name: name });
  created.push(id);
  return id;
}

test("the menu leads to the tag list, which shows the seeded tags", async ({ page }) => {
  await page.goto("/");

  await openFromMenu(page, "Tags", "/tags");

  await expect(page.getByRole("heading", { name: "Tags" })).toBeVisible();
  for (const name of SEEDED) {
    await expect(main(page).getByRole("link", { name, exact: true })).toBeVisible();
  }
  await expect(page.getByRole("link", { name: "Tags" })).toHaveAttribute("aria-current", "page");
});

test("the tag list is in alphabetical order, whatever order the API returns", async ({ page }) => {
  await page.route("**/api/v1/tags", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: (await response.json()).reverse() });
  });

  await page.goto("/tags");
  await expect(main(page).getByRole("link", { name: "Thai", exact: true })).toBeVisible();

  const names = await main(page).locator("tbody tr td:first-child").allTextContents();
  expect(names).toEqual(names.toSorted((a, b) => a.localeCompare(b)));
  expect(names.filter((name) => SEEDED.includes(name))).toEqual(SEEDED);
});

test("opening a tag from the list shows it in read mode", async ({ page }) => {
  await page.goto("/tags");

  await main(page).getByRole("link", { name: "Spicy", exact: true }).click();

  await expect(page).toHaveURL(readView("tags"));
  await expectReadView(page, "Spicy");
});

test("a tag that does not exist shows not found", async ({ page }) => {
  await page.goto("/tags/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("heading", { name: "Tag not found" })).toBeVisible();
  await page.getByRole("link", { name: "Back to tags" }).click();
  await expect(page).toHaveURL("/tags");
});

test("a new tag lands on its read view, loaded fresh, and is still there after leaving and returning", async ({
  page,
}) => {
  const name = uniqueName();
  await page.goto("/tags");
  await page.getByRole("link", { name: "New tag" }).click();
  await expect(page).toHaveURL("/tags/new");
  await page.getByLabel("Name").fill(name);

  const readBack = page.waitForResponse(
    (response) => response.request().method() === "GET" && /\/api\/v1\/tags\/[0-9a-f-]{36}$/.test(response.url()),
  );
  await page.getByRole("button", { name: "Save" }).click();
  await readBack;
  await expect(page).toHaveURL(readView("tags"));
  created.push(page.url().split("/").pop()!);
  await expectReadView(page, name);

  await openFromMenu(page, "Tags", "/tags");
  await main(page).getByRole("link", { name, exact: true }).click();
  await expectReadView(page, name);

  await page.reload();
  await expectReadView(page, name);
});

test("an edited tag shows the change, and keeps it after leaving and returning", async ({ page }) => {
  const name = uniqueName();
  const id = await createTag(page, name);

  await page.getByRole("link", { name: "Edit" }).click();
  await expect(page).toHaveURL(`/tags/${id}/edit`);
  await expect(page.getByLabel("Name")).toHaveValue(name);
  const renamed = uniqueName();
  await page.getByLabel("Name").fill(renamed);
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(`/tags/${id}`);
  await expectReadView(page, renamed);

  await openFromMenu(page, "Tags", "/tags");
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await main(page).getByRole("link", { name: renamed, exact: true }).click();
  await expectReadView(page, renamed);
});

test("the tag edit form shows when, in UTC, and by whom the tag was created", async ({ page }) => {
  const id = await createTag(page, uniqueName());

  await page.goto(`/tags/${id}/edit`);

  await expectCreatedNowBy(page, USERNAME);
});

// Tag names are unique, as the schema has always enforced - see docs/PLAN.md, 6b.
test("a name another tag has shows the server's error on the name field", async ({ page }) => {
  await page.goto("/tags/new");
  await page.getByLabel("Name").fill("Spicy");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("A tag named 'Spicy' already exists");
  await expect(page).toHaveURL("/tags/new");
});

test("an empty tag name shows the validation message on the name field", async ({ page }) => {
  await page.goto("/tags/new");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("Tag name is required");
  await expect(page).toHaveURL("/tags/new");
});

test("cancel leaves the new-tag form without saving", async ({ page }) => {
  const name = uniqueName();
  await page.goto("/tags/new");
  await page.getByLabel("Name").fill(name);

  await page.getByRole("link", { name: "Cancel" }).click();

  await expect(page).toHaveURL("/tags");
  await expect(main(page).getByRole("link", { name: "Thai", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
});

test("deleting a tag asks first, in the page, and after confirming it is gone", async ({ page }) => {
  const name = uniqueName();
  const id = await createTag(page, name);

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: `Delete tag "${name}"?` });
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "Delete" }).click();

  await expect(page).toHaveURL("/tags");
  await expect(main(page).getByRole("link", { name: "Thai", exact: true })).toBeVisible();
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  await page.goto(`/tags/${id}`);
  await expect(page.getByRole("heading", { name: "Tag not found" })).toBeVisible();
});

test("cancelling the tag delete dialog keeps the tag", async ({ page }) => {
  const name = uniqueName();
  await createTag(page, name);

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Cancel" }).click();

  await expect(dialog).toBeHidden();
  await page.reload();
  await expectReadView(page, name);
});

test("a tag recipes use cannot be deleted, and the dialog says why", async ({ page }) => {
  await page.goto("/tags");
  await main(page).getByRole("link", { name: "Godt til kaffen", exact: true }).click();

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: 'Delete tag "Godt til kaffen"?' });
  await dialog.getByRole("button", { name: "Delete" }).click();

  await expect(dialog.getByRole("alert")).toHaveText(
    "Tag 'Godt til kaffen' is used by 3 recipes and cannot be deleted",
  );
  await expect(dialog.getByRole("button")).toHaveText(["OK"]);
  await dialog.getByRole("button", { name: "OK" }).click();
  await expect(dialog).toBeHidden();
  await openFromMenu(page, "Tags", "/tags");
  await expect(main(page).getByRole("link", { name: "Godt til kaffen", exact: true })).toBeVisible();
});
