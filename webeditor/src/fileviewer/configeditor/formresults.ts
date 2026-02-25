/* eslint-disable @typescript-eslint/no-explicit-any */
import {getNewBlockForm, getNewItemForm, getNewMaterialForm, NEW_GLYPH_FORM} from "@/fileviewer/configeditor/forms.ts";

type FieldValue<T> = T extends {type: "text" | "texture"}
    ? string
    : T extends {type: "name"}
      ? Record<string, string>
      : T extends {type: "combobox"; options: readonly (infer O)[]}
        ? O
        : T extends {type: "opencombobox"; options: readonly (infer O)[]}
          ? O | string
          : T extends {type: "int" | "float"}
            ? number
            : T extends {type: "boolean"}
              ? boolean
              : unknown;

type FormToResult<T extends readonly any[]> = {
    [Tab in T[number] as Tab["id"]]: {
        [Field in Tab["fields"][number] as Field["name"]]: Field extends {
            required: true;
        }
            ? FieldValue<Field>
            : FieldValue<Field> | undefined;
    };
};

export type NewItemResult = FormToResult<ReturnType<typeof getNewItemForm>>;
export type NewBlockResult = FormToResult<ReturnType<typeof getNewBlockForm>>;
export type NewMaterialResult = FormToResult<ReturnType<typeof getNewMaterialForm>>;
export type NewGlyphResult = FormToResult<typeof NEW_GLYPH_FORM>;
