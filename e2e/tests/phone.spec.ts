import { expect, test, type Page } from "@playwright/test";
import { main, openFromMenu } from "../support/pages";

// At phone width - 375 px, a small phone. Before Part 8b every signed-in page was about 620 px
// wide, because the header's menu would not wrap.
test.use({ viewport: { width: 375, height: 800 } });

const FULDKORN_ID = "5d22c394-b5ce-48c3-8199-72ccc92c737c";

async function expectNoSidewaysScroll(page: Page) {
  const { scrollWidth, clientWidth } = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }));
  expect(scrollWidth).toBeLessThanOrEqual(clientWidth);
}

for (const path of [
  "/",
  "/recipes",
  `/recipes/${FULDKORN_ID}`,
  `/recipes/${FULDKORN_ID}/edit`,
  "/categories",
  "/ingredients",
  "/units/new",
  "/tags",
]) {
  test(`${path} fits a phone screen without scrolling sideways`, async ({ page }) => {
    await page.goto(path);
    await expect(main(page).getByRole("heading").first()).toBeVisible();

    await expectNoSidewaysScroll(page);
  });
}

test("every menu entry is reachable on a phone", async ({ page }) => {
  await page.goto("/");

  for (const [label, href] of [
    ["Recipes", "/recipes"],
    ["Categories", "/categories"],
    ["Ingredients", "/ingredients"],
    ["Units", "/units"],
    ["Tags", "/tags"],
  ]) {
    await openFromMenu(page, label, href);
  }
  await expect(page.getByRole("button", { name: "Log out" })).toBeVisible();
});

test("a recipe's ingredient lines keep within the page margin on a phone", async ({ page }) => {
  await page.goto(`/recipes/${FULDKORN_ID}/edit`);
  const firstLine = page.getByRole("group", { name: "Ingredient line 1", exact: true });
  await expect(firstLine).toBeVisible();

  // A fieldset is as wide as its content by default; the lines once ran past the margin the other
  // fields keep, still inside the screen, so the sideways check alone would not see it.
  const lineRight = (await firstLine.boundingBox())!;
  const nameRight = (await page.getByLabel("Name").boundingBox())!;
  expect(lineRight.x + lineRight.width).toBeLessThanOrEqual(nameRight.x + nameRight.width + 1);
});
