/* eslint-disable @typescript-eslint/no-explicit-any,react/react-in-jsx-scope */
import {useMemo, useState} from "preact/compat";
import {Button} from "@/components/ui/button.tsx";
import {Plus} from "lucide-react";
import {Input} from "@/components/ui/input.tsx";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table.tsx";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "@/components/ui/tabs.tsx";
import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import type {FormField, FormTab} from "@/fileviewer/configeditor/configeditor.tsx";
import {ValueCombobox} from "@/fileviewer/configeditor/valuecombobox.tsx";
import {Dialog, DialogContent, DialogHeader} from "@/components/ui/dialog.tsx";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";

export function NewItemDialog({
                                  onOpenChange,
                                  tabs,
                                  onSubmit,
                              }: {
    onOpenChange: (open: boolean) => void;
    tabs: FormTab[];
    onSubmit: (data: Record<string, any>) => void;
}) {
    const [data, setData] = useState<Record<string, any>>({});
    const [tabIndex, setTabIndex] = useState(0);

    const tab: FormTab = tabs[tabIndex];

    const handleOpenChange = (isOpen: boolean) => {
        onOpenChange(isOpen);
        if (!isOpen) {
            setData({});
            setTabIndex(0);
        }
    };

    const handleFieldChange = (stepId: string, fieldName: string, value: any) => {
        setData((prev) => ({
            ...prev,
            [stepId]: {
                ...(prev[stepId] || {}),
                [fieldName]: value,
            },
        }));
    };

    const isCurrentStepValid = useMemo(() => {
        if (!tab) return false;
        return tab.fields.every((field) => {
            if (!field.required) return true;
            const value = data[tab.id]?.[field.name];
            return value !== undefined && value !== "";
        });
    }, [tab, data]);

    const handleNext = () => setTabIndex((prev) => Math.min(prev + 1, tabs.length - 1));
    const handleBack = () => setTabIndex((prev) => Math.max(prev - 1, 0));

    const renderField = (stepId: string, field: FormField) => {
        const value = data[stepId]?.[field.name] || "";

        switch (field.type) {
            case "text":
                return (
                    <>
                        <Input
                            placeholder={field.placeholder}
                            value={value}
                            required={field.required}
                            list={`suggestions-${field.name}`}
                            onChange={(e) => handleFieldChange(stepId, field.name, (e.target as HTMLInputElement).value)}
                        />

                        {field.options && field.options.length > 0 && (
                            <datalist id={`suggestions-${field.name}`}>
                                {field.options.map((option: string) => (
                                    <option key={option} value={option}/>
                                ))}
                            </datalist>
                        )}
                    </>
                );
            case "combobox":
                return (
                    <ValueCombobox
                        name={field.label.toLowerCase()}
                        value={value}
                        values={field.options || []}
                        setValue={(val) => handleFieldChange(stepId, field.name, val)}
                    />
                );
            case "int":
            case "float":
                return (
                    <Input
                        placeholder={field.placeholder}
                        value={value}
                        required={field.required}
                        onChange={(e) => handleFieldChange(stepId, field.name, (e.target as HTMLInputElement).value)}
                        type="number"
                    />
                );
            case "name":
                return (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <div>
                                    <TableHead>language</TableHead>
                                    <TableHead>
                                        <Button variant="ghost" size="icon-sm" onClick={event => {
                                            event.stopPropagation();
                                            handleFieldChange(stepId, field.name, ({
                                                ...data[stepId]?.[field.name] || {},
                                                "": ""
                                            }))
                                        }}>
                                            <Plus/>
                                        </Button>
                                    </TableHead>
                                </div>
                                <TableHead className="text-right">name</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {Object.entries(data[stepId]?.[field.name] || {}).map(([lang, name], index) => (
                                <TableRow key={index}>
                                    <TableCell>
                                        <ValueCombobox
                                            value={lang}
                                            setValue={(newLang) => {
                                                const next: Record<string, string> = {};
                                                const prev: Record<string, string> = data[stepId]?.[field.name] || {};
                                                Object.keys(prev).forEach(key => {
                                                    if (key === lang) {
                                                        if (newLang) next[newLang] = name as string;
                                                    } else {
                                                        next[key] = prev[key];
                                                    }
                                                });
                                                handleFieldChange(stepId, field.name, next);
                                            }}
                                            values={languages}
                                            name="language"
                                            lowerCase
                                        />
                                    </TableCell>
                                    <TableCell>
                                        <Input
                                            placeholder="Item Name"
                                            className="text-right"
                                            value={name as string}
                                            onInput={(e) => {
                                                const newName = (e.target as HTMLInputElement).value;
                                                handleFieldChange(stepId, field.name, ({
                                                    ...data[stepId]?.[field.name] || {},
                                                    [lang]: newName
                                                }))
                                            }}
                                        />
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                );
            default:
                return null;
        }
    };

    return (
        <Dialog open={true} onOpenChange={handleOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <Tabs value={tab?.id} onValueChange={(val) => {
                        const index = tabs.findIndex(s => s.id === val);
                        if (index <= tabIndex || isCurrentStepValid) setTabIndex(index);
                    }}>
                        <TabsList>
                            {tabs.map((step, index) => (
                                <TabsTrigger
                                    key={step.id}
                                    value={step.id}
                                    disabled={index > tabIndex && !isCurrentStepValid}
                                >
                                    {step.tabLabel}
                                </TabsTrigger>
                            ))}
                        </TabsList>

                        {tabs.map((step, index) => (
                            <TabsContent key={step.id} value={step.id}>
                                <Card>
                                    <CardHeader>
                                        <CardTitle>{step.title}</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <ScrollArea className="max-h-[400px] w-ful overflow-y-auto">
                                            <div className="grid gap-6">
                                                {step.fields.map((field) => (
                                                    <div key={field.name} className="grid gap-3">
                                                        <label>{field.label}</label>
                                                        {renderField(step.id, field)}
                                                    </div>
                                                ))}
                                            </div>
                                        </ScrollArea>
                                    </CardContent>
                                    <CardFooter className="flex justify-between items-center">
                                        {index === 0 ? (
                                            <Button variant="outline"
                                                    onClick={() => handleOpenChange(false)}>Cancel</Button>
                                        ) : (
                                            <Button variant="outline" onClick={handleBack}>Back</Button>
                                        )}

                                        {index === tabs.length - 1 ? (
                                            <Button disabled={!isCurrentStepValid} onClick={() => onSubmit(data)}>
                                                Submit
                                            </Button>
                                        ) : (
                                            <Button disabled={!isCurrentStepValid} onClick={handleNext}>
                                                Next
                                            </Button>
                                        )}
                                    </CardFooter>
                                </Card>
                            </TabsContent>
                        ))}
                    </Tabs>
                </DialogHeader>
            </DialogContent>
        </Dialog>
    );
}

const languages = ["af_za", "ar_sa", "ast_es", "az_az", "ba_ru", "bar", "be_by", "be_latn", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz", "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca", "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy", "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan", "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr", "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "hal_ua", "haw_us", "he_il", "hi_in", "hn_no", "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is", "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr", "ksh", "kw_gb", "ky_kg", "la_la", "lb_lu", "li_li", "lmo", "lo_la", "lol_us", "lt_lt", "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de", "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pls", "pt_br", "pt_pt", "qcb_es", "qid", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "sah_sah", "se_no", "sk_sk", "sl_si", "so_so", "sq_al", "sr_cs", "sr_sp", "sv_se", "sxu", "szl", "ta_in", "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "tzo_mx", "uk_ua", "val_es", "vec_it", "vi_vn", "vp_vl", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab"];