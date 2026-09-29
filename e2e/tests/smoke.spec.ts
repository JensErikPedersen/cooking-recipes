import { expect, test } from "@playwright/test";

test("home page renders and shows the value fetched from the server", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "Cooking Recipes" })).toBeVisible();
  await expect(page.getByText("Hello from the Next.js server")).toBeVisible();
});
