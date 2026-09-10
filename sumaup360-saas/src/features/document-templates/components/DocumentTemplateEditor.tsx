"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { InfoTip } from "@/components/ui/tooltip";
import { useSaveTemplate, useSetDefaultTemplate, useDebounced } from "../hooks";
import {
  DOCUMENT_TYPE_LABELS,
  parseFooterConfig,
  serializeFooterConfig,
  type DocumentTemplate,
  type DocumentTemplateRequest,
  type DocumentType,
  type FooterMessages,
} from "../types/document-template.types";
import { DocumentBrandingForm } from "./DocumentBrandingForm";
import { DocumentFormatSelector } from "./DocumentFormatSelector";
import { DocumentTemplatePreview } from "./DocumentTemplatePreview";

const DOC_TYPES = Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[];

/** Tipos tributarios: solo visibles con el modulo de facturacion electronica activo. */
const FISCAL_TYPES: DocumentType[] = [
  "SALE_RECEIPT", "INVOICE", "CREDIT_NOTE", "DEBIT_NOTE", "DELIVERY_GUIDE",
];

/** Codigo interno unico generado desde el nombre: el usuario nunca lo escribe. */
function slugCode(name: string): string {
  const slug = name.toLowerCase()
    .normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "")
    .slice(0, 24) || "plantilla";
  return `${slug}-${Math.random().toString(36).slice(2, 6)}`;
}

/**
 * Editor de plantilla para duenos de negocio: nombre, tipo, formato, marca y mensajes
 * del pie en campos simples (nada de JSON). La vista previa se actualiza en vivo con
 * cada cambio, antes de guardar. Crea o edita segun venga una plantilla inicial.
 */
export function DocumentTemplateEditor({
  companyId, initial, presetType, hasDefaultTicket, fiscalEnabled, onClose, onSaved,
}: {
  companyId: string;
  initial?: DocumentTemplate | null;
  presetType?: DocumentType;
  hasDefaultTicket: boolean;
  /** Modulo de facturacion electronica activo: habilita boletas, facturas y QR. */
  fiscalEnabled: boolean;
  onClose: () => void;
  onSaved?: () => void;
}) {
  const save = useSaveTemplate(companyId);
  const setDefault = useSetDefaultTemplate(companyId);
  const isEdit = !!initial;

  const [form, setForm] = useState<DocumentTemplateRequest>(() => initial ? {
    companyId,
    documentType: initial.documentType,
    printFormat: initial.printFormat,
    templateName: initial.templateName,
    templateCode: initial.templateCode,
    primaryColor: initial.primaryColor,
    secondaryColor: initial.secondaryColor,
    logoUrl: initial.logoUrl ?? "",
    showLogo: initial.showLogo,
    showQr: initial.showQr,
    showPaymentInfo: initial.showPaymentInfo,
    showSeller: initial.showSeller,
    showCustomerAddress: initial.showCustomerAddress,
    showBusinessExtraFields: initial.showBusinessExtraFields,
  } : {
    companyId,
    documentType: presetType ?? "POS_TICKET",
    printFormat: presetType === "INVOICE" || presetType === "SALE_RECEIPT" ? "A4" : "THERMAL_80MM",
    templateName: presetType === "POS_TICKET" || !presetType ? "Ticket de mi caja" : "Nueva plantilla",
    templateCode: "",
    primaryColor: "#0B5BFF",
    secondaryColor: "#102A4C",
    logoUrl: "",
    showLogo: true,
    showQr: false,
    showPaymentInfo: true,
    showSeller: true,
    showCustomerAddress: false,
    showBusinessExtraFields: false,
  });
  const [footer, setFooter] = useState<FooterMessages>(() => parseFooterConfig(initial?.footerConfig));
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const patch = (p: Partial<DocumentTemplateRequest>) => {
    setMessage(null);
    setForm((f) => ({ ...f, ...p }));
  };
  const patchFooter = (p: Partial<FooterMessages>) => {
    setMessage(null);
    setFooter((f) => ({ ...f, ...p }));
  };

  // Vista previa EN VIVO: la configuracion actual (sin guardar) viaja al backend
  // con un pequeno retraso para no pedir un render por cada tecla.
  const debounced = useDebounced({ form, footer }, 450);
  const previewRequest = useMemo(() => ({
    companyId,
    documentType: debounced.form.documentType,
    printFormat: debounced.form.printFormat,
    businessType: debounced.form.showBusinessExtraFields ? "restaurant" : null,
    config: {
      primaryColor: debounced.form.primaryColor,
      secondaryColor: debounced.form.secondaryColor,
      logoUrl: debounced.form.logoUrl ?? "",
      showLogo: debounced.form.showLogo,
      showQr: debounced.form.showQr,
      showPaymentInfo: debounced.form.showPaymentInfo,
      showSeller: debounced.form.showSeller,
      showCustomerAddress: debounced.form.showCustomerAddress,
      showBusinessExtraFields: debounced.form.showBusinessExtraFields,
      footerText: debounced.footer.footerText,
      legalMessage: debounced.footer.legalMessage,
      commercialMessage: debounced.footer.commercialMessage,
      thankYouMessage: debounced.footer.thankYouMessage,
    },
  }), [companyId, debounced]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    try {
      const payload = {
        ...form,
        templateCode: isEdit ? form.templateCode : slugCode(form.templateName),
        footerConfig: serializeFooterConfig(footer),
      };
      const saved = await save.mutateAsync(isEdit ? { id: initial!.id, ...payload } : payload);
      // Primera plantilla de ticket del negocio: se conecta sola a la caja.
      if (!isEdit && form.documentType === "POS_TICKET" && !hasDefaultTicket) {
        await setDefault.mutateAsync(saved.id);
        setMessage("Plantilla guardada y conectada a tu caja: tus proximas ventas imprimen con este diseno.");
      } else {
        setMessage(isEdit ? "Cambios guardados. Se aplican desde la proxima impresion." : "Plantilla guardada.");
      }
      onSaved?.();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const saving = save.isPending || setDefault.isPending;

  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
      <form onSubmit={submit} className="space-y-5">
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold">
            {isEdit ? `Editando: ${initial!.templateName}` : "Nueva plantilla"}
          </h3>
          <Button type="button" size="sm" variant="ghost" onClick={onClose}>Cerrar editor</Button>
        </div>

        <div className="space-y-1">
          <Label>Nombre de la plantilla</Label>
          <Input value={form.templateName} required
            onChange={(e) => patch({ templateName: e.target.value })}
            placeholder="Ej. Ticket de mi caja" />
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-1">
            <Label className="flex items-center gap-1.5">
              Tipo de documento
              <InfoTip text="Que documento disena esta plantilla. El Ticket POS es el que imprime tu caja al cobrar." />
            </Label>
            <Select
              value={form.documentType}
              disabled={isEdit}
              onChange={(e) => patch({ documentType: e.target.value as DocumentType })}
            >
              {DOC_TYPES
                .filter((t) => fiscalEnabled || isEdit || !FISCAL_TYPES.includes(t))
                .map((t) => (
                  <option key={t} value={t}>{DOCUMENT_TYPE_LABELS[t]}</option>
                ))}
            </Select>
            {form.documentType === "POS_TICKET" && (
              <p className="text-xs text-primary">
                Este es el diseno que imprime tu caja al momento de vender.
              </p>
            )}
            {!fiscalEnabled && !isEdit && (
              <p className="text-xs text-muted-foreground">
                Boletas y facturas apareceran cuando actives Facturacion electronica en Modulos.
              </p>
            )}
          </div>
          <DocumentFormatSelector
            value={form.printFormat}
            onChange={(printFormat) => patch({ printFormat })}
          />
        </div>

        <DocumentBrandingForm value={form} onChange={patch} fiscalEnabled={fiscalEnabled} />

        {/* MENSAJES DEL PIE, en campos simples */}
        <div className="rounded-xl border border-border bg-muted/40 p-3">
          <p className="mb-2 flex items-center gap-1.5 text-sm font-semibold">
            Mensajes al pie del documento
            <InfoTip align="left" text="Textos que salen al final de cada documento. Todos son opcionales: deja vacio lo que no quieras mostrar." />
          </p>
          <div className="space-y-2.5">
            <div className="space-y-1">
              <Label className="text-xs">Mensaje de agradecimiento</Label>
              <Input value={footer.thankYouMessage}
                onChange={(e) => patchFooter({ thankYouMessage: e.target.value })}
                placeholder="Ej. Gracias por su compra, vuelva pronto" />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Promocion o aviso</Label>
              <Input value={footer.commercialMessage}
                onChange={(e) => patchFooter({ commercialMessage: e.target.value })}
                placeholder="Ej. Delivery gratis por compras mayores a S/ 50" />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Texto legal</Label>
              <Input value={footer.legalMessage}
                onChange={(e) => patchFooter({ legalMessage: e.target.value })}
                placeholder="Ej. No se aceptan devoluciones pasadas 48 horas" />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Linea final</Label>
              <Input value={footer.footerText}
                onChange={(e) => patchFooter({ footerText: e.target.value })}
                placeholder="Ej. Sigue nuestras redes: @minegocio" />
            </div>
          </div>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}
        {message && (
          <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{message}</p>
        )}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
          <Button type="submit" className="min-h-11" disabled={saving || !form.templateName.trim()}>
            {saving ? "Guardando..." : isEdit ? "Guardar cambios" : "Guardar plantilla"}
          </Button>
        </div>
      </form>

      <DocumentTemplatePreview request={previewRequest} printFormat={form.printFormat} />
    </div>
  );
}
