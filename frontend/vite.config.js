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
    // assetsDir: resolve(__dirname, "../backend/priv/static/assets"),
    // outDir: resolve(__dirname, "../backend/priv/static/js"),
    outDir: "public/build",
    assetsDir: "assets",
    emptyOutDir: true,
    watch: {
      include: ["target/external.js", "src/app.css"],
    },
    rollupOptions: {
      input: "target/external.js",
      output: {
        entryFileNames: "js/[name].js",
        assetFileNames: "assets/[name][extname]",
      },
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
