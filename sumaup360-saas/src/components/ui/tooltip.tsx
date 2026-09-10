"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Icono de ayuda contextual: un circulo con "?" que muestra una descripcion
 * al pasar el cursor o al recibir foco (teclado). Sin dependencias externas.
 */
export function InfoTip({ text, className, align = "center" }: {
  text: string;
  className?: string;
  /** Alineacion del globo respecto al icono, para evitar cortes en los bordes. */
  align?: "center" | "left" | "right";
}) {
  const bubblePos = {
    center: "left-1/2 -translate-x-1/2",
    left: "left-0",
    right: "right-0",
  }[align];

  return (
    <span className={cn("group relative inline-flex", className)}>
      <span
        tabIndex={0}
        role="button"
        aria-label={text}
        className={cn(
          "inline-flex size-4 cursor-help items-center justify-center rounded-full",
          "bg-accent text-[10px] font-bold leading-none text-accent-foreground",
          "transition-colors hover:bg-primary hover:text-white",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        )}
      >
        ?
      </span>
      <span
        role="tooltip"
        className={cn(
          "pointer-events-none absolute bottom-full z-50 mb-1.5 w-56 rounded-lg",
          "bg-foreground px-3 py-2 text-xs font-normal leading-relaxed text-white shadow-lg",
          "opacity-0 transition-opacity duration-150",
          "group-hover:opacity-100 group-focus-within:opacity-100",
          bubblePos
        )}
      >
        {text}
      </span>
    </span>
  );
}
