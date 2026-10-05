import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => ({
  plugins: [react({ include: /\.(jsx|js)$/ })],
  // Production bundles are minified and name-mangled, ship no source maps, and have
  // console/debugger statements stripped so no internals are exposed in the browser.
  build: {
    sourcemap: false,
    minify: "esbuild",
  },
  esbuild: mode === "production" ? { drop: ["console", "debugger"], legalComments: "none" } : {},
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
}));
