"use client";

import { InfoTip } from "@/components/ui/tooltip";

export const fmt = (n: number | string | null | undefined) =>
  "S/ " + Number(n ?? 0).toFixed(2);

export const hourOf = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" }) : "";

export const dateOf = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit", year: "numeric" }) : "";

/** Fila etiqueta/valor de los resumenes de caja. */
export function SummaryRow({ label, value, tip, strong, negative }: {
  label: string;
  value: string;
  tip?: string;
  strong?: boolean;
  negative?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className={"flex items-center gap-1.5 " + (strong ? "text-sm font-semibold" : "text-xs text-muted-foreground")}>
        {label}
        {tip && <InfoTip align="left" text={tip} />}
      </span>
      <span className={
        (strong ? "font-heading text-base font-bold " : "text-xs font-medium ") +
        (negative ? "text-destructive" : "text-foreground")
      }>
        {value}
      </span>
    </div>
  );
}
