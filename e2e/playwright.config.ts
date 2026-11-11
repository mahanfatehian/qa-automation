import { defineConfig, devices } from "@playwright/test";

const studioURL = process.env.STUDIO_URL || "http://127.0.0.1:3000";

export default defineConfig({
  testDir: "./tests",
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: studioURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "off",
    channel: process.env.PLAYWRIGHT_CHANNEL || "chrome",
    viewport: { width: 1440, height: 900 },
    launchOptions: {
      args: ["--no-proxy-server"],
    },
  },
});
