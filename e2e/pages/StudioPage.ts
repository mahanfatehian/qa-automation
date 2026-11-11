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
    await this.page.getByRole("button", { name: "Canvas" }).click();
    await expect(this.canvas).toBeVisible();
  }

  async loadFixture() {
    await this.page.getByRole("button", { name: "Canvas" }).click();
    await expect(this.canvas).toBeVisible();
  }

  async save() {
    await this.page.getByRole("button", { name: /^Save/ }).click();
    await expect(this.page.getByRole("button", { name: "Saved" })).toBeVisible();
  }

  async runFixture() {
    await this.page.getByRole("button", { name: "Run blueprint" }).click();
    await expect(this.runStatus).toContainText(/Passed|Failed|Error/, { timeout: 60_000 });
  }
}
