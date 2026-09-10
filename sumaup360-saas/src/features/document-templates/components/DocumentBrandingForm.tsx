"use client";

import { useRef, useState } from "react";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage } from "@/lib/firebase";
import { Button } from "@/components/ui/button";
import { InfoTip } from "@/components/ui/tooltip";
import type { DocumentTemplateRequest } from "../types/document-template.types";

type Patch = Partial<DocumentTemplateRequest>;

/**
 * Marca del documento: logo, colores y que informacion se muestra. Pensado para
 * duenos de negocio, sin tecnicismos: subir imagen, elegir color, marcar casillas.
 */
export function DocumentBrandingForm({
  value,
  onChange,
  fiscalEnabled = true,
}: {
  value: DocumentTemplateRequest;
  onChange: (patch: Patch) => void;
  /** Con facturacion electronica apagada se oculta lo fiscal (QR SUNAT). */
  fiscalEnabled?: boolean;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [logoError, setLogoError] = useState<string | null>(null);

  const toggles: Array<[keyof DocumentTemplateRequest, string, string]> = [
    ["showLogo", "Mostrar mi logo", "Tu logo aparece en la cabecera del documento."],
    ["showSeller", "Mostrar quien atendio", "Imprime el nombre del cajero o vendedor."],
    ["showPaymentInfo", "Mostrar forma de pago", "Efectivo, Yape, tarjeta... como pago el cliente."],
    ["showCustomerAddress", "Mostrar direccion del cliente", "Util para delivery y facturas."],
    ["showBusinessExtraFields", "Mostrar datos de mi rubro", "Por ejemplo mesa y mozo en restaurantes."],
    ...(fiscalEnabled
      ? [["showQr", "Mostrar codigo QR", "El QR de la facturacion electronica SUNAT."] as [keyof DocumentTemplateRequest, string, string]]
      : []),
  ];

  const uploadLogo = async (file: File) => {
    setLogoError(null);
    if (!file.type.startsWith("image/")) {
      setLogoError("El archivo debe ser una imagen (PNG o JPG).");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setLogoError("La imagen no debe superar los 5MB.");
      return;
    }
    setUploading(true);
    try {
      // Assets de marca del negocio: misma ruta permitida que el logo de reservas.
      const dest = ref(storage,
        `booking/${value.companyId}/documentos-logo-${Date.now()}.${file.type.includes("png") ? "png" : "jpg"}`);
      await uploadBytes(dest, file, { contentType: file.type });
      onChange({ logoUrl: await getDownloadURL(dest), showLogo: true });
    } catch {
      setLogoError("No pudimos subir el logo. Intenta de nuevo.");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* LOGO */}
      <div className="rounded-xl border border-border bg-muted/40 p-3">
        <p className="mb-2 flex items-center gap-1.5 text-sm font-semibold">
          Tu logo
          <InfoTip align="left" text="Sale en la cabecera de tus tickets, boletas y demas documentos. PNG o JPG, maximo 5MB." />
        </p>
        <div className="flex items-center gap-4">
          {value.logoUrl ? (
            <div className="rounded-xl border border-border bg-white p-1.5 shadow-sm">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={value.logoUrl} alt="Logo de la empresa"
                className="size-20 rounded-lg object-contain" />
            </div>
          ) : (
            <div className="flex size-20 items-center justify-center rounded-xl border-2 border-dashed border-border text-xs text-muted-foreground">
              Tu logo
            </div>
          )}
          <div className="space-y-1.5">
            <input ref={fileRef} type="file" accept="image/*" className="hidden"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadLogo(f); e.target.value = ""; }} />
            <div className="flex flex-wrap gap-2">
              <Button type="button" size="sm" variant="outline" disabled={uploading}
                onClick={() => fileRef.current?.click()}>
                {uploading ? "Subiendo..." : value.logoUrl ? "Cambiar logo" : "Subir logo"}
              </Button>
              {value.logoUrl && !uploading && (
                <Button type="button" size="sm" variant="ghost"
                  onClick={() => onChange({ logoUrl: "" })}>
                  Quitar
                </Button>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              La vista previa de la derecha se actualiza sola al subirlo.
            </p>
          </div>
        </div>
        {logoError && <p className="mt-2 text-xs text-destructive">{logoError}</p>}
      </div>

      {/* COLORES */}
      <div className="grid grid-cols-2 gap-4">
        <label className="text-sm">
          <span className="flex items-center gap-1.5 font-medium text-foreground">
            Color principal
            <InfoTip text="El color de los titulos y lineas del documento. Usa el color de tu marca." />
          </span>
          <input
            type="color"
            className="mt-1 h-11 w-full cursor-pointer rounded-lg border border-border"
            value={value.primaryColor ?? "#0B5BFF"}
            onChange={(e) => onChange({ primaryColor: e.target.value })}
          />
        </label>
        <label className="text-sm">
          <span className="flex items-center gap-1.5 font-medium text-foreground">
            Color secundario
            <InfoTip text="Color de apoyo para textos y detalles." />
          </span>
          <input
            type="color"
            className="mt-1 h-11 w-full cursor-pointer rounded-lg border border-border"
            value={value.secondaryColor ?? "#102A4C"}
            onChange={(e) => onChange({ secondaryColor: e.target.value })}
          />
        </label>
      </div>

      {/* QUE SE MUESTRA */}
      <div>
        <p className="mb-2 text-sm font-semibold">Que informacion se muestra</p>
        <div className="grid gap-1.5 sm:grid-cols-2">
          {toggles.map(([key, label, help]) => (
            <label key={String(key)}
              className="flex cursor-pointer items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm transition hover:border-primary">
              <input
                type="checkbox"
                className="accent-primary"
                checked={Boolean(value[key])}
                onChange={(e) => onChange({ [key]: e.target.checked } as Patch)}
              />
              <span className="flex-1">{label}</span>
              <InfoTip align="right" text={help} />
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}
