"use client";

import { useCompany } from "@/features/companies/company-context";
import { DocumentSeriesForm } from "@/features/document-templates/components/DocumentSeriesForm";
import { PageHeader } from "@/components/ui/misc";

export default function DocumentSeriesPage() {
  const { currentCompany, hasModule } = useCompany();

  if (!currentCompany) {
    return (
      <div>
        <PageHeader title="Series de documentos" />
        <p className="text-sm text-muted-foreground">Selecciona o crea una empresa primero.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <PageHeader title="Series de documentos" subtitle={currentCompany.legalName} />
      {!hasModule("e-invoicing") && (
        <p className="rounded-xl border border-border bg-muted/40 px-4 py-2.5 text-xs text-muted-foreground">
          Con la facturacion electronica desactivada solo se numeran documentos internos
          (tickets, notas de venta, cotizaciones). Las series de boletas y facturas
          apareceran cuando actives el modulo en la seccion Modulos.
        </p>
      )}
      <DocumentSeriesForm companyId={currentCompany.id} fiscalEnabled={hasModule("e-invoicing")} />
    </div>
  );
}
