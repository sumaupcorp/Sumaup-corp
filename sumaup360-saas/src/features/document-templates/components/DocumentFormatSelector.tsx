"use client";

import { Label } from "@/components/ui/input";
import { InfoTip } from "@/components/ui/tooltip";
import type { PrintFormat } from "../types/document-template.types";
import { PRINT_FORMAT_LABELS } from "../types/document-template.types";

const FORMATS: PrintFormat[] = ["A4", "THERMAL_80MM", "THERMAL_58MM"];

export function DocumentFormatSelector({
  value,
  onChange,
}: {
  value: PrintFormat;
  onChange: (format: PrintFormat) => void;
}) {
  return (
    <div className="space-y-1">
      <Label className="flex items-center gap-1.5">
        Tamano de papel
        <InfoTip text="Termico 80mm es el rollo tipico de las impresoras de caja. A4 es hoja completa, para boletas y facturas." />
      </Label>
      <div className="flex flex-wrap gap-1.5">
        {FORMATS.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => onChange(f)}
            className={`min-h-10 select-none rounded-lg border px-3 text-xs font-medium transition touch-manipulation active:scale-95 ${
              value === f
                ? "border-primary bg-accent text-primary"
                : "border-border text-muted-foreground hover:border-primary"
            }`}
          >
            {PRINT_FORMAT_LABELS[f]}
          </button>
        ))}
      </div>
    </div>
  );
}
