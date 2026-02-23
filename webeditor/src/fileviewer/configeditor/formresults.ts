/* eslint-disable @typescript-eslint/no-explicit-any */
import {
    getNewMaterialForm,
    type NEW_BLOCK_FORM,
    NEW_GLYPH_FORM,
    NEW_ITEM_FORM
} from "@/fileviewer/configeditor/forms.ts";

type FieldValue<T> =
    T extends { type: "text" | "texture" } ? string :
        T extends { type: "name" } ? Record<string, string> :
            T extends { type: "combobox"; options: readonly (infer O)[] } ? O :
                T extends { type: "opencombobox"; options: readonly (infer O)[] } ? O | string :
                    T extends { type: "int" | "float" } ? number :
                        unknown;

type FormToResult<T extends readonly any[]> = {
    [Tab in T[number] as Tab["id"]]: {
        [Field in Tab["fields"][number] as Field["name"]]:
        Field extends { required: true }
            ? FieldValue<Field>
            : FieldValue<Field> | undefined;
    };
};

export type NewItemResult = FormToResult<typeof NEW_ITEM_FORM>;
export type NewBlockResult = FormToResult<typeof NEW_BLOCK_FORM>;
export type NewMaterialResult = FormToResult<ReturnType<typeof getNewMaterialForm>>;
export type NewGlyphResult = FormToResult<typeof NEW_GLYPH_FORM>;