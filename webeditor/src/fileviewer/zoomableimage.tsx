/* eslint-disable react/react-in-jsx-scope */
import {useRef, useState} from "preact/compat";

export function ZoomableImage({src, alt}: {src: string; alt?: string}) {
    const [scale, setScale] = useState(1);
    const [offset, setOffset] = useState({x: 0, y: 0});
    const containerRef = useRef<HTMLDivElement>(null);
    const [isDragging, setIsDragging] = useState(false);
    const lastPos = useRef({x: 0, y: 0});

    const handleWheel = (e: WheelEvent) => {
        e.preventDefault();
        if (!containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const mouseX = e.clientX - rect.left - rect.width / 2;
        const mouseY = e.clientY - rect.top - rect.height / 2;

        const delta = -e.deltaY / 400;
        const newScale = Math.min(Math.max(scale + delta, 0.1), 10);

        if (newScale !== scale) {
            const ratio = newScale / scale;
            setOffset({
                x: mouseX - (mouseX - offset.x) * ratio,
                y: mouseY - (mouseY - offset.y) * ratio,
            });
            setScale(newScale);
        }
    };

    const handleMouseDown = (e: MouseEvent) => {
        if (e.button === 1 || e.button === 0) {
            setIsDragging(true);
            lastPos.current = {x: e.clientX, y: e.clientY};
            e.preventDefault();
        }
    };

    const handleMouseMove = (e: MouseEvent) => {
        if (!isDragging) return;
        const dx = e.clientX - lastPos.current.x;
        const dy = e.clientY - lastPos.current.y;
        setOffset((prev) => ({x: prev.x + dx, y: prev.y + dy}));
        lastPos.current = {x: e.clientX, y: e.clientY};
    };

    const handleMouseUp = () => {
        setIsDragging(false);
    };

    return (
        <div
            ref={containerRef}
            className="h-full w-full overflow-auto flex justify-center items-center p-[15px] pb-10"
            onWheel={handleWheel}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            style={{cursor: isDragging ? "grabbing" : "grab"}}
        >
            <img
                src={src}
                alt={alt}
                style={{
                    height: "256px",
                    width: "auto",
                    transform: `scale(${scale}) translate(${offset.x / scale}px, ${offset.y / scale}px)`,
                    imageRendering: "pixelated",
                    backgroundImage: `
                            linear-gradient(45deg, #80808020 25%, transparent 25%),
                            linear-gradient(-45deg, #80808020 25%, transparent 25%),
                            linear-gradient(45deg, transparent 75%, #80808020 75%),
                            linear-gradient(-45deg, transparent 75%, #80808020 75%)
                        `,
                    backgroundSize: `32px 32px`,
                    backgroundPosition: `0 0, 0 16px, 16px -16px, -16px 0px`,
                }}
            />
        </div>
    );
}
