import { expect, test, type Page } from "@playwright/test";
import { danish, main } from "../support/pages";

// The search field on the lists that have one. Each term is typed in another case than the names it
// finds, and the names other tests create - "E2E " and a hex part - can contain none of them.
const LISTS = [
  { path: "/categories", title: "categories", term: "ER", found: ["Dessert", "Kager"] },
  { path: "/ingredients", title: "ingredients", term: "MEL", found: ["Fuldkorns hvedemel", "Hvedemel", "Rugmel"] },
  { path: "/tags", title: "tags", term: "GODT", found: ["Godt til grillen", "Godt til kaffen"] },
  {
    path: "/recipes",
    title: "recipes",
    term: "BRØD",
    found: ["Fuldkorns hvedebrød", "Hvedebrød med Rugmel", "Ølandshvedebrød"],
  },
];

const names = (page: Page) => main(page).locator("tbody tr td:first-child");

async function search(page: Page, term: string) {
  await page.getByRole("searchbox", { name: "Search by name" }).fill(term);
  await page.getByRole("button", { name: "Search" }).click();
}

for (const { path, title, term, found } of LISTS) {
  test(`searching ${title} for ${term} lists only the ones whose name contains it, in any case`, async ({ page }) => {
    await page.goto(path);
    await expect(names(page).first()).toBeVisible();

    await search(page, term);

    await expect(names(page)).toHaveText(found.toSorted(danish));
  });

  test(`searching ${title} for a name nothing contains says so`, async ({ page }) => {
    await page.goto(path);

    await search(page, "no such name anywhere");

    await expect(main(page)).toContainText(`No ${title} match "no such name anywhere".`);
    await expect(main(page).getByRole("table")).toHaveCount(0);
  });
}

test("clearing the search and searching again brings back the whole list", async ({ page }) => {
  await page.goto("/categories");
  await expect(names(page).first()).toBeVisible();
  // Tests running alongside add and remove "E2E " categories at any moment, so the list is compared
  // without them: all the others back, in order. Compared whole, it once failed on one deleted between.
  const all = (await names(page).allTextContents()).filter((name) => !name.startsWith("E2E "));
  await search(page, "ER");
  await expect(names(page)).toHaveText(["Dessert", "Kager"]);

  await search(page, "");

  await expect(names(page)).toContainText(all);
});

test("Enter in the field searches, as the button does", async ({ page }) => {
  await page.goto("/tags");
  await expect(names(page).first()).toBeVisible();

  await page.getByRole("searchbox", { name: "Search by name" }).fill("GODT");
  await page.getByRole("searchbox", { name: "Search by name" }).press("Enter");

  await expect(names(page)).toHaveText(["Godt til grillen", "Godt til kaffen"]);
});

test("the search asks the API rather than filtering the list the page already has", async ({ page }) => {
  await page.goto("/ingredients");
  await expect(names(page).first()).toBeVisible();

  const request = page.waitForRequest((request) => new URL(request.url()).pathname === "/api/v1/ingredients");
  await search(page, "MEL");

  expect(new URL((await request).url()).searchParams.get("name")).toBe("MEL");
});

test("a search made before the whole list has arrived is what the page shows, whatever arrives last", async ({
  page,
}) => {
  // The page's first, unfiltered list is held back until the search has been answered.
  let release!: () => void;
  const held = new Promise<void>((resolve) => (release = resolve));
  const unfiltered = (url: URL) => url.pathname === "/api/v1/categories" && !url.searchParams.has("name");
  await page.route(unfiltered, async (route) => {
    await held;
    await route.continue();
  });
  await page.goto("/categories");

  await search(page, "ER");
  await expect(names(page)).toHaveText(["Dessert", "Kager"]);
  const late = page.waitForResponse((response) => unfiltered(new URL(response.url())));
  release();
  await late;

  await expect(names(page)).toHaveText(["Dessert", "Kager"]);
});

test("units have no search: a handful of measures needs none", async ({ page }) => {
  await page.goto("/units");
  await expect(names(page).first()).toBeVisible();

  await expect(page.getByRole("searchbox")).toHaveCount(0);
});
