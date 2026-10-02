import { expect, type Page } from "@playwright/test";
import { randomUUID } from "node:crypto";

// What every entity's spec does the same way: find things as a user would, and check the
// read view and the audit fields.

/** The order the app sorts names in: Danish, whatever the locale of the browser or of Node. */
export const danish = (a: string, b: string) => a.localeCompare(b, "da");

/** A name no seed and no other test uses. */
export const uniqueName = () => `E2E ${randomUUID().slice(0, 8)}`;

export const main = (page: Page) => page.getByRole("main");

/** The URL of a read view in a collection, such as /units/<uuid>. */
export const readView = (collection: string) => new RegExp(`/${collection}/[0-9a-f-]{36}$`);

/** Clicks an entry of the header menu and lands on its list. */
export async function openFromMenu(page: Page, label: string, href: string) {
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: label }).click();
  await expect(page).toHaveURL(href);
}

/** The read view: the name as its heading, the given values on the page, and no input field. */
export async function expectReadView(page: Page, name: string, ...values: string[]) {
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  for (const value of values) {
    await expect(main(page)).toContainText(value);
  }
  await expect(page.getByRole("textbox")).toHaveCount(0);
}

/** On an edit form: created by the given user, and created now - read as UTC, so the label is true. */
export async function expectCreatedNowBy(page: Page, username: string) {
  await expect(main(page).locator("dt:text-is('Created by') + dd")).toHaveText(username);
  const created = await main(page).locator("dt:text-is('Created') + dd").textContent();
  expect(created).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$/);
  const createdAt = Date.parse(created!.replace(" ", "T").replace(" UTC", ":00Z"));
  expect(Math.abs(Date.now() - createdAt)).toBeLessThan(2 * 60 * 1000);
}

/** Fills the new-entity form's labelled fields, saves, and returns the id the read view landed on. */
export async function createThroughForm(page: Page, collection: string, fields: Record<string, string>) {
  await page.goto(`/${collection}/new`);
  for (const [label, value] of Object.entries(fields)) {
    await page.getByLabel(label).fill(value);
  }
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(readView(collection));
  return page.url().split("/").pop()!;
}
