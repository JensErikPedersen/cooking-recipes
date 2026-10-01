import { defineConfig, devices } from "@playwright/test";
import path from "node:path";

// The stack's own .env supplies the account the signed-in tests use.
process.loadEnvFile(path.join(__dirname, "..", ".env"));

// Runs against an already running stack (scripts/start.*); it never starts one itself.
export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
