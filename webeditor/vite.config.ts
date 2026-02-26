import path from "path";
import {defineConfig} from "vite";
import preact from "@preact/preset-vite";
import {viteSingleFile} from "vite-plugin-singlefile";
import license from "rollup-plugin-license";

export default defineConfig({
    plugins: [
        preact(),
        viteSingleFile(),
        license({
            thirdParty: {
                // Instead of dumping full text, we can control output
                output: {
                    file: "dist/THIRD_PARTY_LICENSES.txt",
                    encoding: "utf-8",
                },
                includePrivate: false,
            },
        }),
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
