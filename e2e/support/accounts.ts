import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import path from "node:path";

// Housekeeping for the registration tests. Nothing in the API deletes an account - by decision -
// so the accounts they create are removed with SQL, in the stack's own MySQL container.

/** A username no seed and no other test uses, and safe to put in the SQL below. */
export const uniqueUsername = () => `e2e-${randomUUID().slice(0, 8)}`;

/** Deletes the account, if it exists. Only names from uniqueUsername(), since they go into SQL. */
export function deleteAccount(username: string) {
  if (!/^e2e-[0-9a-f]{8}$/.test(username)) {
    throw new Error(`Refusing to delete account '${username}': not a test account`);
  }
  execFileSync(
    "docker",
    [
      "compose",
      "exec",
      "-T",
      "mysql",
      "sh",
      "-c",
      `mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "DELETE FROM app_user WHERE username = '${username}'"`,
    ],
    { cwd: path.join(__dirname, "..", ".."), stdio: "pipe" },
  );
}
