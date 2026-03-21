import path from "path";
import {defineConfig} from "vite";
import preact from "@preact/preset-vite";

export default defineConfig({
    plugins: [
        preact(),
    ],
    build: {
        minify: "terser",
        terserOptions: {
            compress: {
                drop_console: true,
                passes: 3,
                unsafe: true,
                pure_getters: true,
            },
        },
        reportCompressedSize: false,
    },
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
            react: "preact/compat",
            "react-dom": "preact/compat",
        },
    },
});
