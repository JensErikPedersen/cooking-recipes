import { expect, test, type Browser, type Page } from "@playwright/test";
import { deleteAccount, registerThroughPage, uniqueUsername } from "../support/accounts";
import { signIn, USERNAME } from "../support/auth";
import { main, openFromMenu } from "../support/pages";

// The users page, as the admin from .env - every test here starts signed in as that admin - and as
// the accounts the tests register for themselves, each in a browser of its own. The admin's own
// account is never changed.

const PASSWORD = "an account password";

const accounts: string[] = [];
test.afterEach(() => {
  for (const username of accounts.splice(0)) {
    deleteAccount(username);
  }
});

/** Registers a new account in a browser of its own, signed in there. */
async function newAccount(browser: Browser) {
  const username = uniqueUsername();
  accounts.push(username);
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
  const page = await context.newPage();
  await registerThroughPage(page, username, PASSWORD);
  return { username, page };
}

/** The row whose username is exactly this - "admin" is also in every row of an admin. */
const row = (page: Page, username: string) =>
  main(page)
    .getByRole("row")
    .filter({ has: page.getByRole("cell", { name: username, exact: true }) });

test("the admin finds Users in the menu, with every account, its role and status", async ({ page, browser }) => {
  const { username } = await newAccount(browser);

  await page.goto("/");
  await openFromMenu(page, "Users", "/users");

  await expect(page.getByRole("heading", { name: "Users" })).toBeVisible();
  await expect(row(page, username).getByRole("cell")).toContainText([username, "User", "Enabled"]);
  await expect(row(page, USERNAME).getByRole("cell")).toContainText([USERNAME, "Admin", "Enabled"]);
});

test("the admin's own row offers nothing to change", async ({ page }) => {
  await page.goto("/users");

  await expect(row(page, USERNAME)).toBeVisible();
  await expect(row(page, USERNAME).getByRole("button")).toHaveCount(0);
});

test("a disabled account is signed out and cannot sign in, and enabled again it can", async ({ page, browser }) => {
  const account = await newAccount(browser);
  await page.goto("/users");

  await row(page, account.username).getByRole("button", { name: "Disable" }).click();

  await expect(row(page, account.username)).toContainText("Disabled");
  await account.page.goto("/recipes");
  await expect(account.page).toHaveURL("/login?next=%2Frecipes");
  await signIn(account.page, account.username, PASSWORD);
  await expect(account.page.locator("form").getByRole("alert")).toHaveText("Invalid username or password");

  await row(page, account.username).getByRole("button", { name: "Enable" }).click();

  await expect(row(page, account.username)).toContainText("Enabled");
  await signIn(account.page, account.username, PASSWORD);
  await expect(account.page).toHaveURL("/");
});

test("an account made admin has Users in its menu once it signs in again", async ({ page, browser }) => {
  const account = await newAccount(browser);
  await expect(account.page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Users" })).toHaveCount(0);
  await page.goto("/users");

  await row(page, account.username).getByRole("button", { name: "Make admin" }).click();

  await expect(row(page, account.username).getByRole("cell").nth(1)).toHaveText("Admin");
  await expect(row(page, account.username).getByRole("button", { name: "Remove admin" })).toBeVisible();
  // Its session carried the old role, and has ended.
  await account.page.goto("/recipes");
  await expect(account.page).toHaveURL("/login?next=%2Frecipes");
  await signIn(account.page, account.username, PASSWORD);
  await openFromMenu(account.page, "Users", "/users");
  await expect(row(account.page, USERNAME)).toBeVisible();
});

test("an account whose admin role is removed loses the Users page", async ({ page, browser }) => {
  const account = await newAccount(browser);
  await page.goto("/users");
  await row(page, account.username).getByRole("button", { name: "Make admin" }).click();
  await expect(row(page, account.username).getByRole("button", { name: "Remove admin" })).toBeVisible();

  await row(page, account.username).getByRole("button", { name: "Remove admin" }).click();

  await expect(row(page, account.username).getByRole("cell").nth(1)).toHaveText("User");
  // Straight to sign in, with the ended session's cookie still in the browser: that first attempt
  // was once refused with a 401, before the login could run.
  await signIn(account.page, account.username, PASSWORD);
  await expect(account.page).toHaveURL("/");
  await account.page.goto("/users");
  await expect(account.page.getByRole("heading", { name: "Page not found" })).toBeVisible();
});

test("a user has no Users in the menu, and /users is not found - the list is never asked for", async ({ browser }) => {
  const account = await newAccount(browser);
  const asked: string[] = [];
  account.page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/v1/users")) {
      asked.push(request.url());
    }
  });

  await expect(account.page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Users" })).toHaveCount(0);
  await account.page.goto("/users");

  await expect(account.page.getByRole("heading", { name: "Page not found" })).toBeVisible();
  await expect(main(account.page).getByRole("table")).toHaveCount(0);
  await expect(account.page.getByRole("banner")).toContainText(account.username);
  expect(asked).toEqual([]);
});
