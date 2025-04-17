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
      include: ["build/app/**.js"],
    }),
  ],
  mode: "development",
  publicDir: false,
  build: {
    outDir: "public/build/lib",
    assetsDir: "assets",
    emptyOutDir: true,
    watch: {
      include: ["target/external.js", "src/app.css"],
    },
    rollupOptions: {
      input: "target/external.js",
      output: {
        entryFileNames: "[name].js",
        assetFileNames: "[name][extname]",
      },
    },
  },
});
