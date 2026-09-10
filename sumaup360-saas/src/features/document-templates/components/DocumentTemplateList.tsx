"use client";

import { Button } from "@/components/ui/button";
import { Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { useDocumentTemplates, useSetDefaultTemplate } from "../hooks";
import {
  DOCUMENT_TYPE_LABELS,
  PRINT_FORMAT_LABELS,
  type DocumentTemplate,
} from "../types/document-template.types";

/**
 * Plantillas de la empresa. Deja claro CUAL imprime la caja (la predeterminada de
 * tipo Ticket POS) y permite cambiarla con un click. Se actualiza sola tras cada cambio.
 */
export function DocumentTemplateList({
  companyId,
  onEdit,
}: {
  companyId: string;
  onEdit: (t: DocumentTemplate) => void;
}) {
  const templates = useDocumentTemplates(companyId);
  const setDefault = useSetDefaultTemplate(companyId);

  if (templates.isLoading) {
    return <div className="flex justify-center py-8"><Spinner /></div>;
  }
  if (templates.isError) {
    return (
      <p className="py-6 text-center text-sm text-destructive">
        No pudimos cargar tus plantillas. Recarga la pagina o intenta mas tarde.
      </p>
    );
  }

  const items = templates.data ?? [];
  if (items.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Aun no tienes plantillas. Crea la primera con el boton de arriba.
      </p>
    );
  }

  return (
    <ul className="divide-y divide-border">
      {items.map((t) => {
        const printsAtPos = t.documentType === "POS_TICKET" && t.isDefault;
        return (
          <li key={t.id} className="flex flex-wrap items-center gap-3 py-2.5">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="truncate text-sm font-medium">{t.templateName}</p>
                {printsAtPos && (
                  <span className="flex items-center gap-1">
                    <Badge variant="success">Se imprime en tu caja</Badge>
                    <InfoTip text="Esta es la plantilla que usa el POS al cobrar: el ticket, su logo y sus mensajes salen con este diseno." />
                  </span>
                )}
                {!printsAtPos && t.isDefault && <Badge variant="default">Predeterminada</Badge>}
              </div>
              <p className="text-xs text-muted-foreground">
                {DOCUMENT_TYPE_LABELS[t.documentType]} · {PRINT_FORMAT_LABELS[t.printFormat]}
              </p>
            </div>
            <div className="flex shrink-0 gap-1.5">
              {!t.isDefault && (
                <Button size="sm" variant="outline"
                  disabled={setDefault.isPending}
                  onClick={() => setDefault.mutate(t.id)}>
                  {setDefault.isPending
                    ? "Aplicando..."
                    : t.documentType === "POS_TICKET" ? "Usar en mi caja" : "Hacer predeterminada"}
                </Button>
              )}
              <Button size="sm" variant="outline" onClick={() => onEdit(t)}>Editar</Button>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
