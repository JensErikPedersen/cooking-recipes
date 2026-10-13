import { expect, type Page } from "@playwright/test";
import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import path from "node:path";

// Housekeeping for the tests that create accounts. Nothing in the API deletes an account - by
// decision - so the accounts they create are removed with SQL, in the stack's own MySQL container.

/** A username no seed and no other test uses, and safe to put in the SQL below. */
export const uniqueUsername = () => `e2e-${randomUUID().slice(0, 8)}`;

/** The address a test account registers with, unique as its username is. */
export const emailOf = (username: string) => `${username}@example.com`;

/** Registers the account through the register page, which leaves the page signed in as it. */
export async function registerThroughPage(page: Page, username: string, password: string) {
  await page.goto("/register");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Email address").fill(emailOf(username));
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByLabel("Confirm password").fill(password);
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page).toHaveURL("/");
}

/** Deletes the account, if it exists. Only names from uniqueUsername(), since they go into SQL. */
export function deleteAccount(username: string) {
  sql(username, `DELETE FROM app_user WHERE username = '${username}'`);
}

/** Takes the account's address away, as accounts from before addresses were asked for have none. */
export function removeEmail(username: string) {
  sql(username, `UPDATE app_user SET email = NULL WHERE username = '${username}'`);
}

function sql(username: string, statement: string) {
  if (!/^e2e-[0-9a-f]{8}$/.test(username)) {
    throw new Error(`Refusing to change account '${username}': not a test account`);
  }
  execFileSync(
    "docker",
    ["compose", "exec", "-T", "mysql", "sh", "-c", `mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "${statement}"`],
    { cwd: path.join(__dirname, "..", ".."), stdio: "pipe" },
  );
}
