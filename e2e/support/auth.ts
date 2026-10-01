import { expect, type Page } from "@playwright/test";
import path from "node:path";

// The stack's own .env supplies the account. Loaded here, by the module that reads it, because
// imports run before any other code in playwright.config.ts.
process.loadEnvFile(path.join(__dirname, "..", "..", ".env"));

export const USERNAME = process.env.APP_ADMIN_USERNAME!;
export const PASSWORD = process.env.APP_ADMIN_PASSWORD!;

/** Where the signed-in session is saved for every test that starts signed in. Gitignored. */
export const STORAGE_STATE = path.join(__dirname, "..", ".auth", "user.json");

/** Signs in through the login page, as a user would. */
export async function signIn(page: Page, username = USERNAME, password = PASSWORD) {
  await page.goto("/login");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

/** The signed-in shell: the header shows who is signed in. */
export async function expectSignedIn(page: Page) {
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("banner")).toContainText(USERNAME);
}
