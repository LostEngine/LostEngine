"use client";

import React, {useEffect, useRef, useState} from "preact/compat";
import githubLogo from "./assets/github-mark.svg";
import codeLogo from "./assets/code.svg";
import discordLogo from "./assets/discord.svg";
import {Button} from "@/components/ui/button";
import {
    ChevronRight,
    CloudUpload,
    File,
    FileImage,
    FilePlusCorner,
    Folder,
    FolderPlus,
    Moon,
    Pencil,
    Plus,
    RotateCw,
    Search,
    Settings2,
    Sun,
    Trash2,
    Upload,
    X,
} from "lucide-react";
import {
    Sidebar,
    SidebarContent,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarInset,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSub,
    SidebarProvider,
    SidebarTrigger,
} from "@/components/ui/sidebar";

import {CommandDialog, CommandEmpty, CommandInput, CommandItem, CommandList,} from "@/components/ui/command.tsx";
import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger,} from "@/components/ui/context-menu.tsx";
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from "@/components/ui/breadcrumb.tsx";
import {Separator} from "@/components/ui/separator.tsx";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import * as yaml from 'yaml';
import {deleteFile, isFileInData, uploadFile} from "@/lib/utils.ts";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog.tsx";
import {Collapsible, CollapsibleContent, CollapsibleTrigger,} from "@/components/ui/collapsible.tsx";
import {toast} from "sonner";
import {Input} from "@/components/ui/input.tsx";
import {Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle,} from "@/components/ui/dialog.tsx";
import {TextHoverEffect} from "@/components/ui/text-hover-effect";
import {Editor} from "@monaco-editor/react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import {
    FileUpload,
    FileUploadDropzone,
    FileUploadItem,
    FileUploadItemDelete,
    FileUploadItemMetadata,
    FileUploadItemPreview,
    FileUploadList,
} from "@/components/ui/file-upload.tsx";
import {ResizablePanel, ResizablePanelGroup} from "@/components/ui/resizable.tsx";
import type {Config} from "@/config.ts";
import {CardBody, CardContainer, CardItem} from "@/components/ui/3d-card.tsx";
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion.tsx";

function getPreferredTheme(): "dark" | "light" {
    const stored = localStorage.getItem("theme");
    if (stored === "dark" || stored === "light") return stored;
    return window.matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light";
}

type ApiData = {
    items: string[];
    files: TreeItem[];
};

export function App() {
    const [theme, setTheme] = useState<"dark" | "light">(getPreferredTheme());
    const [searchOpen, setSearchOpen] = useState(false);
    const [data, setData] = useState<ApiData>({
        items: [""],
        files: ["loading..."],
        //files: [["default", ["assets", ["textures", ["block", "block.png", "ore.png", "tnt.png"], ["item", "axe.png", "baguette.png", "hoe.png", "ingot.png", "pickaxe.png", "shovel.png", "sword.png",],],], "items.yml",],],
    });
    const [readonly, setReadonly] = useState(false);
    const [token] = useState<string | null>(() => {
        const params = new URLSearchParams(window.location.search);
        const text = params.get("token");
        if (text?.endsWith("_readonly")) setReadonly(true);
        return text;
    });
    const [openedFile, setOpenedFile] = useState<string | null>(null);
    const [fileContent, setFileContent] = useState<string | null>(null);
    const [fileNameDialogOpen, setFileNameDialogOpen] = useState(false);
    const [fileUploadDialogOpen, setFileUploadDialogOpen] = useState(false);
    const [newFilePath, setNewFilePath] = useState("");
    const [newFileDropdownMenuOpen, setNewFileDropdownMenuOpen] = useState(false);
    const [filesFromFolder, setFilesFromFolder] = useState<FileList | null>(null);
    const handleNewTextFile = () => {
        setFileNameDialogOpen(false);
        if (newFilePath.length > 0) {
            uploadFile(
                newFilePath,
                token ?? "",
                new Blob([""], {type: "text/plain"}),
                reload,
            );
        }
    };

    const handleNewTextFileClick = () => {
        setNewFilePath("file.txt");
        setFileNameDialogOpen(true);
    };

    const reload = () => {
        const asyncReload = async () => {
            const response = await fetch(`/api/data?token=${token}`);
            if (!response.ok) {
                console.error(`HTTP error ${response.status}`);
            }
            const json = await response.json();
            setData(json);
            if (openedFile !== null && !isFileInData(json.files, openedFile)) {
                setOpenedFile(null);
                setFileContent(null);
            }
        };

        toast.promise<void>(() => asyncReload(), {
            loading: "Reloading...",
            success: "Data reloaded",
            error: "Error",
            closeButton: true,
        });
    };

    /** Can be useful when getting `Uncaught (in promise) TypeError: Window.getComputedStyle: Argument 1 does not implement interface Element.` */
    // const origGetComputedStyle = window.getComputedStyle;
    // window.getComputedStyle = (el: Element | null | any) => {
    //     if (!(el instanceof Element)) {
    //         console.log('getComputedStyle called with:', el, 'type:', typeof el, 'isElement:', el instanceof Element);
    //     }
    //     return origGetComputedStyle.call(window, el);
    // };

    useEffect(() => {
        const root = document.documentElement;
        if (theme === "dark") {
            root.classList.add("dark");
        } else {
            root.classList.remove("dark");
        }
        localStorage.setItem("theme", theme);
    }, [theme]);

    useEffect(() => {
        if (!token) {
            console.error("Token is null");
            return;
        }
        reload();
    }, [token]);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (readonly) return;
            if (e.ctrlKey && e.key === "s") {
                e.preventDefault();
                if (openedFile !== null && fileContent !== null && token !== null) {
                    const lower = openedFile.toLowerCase();
                    if (
                        !lower.endsWith(".png") &&
                        !lower.endsWith(".jpg") &&
                        !lower.endsWith(".jpeg") &&
                        !lower.endsWith(".gif")
                    ) {
                        uploadFile(
                            openedFile,
                            token,
                            new Blob([fileContent], {type: "text/plain"}),
                            reload,
                        );
                    }
                }
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [openedFile, fileContent, token]);

    return (
        <>
            <header
                className="fixed top-0 left-0 right-0 z-50 w-full flex items-center justify-between p-4 bg-blue-200 dark:bg-blue-800">
                <div className="flex items-center">
                    <div>
                        <TextHoverEffect text="LostEngine"/>
                    </div>
                    <div className="-ml-18 pt-2">
                        {readonly && <span className="m-0 text-sm leading-none text-neutral-400 dark:text-neutral-600">Read-only</span>}
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <Button
                        variant="outline"
                        size="icon-lg"
                        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
                    >
                        {theme === "dark" ? <Moon/> : <Sun/>}
                    </Button>
                    <a
                        href="https://github.com/LostEngine/LostEngine"
                        target="_blank"
                        rel="noreferrer"
                        aria-label="GitHub"
                    >
                        <img
                            src={githubLogo}
                            alt="GitHub"
                            className="w-8 h-8 icon-black icon-white icon-gray-800 icon-gray-200"
                        />
                    </a>
                </div>
            </header>
            <SidebarProvider>
                <Sidebar className="top-18">
                    <SidebarContent>
                        <SidebarGroup>
                            <SidebarGroupContent>
                                <SidebarMenu>
                                    <SidebarMenuItem key="search">
                                        <SidebarMenuButton
                                            onClick={() => setSearchOpen(true)}
                                            className="flex items-center gap-2"
                                        >
                                            <Search/>
                                            <span>Search</span>
                                        </SidebarMenuButton>
                                    </SidebarMenuItem>
                                </SidebarMenu>
                            </SidebarGroupContent>
                        </SidebarGroup>
                        <SidebarGroup>
                            <div className="flex justify-between items-center">
                                <SidebarGroupLabel className="text-neutral-950 dark:text-neutral-50">
                                    Files
                                </SidebarGroupLabel>
                                <div>
                                    <DropdownMenu
                                        modal={false}
                                        open={newFileDropdownMenuOpen}
                                        onOpenChange={setNewFileDropdownMenuOpen}
                                    >
                                        <DropdownMenuTrigger asChild>
                                            <Button variant="ghost" size="icon-sm">
                                                <FilePlusCorner/>
                                            </Button>
                                        </DropdownMenuTrigger>
                                        <DropdownMenuContent className="w-40" align="end">
                                            <DropdownMenuGroup>
                                                <DropdownMenuItem
                                                    disabled={readonly}
                                                    onSelect={() => handleNewTextFileClick()}
                                                >
                                                    New Text File
                                                </DropdownMenuItem>
                                                <DropdownMenuItem
                                                    disabled={readonly}
                                                    onSelect={() => setFileUploadDialogOpen(true)}
                                                >
                                                    Upload File
                                                </DropdownMenuItem>
                                            </DropdownMenuGroup>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                    <Button
                                        variant="ghost"
                                        size="icon-sm"
                                        onClick={() => {
                                            const fileInput = document.createElement('input');
                                            fileInput.type = 'file';
                                            fileInput.webkitdirectory = true;
                                            fileInput.style.display = 'none';
                                            fileInput.addEventListener('change', (event) => {
                                                setNewFilePath((event.target as HTMLInputElement).dirName || "folder");
                                                setFilesFromFolder((event.target as HTMLInputElement).files);
                                            });
                                            document.body.appendChild(fileInput);
                                            fileInput.click();
                                            document.body.removeChild(fileInput);
                                        }}
                                        disabled={readonly}
                                    >
                                        <FolderPlus/>
                                    </Button>
                                    <Button variant="ghost" size="icon-sm" onClick={reload}>
                                        <RotateCw/>
                                    </Button>
                                </div>
                            </div>
                            <div className="pt-2">
                                <SidebarGroupContent>
                                    <SidebarMenu>
                                        {data.files.map((item, index) => (
                                            <Tree
                                                key={index}
                                                item={item}
                                                onOpenFile={(path) => {
                                                    setOpenedFile(path);
                                                    setFileContent(null);
                                                }}
                                                token={token ?? ""}
                                                reload={reload}
                                                newFilePath={newFilePath}
                                                setNewFilePath={setNewFilePath}
                                                setFileNameDialogOpen={setFileNameDialogOpen}
                                                handleNewTextFile={handleNewTextFile}
                                                readonly={readonly}
                                            />
                                        ))}
                                    </SidebarMenu>
                                </SidebarGroupContent>
                            </div>
                        </SidebarGroup>
                    </SidebarContent>
                </Sidebar>
                <SidebarInset className="pt-18">
                    <header className="flex h-16 shrink-0 w-full items-center justify-between gap-2 border-b px-4 fixed top-18 z-50 bg-white dark:bg-neutral-950">
                        <div className="flex items-center gap-4">
                            <SidebarTrigger/>
                            <Separator
                                orientation="vertical"
                                className="mr-2 data-[orientation=vertical]:h-4"
                            />
                            <Path file={openedFile}/>
                        </div>
                        <div className="flex items-center gap-4">
                            {(() => {
                                if (
                                    openedFile === null ||
                                    fileContent === null ||
                                    token === null
                                )
                                    return null;
                                const lower = openedFile.toLowerCase();
                                if (
                                    lower.endsWith(".png") ||
                                    lower.endsWith(".jpg") ||
                                    lower.endsWith(".jpeg") ||
                                    lower.endsWith(".gif")
                                )
                                    return null;
                                return (
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        className=""
                                        onClick={() =>
                                            uploadFile(
                                                openedFile,
                                                token,
                                                new Blob([fileContent], {type: "text/plain"}),
                                                reload,
                                            )
                                        }
                                        disabled={readonly}
                                    >
                                        <Upload/> Upload File
                                    </Button>
                                );
                            })()}
                        </div>
                    </header>
                    <div className="p-[15px] pb-10 h-full w-full pt-18">
                        <FileViewer
                            filePath={openedFile}
                            token={token}
                            content={fileContent}
                            onContentChange={setFileContent}
                            theme={theme}
                        />
                    </div>
                </SidebarInset>
            </SidebarProvider>
            <CommandDialog open={searchOpen} onOpenChange={setSearchOpen}>
                <CommandInput placeholder="Search a file..."/>
                <CommandList>
                    <CommandEmpty>No results found.</CommandEmpty>
                    {data.files.map((item, index) => (
                        <CommandTree
                            key={index}
                            item={item}
                            onOpenFile={(path) => {
                                setOpenedFile(path);
                                setFileContent(null);
                                setSearchOpen(false);
                            }}
                        />
                    ))}
                </CommandList>
            </CommandDialog>
            <Dialog open={fileNameDialogOpen} onOpenChange={setFileNameDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>New File</DialogTitle>
                    </DialogHeader>
                    <Input
                        placeholder="File Path"
                        value={newFilePath}
                        onInput={(e) =>
                            setNewFilePath((e.target as HTMLInputElement).value)
                        }
                    />
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>
                        <Button type="submit" onClick={handleNewTextFile}>
                            Create File
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
            <Dialog
                open={filesFromFolder != null}
                onOpenChange={open => {
                    if (!open) setFilesFromFolder(null);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Upload Folder</DialogTitle>
                    </DialogHeader>
                    <Input
                        placeholder="Folder Path"
                        value={newFilePath}
                        onInput={(e) =>
                            setNewFilePath((e.target as HTMLInputElement).value)
                        }
                    />
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>
                        <Button type="submit" onClick={() => {
                            if (!filesFromFolder) return;
                            const files = filesFromFolder;
                            setFilesFromFolder(null);

                            let completed = 0;
                            const total = files.length;

                            if (total === 0) {
                                reload();
                                return;
                            }

                            for (const file of files) {
                                uploadFile(newFilePath + "/" + file.name, token ?? "", file, () => {
                                    completed++;
                                    if (completed >= total) {
                                        reload();
                                    }
                                });
                            }
                        }}>
                            Create Folder
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
            <FileUploadDialog
                open={fileUploadDialogOpen}
                onOpenChange={setFileUploadDialogOpen}
                token={token ?? ""}
                reload={reload}
            />
            <footer
                className="fixed bottom-0 left-0 w-full py-2 flex justify-center items-center space-x-6 text-sm text-gray-800 dark:text-gray-200">
                <div className="flex items-center gap-2">
                    <img
                        src={codeLogo}
                        alt="code"
                        className="w-5 h-5 icon-gray-800 icon-gray-200"
                    />
                    <span>
            Developed by{" "}
                        <a
                            href="https://github.com/misieur"
                            target="_blank"
                            rel="noreferrer"
                            className="underline dark:hover:text-white hover:text-black"
                        >
              Misieur
            </a>
          </span>
                </div>
                <div className="flex items-center gap-2">
                    <img
                        src={githubLogo}
                        alt="GitHub"
                        className="w-5 h-5 icon-gray-800 icon-gray-200"
                    />
                    <span>
            Source code on{" "}
                        <a
                            href="https://github.com/LostEngine/LostEngine"
                            target="_blank"
                            rel="noreferrer"
                            className="underline dark:hover:text-white hover:text-black"
                        >
              GitHub
            </a>
          </span>
                </div>
                <div className="flex items-center gap-2">
                    <img
                        src={discordLogo}
                        alt="Discord"
                        className="w-5 h-5 icon-gray-800 icon-gray-200"
                    />
                    <span>
            Join my{" "}
                        <a
                            href="https://discord.com/invite/5VSeDcyJt7"
                            target="_blank"
                            rel="noreferrer"
                            className="underline dark:hover:text-white hover:text-black"
                        >
              Discord server
            </a>
          </span>
                </div>
            </footer>
        </>
    );
}

export type TreeItem = string | TreeItem[];

function Tree({
                  item,
                  parentPath = "",
                  onOpenFile,
                  token,
                  reload,
                  setNewFilePath,
                  setFileNameDialogOpen,
                  newFilePath,
                  handleNewTextFile,
                  readonly
              }: {
    item: TreeItem;
    parentPath?: string;
    onOpenFile: (path: string) => void;
    token: string;
    reload: () => void;
    setNewFilePath: (path: string) => void;
    setFileNameDialogOpen: (open: boolean) => void;
    newFilePath: string;
    handleNewTextFile: () => void;
    readonly: boolean;
}) {
    const [rawName, ...items] = Array.isArray(item) ? item : [item];
    const name: string = typeof rawName === "string" ? rawName : "";
    const fullPath: string = parentPath ? `${parentPath}/${name}` : name;
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const handleNewTextFileClick = () => {
        let folderPath = items.length
            ? fullPath
            : fullPath
                .split("/")
                .slice(0, -1)
                .join("/")
                .replace(/^\/+|\/+$/g, "");
        if (folderPath) folderPath += "/";
        setNewFilePath(folderPath + "file.txt");
        setFileNameDialogOpen(true);
    };

    const handleDelete = () => {
        deleteFile(fullPath, token, reload);
    };

    const handleDeleteClick = () => {
        setDeleteDialogOpen(true);
    };

    if (!items.length) {
        const handleOpen = () => {
            onOpenFile(fullPath);
        };

        return (
            <div>
                <ContextMenu>
                    <ContextMenuTrigger asChild>
                        <SidebarMenuButton
                            onClick={handleOpen}
                            isActive={name === "button.tsx"}
                            className="data-[active=true]:bg-transparent"
                        >
                            {fullPath.endsWith(".png") ||
                            fullPath.endsWith(".jpg") ||
                            fullPath.endsWith(".jpeg") ||
                            fullPath.endsWith(".gif") ? (
                                <FileImage/>
                            ) : fullPath.endsWith(".yml") || fullPath.endsWith(".yaml") ? (
                                <Settings2/>
                            ) : (
                                <File/>
                            )}
                            {name}
                        </SidebarMenuButton>
                    </ContextMenuTrigger>

                    <ContextMenuContent>
                        <ContextMenuItem onClick={handleOpen}>Open</ContextMenuItem>
                        <ContextMenuItem onClick={handleNewTextFileClick} disabled={readonly}>
                            New Text File
                        </ContextMenuItem>
                        <ContextMenuItem onClick={handleDeleteClick} disabled={readonly} variant="destructive">
                            Delete File
                        </ContextMenuItem>
                    </ContextMenuContent>
                </ContextMenu>
                <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            {/* `&#34;` = `"` */}
                            <AlertDialogTitle>
                                Delete file &#34;{fullPath}&#34;?
                            </AlertDialogTitle>
                            <AlertDialogDescription>
                                This action cannot be undone.
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction onClick={handleDelete}>
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            </div>
        );
    }

    return (
        <SidebarMenuItem>
            <Collapsible
                className="group/collapsible [&[data-state=open]>button>svg:first-child]:rotate-90"
                defaultOpen={name === "components" || name === "ui"}
            >
                <ContextMenu>
                    <CollapsibleTrigger asChild>
                        <ContextMenuTrigger asChild>
                            <SidebarMenuButton>
                                <ChevronRight className="transition-transform"/>
                                <Folder/>
                                {name}
                            </SidebarMenuButton>
                        </ContextMenuTrigger>
                    </CollapsibleTrigger>

                    <ContextMenuContent>
                        <ContextMenuItem onClick={handleNewTextFileClick} disabled={readonly}>
                            New Text File
                        </ContextMenuItem>
                        <ContextMenuItem onClick={handleDeleteClick} disabled={readonly} variant="destructive">
                            Delete Folder
                        </ContextMenuItem>
                    </ContextMenuContent>
                </ContextMenu>

                <CollapsibleContent>
                    <SidebarMenuSub>
                        {items.map((subItem, index) => (
                            <Tree
                                key={index}
                                item={subItem}
                                parentPath={fullPath}
                                onOpenFile={onOpenFile}
                                token={token}
                                reload={reload}
                                handleNewTextFile={handleNewTextFile}
                                newFilePath={newFilePath}
                                setFileNameDialogOpen={setFileNameDialogOpen}
                                setNewFilePath={setNewFilePath}
                                readonly={readonly}
                            />
                        ))}
                    </SidebarMenuSub>
                </CollapsibleContent>
            </Collapsible>
            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        {/* `&#34;` = `"` */}
                        <AlertDialogTitle>
                            Delete folder &#34;{fullPath}&#34;?
                        </AlertDialogTitle>
                        <AlertDialogDescription>
                            This action cannot be undone. Deleting a folder might delete more
                            files than you think, be careful.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete}>Delete</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </SidebarMenuItem>
    );
}

function CommandTree({
                         item,
                         parentPath = "",
                         onOpenFile,
                     }: {
    item: TreeItem;
    parentPath?: string;
    onOpenFile: (path: string) => void;
}) {
    const [rawName, ...items] = Array.isArray(item) ? item : [item];
    const name: string = typeof rawName === "string" ? rawName : "";
    const fullPath: string = parentPath ? `${parentPath}/${name}` : name;

    if (!items.length) {
        return (
            <CommandItem asChild>
                <button
                    type="button"
                    onClick={() => {
                        onOpenFile(fullPath);
                        console.log("open file:", fullPath);
                    }}
                    className="flex items-center gap-2 w-full text-left"
                >
                    <File/>
                    <span>{fullPath}</span>
                </button>
            </CommandItem>
        );
    }

    return (
        <>
            {items.map((subItem, index) => (
                <CommandTree
                    key={index}
                    item={subItem}
                    parentPath={fullPath}
                    onOpenFile={onOpenFile}
                />
            ))}
        </>
    );
}

function Path({file}: { file: string | null }) {
    if (!file) {
        return (
            <>
                <Skeleton className="h-4 w-[250px]"/>
            </>
        );
    }
    const array = file.split("/");
    return (
        <Breadcrumb>
            <BreadcrumbList className="top-18">
                {array.map((value, index) => (
                    <React.Fragment key={value}>
                        {index < array.length - 1 ? (
                            <>
                                <BreadcrumbItem className="hidden md:block">
                                    <BreadcrumbLink>{value}</BreadcrumbLink>
                                </BreadcrumbItem>
                                <BreadcrumbSeparator/>
                            </>
                        ) : (
                            <BreadcrumbItem>
                                <BreadcrumbPage>{value}</BreadcrumbPage>
                            </BreadcrumbItem>
                        )}
                    </React.Fragment>
                ))}
            </BreadcrumbList>
        </Breadcrumb>
    );
}

function FileViewer({
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
                const url = `/api/download_resource?path=${encodeURIComponent(filePath)}&token=${encodeURIComponent(token)}`;
                const res = await fetch(url);

                if (!res.ok) throw new Error(`Failed to load file: ${res.status}`);

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
            <ResizablePanel defaultSize={70}>
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
        </ResizablePanelGroup>
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
    const dragging = useRef(false);
    const lastPos = useRef({x: 0, y: 0});

    const handleWheel = (e: WheelEvent) => {
        e.preventDefault();
        const delta = -e.deltaY / 400;
        setScale((prev) => Math.min(Math.max(prev + delta, 0.1), 5));
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
                className="block"
                style={{
                    transform: `scale(${scale}) translate(${offset.x / scale}px, ${offset.y / scale}px)`,
                    transformOrigin: "center center",
                    imageRendering: "pixelated",
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
    const [confirmDialogAction, setConfirmDialogAction] = useState<() => void>(() => {});
    const [confirmDialogMessage, setConfirmDialogMessage] = useState("");

    let doc;
    let config: Config | undefined;

    try {
        doc = yaml.parse(text);
        config = doc as Config;
    } catch (err: unknown) {
        const message = err instanceof Error ? err.message : String(err);
        return (<div className="text-red-500">{message}</div>);
    }

    const onEditConfig = () => {
        const updatedText = yaml.stringify(doc);
        onValueChange(updatedText);
    }

    return (
        <>
            <Accordion
                type="single"
                collapsible
                className="w-full"
            >
                <AccordionItem value="item-1">
                    <AccordionTrigger>
                        <div className="flex items-center gap-2">
                            Items
                            <Button variant="ghost" size="icon-sm" onClick={event => event.stopPropagation()}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="flex flex-col gap-4 text-balance">
                        <div className="h-full w-full overflow-auto flex flex-wrap gap-4">
                            {(() => {
                                if (!config.items) return null;
                                return Array.from(Object.entries(config.items)).map(value => {
                                    return (<>
                                        <CardContainer>
                                            <CardBody
                                                className="bg-gray-50 relative group/card  dark:hover:shadow-2xl dark:hover:shadow-emerald-500/[0.1] dark:bg-neutral-950 dark:border-white/[0.2] border-black/[0.1] w-auto sm:w-[15rem] h-auto rounded-xl p-6 border">
                                                <CardItem
                                                    translateZ="50"
                                                    className="text-xl font-bold text-neutral-600 dark:text-white"
                                                >
                                                    {value[0]}
                                                </CardItem>
                                                <CardItem translateZ="50" className="w-full mt-4">
                                                    <img
                                                        src={
                                                            value[1].texture ?
                                                                `/api/download_resource?path=${encodeURIComponent(folder + "/assets/textures/" + (value[1].texture.endsWith(".png") ? value[1].texture : (value[1].texture + ".png")))}&token=${encodeURIComponent(token)}` :
                                                                `data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAAXNSR0IArs4c6QAAAI5JREFUKJG10DsKwlAQRuHvYiAYCCkE9+CC7hJdjQsIWIkIgp2IAQu5FknIw5Q6zcxwDsPw85eKKaZ+DnMEGzzsw0ToUdXt904J38hECTHtvBb+yPFykXGznSg5HSwdZQxKj67eWEErtMqAoOx6RqNYgI0ahxCIaa1QjQ7X4DDk0CqNp/MITZKMidMM/ao+PCEzK4GxfXsAAAAASUVORK5CYII=`
                                                        }
                                                        height="1000"
                                                        width="1000"
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
                                                                    delete config?.items?.[value[0]];
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
                                })
                            })()}
                        </div>
                    </AccordionContent>
                </AccordionItem>
                <AccordionItem value="item-2">
                    <AccordionTrigger>
                        <div className="flex items-center gap-2">
                            Tool Materials
                            <Button variant="ghost" size="icon-sm" onClick={event => event.stopPropagation()}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="flex flex-col gap-4 text-balance">
                        <div className="h-full w-full overflow-auto flex flex-wrap gap-4">
                            {(() => {
                                if (!config.tool_materials) return null;
                                return Array.from(Object.entries(config.tool_materials)).map(value => {
                                    return (<>
                                        <CardContainer>
                                            <CardBody
                                                className="bg-gray-50 relative group/card  dark:hover:shadow-2xl dark:hover:shadow-emerald-500/[0.1] dark:bg-neutral-950 dark:border-white/[0.2] border-black/[0.1] w-auto sm:w-[15rem] h-auto rounded-xl p-6 border">
                                                <CardItem
                                                    translateZ="50"
                                                    className="text-xl font-bold text-neutral-600 dark:text-white"
                                                >
                                                    {value[0]}
                                                </CardItem>
                                                <CardItem translateZ="50" className="w-full mt-4">
                                                    <img
                                                        src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAAXNSR0IArs4c6QAAAI5JREFUKJG10DsKwlAQRuHvYiAYCCkE9+CC7hJdjQsIWIkIgp2IAQu5FknIw5Q6zcxwDsPw85eKKaZ+DnMEGzzsw0ToUdXt904J38hECTHtvBb+yPFykXGznSg5HSwdZQxKj67eWEErtMqAoOx6RqNYgI0ahxCIaa1QjQ7X4DDk0CqNp/MITZKMidMM/ao+PCEzK4GxfXsAAAAASUVORK5CYII="
                                                        height="1000"
                                                        width="1000"
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
                                                                    delete config?.tool_materials?.[value[0]];
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
                                })
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
        </>
    );
}

function FileUploadDialog({
                              open,
                              onOpenChange,
                              token,
                              reload,
                          }: {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    token: string;
    reload: () => void;
}) {
    const [files, setFiles] = useState<File[]>([]);

    useEffect(() => {
        if (!open) setFiles([]);
    }, [open]);

    const handleUpload = () => {
        onOpenChange(false);
        let completed = 0;
        const total = files.length;

        if (total === 0) {
            reload();
            return;
        }

        files.forEach((file) => {
            uploadFile(file.name, token, file, () => {
                completed++;
                if (completed >= total) {
                    reload();
                }
            });
        });
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Upload Files</DialogTitle>
                </DialogHeader>
                <FileUpload
                    value={files}
                    onValueChange={setFiles}
                    multiple
                    maxSize={536870912}
                    onFileReject={(file, message) => {
                        toast.error(
                            message +
                            " (" +
                            file.name +
                            ") LostEngine is not made for uploading big files, " +
                            "uploading your whole computer through LostEngine's integrated web server might not be a good idea.",
                        );
                    }}
                >
                    <FileUploadDropzone className="flex-row flex-wrap border-dotted text-center">
                        <CloudUpload className="size-4"/>
                        Drag and drop or click here to upload files
                    </FileUploadDropzone>
                    <FileUploadList>
                        {files.map((file, index) => (
                            <FileUploadItem key={index} value={file}>
                                <FileUploadItemPreview/>
                                <FileUploadItemMetadata/>
                                <FileUploadItemDelete asChild>
                                    <Button variant="ghost" size="icon" className="size-7">
                                        <X/>
                                        <span className="sr-only">Delete</span>
                                    </Button>
                                </FileUploadItemDelete>
                            </FileUploadItem>
                        ))}
                    </FileUploadList>
                </FileUpload>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline">Cancel</Button>
                    </DialogClose>
                    <Button type="submit" onClick={handleUpload}>
                        Upload Files
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
