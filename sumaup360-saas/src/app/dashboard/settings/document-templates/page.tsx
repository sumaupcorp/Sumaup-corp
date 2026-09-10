"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useDocumentTemplates } from "@/features/document-templates/hooks";
import { DocumentTemplateEditor } from "@/features/document-templates/components/DocumentTemplateEditor";
import { DocumentTemplateList } from "@/features/document-templates/components/DocumentTemplateList";
import type { DocumentTemplate } from "@/features/document-templates/types/document-template.types";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PageHeader, Badge } from "@/components/ui/misc";

type EditorState = { mode: "closed" } | { mode: "new-pos" } | { mode: "new" } | { mode: "edit"; template: DocumentTemplate };

export default function DocumentTemplatesPage() {
  const { currentCompany, hasModule } = useCompany();
  const [editor, setEditor] = useState<EditorState>({ mode: "closed" });
  const templates = useDocumentTemplates(currentCompany?.id);
  const fiscalEnabled = hasModule("e-invoicing");

  if (!currentCompany) {
    return (
      <div>
        <PageHeader title="Diseno de documentos" />
        <p className="text-sm text-muted-foreground">Selecciona o crea una empresa primero.</p>
      </div>
    );
  }

  const items = templates.data ?? [];
  const posTemplate = items.find((t) => t.documentType === "POS_TICKET" && t.isDefault);
  const hasDefaultTicket = !!posTemplate;

  return (
    <div className="space-y-4">
      <PageHeader
        title="Diseno de documentos"
        subtitle="Como se ven tus tickets, boletas y demas documentos"
        action={
          <Button className="min-h-11" onClick={() => setEditor({ mode: "new" })}>
            Nueva plantilla
          </Button>
        }
      />

      {/* CONEXION CON LA CAJA: siempre visible y en cristiano */}
      {!templates.isLoading && (
        posTemplate ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-green-200 bg-green-50/60 px-4 py-3">
            <div className="flex items-center gap-2">
              <Badge variant="success">Conectado</Badge>
              <p className="text-sm">
                Tu caja imprime con la plantilla <span className="font-semibold">{posTemplate.templateName}</span>.
              </p>
            </div>
            <Button size="sm" variant="outline"
              onClick={() => setEditor({ mode: "edit", template: posTemplate })}>
              Editar ese diseno
            </Button>
          </div>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3">
            <p className="text-sm text-amber-800">
              Tu caja imprime con el diseno basico. Crea tu plantilla de ticket con tu logo
              y mensajes, y se conecta sola.
            </p>
            <Button size="sm" className="min-h-10" onClick={() => setEditor({ mode: "new-pos" })}>
              Crear el ticket de mi caja
            </Button>
          </div>
        )
      )}

      {/* FACTURACION ELECTRONICA apagada: aviso discreto, no bloqueo */}
      {!fiscalEnabled && (
        <p className="rounded-xl border border-border bg-muted/40 px-4 py-2.5 text-xs text-muted-foreground">
          Tu negocio trabaja solo con documentos internos (tickets y notas de venta).
          Si algun dia emites boletas o facturas electronicas, activa
          <span className="font-medium text-foreground"> Facturacion electronica </span>
          en la seccion Modulos y aqui apareceran esos disenos.
        </p>
      )}

      {/* LISTA */}
      <Card>
        <CardContent className="p-4">
          <h2 className="mb-2 text-sm font-semibold">Tus plantillas</h2>
          <DocumentTemplateList
            companyId={currentCompany.id}
            onEdit={(t) => setEditor({ mode: "edit", template: t })}
          />
        </CardContent>
      </Card>

      {/* EDITOR */}
      {editor.mode !== "closed" && (
        <Card>
          <CardContent className="p-5">
            <DocumentTemplateEditor
              key={editor.mode === "edit" ? editor.template.id : editor.mode}
              companyId={currentCompany.id}
              initial={editor.mode === "edit" ? editor.template : null}
              presetType={editor.mode === "new-pos" ? "POS_TICKET" : undefined}
              hasDefaultTicket={hasDefaultTicket}
              fiscalEnabled={fiscalEnabled}
              onClose={() => setEditor({ mode: "closed" })}
            />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
