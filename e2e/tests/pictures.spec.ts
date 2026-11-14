import { expect, test, type Page } from "@playwright/test";
import { createById, deleteById } from "../support/api";
import { USERNAME } from "../support/auth";
import { expectReadView, listNames, main, openFromMenu, readView, uniqueName } from "../support/pages";
import { drawPicture, uploadPicture } from "../support/pictures";

// The Pictures pages (docs/PLAN_V8.md Part 5): the grid, the read view with where a picture is
// used, the upload form, the edit form and delete. Tests remove what they create - recipes first,
// as a picture in use cannot be deleted.

const BROED = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

const pictures: string[] = [];
const recipes: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of recipes.splice(0)) await deleteById(page, "recipes", id);
  for (const id of pictures.splice(0)) await deleteById(page, "pictures", id);
});

/** A picture uploaded through the API, for the tests that are not about uploading. */
async function givenPicture(page: Page, name: string, altText = `Alt for ${name}`) {
  const response = await uploadPicture(page, await drawPicture(page, 900, 600), name, altText);
  const { id } = await response.json();
  pictures.push(id);
  return id as string;
}

/** Chooses a file in the upload form's file field. */
async function chooseFile(page: Page, name: string, buffer: Buffer, mimeType = "image/jpeg") {
  await page.getByLabel(/^Choose (a|another) file$/).setInputFiles({ name, mimeType, buffer });
}

/** Whether an <img> has loaded a picture the browser could decode. */
const loaded = (page: Page, alt: string) =>
  main(page).getByRole("img", { name: alt, exact: true }).evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0);

test("the menu leads to Pictures, after Tags, and an empty library says so", async ({ page }) => {
  await page.route("**/api/v1/pictures", (route) => route.fulfill({ json: [] }));
  await page.goto("/");

  await openFromMenu(page, "Pictures", "/pictures");

  await expect(page.getByRole("heading", { name: "Pictures", level: 1 })).toBeVisible();
  await expect(main(page)).toContainText("No pictures yet.");
  const entries = await page.getByRole("navigation", { name: "Main" }).getByRole("link").allTextContents();
  expect(entries.indexOf("Pictures")).toBe(entries.indexOf("Tags") + 1);
});

test("an uploaded picture lands on its read view, shown whole, and is a thumbnail in the grid", async ({ page }) => {
  const name = uniqueName();
  const file = await drawPicture(page, 1200, 800);
  await page.goto("/pictures");
  await page.getByRole("link", { name: "New picture" }).click();
  await expect(page).toHaveURL("/pictures/new");

  await chooseFile(page, "rugbrød.jpg", file);
  // Shown from the browser before it is sent, with its name and size; the name field stays empty.
  await expect(main(page)).toContainText("rugbrød.jpg");
  await expect(page.getByLabel("Choose another file")).toBeAttached();
  await expect(page.getByLabel("Name")).toHaveValue("");
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Alt text").fill("Et rugbrød på et bræt");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(readView("pictures"));
  pictures.push(page.url().split("/").pop()!);
  await expectReadView(page, name, "Et rugbrød på et bræt", USERNAME);
  await expect.poll(() => loaded(page, "Et rugbrød på et bræt")).toBe(true);

  await openFromMenu(page, "Pictures", "/pictures");
  await expect(main(page).getByRole("link", { name, exact: true })).toBeVisible();
  await expect.poll(() => loaded(page, "Et rugbrød på et bræt")).toBe(true);
  await main(page).getByRole("link", { name, exact: true }).click();
  await expectReadView(page, name);
});

test("while it uploads the form is disabled and says what it is sending", async ({ page }) => {
  let release!: () => void;
  const held = new Promise<void>((resolve) => (release = resolve));
  await page.route("**/api/v1/pictures", async (route) => {
    if (route.request().method() === "POST") {
      await held;
    }
    await route.continue();
  });
  await page.goto("/pictures/new");
  await chooseFile(page, "langsom.jpg", await drawPicture(page, 600, 400));
  await page.getByLabel("Name").fill(uniqueName());
  await page.getByLabel("Alt text").fill("Langsomt billede");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByRole("status")).toHaveText("Uploading langsom.jpg");
  await expect(page.getByLabel("Name")).toBeDisabled();
  await expect(page.getByRole("button", { name: "Save" })).toBeDisabled();
  release();
  await expect(page).toHaveURL(readView("pictures"));
  pictures.push(page.url().split("/").pop()!);
});

test("a file that is not a picture is refused on the file field, and stays chosen", async ({ page }) => {
  await page.goto("/pictures/new");
  await chooseFile(page, "noter.jpg", Buffer.from("not a picture"));
  await page.getByLabel("Name").fill(uniqueName());
  await page.getByLabel("Alt text").fill("Ingen billede");

  await page.getByRole("button", { name: "Save" }).click();

  const fileField = page.getByLabel("Choose another file");
  await expect(fileField).toHaveAccessibleDescription(/The file is not a JPEG or PNG picture/);
  await expect(main(page)).toContainText("noter.jpg");
  // The browser cannot draw it either: its name alone, no broken picture.
  await expect(main(page).locator("img")).toHaveCount(0);
  await expect(page).toHaveURL("/pictures/new");
});

test("nothing chosen and nothing filled in is one refusal naming the file, the name and the alt text", async ({ page }) => {
  await page.goto("/pictures/new");

  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Choose a file")).toHaveAccessibleDescription(/Choose a picture to upload/);
  await expect(page.getByLabel("Name")).toHaveAccessibleDescription("Picture name is required");
  await expect(page.getByLabel("Alt text")).toHaveAccessibleDescription(/Alt text is required/);
});

test("an edited picture shows its new name and alt text, and keeps its file", async ({ page }) => {
  const name = uniqueName();
  const id = await givenPicture(page, name);
  await page.goto(`/pictures/${id}`);

  await page.getByRole("link", { name: "Edit" }).click();
  await expect(page).toHaveURL(`/pictures/${id}/edit`);
  // No file on the edit form: for another photo, upload a new one.
  await expect(page.getByLabel("Choose a file")).toHaveCount(0);
  await expect(page.getByLabel("Name")).toHaveValue(name);
  const renamed = uniqueName();
  await page.getByLabel("Name").fill(renamed);
  await page.getByLabel("Alt text").fill("Et nyt alt");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(`/pictures/${id}`);
  await expectReadView(page, renamed, "Et nyt alt");
  await expect.poll(() => loaded(page, "Et nyt alt")).toBe(true);
});

test("an unused picture is deleted and leaves the grid", async ({ page }) => {
  const name = uniqueName();
  const id = await givenPicture(page, name);
  await page.goto(`/pictures/${id}`);

  await page.getByRole("button", { name: "Delete" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Delete" }).click();

  await expect(page).toHaveURL("/pictures");
  await expect(main(page).getByRole("link", { name, exact: true })).toHaveCount(0);
  expect((await page.request.get(`/api/v1/pictures/${id}`)).status()).toBe(404);
});

test("a picture says where it is used, links there, and cannot be deleted while it is", async ({ page }) => {
  const name = uniqueName();
  const id = await givenPicture(page, name);
  const recipeName = uniqueName();
  recipes.push(await createById(page, "recipes", { name: recipeName, category: { id: BROED }, picture: { id } }));
  await page.goto(`/pictures/${id}`);

  await expect(main(page).locator("dt:text-is('Recipes') + dd")).toHaveText(recipeName);
  await expect(main(page).locator("dt:text-is('Ingredients') + dd")).toHaveText("None");
  await expect(main(page).locator("dt:text-is('Welcome page') + dd")).toHaveText("None");

  await page.getByRole("button", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Delete" }).click();
  await expect(dialog.getByRole("alert")).toHaveText(`Picture '${name}' is used by 1 recipe and cannot be deleted`);
  await dialog.getByRole("button", { name: "OK" }).click();
  await expect(dialog).toBeHidden();

  await main(page).getByRole("link", { name: recipeName, exact: true }).click();
  await expect(page).toHaveURL(`/recipes/${recipes[0]}`);
});

test("the grid lists names the Danish way, whatever order the API returns", async ({ page }) => {
  const prefix = uniqueName();
  for (const suffix of ["Ål", "Øl", "Æble", "Brød"]) {
    await givenPicture(page, `${prefix} ${suffix}`);
  }
  await page.route("**/api/v1/pictures", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: (await response.json()).reverse() });
  });

  await page.goto("/pictures");
  await expect(main(page).getByRole("link", { name: `${prefix} Brød`, exact: true })).toBeVisible();

  const ours = (await listNames(page).allTextContents()).filter((name) => name.startsWith(prefix));
  expect(ours).toEqual(["Brød", "Æble", "Øl", "Ål"].map((suffix) => `${prefix} ${suffix}`));
});

test.describe("on a desktop", () => {
  test("a thumbnail grows under the pointer, and in focus only gets the ring", async ({ page }) => {
    const name = uniqueName();
    await givenPicture(page, name);
    await page.goto("/pictures");
    const tile = main(page).getByRole("listitem").filter({ hasText: name });
    const band = tile.locator("div").first();
    const width = async () => (await band.boundingBox())!.width;
    const resting = await width();

    await tile.hover();
    await expect.poll(width).toBeCloseTo(resting * 1.12, 0);

    await page.mouse.move(0, 0);
    await expect.poll(width).toBeCloseTo(resting, 0);
    await main(page).getByRole("link", { name, exact: true }).focus();
    await page.keyboard.press("Shift+Tab");
    await page.keyboard.press("Tab");
    await expect(band).toHaveCSS("outline-style", "solid");
    expect(await width()).toBeCloseTo(resting, 0);
  });

  test("with less motion asked for, a thumbnail stays its size under the pointer", async ({ page }) => {
    await page.emulateMedia({ reducedMotion: "reduce" });
    const name = uniqueName();
    await givenPicture(page, name);
    await page.goto("/pictures");
    const tile = main(page).getByRole("listitem").filter({ hasText: name });
    const band = tile.locator("div").first();
    const resting = (await band.boundingBox())!.width;

    await tile.hover();

    // Longer than the 160 ms the growth takes when motion is allowed.
    await page.waitForTimeout(300);
    expect((await band.boundingBox())!.width).toBeCloseTo(resting, 0);
  });
});

test.describe("on a phone", () => {
  test.use({ viewport: { width: 375, height: 800 } });

  // At Playwright's screen density of 1. A real phone at density 3 rightly fetches large for a read
  // view (docs/PLAN_V8.md Part 2).
  test("the grid and the read view fetch small or medium files, never large", async ({ page }) => {
    const name = uniqueName();
    const id = await givenPicture(page, name, "Et lille billede");
    const sizes: string[] = [];
    page.on("request", (request) => {
      const match = request.url().match(new RegExp(`/api/v1/pictures/${id}/(\\w+)$`));
      if (match) sizes.push(match[1]);
    });

    await page.goto("/pictures");
    await expect.poll(() => loaded(page, "Et lille billede")).toBe(true);
    await main(page).getByRole("link", { name, exact: true }).click();
    await expect(page).toHaveURL(`/pictures/${id}`);
    await expect.poll(() => loaded(page, "Et lille billede")).toBe(true);

    expect(sizes.length).toBeGreaterThan(0);
    expect(sizes).not.toContain("large");
  });
});
