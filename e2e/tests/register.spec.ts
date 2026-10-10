import { expect, test, type Page } from "@playwright/test";
import { deleteAccount, uniqueUsername } from "../support/accounts";
import { deleteById } from "../support/api";
import { signIn, USERNAME } from "../support/auth";
import { createThroughForm, expectCreatedNowBy, uniqueName } from "../support/pages";

// These start signed out, as a visitor who has no account yet does.
test.use({ storageState: { cookies: [], origins: [] } });

const PASSWORD = "long enough";

// Accounts and categories a test created, removed after it whether it passed or not.
const accounts: string[] = [];
const categories: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of categories.splice(0)) {
    await deleteById(page, "categories", id);
  }
  for (const username of accounts.splice(0)) {
    deleteAccount(username);
  }
});

async function register(page: Page, username: string, password = PASSWORD, confirm = password) {
  await page.goto("/register");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByLabel("Confirm password").fill(confirm);
  await page.getByRole("button", { name: "Register" }).click();
}

/** Registers a new account and leaves the page signed in as it. */
async function registerNew(page: Page) {
  const username = uniqueUsername();
  accounts.push(username);
  await register(page, username);
  await expect(page).toHaveURL("/");
  return username;
}

test("the sign-in page leads to registration, and back", async ({ page }) => {
  await page.goto("/login");

  await page.getByRole("link", { name: "Create an account" }).click();
  await expect(page).toHaveURL("/register");
  await expect(page.getByRole("heading", { name: "Create an account" })).toBeVisible();

  await page.getByRole("link", { name: "Back to sign in" }).click();
  await expect(page).toHaveURL("/login");
});

test("registering signs the new user in on the Welcome page, and they can sign in again later", async ({ page }) => {
  const username = await registerNew(page);
  await expect(page.getByRole("banner")).toContainText(username);

  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL("/login");
  await signIn(page, username, PASSWORD);

  await expect(page).toHaveURL("/");
  await expect(page.getByRole("banner")).toContainText(username);
});

test("a taken username, in any case, is shown on the username field", async ({ page }) => {
  const taken = USERNAME.toUpperCase();

  await register(page, taken);

  await expect(page.getByLabel("Username")).toHaveAccessibleDescription(`The username '${taken}' is taken`);
  await expect(page).toHaveURL("/register");
});

test("a confirmation that differs is shown on the confirm field, from the server", async ({ page }) => {
  const username = uniqueUsername();
  accounts.push(username);

  await register(page, username, PASSWORD, PASSWORD + "!");

  await expect(page.getByLabel("Confirm password")).toHaveAccessibleDescription("The passwords do not match");
  await expect(page).toHaveURL("/register");
});

test("a password that is too short is shown on the password field", async ({ page }) => {
  const username = uniqueUsername();
  accounts.push(username);

  await register(page, username, "short", "short");

  await expect(page.getByLabel("Password", { exact: true })).toHaveAccessibleDescription(
    "Password must be at least 8 characters",
  );
  await expect(page).toHaveURL("/register");
});

test("what a new user creates is created by them", async ({ page }) => {
  const username = await registerNew(page);

  const id = await createThroughForm(page, "categories", { Name: uniqueName() });
  categories.push(id);
  await page.goto(`/categories/${id}/edit`);

  await expectCreatedNowBy(page, username);
});
