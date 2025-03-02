import { defineConfig } from "vite";
import preact from "@preact/preset-vite";
import { dirname, resolve } from "path";
import tailwindcss from "@tailwindcss/vite";

import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    preact({
      include: ["**/*.res.mjs"],
    }),
  ],
  build: {
    manifest: true,
    assetsDir: resolve(__dirname, "../backend/priv/static/assets"),
    outDir: resolve(__dirname, "../backend/priv/static/js"),
    emptyOutDir: true,
    rollupOptions: {
      input: "/src/Main.res.mjs",
    },
  },
  server: {
    allowedHosts: true,
    cors: true,
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
