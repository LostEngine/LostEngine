import path from "path"
import {defineConfig} from 'vite'
import preact from '@preact/preset-vite'
import {viteSingleFile} from "vite-plugin-singlefile"

export default defineConfig({
    plugins: [preact(), viteSingleFile()],
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
            "react": "preact/compat",
            "react-dom": "preact/compat",
        },
    },
})
