/* eslint-disable react/react-in-jsx-scope,@typescript-eslint/no-explicit-any */
import {useEffect, useState} from "preact/compat";
import * as yaml from "yaml";
import {type Config} from "@/config.ts";
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Pencil, Plus, Trash2} from "lucide-react";
import {CardBody, CardContainer, CardItem} from "@/components/ui/3d-card.tsx";
import {apiPrefix, useDataStore} from "@/app.tsx";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle
} from "@/components/ui/alert-dialog.tsx";
import {ImageWithSkeleton} from "@/fileviewer/fileviewer.tsx";
import {NewItemDialog} from "@/fileviewer/configeditor/dialog.tsx";
import {
    getNewMaterialForm,
    NEW_BLOCK_FORM, NEW_GLYPH_FORM,
    NEW_ITEM_FORM,
} from "@/fileviewer/configeditor/forms.ts";
import type {
    NewBlockResult,
    NewGlyphResult,
    NewItemResult,
    NewMaterialResult
} from "@/fileviewer/configeditor/formresults.ts";
import {create} from "zustand/react";
import {
    handleNewBlock,
    handleNewGlyph,
    handleNewItem,
    handleNewMaterial
} from "@/fileviewer/configeditor/formsubmits.ts";

type SoundsStore = {
    sounds?: string[];
    setSounds: (newSounds: string[]) => void;
}

export const useSoundsStore = create<SoundsStore>()((set) => ({
    setSounds: (newSounds) => set({sounds: newSounds}),
}))

export function ConfigEditor({text, onValueChange, folder, token}: {
    text: string;
    onValueChange: (value: string) => void;
    folder: string;
    token: string;
}) {
    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [confirmDialogAction, setConfirmDialogAction] = useState<() => void>(() => {
    });
    const [confirmDialogMessage, setConfirmDialogMessage] = useState("");
    const [newItemDialogTabs, setNewItemDialogTabs] = useState<any>();
    const [newItemDialogSubmit, setNewItemDialogSubmit] = useState<(data: Record<string, any>) => void>();
    const data = useDataStore((state) => state.data);
    const setSounds = useSoundsStore((state) => state.setSounds);
    const sounds = useSoundsStore((state) => state.sounds);

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
        onValueChange(doc.toString());
    };

    const fetchSounds = async () => {
        const response = await fetch("https://assets.mcasset.cloud/1.21.11/assets/minecraft/sounds.json");
        if (!response.ok) {
            console.error(`HTTP error ${response.status}`);
        }
        const json = await response.json();
        setSounds(Object.keys(json));
    };

    useEffect(() => {
        if (sounds && sounds.length > 0) {
            const armorTab = (newItemDialogTabs as FormTab[])?.find(tab => tab.id === "armor");

            if (armorTab) {
                // update equip_sound in getNewMaterialForm to add the sounds to the autocomplete
                const updatedTabs = (newItemDialogTabs as FormTab[])?.map(tab => {
                    if (tab.id === "armor") {
                        return {
                            ...tab,
                            fields: tab.fields.map(field =>
                                field.name === "equip_sound"
                                    ? {...field, options: sounds}
                                    : field
                            )
                        };
                    }
                    return tab;
                });

                setNewItemDialogTabs(updatedTabs);
            }
        }
    }, [sounds]);

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
                                setNewItemDialogTabs(NEW_ITEM_FORM);
                                setNewItemDialogSubmit(() => (data: NewItemResult) => {
                                    handleNewItem(data, doc, onEditConfig);
                                    setNewItemDialogTabs(undefined);
                                    setNewItemDialogSubmit(undefined);
                                });
                            }}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="h-full w-full flex flex-wrap gap-4 text-balance">
                        {(() => {
                            if (!config?.items) return;
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
                                                            if (value[1]?.icon) textureName = value[1]?.icon;
                                                            else if (value[1]?.texture) textureName = value[1]?.texture;
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
                                setNewItemDialogTabs(NEW_BLOCK_FORM);
                                setNewItemDialogSubmit(() => (data: NewBlockResult) => {
                                    handleNewBlock(data, doc, onEditConfig);
                                    setNewItemDialogTabs(undefined);
                                    setNewItemDialogSubmit(undefined);
                                });
                            }}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="h-full w-full flex flex-wrap gap-4 text-balance">
                        {(() => {
                            if (!config?.blocks) return;
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
                            <Button variant="ghost" size="icon-sm" onClick={event => {
                                event.stopPropagation();
                                setNewItemDialogTabs(getNewMaterialForm(data.items, sounds));
                                setNewItemDialogSubmit(() => (data: NewMaterialResult) => {
                                    handleNewMaterial(data, doc, onEditConfig);
                                    setNewItemDialogTabs(undefined);
                                    setNewItemDialogSubmit(undefined);
                                });
                                if (!sounds) fetchSounds();
                            }}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="flex flex-col gap-4 text-balance">
                        <div className="h-full w-full overflow-auto flex flex-wrap gap-4">
                            {(() => {
                                if (!config?.materials) return;
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
                                                                const repairItem = value[1]?.repair_item;
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
                            <Button variant="ghost" size="icon-sm" onClick={event => {
                                event.stopPropagation();
                                setNewItemDialogTabs(NEW_GLYPH_FORM);
                                setNewItemDialogSubmit(() => (data: NewGlyphResult) => {
                                    handleNewGlyph(data, doc, onEditConfig);
                                    setNewItemDialogTabs(undefined);
                                    setNewItemDialogSubmit(undefined);
                                });
                            }}>
                                <Plus/>
                            </Button>
                        </div>
                    </AccordionTrigger>
                    <AccordionContent className="flex flex-col gap-4 text-balance">
                        <div className="h-full w-full overflow-auto flex flex-wrap gap-4">
                            {(() => {
                                if (!config?.glyphs) return;
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
            {newItemDialogTabs !== undefined && newItemDialogSubmit !== undefined && <NewItemDialog
                onOpenChange={open => {
                    if (!open) {
                        setNewItemDialogTabs(undefined);
                        setNewItemDialogSubmit(undefined);
                    }
                }}
                tabs={newItemDialogTabs as FormTab[]}
                onSubmit={newItemDialogSubmit as (data: Record<string, any>) => void}
            />}
        </>
    );
}


export type FieldType = "text" | "combobox" | "name" | "int" | "float" | "texture";

export interface FormField {
    name: string;
    label: string;
    type: FieldType;
    required?: boolean;
    options?: string[]; // for combobox
    placeholder?: string;
}

export interface FormTab {
    id: string;
    tabLabel: string;
    title: string;
    fields: FormField[];
}
