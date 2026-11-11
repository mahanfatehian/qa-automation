import { expect, test } from "@playwright/test";
import { StudioPage } from "../pages/StudioPage";

test.describe("Blueprint studio as a QA user", () => {
  test("opens the blueprint canvas", async ({ page }) => {
    const studio = new StudioPage(page);
    await studio.goto();
    await expect(page.getByText("Given setup", { exact: true })).toBeVisible();
    await expect(page.getByText("When act", { exact: true })).toBeVisible();
    await expect(page.getByText("Then check", { exact: true })).toBeVisible();
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

  test("selects every step and resizes the live well", async ({ page }) => {
    const studio = new StudioPage(page);
    await studio.goto();
    await studio.loadFixture();
    await studio.canvas.click();
    await page.keyboard.press("Control+a");
    const nodes = page.locator(".react-flow__node");
    const selected = page.locator(".react-flow__node.selected");
    await expect(selected).toHaveCount(await nodes.count());
    const well = page.getByTestId("live-well");
    const before = (await well.boundingBox())?.height || 0;
    const rail = page.getByTestId("well-rail");
    const box = await rail.boundingBox();
    if (!box) throw new Error("missing resize bar");
    await page.mouse.move(box.x + 40, box.y + 4);
    await page.mouse.down();
    await page.mouse.move(box.x + 40, box.y - 90, { steps: 6 });
    await page.mouse.up();
    await expect.poll(async () => (await well.boundingBox())?.height || 0).toBeGreaterThan(before + 40);
    const shot = page.getByTestId("live-shot");
    const shotBefore = (await shot.boundingBox())?.width || 0;
    const logRail = page.getByTestId("log-rail");
    const logBox = await logRail.boundingBox();
    if (!logBox) throw new Error("missing log bar");
    await page.mouse.move(logBox.x + 4, logBox.y + 20);
    await page.mouse.down();
    await page.mouse.move(logBox.x - 120, logBox.y + 20, { steps: 6 });
    await page.mouse.up();
    await expect.poll(async () => (await shot.boundingBox())?.width || 0).toBeGreaterThan(shotBefore + 60);
  });

  test("runs the fixture blueprint on the grid", async ({ page }) => {
    const studio = new StudioPage(page);
    await studio.goto();
    await studio.loadFixture();
    await studio.save();
    await studio.runFixture();
    await expect(studio.runStatus).toContainText("Passed");
    await expect(page.getByTestId("run-steps")).toContainText("Open page");
    await expect(page.getByTestId("run-steps")).not.toContainText("NODE_");
    await page.screenshot({ path: "artifacts/fixture-passed.png", fullPage: true });
  });
});
