"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useCompanyModules, useSetModule } from "@/features/companies/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";

/** Que hace cada modulo, en lenguaje del negocio (los codigos no le dicen nada a nadie). */
const MODULE_DESCRIPTIONS: Record<string, string> = {
  "e-invoicing": "Emite boletas y facturas electronicas ante SUNAT. Si tu negocio solo lleva control interno con tickets y notas de venta, dejalo desactivado.",
  tables: "Mesas del salon para restaurantes: estado, ocupacion y cobro por mesa.",
  kitchen: "Comandas y cola de cocina: los pedidos llegan a la pantalla de cocina.",
  menu: "Carta del restaurante con sus platos y precios.",
  "batch-expiry": "Lotes y fechas de vencimiento, para boticas y productos perecibles.",
  prescription: "Recetas medicas asociadas a las ventas (boticas y farmacias).",
  variants: "Variantes y unidades de venta: tallas, colores, medidas.",
  appointments: "Agenda de citas con reserva online por QR.",
  "service-orders": "Ordenes de servicio, para lavanderias y talleres.",
  patients: "Fichas de mascotas o pacientes asociadas a tus clientes.",
  "custom-orders": "Encargos y pedidos personalizados con flujo de estados.",
};

const SOURCE_LABELS: Record<string, string> = {
  DEFAULT: "de tu rubro",
  OVERRIDE: "personalizado",
  AVAILABLE: "disponible",
};

export default function ModulesPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const canManage = hasPerm("company:manage");
  const { data: modules = [], isLoading } = useCompanyModules(currentCompany?.id);
  const setModule = useSetModule(currentCompany?.id ?? "");
  const [confirming, setConfirming] = useState<{ code: string; name: string } | null>(null);

  if (!currentCompany) {
    return (
      <div>
        <PageHeader title="Modulos" />
        <p className="text-sm text-muted-foreground">Selecciona una empresa primero.</p>
      </div>
    );
  }

  const disable = async () => {
    if (!confirming) return;
    try {
      await setModule.mutateAsync({ moduleCode: confirming.code, enabled: false });
    } finally {
      setConfirming(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Modulos"
        subtitle={`Habilita o desactiva funciones de ${currentCompany.legalName}`}
      />
      <Card>
        <CardContent className="p-4">
          {isLoading ? (
            <Spinner />
          ) : (
            <ul className="divide-y divide-border">
              {modules.map((m) => (
                <li key={m.moduleCode} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="flex flex-wrap items-center gap-2 font-medium text-foreground">
                      {m.name}
                      {m.core && <Badge variant="muted">base</Badge>}
                      <Badge variant={m.source === "OVERRIDE" ? "warning" : "muted"}>
                        {SOURCE_LABELS[m.source] ?? m.source}
                      </Badge>
                    </p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      {MODULE_DESCRIPTIONS[m.moduleCode] ?? m.moduleCode}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <Badge variant={m.enabled ? "success" : "muted"}>
                      {m.enabled ? "Activo" : "Desactivado"}
                    </Badge>
                    {canManage && (
                      <Button
                        variant={m.enabled ? "outline" : "default"}
                        size="sm"
                        className="min-h-10"
                        disabled={setModule.isPending}
                        onClick={() =>
                          m.enabled
                            ? setConfirming({ code: m.moduleCode, name: m.name })
                            : setModule.mutate({ moduleCode: m.moduleCode, enabled: true })
                        }
                      >
                        {m.enabled ? "Desactivar" : "Activar"}
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      {confirming && (
        <ConfirmDialog
          title={`Desactivar ${confirming.name}`}
          message={
            confirming.code === "e-invoicing"
              ? "Se ocultaran las boletas, facturas y el QR SUNAT de tus documentos. Tus ventas siguen funcionando con tickets internos. Puedes reactivarlo cuando quieras."
              : `Las secciones de ${confirming.name} se ocultaran del menu para todo tu equipo. Los datos no se borran: al reactivar el modulo, todo vuelve.`
          }
          confirmLabel="Si, desactivar"
          destructive
          loading={setModule.isPending}
          onConfirm={disable}
          onCancel={() => setConfirming(null)}
        />
      )}
    </div>
  );
}
