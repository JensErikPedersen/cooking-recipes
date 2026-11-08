import { expect, test, type Page } from "@playwright/test";
import { main } from "../support/pages";

// The sizes of docs/PLAN_V7.md, decision 6, below the desktop: a small phone (375 px), a tablet
// upright (768) and on its side (1024). Before the MVP's Part 8b every signed-in page was about 620 px
// wide at phone size, because the header's menu would not wrap.

const FULDKORN_ID = "5d22c394-b5ce-48c3-8199-72ccc92c737c";

async function expectNoSidewaysScroll(page: Page) {
  const { scrollWidth, clientWidth } = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }));
  expect(scrollWidth).toBeLessThanOrEqual(clientWidth);
}

for (const width of [375, 768, 1024]) {
  test.describe(`${width} px wide`, () => {
    test.use({ viewport: { width, height: 800 } });

    for (const path of [
      "/",
      "/recipes",
      `/recipes/${FULDKORN_ID}`,
      `/recipes/${FULDKORN_ID}/edit`,
      "/categories",
      "/ingredients",
      "/units/new",
      "/tags",
      "/account",
      "/users",
      "/forgot-password",
      "/reset-password?token=any",
    ]) {
      test(`${path} fits without scrolling sideways`, async ({ page }) => {
        await page.goto(path);
        await expect(main(page).getByRole("heading").first()).toBeVisible();

        await expectNoSidewaysScroll(page);
      });
    }
  });
}

test.describe("on a phone", () => {
  test.use({ viewport: { width: 375, height: 800 } });

  test("a recipe's ingredient lines keep within the page margin", async ({ page }) => {
    await page.goto(`/recipes/${FULDKORN_ID}/edit`);
    const firstLine = page.getByRole("group", { name: "Ingredient line 1", exact: true });
    await expect(firstLine).toBeVisible();

    // A fieldset is as wide as its content by default; the lines once ran past the margin the other
    // fields keep, still inside the screen, so the sideways check alone would not see it.
    const lineRight = (await firstLine.boundingBox())!;
    const nameRight = (await page.getByLabel("Name").boundingBox())!;
    expect(lineRight.x + lineRight.width).toBeLessThanOrEqual(nameRight.x + nameRight.width + 1);
  });

  test("Edit and Delete are icons beside the recipe's name, still named by their words", async ({ page }) => {
    await page.goto(`/recipes/${FULDKORN_ID}`);
    const heading = (await main(page).getByRole("heading", { level: 1 }).boundingBox())!;

    // Square 44 px buttons on the heading's first line, not wrapped below it.
    for (const action of [main(page).getByRole("link", { name: "Edit" }), main(page).getByRole("button", { name: "Delete" })]) {
      const box = (await action.boundingBox())!;
      expect(Math.round(box.width)).toBe(44);
      expect(box.y).toBeLessThan(heading.y + 10);
    }
  });
});
