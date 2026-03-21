/* eslint-disable react/react-in-jsx-scope */
import {useEffect, useRef, useState} from "preact/compat";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {ResizablePanel, ResizablePanelGroup} from "@/components/ui/resizable.tsx";
import {Editor, type Monaco} from "@monaco-editor/react";
import {apiPrefix} from "@/app.tsx";
import {type CSSProperties, render} from "preact";
import {ZoomableImage} from "@/fileviewer/zoomableimage.tsx";
import {ConfigEditor} from "@/fileviewer/configeditor/configeditor.tsx";
import {ScrollArea} from "@/components/ui/scroll-area";
import type {editor, IDisposable, languages, Position} from "monaco-editor/esm/vs/editor/editor.api.d.ts";
import missingLogo from "@/assets/missing.svg";
import {ValueCombobox} from "@/fileviewer/configeditor/valuecombobox.tsx";
import {luaApi, luaApiLines} from "@/fileviewer/lua/api.ts";
import init, {LuaAnalyzer} from "wasmluaparser";

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
    const [selectedLanguage, setSelectedLanguage] = useState<string>("plaintext");
    const [monacoLanguages, setMonacoLanguages] = useState<languages.ILanguageExtensionPoint[]>([]);
    const widgetContainerRef = useRef<HTMLDivElement | undefined>();
    const completionItemProviderRef = useRef<IDisposable | undefined>();
    const documentFormattingEditProviderRef = useRef<IDisposable | undefined>();

    useEffect(() => {
        if (filePath) setSelectedLanguage(getLanguage(filePath) || "plaintext");
    }, [filePath, monacoLanguages]);

    useEffect(() => {
        if (!filePath || !token || content !== undefined) {
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
                if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
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

    if (loading) return <Skeleton className="h-full w-full"/>;
    if (error) return <div className="text-red-500">{error}</div>;

    const getLanguage = (filePath: string) => {
        if (!monacoLanguages) return;
        const ext = filePath.split(".").pop()?.toLowerCase();
        if (!ext) return;
        const languageMap = monacoLanguages.reduce((acc, lang) => {
            lang.extensions?.forEach(ext =>
                acc[ext.startsWith('.') ? ext.substring(1) : ext] = lang.id
            );
            return acc;
        }, {} as Record<string, string>);
        return languageMap[ext];
    };

    const renderWidget = (container: HTMLElement) => {
        render(
            <ValueCombobox
                value={selectedLanguage}
                setValue={value => setSelectedLanguage(value || "plaintext")}
                values={monacoLanguages?.map((lang: languages.ILanguageExtensionPoint) => lang.id)}
                name="language"
            />,
            container
        );
    };

    const handleEditorMount = async (editor: editor.IStandaloneCodeEditor, monaco: Monaco) => {
        addStatusBarWidget(editor, monaco);

        await init();
        const analyzer = new LuaAnalyzer();

        const mark = (editor: editor.IStandaloneCodeEditor) => {
            const model = editor.getModel();
            if (!model || model.getLanguageId() !== "lua") return;

            const code = editor.getValue();
            // We add the API at the end of the code
            const errors: EmmyluaError[] = analyzer.check_code(luaApi + code);

            const markers: editor.IMarkerData[] = errors.map(err => {
                // We have to verify the errors aren't from the API we put at the end of the code
                if (err.range.start.line > luaApiLines && err.range.end.line > luaApiLines)
                    return ({
                        message: err.message,
                        severity: monaco.MarkerSeverity.Error,
                        startLineNumber: err.range.start.line + 2 - luaApiLines,
                        startColumn: err.range.start.character + 1,
                        endLineNumber: err.range.end.line + 2 - luaApiLines,
                        endColumn: err.range.end.character + 1,
                        code: err.code,
                    });
                return;
            }).filter(value => value !== undefined);

            monaco.editor.setModelMarkers(editor.getModel(), 'owner', markers);
        };

        const clearMarkers = (editor: editor.IStandaloneCodeEditor) => {
            const model = editor.getModel();
            if (!model) return;
            monaco.editor.setModelMarkers(model, "owner", []);
        };

        if (documentFormattingEditProviderRef.current) documentFormattingEditProviderRef.current.dispose();
        documentFormattingEditProviderRef.current = monaco.languages.registerDocumentFormattingEditProvider("lua", {
            provideDocumentFormattingEdits(model: editor.ITextModel, options: languages.FormattingOptions): languages.ProviderResult<languages.TextEdit[]> {
                const text = analyzer.format_code(model.getValue(), options.tabSize, options.insertSpaces);
                return [{
                    range: {
                        startColumn: 0,
                        startLineNumber: 1,
                        endColumn: model.getLineMaxColumn(model.getLineCount()),
                        endLineNumber: model.getLineCount(),
                    },
                    text: text
                }]
            }
        });

        if (completionItemProviderRef.current) completionItemProviderRef.current.dispose();
        completionItemProviderRef.current = monaco.languages.registerCompletionItemProvider("lua", {
            provideCompletionItems(model: editor.ITextModel, position: Position, context: languages.CompletionContext): languages.ProviderResult<languages.CompletionList> {
                const completion: EmmyluaCompletion[] = analyzer.get_completion(luaApi + model.getValue(), position.lineNumber - 2 + luaApiLines, position.column - 1, context.triggerKind.valueOf());
                if (completion) {
                    return {
                        suggestions: completion.map(value => {
                            return ({
                                label: value.label,
                                detail: value.labelDetails?.detail,
                                kind: value.kind,
                                sortText: value.sortText,
                                insertText: value.insertText ? value.insertText : value.label,
                                insertTextRules: 4, // languages.CompletionItemInsertTextRule.InsertAsSnippet
                                documentation: {
                                    value: value.documentation?.value
                                },
                            })
                        }) as languages.CompletionItem[]
                    }
                }
            }
        });

        editor.onDidChangeModelLanguage((event: editor.IModelLanguageChangedEvent) => {
            if (event.newLanguage !== "lua") clearMarkers(editor);
            else mark(editor);
        });

        editor.onDidChangeModelContent(() => {
            mark(editor);
        });

        mark(editor);
    };

    useEffect(() => {
        if (widgetContainerRef.current)
            renderWidget(widgetContainerRef.current);
    }, [selectedLanguage]);

    const addStatusBarWidget = (editor: editor.IStandaloneCodeEditor, monaco: Monaco) => {
        const container = document.createElement('div');
        renderWidget(container);

        widgetContainerRef.current = container;

        const overlayWidget: editor.IOverlayWidget = {
            getDomNode: () => container,
            getId: () => 'lost.engine.monaco.lang.selector',
            getPosition: () => ({
                preference: monaco.editor.OverlayWidgetPositionPreference.BOTTOM_RIGHT_CORNER
            })
        };

        editor.addOverlayWidget(overlayWidget);
    };

    const handleEditorWillMount = (monaco: Monaco) => {
        setMonacoLanguages(monaco.languages.getLanguages());
    };

    const lower = filePath?.toLowerCase();
    if (!lower) return;
    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
        return <ZoomableImage src={content as string}/>;
    } else if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
        return (
            <div className="h-[calc(100vh-200px)] w-full">
                <ResizablePanelGroup className="h-full w-full">
                    <ResizablePanel defaultSize={70} className="h-full flex">
                        <ScrollArea className="flex-1">
                            <ConfigEditor
                                text={content as string}
                                token={token || ""}
                                folder={filePath?.includes("/") ? filePath.split("/")[0] : ""}
                                onValueChange={onContentChange}
                            />
                        </ScrollArea>
                    </ResizablePanel>
                    <ResizablePanel defaultSize={30} className="h-full">
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
                </ResizablePanelGroup>
            </div>
        );
    } else {
        return (
            <Editor
                height="100%"
                language={selectedLanguage}
                value={content}
                onChange={(value) => {
                    onContentChange(value || "");
                }}
                theme={theme === "dark" ? "vs-dark" : "light"}
                beforeMount={handleEditorWillMount}
                onMount={handleEditorMount}
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

export function ImageWithFallback({
                                      src,
                                      alt,
                                      style,
                                      className,
                                  }: {
    src?: string;
    alt?: string;
    style?: string | CSSProperties;
    className?: string;
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
                    className="h-full w-full object-contain"
                    style={{
                        imageRendering: "inherit",
                    }}
                    draggable={false}
                />
            )}

            <img
                ref={imgRef}
                src={src}
                alt={alt}
                onLoad={() => setLoaded(true)}
                className={`h-full w-full object-contain ${loaded ? "opacity-100" : "opacity-0"}`}
                style={{
                    imageRendering: "inherit",
                }}
                draggable={false}
            />
        </div>
    );
}

type EmmyluaError = {
    range: {
        start: {
            line: number;
            character: number;
        },
        end: {
            line: number;
            character: number;
        }
    }
    severity: number;
    code: string;
    message: string;
}

type EmmyluaCompletion = {
    label?: string,
    labelDetails?: {
        detail?: string,
    }
    kind?: number,
    sortText?: string,
    insertText?: string,
    insertTextFormat?: number,
    documentation?: {
        kind?: string;
        value?: string;
    }
}
