import React, {useEffect, useRef, useState} from "preact/compat";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {ResizablePanel, ResizablePanelGroup} from "@/components/ui/resizable.tsx";
import {Editor} from "@monaco-editor/react";
import {Popover, PopoverContent, PopoverTrigger} from "@/components/ui/popover.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Check, ChevronsUpDown, Pencil, Plus, Trash2} from "lucide-react";
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from "@/components/ui/command.tsx";
import {cn} from "@/lib/utils.ts";
import * as yaml from "yaml";
import {BLOCK_TYPES, type BlockType, type Config, type Item, ITEM_TYPES, type ItemType} from "@/config.ts";
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion.tsx";
import {CardBody, CardContainer, CardItem} from "@/components/ui/3d-card.tsx";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle
} from "@/components/ui/alert-dialog.tsx";
import {Dialog, DialogContent, DialogHeader} from "@/components/ui/dialog.tsx";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "@/components/ui/tabs.tsx";
import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import {Input} from "@/components/ui/input.tsx";
import {apiPrefix} from "@/app.tsx";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table.tsx";
import type {CSSProperties} from "preact";

export function FileViewer({
                               filePath,
                               token,
                               content,
                               onContentChange,
                               theme,
                           }: {
    filePath: string | null;
    token: string | null;
    content: string | null;
    onContentChange: (content: string | null) => void;
    theme: string;
}) {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!filePath || !token || content !== null) {
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(null);

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
        return <ZoomableImage src={content}/>;
    } else if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
        return <ResizablePanelGroup
            className="h-full w-full"
        >
            <ResizablePanel defaultSize={70} className="overflow-y-auto max-h-[calc(100vh-180px)]">
                <ConfigEditor
                    text={content}
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

function ZoomableImage({src, alt}: { src: string; alt?: string }) {
    const [scale, setScale] = useState(1);
    const [offset, setOffset] = useState({x: 0, y: 0});
    const containerRef = useRef<HTMLDivElement>(null);
    const [isDragging, setIsDragging] = useState(false);
    const lastPos = useRef({x: 0, y: 0});

    const handleWheel = (e: WheelEvent) => {
        e.preventDefault();
        if (!containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const mouseX = e.clientX - rect.left - rect.width / 2;
        const mouseY = e.clientY - rect.top - rect.height / 2;

        const delta = -e.deltaY / 400;
        const newScale = Math.min(Math.max(scale + delta, 0.1), 10);

        if (newScale !== scale) {
            const ratio = newScale / scale;
            setOffset({
                x: mouseX - (mouseX - offset.x) * ratio,
                y: mouseY - (mouseY - offset.y) * ratio,
            });
            setScale(newScale);
        }
    };

    const handleMouseDown = (e: MouseEvent) => {
        if (e.button === 1 || e.button === 0) {
            setIsDragging(true);
            lastPos.current = {x: e.clientX, y: e.clientY};
            e.preventDefault();
        }
    };

    const handleMouseMove = (e: MouseEvent) => {
        if (!isDragging) return;
        const dx = e.clientX - lastPos.current.x;
        const dy = e.clientY - lastPos.current.y;
        setOffset((prev) => ({x: prev.x + dx, y: prev.y + dy}));
        lastPos.current = {x: e.clientX, y: e.clientY};
    };

    const handleMouseUp = () => {
        setIsDragging(false);
    };

    return (
        <div
            ref={containerRef}
            className="h-full w-full overflow-auto flex justify-center items-center p-[15px] pb-10"
            onWheel={handleWheel}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            style={{cursor: isDragging ? "grabbing" : "grab"}}
        >
            <img
                src={src}
                alt={alt}
                style={{
                    height: "256px",
                    width: "auto",
                    transform: `scale(${scale}) translate(${offset.x / scale}px, ${offset.y / scale}px)`,
                    imageRendering: "pixelated",
                    backgroundImage: `
                            linear-gradient(45deg, #80808020 25%, transparent 25%),
                            linear-gradient(-45deg, #80808020 25%, transparent 25%),
                            linear-gradient(45deg, transparent 75%, #80808020 75%),
                            linear-gradient(-45deg, transparent 75%, #80808020 75%)
                        `,
                    backgroundSize: `32px 32px`,
                    backgroundPosition: `0 0, 0 16px, 16px -16px, -16px 0px`
                }}
            />
        </div>
    );
}

function ConfigEditor({text, onValueChange, folder, token}: {
    text: string;
    onValueChange: (value: string) => void;
    folder: string;
    token: string;
}) {

    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [confirmDialogAction, setConfirmDialogAction] = useState<() => void>(() => {
    });
    const [confirmDialogMessage, setConfirmDialogMessage] = useState("");
    const [newItemDialogOpen, setNewItemDialogOpen] = useState(false);

    let doc: yaml.Document;
    let config: Config;

    try {
        doc = yaml.parseDocument(text);
        config = doc.toJS() as Config;
    } catch (err: unknown) {
        const message = err instanceof Error ? err.message : String(err);
        return (<div className="text-red-500">{message}</div>);
    }

    const onEditConfig = () => {
        onValueChange(String(doc));
    };

    return (
        <>
            <Accordion
                type="single"
                collapsible
                className="w-full"
            >
                <AccordionItem value="items">
                    <AccordionTrigger>
                        <div className="flex items-center gap-2">
                            Items
                            <Button variant="ghost" size="icon-sm" onClick={event => {
                                event.stopPropagation();
                                setNewItemDialogOpen(true);
                            }}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="h-full w-full flex flex-wrap gap-4 text-balance">
                        {(() => {
                            if (!config.items) return;
                            return Array.from(Object.entries(config.items)).map(value => {
                                return (<>
                                    <CardContainer>
                                        <CardBody
                                            className="bg-gray-50 relative group/card  dark:hover:shadow-2xl dark:hover:shadow-emerald-500/10 dark:bg-neutral-950 dark:border-white/20 border-black/10 w-auto sm:w-60 h-auto rounded-xl p-6 border">
                                            <CardItem
                                                translateZ="50"
                                                className="text-xl font-bold text-neutral-600 dark:text-white"
                                            >
                                                {value[0]}
                                            </CardItem>
                                            <CardItem translateZ="50" className="w-full mt-4">
                                                <ImageWithSkeleton
                                                    src={
                                                        (() => {
                                                            let textureName;
                                                            if (value[1].icon) textureName = value[1].icon;
                                                            else if (value[1].texture) textureName = value[1].texture;
                                                            else return;
                                                            if (!textureName.endsWith(".png")) textureName += ".png";
                                                            return `${apiPrefix}/download_resource?path=${encodeURIComponent(folder + "/assets/textures/" + textureName)}&token=${encodeURIComponent(token)}`;
                                                        })()
                                                    }
                                                    style={{
                                                        imageRendering: "pixelated",
                                                    }}
                                                    className="w-full object-cover group-hover/card:shadow-xl"
                                                />
                                            </CardItem>
                                            <div className="flex justify-between items-center mt-20">
                                                <CardItem translateZ={20}>
                                                    <Button variant="outline" size="sm">
                                                        <Pencil/> Edit
                                                    </Button>
                                                </CardItem>
                                                <CardItem translateZ={20}>
                                                    <Button
                                                        variant="destructive"
                                                        size="sm"
                                                        onClick={() => {
                                                            setConfirmDialogAction(() => () => {
                                                                const itemsNode = doc.get("items", true) as yaml.YAMLMap | null;

                                                                itemsNode?.delete(value[0]);
                                                                onEditConfig();
                                                            });
                                                            setConfirmDialogMessage(`Delete item "${value[0]}"?`);
                                                            setConfirmDialogOpen(true);
                                                        }}
                                                    >
                                                        <Trash2/>
                                                        Delete
                                                    </Button>
                                                </CardItem>
                                            </div>
                                        </CardBody>
                                    </CardContainer>
                                </>);
                            });
                        })()}
                    </AccordionContent>
                </AccordionItem>
                <AccordionItem value="blocks">
                    <AccordionTrigger>
                        <div className="flex items-center gap-2">
                            Blocks
                            <Button variant="ghost" size="icon-sm" onClick={event => {
                                event.stopPropagation();
                            }}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="h-full w-full flex flex-wrap gap-4 text-balance">
                        {(() => {
                            if (!config.blocks) return;
                            return Array.from(Object.entries(config.blocks)).map(value => {
                                return (<>
                                    <CardContainer>
                                        <CardBody
                                            className="bg-gray-50 relative group/card  dark:hover:shadow-2xl dark:hover:shadow-emerald-500/10 dark:bg-neutral-950 dark:border-white/20 border-black/10 w-auto sm:w-60 h-auto rounded-xl p-6 border">
                                            <CardItem
                                                translateZ="50"
                                                className="text-xl font-bold text-neutral-600 dark:text-white"
                                            >
                                                {value[0]}
                                            </CardItem>
                                            <CardItem translateZ="50" className="w-full mt-4">
                                                <ImageWithSkeleton
                                                    src={
                                                        (() => {
                                                            let textureName = value[1].texture;
                                                            if (textureName) {
                                                                if (!textureName.endsWith(".png")) textureName += ".png";
                                                                return `${apiPrefix}/download_resource?path=${encodeURIComponent(folder + "/assets/textures/" + textureName)}&token=${encodeURIComponent(token)}`;
                                                            }
                                                        })()
                                                    }
                                                    style={{
                                                        imageRendering: "pixelated",
                                                    }}
                                                    className="w-full object-cover group-hover/card:shadow-xl"
                                                />
                                            </CardItem>
                                            <div className="flex justify-between items-center mt-20">
                                                <CardItem translateZ={20}>
                                                    <Button variant="outline" size="sm">
                                                        <Pencil/> Edit
                                                    </Button>
                                                </CardItem>
                                                <CardItem translateZ={20}>
                                                    <Button
                                                        variant="destructive"
                                                        size="sm"
                                                        onClick={() => {
                                                            setConfirmDialogAction(() => () => {
                                                                const blocksNode = doc.get("blocks", true) as yaml.YAMLMap | null;

                                                                blocksNode?.delete(value[0]);
                                                                onEditConfig();
                                                            });
                                                            setConfirmDialogMessage(`Delete block "${value[0]}"?`);
                                                            setConfirmDialogOpen(true);
                                                        }}
                                                    >
                                                        <Trash2/>
                                                        Delete
                                                    </Button>
                                                </CardItem>
                                            </div>
                                        </CardBody>
                                    </CardContainer>
                                </>);
                            });
                        })()}
                    </AccordionContent>
                </AccordionItem>
                <AccordionItem value="materials">
                    <AccordionTrigger>
                        <div className="flex items-center gap-2">
                            Materials
                            <Button variant="ghost" size="icon-sm" onClick={event => event.stopPropagation()}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="flex flex-col gap-4 text-balance">
                        <div className="h-full w-full overflow-auto flex flex-wrap gap-4">
                            {(() => {
                                if (!config.materials) return;
                                return Array.from(Object.entries(config.materials)).map(value => {
                                    return (<>
                                        <CardContainer>
                                            <CardBody
                                                className="bg-gray-50 relative group/card  dark:hover:shadow-2xl dark:hover:shadow-emerald-500/10 dark:bg-neutral-950 dark:border-white/20 border-black/10 w-auto sm:w-60 h-auto rounded-xl p-6 border">
                                                <CardItem
                                                    translateZ="50"
                                                    className="text-xl font-bold text-neutral-600 dark:text-white"
                                                >
                                                    {value[0]}
                                                </CardItem>
                                                <CardItem translateZ="50" className="w-full mt-4">
                                                    <ImageWithSkeleton
                                                        src={
                                                            (() => {
                                                                const repairItem = value[1].repair_item;
                                                                if (repairItem) {
                                                                    const repairItemValue = config.items?.[repairItem];
                                                                    let textureName;
                                                                    if (repairItemValue?.icon) textureName = repairItemValue.icon;
                                                                    else if (repairItemValue?.texture) textureName = repairItemValue.texture;
                                                                    else return;
                                                                    if (!textureName.endsWith(".png")) textureName += ".png";
                                                                    return `${apiPrefix}/download_resource?path=${encodeURIComponent(folder + "/assets/textures/" + textureName)}&token=${encodeURIComponent(token)}`;
                                                                }
                                                            })()
                                                        }
                                                        style={{
                                                            imageRendering: "pixelated",
                                                        }}
                                                        className="w-full object-cover group-hover/card:shadow-xl"
                                                        alt="thumbnail"
                                                    />
                                                </CardItem>
                                                <div className="flex justify-between items-center mt-20">
                                                    <CardItem translateZ={20}>
                                                        <Button variant="outline" size="sm">
                                                            <Pencil/> Edit
                                                        </Button>
                                                    </CardItem>
                                                    <CardItem translateZ={20}>
                                                        <Button
                                                            variant="destructive"
                                                            size="sm"
                                                            onClick={() => {
                                                                setConfirmDialogAction(() => () => {
                                                                    const materialsNode = doc.get("materials", true) as yaml.YAMLMap | null;

                                                                    materialsNode?.delete(value[0]);
                                                                    onEditConfig();
                                                                });
                                                                setConfirmDialogMessage(`Delete material "${value[0]}"?`);
                                                                setConfirmDialogOpen(true);
                                                            }}
                                                        >
                                                            <Trash2/>
                                                            Delete
                                                        </Button>
                                                    </CardItem>
                                                </div>
                                            </CardBody>
                                        </CardContainer>
                                    </>);
                                });
                            })()}
                        </div>
                    </AccordionContent>
                </AccordionItem>
                <AccordionItem value="glyphs">
                    <AccordionTrigger>
                        <div className="flex items-center gap-2">
                            Glyphs
                            <Button variant="ghost" size="icon-sm" onClick={event => event.stopPropagation()}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="flex flex-col gap-4 text-balance">
                        <div className="h-full w-full overflow-auto flex flex-wrap gap-4">
                            {(() => {
                                if (!config.glyphs) return;
                                return Array.from(Object.entries(config.glyphs)).map(value => {
                                    return (<>
                                        <CardContainer>
                                            <CardBody
                                                className="bg-gray-50 relative group/card  dark:hover:shadow-2xl dark:hover:shadow-emerald-500/10 dark:bg-neutral-950 dark:border-white/20 border-black/10 w-auto sm:w-60 h-auto rounded-xl p-6 border">
                                                <CardItem
                                                    translateZ="50"
                                                    className="text-xl font-bold text-neutral-600 dark:text-white"
                                                >
                                                    {value[0]}
                                                </CardItem>
                                                <CardItem translateZ="50" className="w-full mt-4">
                                                    <ImageWithSkeleton
                                                        src={(() => {
                                                            let textureName = value[1].image_path;
                                                            if (textureName) {
                                                                if (!textureName.endsWith(".png")) textureName += ".png";
                                                                return `${apiPrefix}/download_resource?path=${encodeURIComponent(folder + "/assets/textures/" + textureName)}&token=${encodeURIComponent(token)}`;
                                                            }
                                                        })()}
                                                        className="w-full object-cover group-hover/card:shadow-xl"
                                                        style={{
                                                            imageRendering: "pixelated",
                                                        }}
                                                        alt="thumbnail"
                                                    />
                                                </CardItem>
                                                <div className="flex justify-between items-center mt-20">
                                                    <CardItem translateZ={20}>
                                                        <Button variant="outline" size="sm">
                                                            <Pencil/> Edit
                                                        </Button>
                                                    </CardItem>
                                                    <CardItem translateZ={20}>
                                                        <Button
                                                            variant="destructive"
                                                            size="sm"
                                                            onClick={() => {
                                                                setConfirmDialogAction(() => () => {
                                                                    const glyphsNode = doc.get("glyphs", true) as yaml.YAMLMap | null;

                                                                    glyphsNode?.delete(value[0]);
                                                                    onEditConfig();
                                                                });
                                                                setConfirmDialogMessage(`Delete glyph "${value[0]}"?`);
                                                                setConfirmDialogOpen(true);
                                                            }}
                                                        >
                                                            <Trash2/>
                                                            Delete
                                                        </Button>
                                                    </CardItem>
                                                </div>
                                            </CardBody>
                                        </CardContainer>
                                    </>);
                                });
                            })()}
                        </div>
                    </AccordionContent>
                </AccordionItem>
            </Accordion>
            <AlertDialog open={confirmDialogOpen} onOpenChange={setConfirmDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>
                            {confirmDialogMessage}
                        </AlertDialogTitle>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={confirmDialogAction}>Confirm</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
            <NewItemDialog onOpenChange={setNewItemDialogOpen} open={newItemDialogOpen} doc={doc}
                           onEditConfig={onEditConfig}/>
        </>
    );
}

function NewItemDialog({
                           onOpenChange,
                           open,
                           doc,
                           onEditConfig,
                           isBlock
                       }: {
    onOpenChange: (open: boolean) => void;
    open: boolean;
    doc: yaml.Document;
    onEditConfig: () => void;
    isBlock?: boolean;
}) {
    const [tab, setTab] = useState("itemidtype");
    const [itemID, setItemID] = useState("");
    const [itemType, setItemType] = useState<ItemType | undefined>();
    const [blockType, setBlockType] = useState<BlockType | undefined>();
    const [itemNames, setItemNames] = useState<Record<string, string>>({});
    const setOpen = (open: boolean) => {
        onOpenChange(open);
        if (!open) {
            setItemID("");
            setItemType(undefined);
            setTab("itemidtype");
            setItemNames({});
        }
    };

    return (<>
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogContent>
                <DialogHeader>
                    <Tabs value={tab} onValueChange={setTab}>
                        <TabsList>
                            <TabsTrigger value="itemidtype">{isBlock ? "Block" : "Item"} ID and Type</TabsTrigger>
                            <TabsTrigger value="name"
                                         disabled={!(itemID && itemType)}>{isBlock ? "Block" : "Item"} Name</TabsTrigger>
                        </TabsList>
                        <TabsContent value="itemidtype">
                            <Card>
                                <CardHeader>
                                    <CardTitle>New Item</CardTitle>
                                </CardHeader>
                                <CardContent className="grid gap-6">
                                    <div className="grid gap-3">
                                        Item ID
                                        <Input onInput={e => setItemID((e.target as HTMLInputElement).value)}
                                               placeholder="my_custom_item"
                                               value={itemID}
                                               required/>
                                    </div>
                                    <div className="grid gap-3">
                                        Type
                                        {isBlock ?
                                            <ValueCombobox<BlockType> value={blockType} setValue={setBlockType} name="Block" values={[...BLOCK_TYPES]}/> :
                                            <ValueCombobox<ItemType> value={itemType} setValue={setItemType} name="Item" values={[...ITEM_TYPES]}/>
                                        }
                                    </div>
                                </CardContent>
                                <CardFooter className="flex justify-between items-center">
                                    <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
                                    <Button
                                        disabled={!(itemID && itemType)}
                                        onClick={() => setTab("name")}
                                    >
                                        Next
                                    </Button>
                                </CardFooter>
                            </Card>
                        </TabsContent>
                        <TabsContent value="name">
                            <Card>
                                <CardHeader>
                                    <CardTitle>Item Name</CardTitle>
                                </CardHeader>
                                <CardContent className="grid gap-6">
                                    <Table>
                                        <TableHeader>
                                            <TableRow>
                                                <div>
                                                    <TableHead>Language</TableHead>
                                                    <TableHead>
                                                        <Button variant="ghost" size="icon-sm" onClick={event => {
                                                            event.stopPropagation();
                                                            setItemNames(prev => ({
                                                                ...prev,
                                                                "": ""
                                                            }));
                                                        }}>
                                                            <Plus/>
                                                        </Button>
                                                    </TableHead>
                                                </div>
                                                <TableHead className="text-right">Name</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {Object.entries(itemNames).map(([lang, name], index) => (
                                                <TableRow key={index}>
                                                    <TableCell>
                                                        <ValueCombobox
                                                            value={lang}
                                                            setValue={(newLang) => {
                                                                setItemNames(prev => {
                                                                    const next: Record<string, string> = {};
                                                                    Object.keys(prev).forEach(key => {
                                                                        if (key === lang) {
                                                                            if (newLang) next[newLang] = name;
                                                                        } else {
                                                                            next[key] = prev[key];
                                                                        }
                                                                    });
                                                                    return next;
                                                                });
                                                            }}
                                                            values={languages}
                                                            name="Language"
                                                        />
                                                    </TableCell>
                                                    <TableCell>
                                                        <Input
                                                            placeholder="Item Name"
                                                            className="text-right"
                                                            value={name}
                                                            onInput={(e) => {
                                                                const newName = (e.target as HTMLInputElement).value;
                                                                setItemNames(prev => ({
                                                                    ...prev,
                                                                    [lang]: newName
                                                                }));
                                                            }}
                                                        />
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </CardContent>
                                <CardFooter className="flex justify-between items-center">
                                    <Button
                                        variant="outline"
                                        onClick={() => setTab("itemidtype")}
                                    >
                                        Back
                                    </Button>
                                    <Button onClick={() => {
                                        let itemsNode = doc.get("items", true) as yaml.YAMLMap | null;

                                        if (!itemsNode) {
                                            doc.set("items", doc.createNode({}));
                                            itemsNode = doc.get("items") as yaml.YAMLMap;
                                        }

                                        const filteredNames = Object.fromEntries(
                                            Object.entries(itemNames).filter(([lang]) => lang.trim() !== "")
                                        );

                                        const newItemData: Item = {
                                            type: itemType,
                                            name: filteredNames,
                                        };

                                        itemsNode.set(itemID, doc.createNode(newItemData));
                                        onEditConfig();
                                        setOpen(false);
                                    }}>
                                        Create Item
                                    </Button>
                                </CardFooter>
                            </Card>
                        </TabsContent>
                    </Tabs>
                </DialogHeader>
            </DialogContent>
        </Dialog>
    </>);
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
                <Skeleton className="absolute inset-0"/>
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

function ValueCombobox<T extends string>({
                                             value,
                                             setValue,
                                             values,
                                             name
                                         }: {
    value?: T,
    setValue: (value?: T) => void,
    values: T[],
    name: string
}) {
    const [open, setOpen] = React.useState(false);

    return (
        <Popover open={open} onOpenChange={setOpen} modal={true}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    className="w-[200px] justify-between"
                >
                    {value
                        ? values.find((type) => type === value)?.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase()).trim()
                        : `Select ${name} type...`}
                    <ChevronsUpDown className="opacity-50"/>
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[200px] p-0">
                <Command>
                    <CommandInput placeholder={`Search ${name} type...`} className="h-9"/>
                    <CommandList>
                        <CommandEmpty>No item type found.</CommandEmpty>
                        <CommandGroup>
                            {values.map((type) => (
                                <CommandItem
                                    key={type}
                                    value={type}
                                    onSelect={(currentValue) => {
                                        const newValue = currentValue === value ? undefined : currentValue  as T;
                                        setValue(newValue);
                                        setOpen(false);
                                    }}
                                >
                                    {type.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase()).trim()}
                                    <Check
                                        className={cn(
                                            "ml-auto",
                                            value === type ? "opacity-100" : "opacity-0"
                                        )}
                                    />
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}

const languages = ["af_za", "ar_sa", "ast_es", "az_az", "ba_ru", "bar", "be_by", "be_latn", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz", "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca", "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy", "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan", "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr", "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "hal_ua", "haw_us", "he_il", "hi_in", "hn_no", "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is", "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr", "ksh", "kw_gb", "ky_kg", "la_la", "lb_lu", "li_li", "lmo", "lo_la", "lol_us", "lt_lt", "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de", "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pls", "pt_br", "pt_pt", "qcb_es", "qid", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "sah_sah", "se_no", "sk_sk", "sl_si", "so_so", "sq_al", "sr_cs", "sr_sp", "sv_se", "sxu", "szl", "ta_in", "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "tzo_mx", "uk_ua", "val_es", "vec_it", "vi_vn", "vp_vl", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab"];
