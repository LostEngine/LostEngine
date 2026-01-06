"use client"

import * as React from "preact/compat"
import {Slot} from "@radix-ui/react-slot"
import {cva, type VariantProps} from "class-variance-authority"
import {PanelLeftIcon} from "lucide-react"

import {useIsMobile} from "@/hooks/use-mobile"
import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle,} from "@/components/ui/sheet"
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger,} from "@/components/ui/tooltip"

const SIDEBAR_WIDTH_MOBILE = "18rem"
const DEFAULT_SIDEBAR_WIDTH = 288
const SIDEBAR_WIDTH_ICON = "3rem"
const SIDEBAR_KEYBOARD_SHORTCUT = "b"
const MIN_WIDTH = 192
const MAX_WIDTH = 512

type SidebarContextProps = {
    state: "expanded" | "collapsed"
    open: boolean
    setOpen: (open: boolean) => void
    openMobile: boolean
    setOpenMobile: (open: boolean) => void
    isMobile: boolean
    toggleSidebar: () => void
    sidebarWidth: number
    setSidebarWidth: (width: number) => void
    isDragging: boolean
    setIsDragging: (isDragging: boolean) => void
}

const SidebarContext = React.createContext<SidebarContextProps | null>(null)

function useSidebar() {
    const context = React.useContext(SidebarContext)
    if (!context) {
        throw new Error("useSidebar must be used within a SidebarProvider.")
    }

    return context
}

function SidebarProvider({
                             defaultOpen = true,
                             open: openProp,
                             onOpenChange: setOpenProp,
                             className,
                             style,
                             children,
                             ...props
                         }: React.ComponentProps<"div"> & {
    defaultOpen?: boolean
    open?: boolean
    onOpenChange?: (open: boolean) => void
}) {
    const isMobile = useIsMobile()
    const [openMobile, setOpenMobile] = React.useState(false)

    // This is the internal state of the sidebar.
    // We use openProp and setOpenProp for control from outside the component.
    const [_open, _setOpen] = React.useState(defaultOpen)
    const open = openProp ?? _open
    const setOpen = React.useCallback(
        (value: boolean | ((value: boolean) => boolean)) => {
            const openState = typeof value === "function" ? value(open) : value
            if (setOpenProp) {
                setOpenProp(openState)
            } else {
                _setOpen(openState)
            }

        },
        [setOpenProp, open]
    )

    const [sidebarWidth, setSidebarWidth] = React.useState<number>(DEFAULT_SIDEBAR_WIDTH)

    // Helper to toggle the sidebar.
    const toggleSidebar = React.useCallback(() => {
        return isMobile ? setOpenMobile((open) => !open) : setOpen((open) => !open)
    }, [isMobile, setOpen, setOpenMobile])

    // Adds a keyboard shortcut to toggle the sidebar.
    React.useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (
                event.key === SIDEBAR_KEYBOARD_SHORTCUT &&
                (event.metaKey || event.ctrlKey)
            ) {
                event.preventDefault()
                toggleSidebar()
            }
        }

        window.addEventListener("keydown", handleKeyDown)
        return () => window.removeEventListener("keydown", handleKeyDown)
    }, [toggleSidebar])

    // We add a state so that we can do data-state="expanded" or "collapsed".
    // This makes it easier to style the sidebar with Tailwind classes.
    const state = open ? "expanded" : "collapsed"

    const [isDragging, setIsDragging] = React.useState(false)

    const contextValue = React.useMemo<SidebarContextProps>(
        () => ({
            state,
            open,
            setOpen,
            isMobile,
            openMobile,
            setOpenMobile,
            toggleSidebar,
            sidebarWidth,
            setSidebarWidth,
            isDragging,
            setIsDragging
        }),
        [state, open, setOpen, isMobile, openMobile, setOpenMobile, toggleSidebar, sidebarWidth, isDragging, setIsDragging]
    )

    return (
        <SidebarContext.Provider value={contextValue}>
            <TooltipProvider delayDuration={0}>
                <div
                    data-slot="sidebar-wrapper"
                    style={
                        {
                            "--sidebar-width": `${sidebarWidth}px`,
                            "--sidebar-width-icon": SIDEBAR_WIDTH_ICON,
                            style,
                        } as React.CSSProperties
                    }
                    className={cn(
                        "group/sidebar-wrapper has-data-[variant=inset]:bg-sidebar flex min-h-svh w-full",
                        className
                    )}
                    {...props}
                >
                    {children}
                </div>
            </TooltipProvider>
        </SidebarContext.Provider>
    )
}

function SidebarResizableHandle({side = "left"}: { side: "left" | "right" }) {
    const {isMobile, state, setSidebarWidth, isDragging, setIsDragging} = useSidebar()

    const handleRef = React.useRef<HTMLDivElement>(null)

    React.useEffect(() => {
        if (!isDragging) return

        const handlePointerMove = (e: PointerEvent) => {
            if (!handleRef.current) return

            const wrapper = handleRef.current.closest("[data-slot='sidebar-wrapper']") as HTMLElement
            if (!wrapper) return

            const wrapperRect = wrapper.getBoundingClientRect()
            let newWidth: number

            if (side === "left") {
                newWidth = e.clientX - wrapperRect.left
            } else {
                newWidth = wrapperRect.right - e.clientX
            }

            newWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newWidth))
            setSidebarWidth(newWidth)
        }

        const handlePointerUp = () => {
            setIsDragging(false)
            document.body.style.cursor = ""
            document.body.style.userSelect = ""
        }

        document.addEventListener("pointermove", handlePointerMove)
        document.addEventListener("pointerup", handlePointerUp)
        document.body.style.cursor = "ew-resize"
        document.body.style.userSelect = "none"

        return () => {
            document.removeEventListener("pointermove", handlePointerMove)
            document.removeEventListener("pointerup", handlePointerUp)
            document.body.style.cursor = ""
            document.body.style.userSelect = ""
        }
    }, [isDragging, side, setSidebarWidth])

    if (isMobile || state === "collapsed") return null

    return (
        <div
            ref={handleRef}
            data-sidebar="resizable-handle"
            onPointerDown={event => {
                event.preventDefault()
                setIsDragging(true)
            }}
            className={cn(
                "absolute top-0 bottom-0 w-1 z-30",
                !isDragging && "transition-colors",
                "group-hover/sidebar-container:bg-sidebar-border/60",
                "hover:bg-sidebar-border/80 active:bg-sidebar-border",
                side === "left" ? "right-0 cursor-ew-resize" : "left-0 cursor-ew-resize",
                !isDragging && "bg-sidebar-border"
            )}
            style={{
                backgroundImage: isDragging
                    ? undefined
                    : "linear-gradient(to right, transparent 0.5px, hsl(var(--sidebar-border)) 0.5px, hsl(var(--sidebar-border)) calc(100% - 0.5px), transparent calc(100% - 0.5px))",
                backgroundSize: "1px 100%",
            }}
        >
            <div
                className={cn(
                    "absolute inset-y-0 w-px bg-sidebar-border/40 group-hover/sidebar-container:bg-sidebar-border/80",
                    !isDragging && "transition-colors",
                )}
                style={{left: "50%", transform: "translateX(-50%)"}}
            />
        </div>
    )
}

function Sidebar({
                     side = "left",
                     variant = "sidebar",
                     collapsible = "offcanvas",
                     className,
                     children,
                     ...props
                 }: React.ComponentProps<"div"> & {
    side?: "left" | "right"
    variant?: "sidebar" | "floating" | "inset"
    collapsible?: "offcanvas" | "icon" | "none"
}) {
    const {isMobile, state, openMobile, setOpenMobile, isDragging} = useSidebar()

    if (collapsible === "none") {
        return (
            <div
                data-slot="sidebar"
                className={cn(
                    "bg-sidebar text-sidebar-foreground flex h-full w-(--sidebar-width) flex-col",
                    className
                )}
                {...props}
            >
                {children}
            </div>
        )
    }

    if (isMobile) {
        return (
            <Sheet open={openMobile} onOpenChange={setOpenMobile} {...props}>
                <SheetContent
                    data-sidebar="sidebar"
                    data-slot="sidebar"
                    data-mobile="true"
                    className="bg-sidebar text-sidebar-foreground w-(--sidebar-width) p-0 [&>button]:hidden"
                    style={
                        {
                            "--sidebar-width": SIDEBAR_WIDTH_MOBILE,
                        } as React.CSSProperties
                    }
                    side={side}
                >
                    <SheetHeader className="sr-only">
                        <SheetTitle>Sidebar</SheetTitle>
                        <SheetDescription>Displays the mobile sidebar.</SheetDescription>
                    </SheetHeader>
                    <div className="flex h-full w-full flex-col">{children}</div>
                </SheetContent>
            </Sheet>
        )
    }

    return (
        <div
            className="group peer text-sidebar-foreground hidden md:block"
            data-state={state}
            data-collapsible={state === "collapsed" ? collapsible : ""}
            data-variant={variant}
            data-side={side}
            data-slot="sidebar"
        >
            {/* This is what handles the sidebar gap on desktop */}
            <div
                data-slot="sidebar-gap"
                className={cn(
                    "relative w-(--sidebar-width) bg-transparent",
                    "group-data-[collapsible=offcanvas]:w-0",
                    "group-data-[side=right]:rotate-180",
                    !isDragging && "transition-[width] duration-200",
                    variant === "floating" || variant === "inset"
                        ? "group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)+(--spacing(4)))]"
                        : "group-data-[collapsible=icon]:w-(--sidebar-width-icon)"
                )}
            />
            <div
                data-slot="sidebar-container"
                className={cn(
                    "fixed inset-y-0 z-10 hidden h-svh w-(--sidebar-width) md:flex",
                    !isDragging && "transition-[left,right,width] ease-linear",
                    side === "left"
                        ? "left-0 group-data-[collapsible=offcanvas]:left-[calc(var(--sidebar-width)*-1)]"
                        : "right-0 group-data-[collapsible=offcanvas]:right-[calc(var(--sidebar-width)*-1)]",
                    // Adjust the padding for floating and inset variants.
                    variant === "floating" || variant === "inset"
                        ? "p-2 group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)+(--spacing(4))+2px)]"
                        : "group-data-[collapsible=icon]:w-(--sidebar-width-icon) group-data-[side=left]:border-r group-data-[side=right]:border-l",
                    className
                )}
                {...props}
            >
                <div
                    data-sidebar="sidebar"
                    data-slot="sidebar-inner"
                    className="bg-sidebar group-data-[variant=floating]:border-sidebar-border relative flex h-full w-full flex-col overflow-hidden group-data-[variant=floating]:rounded-lg group-data-[variant=floating]:border group-data-[variant=floating]:shadow-sm"
                >
                    {children}
                    <SidebarResizableHandle side={side}/>
                </div>
            </div>
        </div>
    )
}

function SidebarTrigger({
                            className,
                            onClick,
                            ...props
                        }: React.ComponentProps<typeof Button>) {
    const {toggleSidebar} = useSidebar()

    return (
        <Button
            data-sidebar="trigger"
            data-slot="sidebar-trigger"
            variant="ghost"
            size="icon"
            className={cn("size-7", className)}
            onClick={(event) => {
                onClick?.(event)
                toggleSidebar()
            }}
            {...props}
        >
            <PanelLeftIcon/>
            <span className="sr-only">Toggle Sidebar</span>
        </Button>
    )
}

function SidebarInset({className, ...props}: React.ComponentProps<"main">) {
    const {isDragging} = useSidebar()
    return (
        <main
            data-slot="sidebar-inset"
            className={cn(
                "bg-background relative flex w-full flex-1 flex-col",
                !isDragging && "transition-margin duration-200",
                "md:peer-data-[variant=inset]:m-2 md:peer-data-[variant=inset]:ml-0 md:peer-data-[variant=inset]:rounded-xl md:peer-data-[variant=inset]:shadow-sm md:peer-data-[variant=inset]:peer-data-[state=collapsed]:ml-2",
                className
            )}
            {...props}
        />
    )
}

function SidebarContent({className, ...props}: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="sidebar-content"
            data-sidebar="content"
            className={cn(
                "flex min-h-0 flex-1 flex-col gap-2 overflow-auto group-data-[collapsible=icon]:overflow-hidden",
                className
            )}
            {...props}
        />
    )
}

function SidebarGroup({className, ...props}: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="sidebar-group"
            data-sidebar="group"
            className={cn("relative flex w-full min-w-0 flex-col p-2", className)}
            {...props}
        />
    )
}

function SidebarGroupLabel({
                               className,
                               asChild = false,
                               ...props
                           }: React.ComponentProps<"div"> & { asChild?: boolean }) {
    const Comp: React.ElementType = asChild ? Slot : "div"
    const {isDragging} = useSidebar()

    return (
        <Comp
            data-slot="sidebar-group-label"
            data-sidebar="group-label"
            className={cn(
                "text-sidebar-foreground/70 ring-sidebar-ring flex h-8 shrink-0 items-center rounded-md px-2 text-xs font-medium outline-hidden focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0",
                !isDragging && "transition-[margin,opacity] duration-200 ease-linear",
                "group-data-[collapsible=icon]:-mt-8 group-data-[collapsible=icon]:opacity-0",
                className
            )}
            {...props}
        />
    )
}

function SidebarGroupContent({
                                 className,
                                 ...props
                             }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="sidebar-group-content"
            data-sidebar="group-content"
            className={cn("w-full text-sm", className)}
            {...props}
        />
    )
}

function SidebarMenu({className, ...props}: React.ComponentProps<"ul">) {
    return (
        <ul
            data-slot="sidebar-menu"
            data-sidebar="menu"
            className={cn("flex w-full min-w-0 flex-col gap-1", className)}
            {...props}
        />
    )
}

function SidebarMenuItem({className, ...props}: React.ComponentProps<"li">) {
    return (
        <li
            data-slot="sidebar-menu-item"
            data-sidebar="menu-item"
            className={cn("group/menu-item relative", className)}
            {...props}
        />
    )
}

const sidebarMenuButtonVariants = cva(
    "peer/menu-button flex w-full items-center gap-2 overflow-hidden rounded-md p-2 text-left text-sm outline-hidden ring-sidebar-ring hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 group-has-data-[sidebar=menu-action]/menu-item:pr-8 aria-disabled:pointer-events-none aria-disabled:opacity-50 data-[active=true]:bg-sidebar-accent data-[active=true]:font-medium data-[active=true]:text-sidebar-accent-foreground data-[state=open]:hover:bg-sidebar-accent data-[state=open]:hover:text-sidebar-accent-foreground group-data-[collapsible=icon]:size-8! group-data-[collapsible=icon]:p-2! [&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0",
    {
        variants: {
            variant: {
                default: "hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                outline:
                    "bg-background shadow-[0_0_0_1px_hsl(var(--sidebar-border))] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground hover:shadow-[0_0_0_1px_hsl(var(--sidebar-accent))]",
            },
            size: {
                default: "h-8 text-sm",
                sm: "h-7 text-xs",
                lg: "h-12 text-sm group-data-[collapsible=icon]:p-0!",
            },
        },
        defaultVariants: {
            variant: "default",
            size: "default",
        },
    }
)

function SidebarMenuButton({
                               asChild = false,
                               isActive = false,
                               variant = "default",
                               size = "default",
                               tooltip,
                               className,
                               ...props
                           }: React.ComponentProps<"button"> & {
    asChild?: boolean
    isActive?: boolean
    tooltip?: string | React.ComponentProps<typeof TooltipContent>
} & VariantProps<typeof sidebarMenuButtonVariants>) {
    const Comp: React.ElementType = asChild ? Slot : "button"
    const {isMobile, state} = useSidebar()

    const button = (
        <Comp
            data-slot="sidebar-menu-button"
            data-sidebar="menu-button"
            data-size={size}
            data-active={isActive}
            className={cn(sidebarMenuButtonVariants({variant, size}), className)}
            {...props}
        />
    )

    if (!tooltip) {
        return button
    }

    if (typeof tooltip === "string") {
        tooltip = {
            children: tooltip,
        }
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>{button}</TooltipTrigger>
            <TooltipContent
                side="right"
                align="center"
                hidden={state !== "collapsed" || isMobile}
                {...tooltip}
            />
        </Tooltip>
    )
}

function SidebarMenuSub({ className, ...props }: React.ComponentProps<"ul">) {
    return (
        <ul
            data-slot="sidebar-menu-sub"
            data-sidebar="menu-sub"
            className={cn(
                "bg-sidebar flex h-full w-full flex-col",
                "border-l border-sidebar-border pl-6",
                "group-data-[variant=floating]:border-sidebar-border group-data-[variant=floating]:rounded-lg group-data-[variant=floating]:border group-data-[variant=floating]:shadow-sm",
                className
            )}
            {...props}
        />
    )
}

export {
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
    useSidebar,
}
