import { expect, test, type Page } from "@playwright/test";
import { deleteAccount, emailOf, registerThroughPage, removeEmail, uniqueUsername } from "../support/accounts";

// Each test changes the address of an account of its own, registered here - never the admin's,
// which every other test signs in with. So these start signed out.
test.use({ storageState: { cookies: [], origins: [] } });

const PASSWORD = "long enough";

const accounts: string[] = [];
test.afterEach(() => {
  for (const username of accounts.splice(0)) {
    deleteAccount(username);
  }
});

/** Registers a new account through the page, signed in as it. */
async function registerNew(page: Page) {
  const username = uniqueUsername();
  accounts.push(username);
  await registerThroughPage(page, username, PASSWORD);
  return username;
}

/** The account page's address half: the password half has a "Current password" field too. */
const emailSection = (page: Page) => page.getByRole("region", { name: "Email address" });

async function change(page: Page, email: string, current = PASSWORD) {
  const section = emailSection(page);
  await section.getByLabel("New email address").fill(email);
  await section.getByLabel("Current password").fill(current);
  await section.getByRole("button", { name: "Change email address" }).click();
}

test("the account page shows the address registered, and a new one replaces it for good", async ({ page }) => {
  const username = await registerNew(page);
  await page.getByRole("banner").getByRole("link", { name: username }).click();
  await expect(page).toHaveURL("/account");
  await expect(emailSection(page)).toContainText(`In use: ${emailOf(username)}`);
  const email = `new-${username}@example.com`;

  await change(page, email);

  await expect(emailSection(page).getByRole("status")).toHaveText("Your email address has been changed.");
  await expect(emailSection(page)).toContainText(`In use: ${email}`);
  await expect(emailSection(page).getByLabel("New email address")).toHaveValue("");
  await expect(emailSection(page).getByLabel("Current password")).toHaveValue("");
  await page.reload();
  await expect(emailSection(page)).toContainText(`In use: ${email}`);
});

test("an account without an address is told what it misses, and adds one", async ({ page }) => {
  const username = await registerNew(page);
  removeEmail(username);
  await page.goto("/account");
  await expect(emailSection(page)).toContainText("None yet. Without one, a forgotten password cannot be reset.");

  await change(page, emailOf(username));

  await expect(emailSection(page)).toContainText(`In use: ${emailOf(username)}`);
});

test("a wrong current password is shown on its field, and the address stays", async ({ page }) => {
  const username = await registerNew(page);
  await page.goto("/account");

  await change(page, `new-${username}@example.com`, "not the password");

  await expect(emailSection(page).getByLabel("Current password")).toHaveAccessibleDescription(
    "The current password is wrong",
  );
  await expect(page).toHaveURL("/account");
  await expect(emailSection(page).getByRole("status")).toHaveCount(0);
  await page.reload();
  await expect(emailSection(page)).toContainText(`In use: ${emailOf(username)}`);
});

test("another account's address, in any case, is shown on the address field", async ({ page }) => {
  const other = await registerNew(page);
  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL("/login");
  await registerNew(page);
  await page.goto("/account");
  const taken = emailOf(other).toUpperCase();

  await change(page, taken);

  await expect(emailSection(page).getByLabel("New email address")).toHaveAccessibleDescription(
    `The email address '${taken}' is taken`,
  );
});

test("a malformed address is the server's message on its field, not the browser's", async ({ page }) => {
  await registerNew(page);
  await page.goto("/account");

  await change(page, "not-an-address");

  await expect(emailSection(page).getByLabel("New email address")).toHaveAccessibleDescription(
    "Email address is not valid",
  );
});
