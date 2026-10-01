import { expect, test } from "@playwright/test";
import { expectSignedIn, PASSWORD, signIn, USERNAME } from "../support/auth";

// These start signed out, and sign in on their own session - so logging out here cannot end the
// session the other tests share.
test.use({ storageState: { cookies: [], origins: [] } });

// Next.js renders its own role="alert" route announcer, so the error is looked up inside the form.
const errorMessage = (page: import("@playwright/test").Page) => page.locator("form").getByRole("alert");

test("a visitor who has not signed in is sent to the login page", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveURL("/login");
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
});

test("a wrong password shows an error and stays on the login page", async ({ page }) => {
  await signIn(page, USERNAME, "definitely-not-the-password");

  await expect(errorMessage(page)).toHaveText("Invalid username or password");
  await expect(page).toHaveURL("/login");
});

test("an unknown user gets exactly the same error as a wrong password", async ({ page }) => {
  await signIn(page, "no-such-person", PASSWORD);

  await expect(errorMessage(page)).toHaveText("Invalid username or password");
});

test("the right credentials enter the application", async ({ page }) => {
  await signIn(page);

  await expectSignedIn(page);
});

test("logging out returns to the login page, and the back button does not re-enter", async ({ page }) => {
  await signIn(page);
  await expectSignedIn(page);

  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL("/login");

  await page.goBack();
  await expect(page).toHaveURL("/login");
  await expect(page.getByRole("banner")).toHaveCount(0);
});
