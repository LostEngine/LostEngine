import {type ClassValue, clsx} from "clsx"
import {twMerge} from "tailwind-merge"
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

    toast.promise<void>(
        () => asyncDeleteFile(),
        {
            loading: "Deleting file...",
            success: "File deleted",
            error: "Error",
            closeButton: true,
        }
    );
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

    toast.promise<void>(
        () => asyncUploadFile(),
        {
            loading: "Uploading file...",
            success: "File uploaded",
            error: "Error",
            closeButton: true,
        }
    );
}

export function moveResource(path: string, destination: string, token: string, reload: () => void) {
    const asyncDeleteFile = async () => {
        const response = await fetch(`${apiPrefix}/move_resource?path=${encodeURIComponent(path)}&destination=${encodeURIComponent(destination)}&token=${encodeURIComponent(token)}`, {
            method: "POST",
        });
        if (!response.ok) {
            throw new Error(`HTTP error ${response.status}`);
        }
        reload();
    };

    toast.promise<void>(
        () => asyncDeleteFile(),
        {
            loading: "Moving file...",
            success: "File moved",
            error: "Error",
            closeButton: true,
        }
    );
}

export function isFileInData(item: TreeItem, targetPath: string, parentPath = ""): boolean {
    const [rawName, ...items] = Array.isArray(item) ? item : [item];
    const name: string = typeof rawName === "string" ? rawName : "";
    const fullPath: string = parentPath ? `${parentPath}/${name}` : name;

    if (!items.length) {
        return fullPath === targetPath;
    }

    for (const subItem of items) {
        if (isFileInData(subItem, targetPath, fullPath)) {
            return true;
        }
    }

    return false;
}
