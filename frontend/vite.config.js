import { defineConfig } from "vite";
import preact from "@preact/preset-vite";
import { dirname, resolve } from "path";

import { fileURLTOPath } from "url";

const __dirname = dirname(fileURLTOPath(import.meta.url));

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    preact({
      include: ["**/*.res.mjs"],
    }),
  ],
  build: {
    manifest: true,
    outDir: resolve(__dirname, "../backend/priv/static/assets"),
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:4000",
        changeOrigin: true,
      },
      "/socket": {
        target: "ws://localhost:4000",
        ws: true,
      },
    },
  },
});
