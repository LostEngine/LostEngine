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
import type {Config} from "@/config.ts";
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

const notfoundImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAAAAAA6mKC9AAAAAnRSTlMAAHaTzTgAAABTSURBVHjalMixDcRACERRmtpKTkRIE08HZNR+oBHWOvTP3rcvub/lZNzKTme1xyufqjLMWasmTw+WhAhoUIogZ4Bc4hyzOTPQGurgt9LxW/8RBAD63zSW3JrlEQAAAABJRU5ErkJggg==";

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
        return <ZoomableImage src={content} alt={filePath || ""}/>;
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
    const dragging = useRef(false);
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
            dragging.current = true;
            lastPos.current = {x: e.clientX, y: e.clientY};
            e.preventDefault();
        }
    };

    const handleMouseMove = (e: MouseEvent) => {
        if (!dragging.current) return;
        const dx = e.clientX - lastPos.current.x;
        const dy = e.clientY - lastPos.current.y;
        setOffset((prev) => ({x: prev.x + dx, y: prev.y + dy}));
        lastPos.current = {x: e.clientX, y: e.clientY};
    };

    const handleMouseUp = () => {
        dragging.current = false;
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
            style={{cursor: dragging.current ? "grabbing" : "grab"}}
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
                draggable={false}
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
                            if (!config.items) return null;
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
                                                <img
                                                    src={
                                                        (() => {
                                                            let textureName;
                                                            if (value[1].icon) textureName = value[1].icon;
                                                            else if (value[1].texture) textureName = value[1].texture;
                                                            else return notfoundImage;
                                                            if (!textureName.endsWith(".png")) textureName += ".png";
                                                            return `${apiPrefix}/download_resource?path=${encodeURIComponent(folder + "/assets/textures/" + textureName)}&token=${encodeURIComponent(token)}`;
                                                        })()
                                                    }
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
                        })()}y
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
                                if (!config.materials) return null;
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
                                                    <img
                                                        src={notfoundImage}
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
                                                                    const materialsNode = doc.get("materials", true) as yaml.YAMLMap | null;

                                                                    materialsNode?.delete(value[0]);
                                                                    onEditConfig();
                                                                });
                                                                setConfirmDialogMessage(`Delete tool material "${value[0]}"?`);
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
            <NewItemDialog onOpenChange={setNewItemDialogOpen} open={newItemDialogOpen}/>
        </>
    );
}

function NewItemDialog({
                           onOpenChange,
                           open
                       }: {
    onOpenChange: (open: boolean) => void;
    open: boolean;
}) {
    const [tab, setTab] = useState("itemidtype");
    const [itemID, setItemID] = useState("");
    const [itemType, setItemType] = React.useState("");

    return (<>
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <Tabs value={tab} onValueChange={setTab}>
                        <TabsList>
                            <TabsTrigger value="itemidtype">Item ID and Type</TabsTrigger>
                            <TabsTrigger value="display" disabled={!(itemID && itemType)}>Display</TabsTrigger>
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
                                        <ItemTypeCombobox value={itemType} setValue={setItemType}/>
                                    </div>
                                </CardContent>
                                <CardFooter className="flex justify-between items-center">
                                    <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
                                    <Button
                                        disabled={!(itemID && itemType)}
                                        onClick={() => setTab("display")}
                                    >
                                        Next
                                    </Button>
                                </CardFooter>
                            </Card>
                        </TabsContent>
                        <TabsContent value="display">
                            <Card>
                                <CardHeader>
                                    <CardTitle>Display</CardTitle>
                                </CardHeader>
                                <CardContent className="grid gap-6">
                                    W.I.P.
                                </CardContent>
                                <CardFooter className="flex justify-between items-center">
                                    <Button
                                        variant="outline"
                                        onClick={() => setTab("itemidtype")}
                                    >
                                        Back
                                    </Button>
                                    <Button>
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

const itemTypes = [
    {value: "generic", label: "Generic"},
    {value: "sword", label: "Sword"},
    {value: "shovel", label: "Shovel"},
    {value: "pickaxe", label: "Pickaxe"},
    {value: "axe", label: "Axe"},
    {value: "hoe", label: "Hoe"},
    {value: "armor", label: "Armor"},
    {value: "elytra", label: "Elytra"},
    {value: "trident", label: "Trident"}
];

function ItemTypeCombobox({
                              value,
                              setValue
                          }: {
    value: string;
    setValue: (value: string) => void;
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
                        ? itemTypes.find((type) => type.value === value)?.label
                        : "Select item type..."}
                    <ChevronsUpDown className="opacity-50"/>
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[200px] p-0">
                <Command>
                    <CommandInput placeholder="Search item type..." className="h-9"/>
                    <CommandList>
                        <CommandEmpty>No item type found.</CommandEmpty>
                        <CommandGroup>
                            {itemTypes.map((type) => (
                                <CommandItem
                                    key={type.value}
                                    value={type.value}
                                    onSelect={(currentValue) => {
                                        setValue(currentValue === value ? "" : currentValue);
                                        setOpen(false);
                                    }}
                                >
                                    {type.label}
                                    <Check
                                        className={cn(
                                            "ml-auto",
                                            value === type.value ? "opacity-100" : "opacity-0"
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