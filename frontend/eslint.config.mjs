import { defineConfig, globalIgnores } from "eslint/config";

export default defineConfig([
  globalIgnores([
    "dist/**",
    "node_modules/**",
    "build/**"
  ]),
  {
    files: ["src/**/*.{ts,tsx}"],
    rules: {}
  }
]);
