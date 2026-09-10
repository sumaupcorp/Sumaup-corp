"use client";

import { Spinner } from "@/components/ui/misc";
import { useTemplatePreview } from "../hooks";
import type { DocumentPreviewRequest, PrintFormat } from "../types/document-template.types";

/**
 * Vista previa EN VIVO: pide el HTML de ejemplo al backend con la configuracion actual
 * del editor (aun sin guardar) y lo muestra en un iframe. Mantiene la version anterior
 * mientras llega la nueva, para que se sienta en tiempo real y sin parpadeos.
 */
export function DocumentTemplatePreview({
  request,
  printFormat,
}: {
  request: DocumentPreviewRequest;
  printFormat: PrintFormat;
}) {
  const preview = useTemplatePreview(request);

  const width =
    printFormat === "THERMAL_58MM" ? 240
      : printFormat === "THERMAL_80MM" ? 320
      : 720;

  return (
    <div className="space-y-2 lg:sticky lg:top-6">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-foreground">
          Asi se vera tu documento
        </p>
        <span className="text-xs text-muted-foreground">
          {preview.isFetching ? "Actualizando…" : "Datos de ejemplo"}
        </span>
      </div>

      {preview.isError ? (
        <div className="flex h-96 items-center justify-center rounded-xl border border-border bg-muted/40">
          <p className="px-6 text-center text-sm text-destructive">
            No pudimos generar la vista previa. Revisa tu conexion e intenta de nuevo.
          </p>
        </div>
      ) : !preview.data ? (
        <div className="flex h-96 items-center justify-center rounded-xl border border-border bg-muted/40">
          <Spinner />
        </div>
      ) : (
        <div className={"overflow-auto rounded-xl border border-border bg-muted/40 p-4 transition-opacity " +
          (preview.isFetching ? "opacity-70" : "opacity-100")}>
          <iframe
            title="Vista previa del documento"
            srcDoc={preview.data}
            style={{ width, height: 600, background: "white" }}
            className="mx-auto rounded-lg border border-border shadow-sm"
          />
        </div>
      )}
    </div>
  );
}
