import {type ClassValue, clsx} from "clsx";
import {twMerge} from "tailwind-merge";
import {apiPrefix, type TreeItem} from "@/app.tsx";
import {toast} from "sonner";

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

export function deleteFile(path: string, token: string, reload: () => void) {
    const asyncDeleteFile = async () => {
        const response = await fetch(`${apiPrefix}/delete_resource?path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`, {
            method: "DELETE",
        });
        if (!response.ok) {
            throw new Error(`HTTP error ${response.status}`);
        }
        reload();
    };

    toast.promise<void>(() => asyncDeleteFile(), {
        loading: "Deleting file...",
        success: "File deleted",
        error: "Error",
        closeButton: true,
    });
}

export function uploadFile(path: string, token: string, file: Blob | File | ArrayBuffer, reload: () => void) {
    const asyncUploadFile = async () => {
        const form = new FormData();
        form.append("path", path);

        if (file instanceof Blob || file instanceof File) {
            form.append("file", file);
        } else {
            form.append("file", new Blob([file]));
        }

        const response = await fetch(`${apiPrefix}/upload_resource?token=${encodeURIComponent(token)}`, {
            method: "POST",
            body: form,
        });

        if (!response.ok) {
            throw new Error(`HTTP error ${response.status}`);
        }
        reload();
    };

    toast.promise<void>(() => asyncUploadFile(), {
        loading: "Uploading file...",
        success: "File uploaded",
        error: "Error",
        closeButton: true,
    });
}

export function moveResource(path: string, destination: string, token: string, reload: () => void) {
    const asyncDeleteFile = async () => {
        const response = await fetch(
            `${apiPrefix}/move_resource?path=${encodeURIComponent(path)}&destination=${encodeURIComponent(destination)}&token=${encodeURIComponent(token)}`,
            {
                method: "POST",
            },
        );
        if (!response.ok) {
            throw new Error(`HTTP error ${response.status}`);
        }
        reload();
    };

    toast.promise<void>(() => asyncDeleteFile(), {
        loading: "Moving file...",
        success: "File moved",
        error: "Error",
        closeButton: true,
    });
}

export function getFiles(files: TreeItem[], prefix: string): string[] {
    const result: string[] = [];

    function walk(items: TreeItem[], currentPath: string[]) {
        for (const item of items) {
            if (typeof item === "string") {
                const path = [...currentPath, item].join("/");
                if (path.startsWith(prefix)) result.push(path);
            } else if (Array.isArray(item)) {
                const [folderName, ...children] = item as [string, ...TreeItem[]];
                walk(children, [...currentPath, folderName]);
            }
        }
    }
    walk(files, []);
    return result;
}

export function isFileInData(files: TreeItem[], targetPath: string): boolean {
    const lastSlashIndex = targetPath.lastIndexOf("/");
    const prefix = targetPath.substring(0, lastSlashIndex + 1);
    return getFiles(files, prefix).filter((value) => value === targetPath).length > 0;
}
