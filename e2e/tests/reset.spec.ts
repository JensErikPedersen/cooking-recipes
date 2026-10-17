import { expect, test, type Page } from "@playwright/test";
import { deleteAccount, emailOf, registerThroughPage, uniqueUsername } from "../support/accounts";
import { signIn } from "../support/auth";
import { deleteMailsTo, mailsTo, resetLinkTo } from "../support/mailpit";
import { main } from "../support/pages";

// A forgotten password, reset through the mail Mailpit catches, as a visitor would: these start
// signed out. Each test registers an account of its own, never touching the admin's.
test.use({ storageState: { cookies: [], origins: [] } });

const OLD = "the old password";
const NEW = "the new password";
const SENT = "If an account has that address, a mail with a link to choose a new password is on its way.";
const INVALID = "This reset link is invalid or has expired.";

// Accounts and the addresses mailed to, removed after each test whether it passed or not.
const accounts: string[] = [];
const addresses: string[] = [];
test.afterEach(async () => {
  for (const address of addresses.splice(0)) {
    await deleteMailsTo(address);
  }
  for (const username of accounts.splice(0)) {
    deleteAccount(username);
  }
});

/** Registers an account through the page, then signs it out, as one who forgot would be. */
async function registerAndSignOut(page: Page) {
  const username = uniqueUsername();
  accounts.push(username);
  addresses.push(emailOf(username));
  await registerThroughPage(page, username, OLD);
  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL("/login");
  return username;
}

async function askForLink(page: Page, email: string) {
  await page.goto("/forgot-password");
  await page.getByLabel("Email address").fill(email);
  await page.getByRole("button", { name: "Send reset link" }).click();
}

async function setNewPassword(page: Page, password: string, confirm = password) {
  await page.getByLabel("New password", { exact: true }).fill(password);
  await page.getByLabel("Confirm new password").fill(confirm);
  await page.getByRole("button", { name: "Set new password" }).click();
}

test("the sign-in page leads to the forgotten password page, and back", async ({ page }) => {
  await page.goto("/login");

  await page.getByRole("link", { name: "Forgot your password?" }).click();

  await expect(page).toHaveURL("/forgot-password");
  await expect(page.getByRole("heading", { name: "Forgot your password?" })).toBeVisible();
  await page.getByRole("link", { name: "Back to sign in" }).click();
  await expect(page).toHaveURL("/login");
});

test("a forgotten password is reset through the mailed link, and only the new one signs in", async ({ page }) => {
  const username = await registerAndSignOut(page);
  await page.getByRole("link", { name: "Forgot your password?" }).click();
  await page.getByLabel("Email address").fill(emailOf(username).toUpperCase());
  await page.getByRole("button", { name: "Send reset link" }).click();
  await expect(page.getByRole("status")).toHaveText(SENT);

  await page.goto(await resetLinkTo(emailOf(username)));
  await expect(page.getByRole("heading", { name: "Choose a new password" })).toBeVisible();
  await setNewPassword(page, NEW);

  await expect(page).toHaveURL("/login?reset");
  await expect(page.getByRole("status")).toHaveText("Your password has been reset. Sign in with the new one.");
  await signIn(page, username, OLD);
  await expect(page.locator("form").getByRole("alert")).toHaveText("Invalid username or password");
  await signIn(page, username, NEW);
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("banner")).toContainText(username);
});

test("the link works once: followed again it says so, and leads to ask for a new one", async ({ page }) => {
  const username = await registerAndSignOut(page);
  await askForLink(page, emailOf(username));
  const link = await resetLinkTo(emailOf(username));
  await page.goto(link);
  await setNewPassword(page, NEW);
  await expect(page).toHaveURL("/login?reset");

  await page.goto(link);
  await setNewPassword(page, "yet another password");

  // In main: Next's route announcer is an alert too.
  await expect(main(page).getByRole("alert")).toContainText(INVALID);
  await expect(page).toHaveURL(link);
  await page.getByRole("link", { name: "Ask for a new link" }).click();
  await expect(page).toHaveURL("/forgot-password");
  await signIn(page, username, NEW);
  await expect(page).toHaveURL("/");
});

test("an unknown address gets the same message, and no mail", async ({ page }) => {
  const unknown = emailOf(uniqueUsername());
  addresses.push(unknown);

  await askForLink(page, unknown);

  await expect(page.getByRole("status")).toHaveText(SENT);
  expect(await mailsTo(unknown)).toHaveLength(0);
});

test("a malformed address is the server's message on its field, not the browser's", async ({ page }) => {
  await askForLink(page, "not-an-address");

  await expect(page.getByLabel("Email address")).toHaveAccessibleDescription("Email address is not valid");
  await expect(page.getByRole("status")).toHaveCount(0);
});

test("a new password breaking the rules is shown on its field, and the link still works", async ({ page }) => {
  const username = await registerAndSignOut(page);
  await askForLink(page, emailOf(username));
  await page.goto(await resetLinkTo(emailOf(username)));

  await setNewPassword(page, NEW, NEW + "!");

  await expect(page.getByLabel("Confirm new password")).toHaveAccessibleDescription("The passwords do not match");
  await setNewPassword(page, "short");
  await expect(page.getByLabel("New password", { exact: true })).toHaveAccessibleDescription(
    "Password must be at least 8 characters",
  );
  await setNewPassword(page, NEW);
  await expect(page).toHaveURL("/login?reset");
});

test("typing waits until the page is ready, so an address typed early is not lost", async ({ page }) => {
  const username = await registerAndSignOut(page);
  // As on the login and register pages: with the script delayed and the load event not awaited,
  // early typing would be lost without the wait.
  await page.route("**/_next/static/**", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    await route.continue();
  });
  await page.goto("/forgot-password", { waitUntil: "commit" });

  await page.getByLabel("Email address").fill(emailOf(username));
  await page.getByRole("button", { name: "Send reset link" }).click();

  await expect(page.getByRole("status")).toHaveText(SENT);
  await expect.poll(async () => (await mailsTo(emailOf(username))).length).toBe(1);
});
