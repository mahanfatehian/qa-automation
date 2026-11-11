import { expect, test } from "@playwright/test";
import { StudioPage } from "../pages/StudioPage";

test.describe("Blueprint studio as a QA user", () => {
  test("opens the blueprint canvas", async ({ page }) => {
    const studio = new StudioPage(page);
    await studio.goto();
    await expect(page.getByText("Given")).toBeVisible();
    await expect(page.getByText("When")).toBeVisible();
    await expect(page.getByText("Then")).toBeVisible();
    await expect(page.getByRole("button", { name: "Find element" })).toBeVisible();
    await expect(page.getByRole("button", { name: "Run blueprint" })).toBeVisible();
    await page.screenshot({ path: "artifacts/studio-open.png", fullPage: true });
  });

  test("loads the fixture blueprint and saves it", async ({ page }) => {
    const studio = new StudioPage(page);
    await studio.goto();
    await studio.loadFixture();
    await studio.nameInput.fill("Playwright fixture");
    await studio.save();
    await page.screenshot({ path: "artifacts/fixture-loaded.png", fullPage: true });
  });

  test("runs the fixture blueprint on the grid", async ({ page }) => {
    const studio = new StudioPage(page);
    await studio.goto();
    await studio.loadFixture();
    await studio.save();
    await studio.runFixture();
    await expect(studio.runStatus).toContainText("PASSED");
    await page.screenshot({ path: "artifacts/fixture-passed.png", fullPage: true });
  });
});
