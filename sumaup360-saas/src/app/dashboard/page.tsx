"use client";

import { useSession, useHasPermission } from "@/features/auth/session";
import { useCompany } from "@/features/companies/company-context";
import { useSalesSummary } from "@/features/reports/api";
import { TicketSearch } from "@/features/appointments/ticket-search";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Badge } from "@/components/ui/misc";

function Kpi({ label, value }: { label: string; value: string }) {
  return (
    <Card>
      <CardContent className="p-5">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="mt-1 font-heading text-2xl font-bold text-foreground">{value}</p>
      </CardContent>
    </Card>
  );
}

export default function DashboardHome() {
  const { data: session } = useSession();
  const { currentCompany, enabledModules, hasModule } = useCompany();
  const hasPerm = useHasPermission();
  const canReport = hasPerm("report:read");
  const { data: summary } = useSalesSummary();
  const showTicketSearch = hasModule("appointments") && hasPerm("appointment:read");

  return (
    <div>
      <PageHeader
        title={`Hola${session?.email ? ", " + session.email.split("@")[0] : ""}`}
        subtitle={currentCompany ? currentCompany.legalName : "Selecciona o crea una empresa"}
      />

      <div className="grid gap-4 sm:grid-cols-3">
        {canReport && (
          <>
            <Kpi label="Ventas (total)" value={`S/ ${summary?.totalAmount ?? "0.00"}`} />
            <Kpi label="N.o de ventas" value={String(summary?.salesCount ?? 0)} />
          </>
        )}
        <Kpi label="Modulos habilitados" value={String(enabledModules.length)} />
      </div>

      {showTicketSearch && (
        <div className="mt-6">
          <TicketSearch withLink />
        </div>
      )}

      <Card className="mt-6">
        <CardContent className="p-5">
          <p className="mb-2 text-sm font-medium text-foreground">Tu acceso</p>
          <div className="flex flex-wrap gap-2">
            <Badge variant="muted">Tipo: {session?.userType}</Badge>
            {(session?.roles ?? []).map((r) => (
              <Badge key={r}>{r}</Badge>
            ))}
          </div>
          <p className="mt-3 text-xs text-muted-foreground">
            {session?.permissions.length ?? 0} permisos · {enabledModules.length} modulos activos
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
