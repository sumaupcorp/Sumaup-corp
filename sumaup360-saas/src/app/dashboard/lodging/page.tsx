"use client";

import { useEffect, useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useHasPermission } from "@/features/auth/session";
import { RackView } from "@/features/lodging/components/RackView";
import { StaysView } from "@/features/lodging/components/StaysView";
import { RoomsView } from "@/features/lodging/components/RoomsView";
import { PageHeader } from "@/components/ui/misc";
import { Card, CardContent } from "@/components/ui/card";

type Tab = "rack" | "reservas" | "habitaciones";

const TABS: { key: Tab; label: string }[] = [
  { key: "rack", label: "Rack" },
  { key: "reservas", label: "Reservas" },
  { key: "habitaciones", label: "Habitaciones" },
];

/**
 * Modulo Hospedaje: rack de habitaciones, reservas/estadias (check-in, cargos,
 * check-out por el POS) y catalogo de tipos y habitaciones. La visibilidad real
 * la decide el backend (modulo lodging + permisos lodging:read/manage).
 */
export default function LodgingPage() {
  const { hasModule } = useCompany();
  const hasPerm = useHasPermission();
  const canManage = hasPerm("lodging:manage");
  const [tab, setTab] = useState<Tab>("rack");

  // La pestana vive en la URL (?tab=reservas) para sobrevivir al refresh.
  useEffect(() => {
    const t = new URLSearchParams(window.location.search).get("tab") as Tab | null;
    if (t && TABS.some((x) => x.key === t)) setTab(t);
  }, []);
  const changeTab = (t: Tab) => {
    setTab(t);
    const url = new URL(window.location.href);
    if (t === "rack") url.searchParams.delete("tab");
    else url.searchParams.set("tab", t);
    window.history.replaceState(null, "", url);
  };

  if (!hasModule("lodging")) {
    return (
      <div>
        <PageHeader title="Hospedaje" />
        <Card>
          <CardContent className="p-8 text-center text-sm text-muted-foreground">
            El modulo de hospedaje no esta activo para esta empresa. Activalo en Modulos.
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Hospedaje"
        subtitle="Habitaciones, reservas, check-in y check-out con cobro por caja"
      />

      <div className="mb-6 inline-flex rounded-lg border border-border bg-muted p-1">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => changeTab(t.key)}
            className={
              "rounded-md px-4 py-1.5 text-sm font-medium transition " +
              (tab === t.key ? "bg-white text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground")
            }
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "rack" && <RackView canManage={canManage} />}
      {tab === "reservas" && <StaysView canManage={canManage} />}
      {tab === "habitaciones" && <RoomsView canManage={canManage} />}
    </div>
  );
}
