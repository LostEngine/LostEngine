import React from "preact/compat";
import {Popover, PopoverContent, PopoverTrigger} from "@/components/ui/popover.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Check, ChevronsUpDown} from "lucide-react";
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from "@/components/ui/command.tsx";
import {cn} from "@/lib/utils.ts";

export function ValueCombobox<T extends string>({
                                                    value,
                                                    setValue,
                                                    values,
                                                    name,
                                                    lowerCase
                                                }: {
    value?: T,
    setValue: (value?: T) => void,
    values: T[],
    name: string,
    lowerCase?: boolean
}) {
    const [open, setOpen] = React.useState(false);

    function format(text?: string, lowerCase?: boolean) {
        if (lowerCase) return text;
        return text?.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase()).trim();
    }

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
                        ? format(value, lowerCase)
                        : `Select ${name}...`}
                    <ChevronsUpDown className="opacity-50"/>
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[200px] p-0">
                <Command>
                    <CommandInput placeholder={`Search ${name}...`} className="h-9"/>
                    <CommandList>
                        <CommandEmpty>No {name} found.</CommandEmpty>
                        <CommandGroup>
                            {values.map((type) => (
                                <CommandItem
                                    key={type}
                                    value={type}
                                    onSelect={(currentValue) => {
                                        const newValue = currentValue === value ? undefined : currentValue as T;
                                        setValue(newValue);
                                        setOpen(false);
                                    }}
                                >
                                    {format(type, lowerCase)}
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