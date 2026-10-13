import { expect, test, type Page } from "@playwright/test";
import { deleteAccount, emailOf, uniqueUsername } from "../support/accounts";
import { signIn } from "../support/auth";

// Each test changes the password of an account of its own, registered here - never the admin's,
// which every other test signs in with. So these start signed out.
test.use({ storageState: { cookies: [], origins: [] } });

const OLD = "the old password";
const NEW = "the new password";

const accounts: string[] = [];
test.afterEach(() => {
  for (const username of accounts.splice(0)) {
    deleteAccount(username);
  }
});

/** Registers a new account, signed in as it, and opens the password page from the header. */
async function openAsNewUser(page: Page) {
  const username = uniqueUsername();
  accounts.push(username);
  await page.goto("/register");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Email address").fill(emailOf(username));
  await page.getByLabel("Password", { exact: true }).fill(OLD);
  await page.getByLabel("Confirm password").fill(OLD);
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page).toHaveURL("/");

  await page.getByRole("banner").getByRole("link", { name: username }).click();
  await expect(page).toHaveURL("/account/password");
  await expect(page.getByRole("heading", { name: "Change password" })).toBeVisible();
  return username;
}

async function change(page: Page, current: string, newPassword: string, confirm = newPassword) {
  await page.getByLabel("Current password").fill(current);
  await page.getByLabel("New password", { exact: true }).fill(newPassword);
  await page.getByLabel("Confirm new password").fill(confirm);
  await page.getByRole("button", { name: "Change password" }).click();
}

test("a changed password is the one that signs in afterwards, and the old one no longer does", async ({ page }) => {
  const username = await openAsNewUser(page);

  await change(page, OLD, NEW);

  await expect(page.getByRole("status")).toHaveText("Your password has been changed.");
  await expect(page.getByLabel("Current password")).toHaveValue("");
  await expect(page.getByLabel("New password", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("Confirm new password")).toHaveValue("");

  await page.getByRole("button", { name: "Log out" }).click();
  await signIn(page, username, OLD);
  await expect(page.locator("form").getByRole("alert")).toHaveText("Invalid username or password");
  await signIn(page, username, NEW);
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("banner")).toContainText(username);
});

test("the session survives the change: the next page still opens signed in", async ({ page }) => {
  const username = await openAsNewUser(page);

  await change(page, OLD, NEW);
  await expect(page.getByRole("status")).toBeVisible();

  await page.goto("/recipes");
  await expect(page).toHaveURL("/recipes");
  await expect(page.getByRole("banner")).toContainText(username);
});

test("a change signs the account out in every other browser, also after the first one's session got a new id", async ({
  page,
  browser,
}) => {
  const username = await openAsNewUser(page);
  const otherContext = await browser.newContext({ storageState: { cookies: [], origins: [] } });
  const other = await otherContext.newPage();
  await signIn(other, username, OLD);
  await expect(other).toHaveURL("/");

  await test.step("a change here sends the other browser to sign in at its next page", async () => {
    await change(page, OLD, NEW);
    await expect(page.getByRole("status")).toBeVisible();

    await other.goto("/recipes");
    await expect(other).toHaveURL("/login?next=%2Frecipes");
    await page.goto("/recipes");
    await expect(page.getByRole("banner")).toContainText(username);
  });

  // This page's session changed its id with the change above. The backend's record of the account's
  // sessions must have followed it, or the next change would miss this one.
  await test.step("a change in the other browser, signed in again, sends this one to sign in", async () => {
    await signIn(other, username, NEW);
    await expect(other).toHaveURL("/");
    await other.goto("/account/password");
    await change(other, NEW, OLD);
    await expect(other.getByRole("status")).toBeVisible();

    await page.goto("/categories");
    await expect(page).toHaveURL("/login?next=%2Fcategories");
  });

  await otherContext.close();
});

test("a wrong current password is shown on its field, and does not end the session", async ({ page }) => {
  await openAsNewUser(page);

  await change(page, "not the password", NEW);

  await expect(page.getByLabel("Current password")).toHaveAccessibleDescription("The current password is wrong");
  await expect(page).toHaveURL("/account/password");
  await expect(page.getByRole("status")).toHaveCount(0);
});

test("a confirmation that differs is shown on the confirm field, from the server", async ({ page }) => {
  await openAsNewUser(page);

  await change(page, OLD, NEW, NEW + "!");

  await expect(page.getByLabel("Confirm new password")).toHaveAccessibleDescription("The passwords do not match");
});

test("a new password that is too short is shown on the new password field", async ({ page }) => {
  await openAsNewUser(page);

  await change(page, OLD, "short");

  await expect(page.getByLabel("New password", { exact: true })).toHaveAccessibleDescription(
    "Password must be at least 8 characters",
  );
});
