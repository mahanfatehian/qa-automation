import { expect, type Locator, type Page } from "@playwright/test";

export class StudioPage {
  readonly page: Page;
  readonly canvas: Locator;
  readonly runStatus: Locator;
  readonly nameInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.canvas = page.getByTestId("blueprint-canvas");
    this.runStatus = page.getByTestId("run-status");
    this.nameInput = page.locator("header input").first();
  }

  async goto() {
    await this.page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(this.page.getByTestId("studio-header")).toBeVisible();
    await expect(this.canvas).toBeVisible();
  }

  async loadFixture() {
    this.page.once("dialog", (dialog) => dialog.accept());
    await this.page.getByRole("button", { name: "Load fixture" }).click();
    await expect(this.canvas.getByText("Open page")).toBeVisible();
  }

  async save() {
    await this.page.getByRole("button", { name: /^Save/ }).click();
    await expect(this.page.getByRole("button", { name: "Saved" })).toBeVisible();
  }

  async runFixture() {
    await this.page.getByRole("button", { name: "Run blueprint" }).click();
    await expect(this.runStatus).toContainText(/PASSED|FAILED|ERROR/, { timeout: 60_000 });
  }
}
