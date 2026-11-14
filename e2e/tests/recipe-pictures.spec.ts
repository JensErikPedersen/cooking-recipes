import { expect, test, type Page } from "@playwright/test";
import { createById, deleteById } from "../support/api";
import { main, readView, uniqueName } from "../support/pages";
import { drawPicture, uploadPicture } from "../support/pictures";

// A picture on a recipe and an ingredient (docs/PLAN_V8.md Part 6): the picture field and its
// picker on both forms, the recipe card and the read views. Tests remove what they create - recipes
// and ingredients first, as a picture in use cannot be deleted.

const BROED = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

const recipes: string[] = [];
const ingredients: string[] = [];
const pictures: string[] = [];
test.afterEach(async ({ page }) => {
  for (const id of recipes.splice(0)) await deleteById(page, "recipes", id);
  for (const id of ingredients.splice(0)) await deleteById(page, "ingredients", id);
  for (const id of pictures.splice(0)) await deleteById(page, "pictures", id);
});

async function givenPicture(page: Page, name: string, altText: string) {
  const response = await uploadPicture(page, await drawPicture(page, 900, 600), name, altText);
  const { id } = await response.json();
  pictures.push(id);
  return id as string;
}

async function givenRecipe(page: Page, name: string, pictureId?: string) {
  const id = await createById(page, "recipes", {
    name,
    category: { id: BROED },
    ...(pictureId && { picture: { id: pictureId } }),
  });
  recipes.push(id);
  return id;
}

const field = (page: Page) => page.getByRole("group", { name: "Picture", exact: true });
const picker = (page: Page) => page.getByRole("dialog", { name: "Choose a picture" });

/** Whether the read view or a card shows a loaded picture with this alt text. */
const shown = (page: Page, alt: string) =>
  main(page)
    .getByRole("img", { name: alt, exact: true })
    .evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0);

test("a recipe is given a picture from the library, shown on its read view and its card", async ({ page }) => {
  const pictureName = uniqueName();
  const pictureId = await givenPicture(page, pictureName, "Et brød på et bræt");
  const recipeName = uniqueName();
  const recipeId = await givenRecipe(page, recipeName);
  await page.goto(`/recipes/${recipeId}/edit`);
  await expect(field(page)).toContainText("None");

  await field(page).getByRole("button", { name: "Choose" }).click();
  await picker(page).getByRole("button", { name: pictureName, exact: true }).click();

  await expect(picker(page)).toBeHidden();
  await expect(field(page)).toContainText(pictureName);
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/recipes/${recipeId}`);
  await expect.poll(() => shown(page, "Et brød på et bræt")).toBe(true);

  await page.goto(`/recipes?name=${encodeURIComponent(recipeName)}`);
  await page.getByRole("searchbox").fill(recipeName);
  await page.getByRole("button", { name: "Search" }).click();
  const card = main(page).getByRole("listitem").filter({ has: page.getByRole("heading", { name: recipeName }) });
  await expect(card.getByRole("img", { name: "Et brød på et bræt" })).toBeVisible();

  await page.goto(`/pictures/${pictureId}`);
  await expect(main(page).locator("dt:text-is('Recipes') + dd")).toHaveText(recipeName);
});

test("a picture uploaded from the recipe form is chosen for the new recipe and joins the library", async ({ page }) => {
  const recipeName = uniqueName();
  const pictureName = uniqueName();
  await page.goto("/recipes/new");
  await page.getByLabel("Name").fill(recipeName);
  await page.getByLabel("Category").selectOption({ label: "Brød" });

  await field(page).getByRole("button", { name: "Choose" }).click();
  await picker(page).getByRole("button", { name: "Upload new" }).click();
  const upload = page.getByRole("dialog", { name: "Upload a picture" });
  await upload.getByLabel("Choose a file").setInputFiles({
    name: "boller.jpg",
    mimeType: "image/jpeg",
    buffer: await drawPicture(page, 800, 600),
  });
  await upload.getByLabel("Name").fill(pictureName);
  await upload.getByLabel("Alt text").fill("Boller på en plade");
  await upload.getByRole("button", { name: "Upload" }).click();

  await expect(upload).toBeHidden();
  await expect(field(page)).toContainText(pictureName);
  // The recipe's own Name is untouched by the picker's.
  await expect(page.getByLabel("Name")).toHaveValue(recipeName);
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page).toHaveURL(readView("recipes"));
  recipes.push(page.url().split("/").pop()!);
  await expect.poll(() => shown(page, "Boller på en plade")).toBe(true);
  const library = await (await page.request.get("/api/v1/pictures")).json();
  const uploaded = library.find((picture: { name: string }) => picture.name === pictureName);
  expect(uploaded).toBeDefined();
  pictures.push(uploaded.id);
});

test("an upload refused in the picker shows why, and pressing Enter there does not save the recipe", async ({ page }) => {
  const recipeName = uniqueName();
  const recipeId = await givenRecipe(page, recipeName);
  await page.goto(`/recipes/${recipeId}/edit`);
  await field(page).getByRole("button", { name: "Choose" }).click();
  await picker(page).getByRole("button", { name: "Upload new" }).click();
  const upload = page.getByRole("dialog", { name: "Upload a picture" });

  await upload.getByLabel("Name").press("Enter");

  await expect(upload.getByLabel("Choose a file")).toHaveAccessibleDescription(/Choose a picture to upload/);
  await expect(upload.getByLabel("Name")).toHaveAccessibleDescription("Picture name is required");
  await expect(page).toHaveURL(`/recipes/${recipeId}/edit`);

  await upload.getByRole("button", { name: "Back" }).click();
  await expect(picker(page)).toBeVisible();
  await picker(page).getByRole("button", { name: "Cancel" }).click();
  await expect(picker(page)).toBeHidden();
  await expect(field(page)).toContainText("None");
});

test("the picker marks the chosen picture, and Escape leaves the choice as it was", async ({ page }) => {
  const pictureName = uniqueName();
  const pictureId = await givenPicture(page, pictureName, "Det valgte billede");
  const otherName = uniqueName();
  await givenPicture(page, otherName, "Et andet billede");
  const recipeId = await givenRecipe(page, uniqueName(), pictureId);
  await page.goto(`/recipes/${recipeId}/edit`);

  await field(page).getByRole("button", { name: "Choose" }).click();

  await expect(picker(page).getByRole("button", { name: pictureName, exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(picker(page).getByRole("button", { name: otherName, exact: true })).toHaveAttribute("aria-pressed", "false");
  await page.keyboard.press("Escape");
  await expect(picker(page)).toBeHidden();
  await expect(field(page)).toContainText(pictureName);
});

test("Remove takes the picture off the recipe on save, and the picture stays in the library", async ({ page }) => {
  const pictureId = await givenPicture(page, uniqueName(), "Et billede der fjernes");
  const recipeId = await givenRecipe(page, uniqueName(), pictureId);
  await page.goto(`/recipes/${recipeId}/edit`);

  await field(page).getByRole("button", { name: "Remove" }).click();

  await expect(field(page)).toContainText("None");
  await expect(field(page).getByRole("button", { name: "Remove" })).toHaveCount(0);
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/recipes/${recipeId}`);
  await expect(main(page).getByRole("heading", { level: 1 })).toBeVisible();
  await expect(main(page).getByRole("img")).toHaveCount(0);
  expect((await page.request.get(`/api/v1/pictures/${pictureId}`)).status()).toBe(200);
});

test("a recipe without a picture shows none, on its card or its read view", async ({ page }) => {
  const recipeName = uniqueName();
  const recipeId = await givenRecipe(page, recipeName);

  await page.goto("/recipes");
  await page.getByRole("searchbox").fill(recipeName);
  await page.getByRole("button", { name: "Search" }).click();
  const card = main(page).getByRole("listitem").filter({ has: page.getByRole("heading", { name: recipeName }) });
  await expect(card).toBeVisible();
  await expect(card.getByRole("img")).toHaveCount(0);

  await page.goto(`/recipes/${recipeId}`);
  await expect(main(page).getByRole("heading", { name: recipeName })).toBeVisible();
  await expect(main(page).getByRole("img")).toHaveCount(0);
});

test("one picture on a recipe and an ingredient cannot be deleted on the Pictures page, which says where it is used", async ({
  page,
}) => {
  const pictureName = uniqueName();
  const pictureId = await givenPicture(page, pictureName, "Mel i en skål");
  const recipeName = uniqueName();
  await givenRecipe(page, recipeName, pictureId);
  const ingredientName = uniqueName();
  const ingredientId = await createById(page, "ingredients", { name: ingredientName });
  ingredients.push(ingredientId);

  await page.goto(`/ingredients/${ingredientId}/edit`);
  await field(page).getByRole("button", { name: "Choose" }).click();
  await picker(page).getByRole("button", { name: pictureName, exact: true }).click();
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page).toHaveURL(`/ingredients/${ingredientId}`);
  await expect.poll(() => shown(page, "Mel i en skål")).toBe(true);

  await page.goto(`/pictures/${pictureId}`);
  await expect(main(page).locator("dt:text-is('Ingredients') + dd")).toHaveText(ingredientName);
  await page.getByRole("button", { name: "Delete" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Delete" }).click();
  await expect(page.getByRole("dialog").getByRole("alert")).toHaveText(
    `Picture '${pictureName}' is used by 1 recipe and 1 ingredient and cannot be deleted`,
  );
});

for (const [width, beside] of [
  [375, false],
  [1024, true],
] as const) {
  test.describe(`${width} px wide`, () => {
    test.use({ viewport: { width, height: 800 } });

    test(`a recipe's picture stands ${beside ? "beside" : "above"} its details, within the page`, async ({ page }) => {
      const pictureId = await givenPicture(page, uniqueName(), "Et brød i ovnen");
      const recipeId = await givenRecipe(page, uniqueName(), pictureId);
      await page.goto(`/recipes/${recipeId}`);
      await expect.poll(() => shown(page, "Et brød i ovnen")).toBe(true);

      const picture = (await main(page).getByRole("img", { name: "Et brød i ovnen" }).boundingBox())!;
      const details = (await main(page).locator("dl").boundingBox())!;
      if (beside) {
        expect(details.x).toBeGreaterThan(picture.x + picture.width);
        expect(Math.abs(details.y - picture.y)).toBeLessThan(20);
      } else {
        expect(details.y).toBeGreaterThan(picture.y + picture.height);
      }
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
