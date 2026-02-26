/* eslint-disable react/react-in-jsx-scope */
import {render} from "preact";
import {App} from "./app.tsx";
import "./index.css";
import {Toaster} from "@/components/ui/sonner.tsx";

render(
    <>
        <App />
        <Toaster />
    </>,
    document.getElementById("app")!,
);
