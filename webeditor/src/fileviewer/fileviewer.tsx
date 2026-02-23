/* eslint-disable react/react-in-jsx-scope */
import {useEffect, useRef, useState} from "preact/compat";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {ResizablePanel, ResizablePanelGroup} from "@/components/ui/resizable.tsx";
import {Editor} from "@monaco-editor/react";
import {apiPrefix} from "@/app.tsx";
import type {CSSProperties} from "preact";
import {ZoomableImage} from "@/fileviewer/zoomableimage.tsx";
import {ConfigEditor} from "@/fileviewer/configeditor/configeditor.tsx";
import missingLogo from "@/assets/missing.svg";

export function FileViewer({
                               filePath,
                               token,
                               content,
                               onContentChange,
                               theme,
                           }: {
    filePath?: string;
    token: string | null;
    content?: string;
    onContentChange: (content: string) => void;
    theme: string;
}) {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>();

    useEffect(() => {
        if (!filePath || !token || content) {
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(undefined);

        const fetchFile = async () => {
            try {
                const url = `${apiPrefix}/download_resource?path=${encodeURIComponent(filePath)}&token=${encodeURIComponent(token)}`;
                const res = await fetch(url);

                if (!res.ok) {
                    setError(`Failed to load file: ${res.status}`);
                    return;
                }

                const lower = filePath.toLowerCase();
                if (
                    lower.endsWith(".png") ||
                    lower.endsWith(".jpg") ||
                    lower.endsWith(".jpeg") ||
                    lower.endsWith(".gif")
                ) {
                    onContentChange(url);
                } else {
                    const text = await res.text();
                    onContentChange(text);
                }
            } catch (err: unknown) {
                console.error(err);
                if (err instanceof Error) setError(err.message);
                else setError("An unexpected error occurred");
            } finally {
                setLoading(false);
            }
        };

        fetchFile();
    }, [filePath, token, content, onContentChange]);

    if (loading || content === null) return <Skeleton className="h-full w-full"/>;
    if (error) return <div className="text-red-500">{error}</div>;

    const getLanguage = (filePath: string) => {
        const ext = filePath.split(".").pop()?.toLowerCase();
        const languageMap: Record<string, string> = {
            css: "css",
            go: "go",
            html: "html",
            htm: "html",
            ini: "ini",
            java: "java",
            js: "javascript",
            mjs: "javascript",
            cjs: "javascript",
            jsx: "javascript",
            kt: "kotlin",
            kts: "kotlin",
            markdown: "markdown",
            md: "markdown",
            php: "php",
            ps1: "powershell",
            psm1: "powershell",
            psd1: "powershell",
            py: "python",
            pyw: "python",
            rs: "rust",
            sh: "shell",
            bash: "shell",
            sql: "sql",
            ts: "typescript",
            tsx: "typescript",
            xml: "xml",
            yaml: "yaml",
            yml: "yaml",
        };
        return languageMap[ext || ""] || "plaintext";
    };

    const lower = filePath?.toLowerCase() || "";
    if (
        lower.endsWith(".png") ||
        lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".gif")
    ) {
        return <ZoomableImage src={content as string}/>;
    } else if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
        return <ResizablePanelGroup
            className="h-full w-full"
        >
            <ResizablePanel defaultSize={70} className="overflow-y-auto max-h-[calc(100vh-180px)]">
                <ConfigEditor
                    text={content as string}
                    token={token || ""}
                    folder={filePath?.includes("/") ? filePath.split("/")[0] : ""}
                    onValueChange={onContentChange}
                />
            </ResizablePanel>
            <ResizablePanel defaultSize={30}>
                <Editor
                    height="100%"
                    defaultLanguage={"yaml"}
                    value={content}
                    onChange={(value) => {
                        onContentChange(value || "");
                    }}
                    theme={theme === "dark" ? "vs-dark" : "light"}
                    options={{
                        minimap: {enabled: false},
                        fontSize: 14,
                        wordWrap: "on",
                        formatOnPaste: true,
                        formatOnType: true,
                        automaticLayout: true,
                    }}
                />
            </ResizablePanel>
        </ResizablePanelGroup>;
    } else {

        return (
            <Editor
                height="100%"
                defaultLanguage={getLanguage(filePath || "file.txt")}
                value={content}
                onChange={(value) => {
                    onContentChange(value || "");
                }}
                theme={theme === "dark" ? "vs-dark" : "light"}
                options={{
                    minimap: {enabled: false},
                    fontSize: 14,
                    wordWrap: "on",
                    formatOnPaste: true,
                    formatOnType: true,
                    automaticLayout: true,
                }}
            />
        );
    }
}

export function ImageWithSkeleton({
                                      src,
                                      alt,
                                      style,
                                      className,
                                  }: {
    src?: string
    alt?: string
    style?: string | CSSProperties
    className?: string
}) {
    const imgRef = useRef<HTMLImageElement | null>(null);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        setLoaded(false);
        const img = imgRef.current;
        if (!img) return;

        if (img.complete && img.naturalWidth > 0) {
            setLoaded(true);
        }
    }, [src]);

    return (
        <div className={`relative overflow-hidden aspect-square ${className}`} style={style}>
            {!loaded && (
                <img
                    src={missingLogo}
                    alt={alt}
                    className="h-full w-full"
                />
            )}

            <img
                ref={imgRef}
                src={src}
                alt={alt}
                onLoad={() => setLoaded(true)}
                className={`h-full w-full ${
                    loaded ? "opacity-100" : "opacity-0"
                }`}
                style={{
                    imageRendering: "inherit"
                }}
                draggable={false}
            />
        </div>
    );
}

