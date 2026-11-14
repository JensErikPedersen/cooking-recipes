import { expect, test, type Page } from "@playwright/test";
import { createById, csrfHeader, deleteById } from "../support/api";
import { main, uniqueName } from "../support/pages";
import { drawPicture, uploadPicture } from "../support/pictures";

// The Welcome page's picture (docs/PLAN_V8.md Part 7). The page has one picture for every account,
// so every test that sets it is here, one after another; each puts back what the page had before.

test.describe.configure({ mode: "serial" });

const BROED = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

const putWelcome = async (page: Page, picture: { id: string } | null) =>
  page.request.put("/api/v1/welcome-page", { headers: await csrfHeader(page), data: { picture } });
const welcomePictureId = async (page: Page) =>
  ((await (await page.request.get("/api/v1/welcome-page")).json()).picture?.id ?? null) as string | null;

let before: string | null;
const recipes: string[] = [];
const pictures: string[] = [];
test.beforeEach(async ({ page }) => {
  before = await welcomePictureId(page);
  await putWelcome(page, null);
});
test.afterEach(async ({ page }) => {
  await putWelcome(page, before && { id: before });
  for (const id of recipes.splice(0)) await deleteById(page, "recipes", id);
  for (const id of pictures.splice(0)) await deleteById(page, "pictures", id);
});

async function givenPicture(page: Page, name: string, altText: string) {
  const { id } = await (await uploadPicture(page, await drawPicture(page, 900, 600), name, altText)).json();
  pictures.push(id);
  return id as string;
}

const field = (page: Page) => main(page).getByRole("group", { name: "Welcome picture" });
const picker = (page: Page) => page.getByRole("dialog", { name: "Choose a picture" });
const shown = (page: Page, alt: string) =>
  main(page)
    .getByRole("img", { name: alt, exact: true })
    .evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0);

test("a picture in use cannot be deleted until every page lets it go", async ({ page }) => {
  const picture = await givenPicture(page, "Køkkenet", "Et køkken i morgenlys");
  const name = uniqueName();
  const recipe = await createById(page, "recipes", { name, category: { id: BROED }, picture: { id: picture } });
  recipes.push(recipe);
  expect((await putWelcome(page, { id: picture })).status()).toBe(200);
  expect(await welcomePictureId(page)).toBe(picture);

  const refused = await page.request.delete(`/api/v1/pictures/${picture}`, { headers: await csrfHeader(page) });
  expect(refused.status()).toBe(409);
  expect(await refused.json()).toMatchObject({
    errorCode: 711,
    message: "Picture 'Køkkenet' is used by 1 recipe and the Welcome page and cannot be deleted",
  });

  await putWelcome(page, null);
  expect(await welcomePictureId(page)).toBeNull();
  // Unlinked from the recipe, which keeps everything else.
  const unlinked = await page.request.put(`/api/v1/recipes/${recipe}`, {
    headers: await csrfHeader(page),
    data: { name, category: { id: BROED } },
  });
  expect(unlinked.status()).toBe(200);
  expect((await (await page.request.get(`/api/v1/recipes/${recipe}`)).json()).picture).toBeUndefined();

  const deleted = await page.request.delete(`/api/v1/pictures/${picture}`, { headers: await csrfHeader(page) });
  expect(deleted.status()).toBe(204);
  expect((await page.request.get(`/api/v1/pictures/${picture}/small`)).status()).toBe(404);
});

test("without a picture the Welcome page shows none, and offers to choose one", async ({ page }) => {
  await page.goto("/");

  await expect(field(page).getByRole("button", { name: "Choose a picture" })).toBeVisible();
  await expect(field(page).getByRole("button", { name: "Remove" })).toHaveCount(0);
  await expect(main(page).getByRole("img")).toHaveCount(0);
});

test("a picture chosen from the library is saved at once and shown on the Welcome page", async ({ page }) => {
  const pictureName = uniqueName();
  const pictureId = await givenPicture(page, pictureName, "Et dækket bord");
  await page.goto("/");

  await field(page).getByRole("button", { name: "Choose a picture" }).click();
  await picker(page).getByRole("button", { name: pictureName, exact: true }).click();

  await expect(picker(page)).toBeHidden();
  await expect.poll(() => shown(page, "Et dækket bord")).toBe(true);
  await expect.poll(() => welcomePictureId(page)).toBe(pictureId);
  await page.reload();
  await expect.poll(() => shown(page, "Et dækket bord")).toBe(true);

  // The picker marks it, and Cancel keeps it.
  await field(page).getByRole("button", { name: "Choose", exact: true }).click();
  await expect(picker(page).getByRole("button", { name: pictureName, exact: true })).toHaveAttribute("aria-pressed", "true");
  await picker(page).getByRole("button", { name: "Cancel" }).click();
  await expect(picker(page)).toBeHidden();
  expect(await welcomePictureId(page)).toBe(pictureId);
});

test("a picture uploaded from the Welcome page is chosen for it and joins the library", async ({ page }) => {
  const pictureName = uniqueName();
  await page.goto("/");

  await field(page).getByRole("button", { name: "Choose a picture" }).click();
  await picker(page).getByRole("button", { name: "Upload new" }).click();
  const upload = page.getByRole("dialog", { name: "Upload a picture" });
  await upload.getByLabel("Choose a file").setInputFiles({
    name: "bord.jpg",
    mimeType: "image/jpeg",
    buffer: await drawPicture(page, 800, 600),
  });
  await upload.getByLabel("Name").fill(pictureName);
  await upload.getByLabel("Alt text").fill("Et køkkenbord");
  await upload.getByRole("button", { name: "Upload" }).click();

  await expect(upload).toBeHidden();
  await expect.poll(() => shown(page, "Et køkkenbord")).toBe(true);
  const id = await welcomePictureId(page);
  expect(id).not.toBeNull();
  pictures.push(id!);
  const library = await (await page.request.get("/api/v1/pictures")).json();
  expect(library.find((picture: { id: string }) => picture.id === id).name).toBe(pictureName);
});

test("Remove takes the picture off the Welcome page at once, and the picture stays in the library", async ({ page }) => {
  const pictureId = await givenPicture(page, uniqueName(), "Et billede der fjernes");
  await putWelcome(page, { id: pictureId });
  await page.goto("/");
  await expect.poll(() => shown(page, "Et billede der fjernes")).toBe(true);

  await field(page).getByRole("button", { name: "Remove" }).click();

  await expect(main(page).getByRole("img")).toHaveCount(0);
  await expect(field(page).getByRole("button", { name: "Choose a picture" })).toBeVisible();
  await expect.poll(() => welcomePictureId(page)).toBeNull();
  await page.reload();
  await expect(field(page).getByRole("button", { name: "Choose a picture" })).toBeVisible();
  await expect(main(page).getByRole("img")).toHaveCount(0);
  expect((await page.request.get(`/api/v1/pictures/${pictureId}`)).status()).toBe(200);
});

test("the Welcome page's picture cannot be deleted on the Pictures page, which says it is shown there", async ({ page }) => {
  const pictureName = uniqueName();
  const pictureId = await givenPicture(page, pictureName, "Et billede på forsiden");
  await putWelcome(page, { id: pictureId });

  await page.goto(`/pictures/${pictureId}`);
  await expect(main(page).locator("dt:text-is('Welcome page') + dd")).toHaveText("Shown there");
  await page.getByRole("button", { name: "Delete" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Delete" }).click();

  await expect(page.getByRole("dialog").getByRole("alert")).toHaveText(
    `Picture '${pictureName}' is used by the Welcome page and cannot be deleted`,
  );
});

for (const [width, beside] of [
  [375, false],
  [768, true],
  [1024, true],
  [1440, true],
] as const) {
  test.describe(`${width} px wide`, () => {
    test.use({ viewport: { width, height: 900 } });

    test(`the Welcome page's picture stands ${beside ? "beside" : "under"} the introduction, above the shelf`, async ({ page }) => {
      const pictureId = await givenPicture(page, uniqueName(), "Et køkken");
      await putWelcome(page, { id: pictureId });
      await page.goto("/");
      await expect.poll(() => shown(page, "Et køkken")).toBe(true);

      const picture = (await main(page).getByRole("img", { name: "Et køkken" }).boundingBox())!;
      const intro = (await main(page).getByText("Keep your recipes in one place.").boundingBox())!;
      const shelf = (await main(page).getByRole("list").first().boundingBox())!;
      if (beside) {
        expect(picture.x).toBeGreaterThan(intro.x + intro.width);
        expect(picture.width).toBeCloseTo(18 * 16 - 12, 0);
      } else {
        expect(picture.y).toBeGreaterThan(intro.y + intro.height);
      }
      expect(shelf.y).toBeGreaterThan(picture.y + picture.height);
      // 4:3, in its box.
      expect(picture.width / picture.height).toBeCloseTo(4 / 3, 1);
      const { scrollWidth, clientWidth } = await page.evaluate(() => ({
        scrollWidth: document.documentElement.scrollWidth,
        clientWidth: document.documentElement.clientWidth,
      }));
      expect(scrollWidth).toBeLessThanOrEqual(clientWidth);
    });
  });
}
