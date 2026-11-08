import { expect, test, type Page } from "@playwright/test";
import { USERNAME } from "../support/auth";
import { menuButton } from "../support/pages";

// The header's menu (docs/PLAN_V7.md, decision 5): on a phone a panel behind the Menu button, a
// disclosure that also holds the account and Log out; from md always in the header. Every test here
// is signed in as the admin from .env, so Users is among the entries.

const ENTRIES = ["Recipes", "Categories", "Ingredients", "Units", "Tags", "Users"];

const nav = (page: Page) => page.getByRole("navigation", { name: "Main" });
// Beside the menu: in the phone panel with it, in the band from md.
const account = (page: Page) => page.getByRole("banner").getByRole("link", { name: USERNAME, exact: true });
const logOut = (page: Page) => page.getByRole("banner").getByRole("button", { name: "Log out" });

test.describe("on a phone", () => {
  test.use({ viewport: { width: 375, height: 800 } });

  test.beforeEach(async ({ page }) => {
    await page.goto("/recipes");
    await expect(menuButton(page)).toBeVisible();
  });

  test("the menu is hidden until the button opens it, and the button says so", async ({ page }) => {
    await expect(menuButton(page)).toHaveAttribute("aria-expanded", "false");
    await expect(nav(page)).toBeHidden();

    await menuButton(page).click();

    await expect(menuButton(page)).toHaveAttribute("aria-expanded", "true");
    for (const label of ENTRIES) {
      await expect(nav(page).getByRole("link", { name: label, exact: true })).toBeVisible();
    }
    await expect(account(page)).toBeVisible();
    await expect(logOut(page)).toBeVisible();
    await expect(nav(page).getByRole("link", { name: "Recipes", exact: true })).toHaveAttribute("aria-current", "page");

    await menuButton(page).click();

    await expect(menuButton(page)).toHaveAttribute("aria-expanded", "false");
    await expect(nav(page)).toBeHidden();
    await expect(account(page)).toBeHidden();
    await expect(logOut(page)).toBeHidden();
  });

  test("choosing an entry goes there and closes the menu, which then marks it", async ({ page }) => {
    await menuButton(page).click();
    await nav(page).getByRole("link", { name: "Categories", exact: true }).click();

    await expect(page).toHaveURL("/categories");
    await expect(page.getByRole("heading", { name: "Categories" })).toBeVisible();
    await expect(nav(page)).toBeHidden();

    await menuButton(page).click();
    await expect(nav(page).getByRole("link", { name: "Categories", exact: true })).toHaveAttribute("aria-current", "page");
    await expect(nav(page).getByRole("link", { name: "Recipes", exact: true })).not.toHaveAttribute("aria-current");
  });

  test("Escape closes the menu and puts focus back on its button", async ({ page }) => {
    await menuButton(page).click();
    await nav(page).getByRole("link", { name: "Units", exact: true }).focus();

    await page.keyboard.press("Escape");

    await expect(nav(page)).toBeHidden();
    await expect(menuButton(page)).toHaveAttribute("aria-expanded", "false");
    await expect(menuButton(page)).toBeFocused();
  });

  test("the account page is in the menu too, and choosing it closes the menu", async ({ page }) => {
    await menuButton(page).click();
    await account(page).click();

    await expect(page).toHaveURL("/account");
    await expect(nav(page)).toBeHidden();
  });
});

for (const width of [768, 1280]) {
  test.describe(`${width} px wide`, () => {
    test.use({ viewport: { width, height: 800 } });

    test("the menu is in the header, with no Menu button", async ({ page }) => {
      await page.goto("/recipes");
      await expect(nav(page)).toBeVisible();

      await expect(menuButton(page)).toBeHidden();
      for (const label of ENTRIES) {
        await expect(nav(page).getByRole("link", { name: label, exact: true })).toBeVisible();
      }
      await expect(nav(page).getByRole("link", { name: "Recipes", exact: true })).toHaveAttribute("aria-current", "page");
      await expect(account(page)).toBeVisible();
      await expect(logOut(page)).toBeVisible();
    });
  });
}
