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
    RotateCw,
    Search,
    Settings2,
    Sun,
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

import {CommandDialog, CommandEmpty, CommandInput, CommandItem, CommandList} from "@/components/ui/command.tsx";
import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger} from "@/components/ui/context-menu.tsx";
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
import {deleteFile, isFileInData, moveResource, uploadFile} from "@/lib/utils.ts";
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
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible.tsx";
import {toast} from "sonner";
import {Input} from "@/components/ui/input.tsx";
import {Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle} from "@/components/ui/dialog.tsx";
import {TextHoverEffect} from "@/components/ui/text-hover-effect";
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
import {FileViewer} from "@/fileviewer/fileviewer.tsx";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";
import {create} from "zustand/react";

function getPreferredTheme(): "dark" | "light" {
    const stored = localStorage.getItem("theme");
    if (stored === "dark" || stored === "light") return stored;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

type ApiData = {
    items: string[];
    files: TreeItem[];
    tool_materials: string[];
    armor_materials: string[];
};

export const apiPrefix = "http://localhost:7270/api";
//export const apiPrefix = "/api";

type DataStore = {
    data: ApiData;
    setData: (newData: ApiData) => void;
};

export const useDataStore = create<DataStore>()((set) => ({
    data: {items: [""], files: ["loading..."], tool_materials: [], armor_materials: []},
    setData: (newData) => set({data: newData}),
}));

export function App() {
    const [theme, setTheme] = useState<"dark" | "light">(getPreferredTheme());
    const [searchOpen, setSearchOpen] = useState(false);
    const setData = useDataStore((state) => state.setData);
    const data = useDataStore((state) => state.data);
    const [readonly, setReadonly] = useState(false);
    const [token] = useState<string | null>(() => {
        const params = new URLSearchParams(window.location.search);
        const text = params.get("token");
        if (text?.endsWith("_readonly")) setReadonly(true);
        return text;
    });
    const [openedFile, setOpenedFile] = useState<string>();
    const [fileContent, setFileContent] = useState<string>();
    const [fileNameDialogOpen, setFileNameDialogOpen] = useState(false);
    const [fileUploadDialogOpen, setFileUploadDialogOpen] = useState(false);
    const [newFilePath, setNewFilePath] = useState("");
    const [newFileDropdownMenuOpen, setNewFileDropdownMenuOpen] = useState(false);
    const [filesFromFolder, setFilesFromFolder] = useState<FileList>();
    const saveTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
    const wasFileContentLoaded = useRef(false);
    const handleNewTextFile = () => {
        setFileNameDialogOpen(false);
        if (newFilePath?.length > 0) {
            uploadFile(newFilePath, token ?? "", new Blob([""], {type: "text/plain"}), reload);
        }
    };

    const handleNewTextFileClick = () => {
        setNewFilePath("file.txt");
        setFileNameDialogOpen(true);
    };

    const reload = (useToast = false) => {
        const asyncReload = async () => {
            const response = await fetch(`${apiPrefix}/data?token=${token}`);
            if (!response.ok) {
                console.error(`HTTP error ${response.status}`);
            }
            const json = await response.json();
            setData(json);
            if (openedFile && !isFileInData(json.files, openedFile)) {
                setFileContent(undefined);
                setOpenedFile(undefined);
            }
        };

        if (useToast)
            toast.promise<void>(() => asyncReload(), {
                loading: "Reloading...",
                success: "Data reloaded",
                error: "Error",
                closeButton: true,
            });
        else asyncReload();
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

    const saveFileNow = () => {
        if (!openedFile || fileContent === undefined || !token) return;

        const lower = openedFile.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
            return;
        }

        uploadFile(openedFile, token, new Blob([fileContent], {type: "text/plain"}), reload);
    };

    useEffect(() => {
        if (fileContent === undefined) {
            wasFileContentLoaded.current = false;
            return;
        }
        if (!openedFile || !token) return;
        if (/\.(png|jpg|jpeg|gif)$/i.test(openedFile)) return;

        if (!wasFileContentLoaded.current) {
            wasFileContentLoaded.current = true;
            return;
        }

        if (saveTimeout.current) {
            clearTimeout(saveTimeout.current);
        }

        saveTimeout.current = setTimeout(() => {
            saveFileNow();
            saveTimeout.current = null;
        }, 3000);

        return () => {
            if (saveTimeout.current) {
                clearTimeout(saveTimeout.current);
            }
        };
    }, [fileContent]);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (readonly) return;
            if (e.ctrlKey && e.key === "s") {
                e.preventDefault();
                if (saveTimeout.current) {
                    clearTimeout(saveTimeout.current);
                    saveTimeout.current = null;
                }
                saveFileNow();
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [openedFile, fileContent, token]);

    return (
        <>
            <header className="fixed top-0 left-0 right-0 z-50 w-full flex items-center justify-between p-4 bg-blue-200 dark:bg-blue-800">
                <div className="flex items-center">
                    <div>
                        <TextHoverEffect text="LostEngine" />
                    </div>
                    <div className="-ml-18 pt-2">
                        {readonly && <span className="m-0 text-sm leading-none text-neutral-400 dark:text-neutral-600">Read-only</span>}
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <Button variant="outline" size="icon-lg" onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
                        {theme === "dark" ? <Moon /> : <Sun />}
                    </Button>
                    <a href="https://github.com/LostEngine/LostEngine" target="_blank" rel="noreferrer" aria-label="GitHub">
                        <img src={githubLogo} alt="GitHub" className="w-8 h-8 icon-black icon-white icon-gray-800 icon-gray-200" />
                    </a>
                </div>
            </header>
            <SidebarProvider>
                <Sidebar className="pt-18">
                    <SidebarContent>
                        <SidebarGroup>
                            <SidebarGroupContent>
                                <SidebarMenu>
                                    <SidebarMenuItem key="search">
                                        <SidebarMenuButton onClick={() => setSearchOpen(true)} className="flex items-center gap-2">
                                            <Search />
                                            <span>Search</span>
                                        </SidebarMenuButton>
                                    </SidebarMenuItem>
                                </SidebarMenu>
                            </SidebarGroupContent>
                        </SidebarGroup>
                        <SidebarGroup>
                            <div className="flex justify-between items-center">
                                <SidebarGroupLabel className="text-neutral-950 dark:text-neutral-50">Files</SidebarGroupLabel>
                                <div>
                                    <DropdownMenu modal={false} open={newFileDropdownMenuOpen} onOpenChange={setNewFileDropdownMenuOpen}>
                                        <DropdownMenuTrigger asChild>
                                            <Button variant="ghost" size="icon-sm">
                                                <FilePlusCorner />
                                            </Button>
                                        </DropdownMenuTrigger>
                                        <DropdownMenuContent className="w-40" align="end">
                                            <DropdownMenuGroup>
                                                <DropdownMenuItem disabled={readonly} onSelect={() => handleNewTextFileClick()}>
                                                    New Text File
                                                </DropdownMenuItem>
                                                <DropdownMenuItem disabled={readonly} onSelect={() => setFileUploadDialogOpen(true)}>
                                                    Upload File
                                                </DropdownMenuItem>
                                            </DropdownMenuGroup>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                    <Button
                                        variant="ghost"
                                        size="icon-sm"
                                        onClick={() => {
                                            const fileInput = document.createElement("input");
                                            fileInput.type = "file";
                                            fileInput.webkitdirectory = true;
                                            fileInput.style.display = "none";
                                            fileInput.addEventListener("change", (event) => {
                                                console.log(event);
                                                const files = (event.target as HTMLInputElement).files as FileList | undefined;
                                                setFilesFromFolder(files);
                                                // We want to get the folder using the first file
                                                const firstFilePath = files ? files[0]?.webkitRelativePath : undefined;
                                                const match = firstFilePath?.match(/^[^/]+/);
                                                setNewFilePath(match ? match[0] : "folder");
                                            });
                                            document.body.appendChild(fileInput);
                                            fileInput.click();
                                            document.body.removeChild(fileInput);
                                        }}
                                        disabled={readonly}
                                    >
                                        <FolderPlus />
                                    </Button>
                                    <Button variant="ghost" size="icon-sm" onClick={() => reload(true)}>
                                        <RotateCw />
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
                                                    setFileContent(undefined);
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
                            <SidebarTrigger />
                            <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
                            <Path file={openedFile as string} />
                        </div>
                        <div className="flex items-center gap-4">
                            {(() => {
                                if (!openedFile || !fileContent || !token) return;
                                const lower = openedFile?.toLowerCase();
                                if (
                                    lower?.endsWith(".png") ||
                                    lower?.endsWith(".jpg") ||
                                    lower?.endsWith(".jpeg") ||
                                    lower?.endsWith(".gif")
                                )
                                    return;
                                return (
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() =>
                                            uploadFile(
                                                openedFile as string,
                                                token,
                                                new Blob([fileContent as string], {
                                                    type: "text/plain",
                                                }),
                                                reload,
                                            )
                                        }
                                        disabled={readonly}
                                    >
                                        <Upload /> Upload Files
                                    </Button>
                                );
                            })()}
                        </div>
                    </header>
                    <div className="p-3.75 pb-10 h-full w-full pt-18">
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
                <CommandInput placeholder="Search a file..." />
                <CommandList>
                    <CommandEmpty>No results found.</CommandEmpty>
                    {data.files.map((item, index) => (
                        <CommandTree
                            key={index}
                            item={item}
                            onOpenFile={(path) => {
                                setOpenedFile(path);
                                setFileContent(undefined);
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
                        onInput={(e) => setNewFilePath((e.target as HTMLInputElement).value)}
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
                open={filesFromFolder !== undefined}
                onOpenChange={(open) => {
                    if (!open) setFilesFromFolder(undefined);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Upload Folder</DialogTitle>
                    </DialogHeader>
                    <Input
                        placeholder="Folder Path"
                        value={newFilePath}
                        onInput={(e) => setNewFilePath((e.target as HTMLInputElement).value)}
                    />
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>
                        <Button
                            type="submit"
                            onClick={() => {
                                if (!filesFromFolder) return;
                                setFilesFromFolder(undefined);

                                let completed = 0;
                                const total = filesFromFolder.length;

                                if (total === 0) {
                                    reload();
                                    return;
                                }

                                for (const file of filesFromFolder) {
                                    uploadFile(
                                        newFilePath + "/" + file.webkitRelativePath.replace(/^[^/]+\//, ""),
                                        token ?? "",
                                        file,
                                        () => {
                                            completed++;
                                            if (completed >= total) {
                                                reload();
                                            }
                                        },
                                    );
                                }
                            }}
                        >
                            Create Folder
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
            <FileUploadDialog open={fileUploadDialogOpen} onOpenChange={setFileUploadDialogOpen} token={token ?? ""} reload={reload} />
            <footer className="fixed bottom-0 left-0 w-full py-2 flex justify-center items-center space-x-6 text-sm text-gray-800 dark:text-gray-200">
                <div className="flex items-center gap-2">
                    <img src={codeLogo} alt="code" className="w-5 h-5 icon-gray-800 icon-gray-200" />
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
                    <img src={githubLogo} alt="GitHub" className="w-5 h-5 icon-gray-800 icon-gray-200" />
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
                    <img src={discordLogo} alt="Discord" className="w-5 h-5 icon-gray-800 icon-gray-200" />
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
    readonly,
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
    const [moveDialogOpen, setMoveDialogOpen] = useState(false);
    const [moveDialogPath, setMoveDialogPath] = useState(fullPath);

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
                                <FileImage />
                            ) : fullPath.endsWith(".yml") || fullPath.endsWith(".yaml") ? (
                                <Settings2 />
                            ) : (
                                <File />
                            )}
                            {name}
                        </SidebarMenuButton>
                    </ContextMenuTrigger>

                    <ContextMenuContent>
                        <ContextMenuItem onClick={handleOpen}>Open</ContextMenuItem>
                        <ContextMenuItem onClick={handleNewTextFileClick} disabled={readonly}>
                            New Text File
                        </ContextMenuItem>
                        <ContextMenuItem onClick={() => setMoveDialogOpen(true)} disabled={readonly}>
                            Move File
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
                            <AlertDialogTitle>Delete file &#34;{fullPath}&#34;?</AlertDialogTitle>
                            <AlertDialogDescription>This action cannot be undone.</AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction onClick={handleDelete}>Delete</AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
                <Dialog
                    open={moveDialogOpen}
                    onOpenChange={(open) => {
                        setMoveDialogOpen(open);
                        if (!open) setMoveDialogPath(fullPath);
                    }}
                >
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Move File</DialogTitle>
                        </DialogHeader>
                        <Input value={moveDialogPath} onInput={(e) => setMoveDialogPath((e.target as HTMLInputElement).value)} />
                        <DialogFooter>
                            <DialogClose asChild>
                                <Button variant="outline">Cancel</Button>
                            </DialogClose>
                            <Button
                                type="submit"
                                onClick={() => {
                                    if (!moveDialogPath) return;
                                    moveResource(fullPath, moveDialogPath, token, reload);
                                    setMoveDialogPath(fullPath);
                                    setMoveDialogOpen(false);
                                }}
                            >
                                Move File
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
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
                                <ChevronRight className="transition-transform" />
                                <Folder />
                                {name}
                            </SidebarMenuButton>
                        </ContextMenuTrigger>
                    </CollapsibleTrigger>

                    <ContextMenuContent>
                        <ContextMenuItem onClick={handleNewTextFileClick} disabled={readonly}>
                            New Text File
                        </ContextMenuItem>
                        <ContextMenuItem onClick={() => setMoveDialogOpen(true)} disabled={readonly}>
                            Move Folder
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
                        <AlertDialogTitle>Delete folder &#34;{fullPath}&#34;?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This action cannot be undone. Deleting a folder might delete more files than you think, be careful.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete}>Delete</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
            <Dialog
                open={moveDialogOpen}
                onOpenChange={(open) => {
                    setMoveDialogOpen(open);
                    if (!open) setMoveDialogPath(fullPath);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Move Folder</DialogTitle>
                    </DialogHeader>
                    <Input value={moveDialogPath} onInput={(e) => setMoveDialogPath((e.target as HTMLInputElement).value)} />
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>
                        <Button
                            type="submit"
                            onClick={() => {
                                if (!moveDialogPath) return;
                                moveResource(fullPath, moveDialogPath, token, reload);
                                setMoveDialogPath(fullPath);
                                setMoveDialogOpen(false);
                            }}
                        >
                            Move Folder
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </SidebarMenuItem>
    );
}

function CommandTree({item, parentPath = "", onOpenFile}: {item: TreeItem; parentPath?: string; onOpenFile: (path: string) => void}) {
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
                    <File />
                    <span>{fullPath}</span>
                </button>
            </CommandItem>
        );
    }

    return (
        <>
            {items.map((subItem, index) => (
                <CommandTree key={index} item={subItem} parentPath={fullPath} onOpenFile={onOpenFile} />
            ))}
        </>
    );
}

function Path({file}: {file: string | null}) {
    if (!file) {
        return (
            <>
                <Skeleton className="h-4 w-62.5" />
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
                                <BreadcrumbSeparator />
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
                        <CloudUpload className="size-4" />
                        Drag and drop or click here to upload files
                    </FileUploadDropzone>
                    <FileUploadList>
                        <ScrollArea className="h-50 w-full rounded-md border p-4">
                            {files.map((file, index) => (
                                <FileUploadItem key={index} value={file}>
                                    <FileUploadItemPreview />
                                    <FileUploadItemMetadata />
                                    <FileUploadItemDelete asChild>
                                        <Button variant="ghost" size="icon" className="size-7">
                                            <X />
                                            <span className="sr-only">Delete</span>
                                        </Button>
                                    </FileUploadItemDelete>
                                </FileUploadItem>
                            ))}
                        </ScrollArea>
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
