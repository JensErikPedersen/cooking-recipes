import { expect, type Page, test } from "@playwright/test";
import { createById, csrfHeader, deleteById } from "../support/api";
import { drawPicture, pictureOfSize, pictureWidth, uploadPicture } from "../support/pictures";

// The picture library's API, through the frontend origin: the file crosses the Next.js proxy to the
// backend, which keeps it in three sizes on its volume (docs/PLAN_V8.md Parts 3 and 4). What sets
// the Welcome page's picture is in welcome-picture.spec.ts. Each test deletes what it creates.

const MB = 1024 * 1024;

// Seeded category "Brød".
const BROED = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

test.describe("signed in", () => {
  const uploaded: string[] = [];

  test.afterEach(async ({ page }) => {
    for (const id of uploaded.splice(0)) {
      await deleteById(page, "pictures", id);
    }
  });

  test("a picture is uploaded, listed and served in three sizes", async ({ page }) => {
    const response = await uploadPicture(page, await drawPicture(page, 2000, 1000), "Rugbrød på bræt", "Et skåret rugbrød");

    expect(response.status()).toBe(201);
    const picture = await response.json();
    uploaded.push(picture.id);
    expect(picture).toMatchObject({ name: "Rugbrød på bræt", altText: "Et skåret rugbrød" });
    expect(response.headers()["location"]).toBe(`/api/v1/pictures/${picture.id}`);

    const list = await (await page.request.get("/api/v1/pictures")).json();
    expect(list.map((p: { id: string }) => p.id)).toContain(picture.id);

    for (const [size, width] of [["small", 400], ["medium", 800], ["large", 1600]] as const) {
      const file = await page.request.get(`/api/v1/pictures/${picture.id}/${size}`);
      expect(file.status()).toBe(200);
      expect(file.headers()["content-type"]).toBe("image/jpeg");
      expect(file.headers()["cache-control"]).toBe("max-age=31536000, private, immutable");
      expect(await pictureWidth(page, await file.body())).toBe(width);
    }
  });

  test("a PNG is accepted and served as JPEG", async ({ page }) => {
    const response = await uploadPicture(page, await drawPicture(page, 600, 400, "image/png"), "PNG", "En tegning", "tegning.png");

    expect(response.status()).toBe(201);
    const { id } = await response.json();
    uploaded.push(id);
    const file = await page.request.get(`/api/v1/pictures/${id}/large`);
    expect(file.headers()["content-type"]).toBe("image/jpeg");
    expect(await pictureWidth(page, await file.body())).toBe(600);
  });

  test("a file that is not a picture is refused on the file field", async ({ page }) => {
    const response = await uploadPicture(page, Buffer.from("not a picture"), "Tekst", "Ingen billede", "noter.jpg");

    expect(response.status()).toBe(400);
    expect(await response.json()).toMatchObject({
      errorCode: 720,
      validationExceptions: [{ objectName: "file", message: "The file is not a JPEG or PNG picture" }],
    });
  });

  test("a picture just under 10 MB crosses the proxy and is accepted", async ({ page }) => {
    const response = await uploadPicture(page, await pictureOfSize(page, 10 * MB - 4096), "Stor", "Et stort billede");

    expect(response.status()).toBe(201);
    uploaded.push((await response.json()).id);
  });

  test("a picture over 10 MB is refused with 413 on the file field", async ({ page }) => {
    const response = await uploadPicture(page, await pictureOfSize(page, 10 * MB + 100 * 1024), "For stor", "Et for stort billede");

    expect(response.status()).toBe(413);
    expect(await response.json()).toMatchObject({
      errorCode: 721,
      validationExceptions: [{ objectName: "file", message: "The picture is larger than 10 MB" }],
    });
  });

  test("an unknown size is a 404", async ({ page }) => {
    const response = await uploadPicture(page, await drawPicture(page, 500, 400), "Lille", "Et lille billede");
    const { id } = await response.json();
    uploaded.push(id);

    expect((await page.request.get(`/api/v1/pictures/${id}/huge`)).status()).toBe(404);
  });
});

test.describe("where a picture is used", () => {
  // Deleted in this order: a picture in use cannot be.
  const recipes: string[] = [];
  const ingredients: string[] = [];
  const pictures: string[] = [];

  test.afterEach(async ({ page }) => {
    for (const id of recipes.splice(0)) await deleteById(page, "recipes", id);
    for (const id of ingredients.splice(0)) await deleteById(page, "ingredients", id);
    for (const id of pictures.splice(0)) await deleteById(page, "pictures", id);
  });

  async function upload(page: Page, name: string, altText: string) {
    const response = await uploadPicture(page, await drawPicture(page, 600, 400), name, altText);
    const { id } = await response.json();
    pictures.push(id);
    return id as string;
  }

  async function putJson(page: Page, path: string, data: unknown) {
    return page.request.put(path, { headers: await csrfHeader(page), data });
  }

  const unique = () => crypto.randomUUID().slice(0, 8);

  test("a picture's name and alt text are edited, its files unchanged", async ({ page }) => {
    const id = await upload(page, "Rugbrød", "Et rugbrød");
    const before = await (await page.request.get(`/api/v1/pictures/${id}/small`)).body();

    const response = await putJson(page, `/api/v1/pictures/${id}`, { name: "Rugbrød på bræt", altText: "Et skåret rugbrød" });

    expect(response.status()).toBe(200);
    expect(await (await page.request.get(`/api/v1/pictures/${id}`)).json()).toMatchObject({
      name: "Rugbrød på bræt",
      altText: "Et skåret rugbrød",
    });
    expect(await (await page.request.get(`/api/v1/pictures/${id}/small`)).body()).toEqual(before);
  });

  test("a recipe and an ingredient share a picture, and each is named where it is used", async ({ page }) => {
    const picture = await upload(page, "Mel", "En sæk mel");
    const recipeName = `Billedbrød ${unique()}`;
    const recipe = await createById(page, "recipes", { name: recipeName, category: { id: BROED }, picture: { id: picture } });
    recipes.push(recipe);
    const ingredientName = `Billedmel ${unique()}`;
    const ingredient = await createById(page, "ingredients", { name: ingredientName, picture: { id: picture } });
    ingredients.push(ingredient);

    expect((await (await page.request.get(`/api/v1/recipes/${recipe}`)).json()).picture).toMatchObject({
      id: picture,
      name: "Mel",
      altText: "En sæk mel",
    });
    expect((await (await page.request.get(`/api/v1/ingredients/${ingredient}`)).json()).picture.id).toBe(picture);
    expect(await (await page.request.get(`/api/v1/pictures/${picture}`)).json()).toMatchObject({
      recipes: [{ id: recipe, name: recipeName }],
      ingredients: [{ id: ingredient, name: ingredientName }],
      welcomePage: false,
    });
  });

  test("an unknown picture is refused on the field picture", async ({ page }) => {
    const response = await page.request.post("/api/v1/ingredients", {
      headers: await csrfHeader(page),
      data: { name: `Billedmel ${unique()}`, picture: { id: crypto.randomUUID() } },
    });

    expect(response.status()).toBe(400);
    expect(await response.json()).toMatchObject({ errorCode: 710, validationExceptions: [{ objectName: "picture" }] });
  });
});

test.describe("signed out", () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test("a picture's file is not served", async ({ request }) => {
    const response = await request.get(`/api/v1/pictures/${crypto.randomUUID()}/small`);

    expect(response.status()).toBe(401);
    expect(await response.json()).toMatchObject({ errorCode: 600 });
  });
});
