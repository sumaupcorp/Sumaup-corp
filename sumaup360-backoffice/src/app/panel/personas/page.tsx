"use client";

import { useEffect, useState } from "react";
import {
  usePersons, useActivatePremium, useCancelPremium, useAiConfig, useUpdateAiConfig,
  useUpdateClientType, useResetDiagnosis, useResetOrientation,
  type PersonRow, type OrientationStatus,
} from "@/features/persons/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { SearchBox } from "@/components/ui/table-tools";

export default function PersonasPage() {
  const [query, setQuery] = useState("");
  const { data: persons = [], isLoading } = usePersons(query);
  const activate = useActivatePremium();
  const cancel = useCancelPremium();
  const resetDiag = useResetDiagnosis();
  const resetOrientation = useResetOrientation();
  const canManage = useHasPermission()("person:profile:manage");

  function premiumPlanFor(p: PersonRow) {
    if (p.clientType === "DELIVERY_PEYA") return "peya-premium";
    if (p.clientType === "SERVICIOS_PROFESIONALES") return "serv-premium";
    return "taxi-premium";
  }

  return (
    <div>
      <PageHeader title="Personas" subtitle="Usuarios de la app movil: planes premium y consumo de IA" />

      <AiConfigCard />

      <div className="mb-4">
        <SearchBox value={query} onChange={setQuery} placeholder="Buscar por nombre, correo, DNI o RUC…" />
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5"><Spinner /></div>
          ) : persons.length === 0 ? (
            <div className="p-5 text-sm text-muted-foreground">No hay personas que coincidan.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-xs text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">Usuario</th>
                    <th className="px-4 py-3">DNI / RUC</th>
                    <th className="px-4 py-3">Tipo</th>
                    <th className="px-4 py-3">Perfil</th>
                    <th className="px-4 py-3">Orientacion</th>
                    <th className="px-4 py-3">Plan</th>
                    <th className="px-4 py-3 text-right">Accion</th>
                  </tr>
                </thead>
                <tbody>
                  {persons.map((p) => (
                    <tr key={p.userId} className="border-b border-border/60">
                      <td className="px-4 py-3">
                        <div className="font-medium">{p.displayName || "—"}</div>
                        <div className="text-xs text-muted-foreground">{p.email}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div>{p.dni || "—"}</div>
                        <div className="text-xs text-muted-foreground">{p.ruc || "sin RUC"}</div>
                      </td>
                      <td className="px-4 py-3"><ClientTypeCell person={p} /></td>
                      <td className="px-4 py-3">
                        {p.profileCompleted
                          ? <Badge variant="success">Completo</Badge>
                          : <Badge variant="warning">Incompleto</Badge>}
                      </td>
                      <td className="px-4 py-3"><OrientationBadge status={p.orientationStatus} /></td>
                      <td className="px-4 py-3">
                        {p.premium
                          ? <Badge variant="success">Premium</Badge>
                          : <Badge variant="muted">{p.planCode || "Free"}</Badge>}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-col items-end gap-1.5">
                          {p.premium ? (
                            <Button size="sm" variant="outline"
                              disabled={cancel.isPending}
                              onClick={() => cancel.mutate(p.userId)}>
                              Cancelar
                            </Button>
                          ) : (
                            <Button size="sm"
                              disabled={activate.isPending}
                              onClick={() => activate.mutate({ userId: p.userId, planCode: premiumPlanFor(p), months: 1 })}>
                              Activar Premium
                            </Button>
                          )}
                          {canManage && !p.diagnosisAvailable && (
                            <button
                              type="button"
                              className="text-xs text-primary hover:underline disabled:opacity-50"
                              disabled={resetDiag.isPending}
                              onClick={() => resetDiag.mutate(p.userId)}
                            >
                              Rehabilitar diagnostico
                            </button>
                          )}
                          {canManage && p.orientationStatus === "COMPLETED" && (
                            <button
                              type="button"
                              className="text-xs text-primary hover:underline disabled:opacity-50"
                              disabled={resetOrientation.isPending}
                              onClick={() => resetOrientation.mutate(p.userId)}
                            >
                              Reactivar orientacion
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

/** Estado de la orientacion tributaria del usuario en la app. */
function OrientationBadge({ status }: { status: OrientationStatus | null }) {
  if (status === "PENDING") return <Badge variant="warning">Pendiente</Badge>;
  if (status === "COMPLETED") return <Badge variant="success">Completada</Badge>;
  if (status === "NOT_REQUIRED") return <Badge variant="muted">No requerida</Badge>;
  return <span className="text-muted-foreground">—</span>;
}

function labelClientType(ct: string | null): string {
  if (ct === "TAXISTA") return "Taxista";
  if (ct === "DELIVERY_PEYA") return "Delivery/Peya";
  if (ct === "SERVICIOS_PROFESIONALES") return "Servicios prof.";
  return "—";
}

/**
 * Muestra el rubro del usuario y, si el staff tiene permiso, permite reasignarlo.
 * El usuario de la app no puede cambiar su tipo por si mismo: solo soporte/admin desde aqui.
 */
function ClientTypeCell({ person }: { person: PersonRow }) {
  const canManage = useHasPermission()("person:profile:manage");
  const update = useUpdateClientType();
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(person.clientType ?? "TAXISTA");

  if (!canManage) return <>{labelClientType(person.clientType)}</>;

  if (!editing) {
    return (
      <div className="flex items-center gap-2">
        <span>{labelClientType(person.clientType)}</span>
        <button
          type="button"
          className="text-xs text-primary hover:underline"
          onClick={() => { setValue(person.clientType ?? "TAXISTA"); setEditing(true); }}
        >
          Cambiar
        </button>
      </div>
    );
  }

  function save() {
    if (value === person.clientType) { setEditing(false); return; }
    const ok = window.confirm(
      `Cambiar el rubro de ${person.displayName || person.email || "este usuario"} a ${labelClientType(value)}? ` +
      "Esto reasigna su tipo de cuenta en la app."
    );
    if (!ok) return;
    update.mutate(
      { userId: person.userId, clientType: value },
      { onSuccess: () => setEditing(false) }
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Select value={value} onChange={(e) => setValue(e.target.value)} className="h-8 w-40">
        <option value="TAXISTA">Taxista</option>
        <option value="DELIVERY_PEYA">Delivery/Peya</option>
        <option value="SERVICIOS_PROFESIONALES">Servicios profesionales</option>
      </Select>
      <Button size="sm" onClick={save} disabled={update.isPending}>
        {update.isPending ? "…" : "Guardar"}
      </Button>
      <button
        type="button"
        className="text-xs text-muted-foreground hover:underline"
        onClick={() => setEditing(false)}
      >
        Cancelar
      </button>
    </div>
  );
}

function AiConfigCard() {
  const { data: cfg, isLoading } = useAiConfig();
  const update = useUpdateAiConfig();
  const [freeIa, setFreeIa] = useState("");
  const [premiumConsultas, setPremiumConsultas] = useState("");
  const [freeIniciales, setFreeIniciales] = useState("");
  const [periodo, setPeriodo] = useState("MENSUAL");

  useEffect(() => {
    if (cfg) {
      setFreeIa(String(cfg.freeIa));
      setPremiumConsultas(String(cfg.premiumConsultas));
      setFreeIniciales(String(cfg.freeIniciales));
      setPeriodo(cfg.periodo);
    }
  }, [cfg]);

  function save() {
    update.mutate({
      freeIa: Number(freeIa),
      premiumConsultas: Number(premiumConsultas),
      freeIniciales: Number(freeIniciales),
      periodo,
    });
  }

  return (
    <Card className="mb-6">
      <CardContent className="p-5">
        <h2 className="mb-1 font-heading text-base font-semibold">Limites de IA</h2>
        <p className="mb-4 text-sm text-muted-foreground">
          Consultas del asesor IA por usuario. Aplica a todas las personas segun su plan.
        </p>
        {isLoading ? (
          <Spinner />
        ) : (
          <div className="flex flex-wrap items-end gap-4">
            <div>
              <Label>Consultas free</Label>
              <Input type="number" value={freeIa} onChange={(e) => setFreeIa(e.target.value)} className="w-28" />
            </div>
            <div>
              <Label>Consultas premium</Label>
              <Input type="number" value={premiumConsultas} onChange={(e) => setPremiumConsultas(e.target.value)} className="w-28" />
            </div>
            <div>
              <Label>Consultas SUNAT iniciales</Label>
              <Input type="number" value={freeIniciales} onChange={(e) => setFreeIniciales(e.target.value)} className="w-28" />
            </div>
            <div>
              <Label>Periodo</Label>
              <Select value={periodo} onChange={(e) => setPeriodo(e.target.value)} className="h-10 w-32">
                <option value="MENSUAL">Mensual</option>
                <option value="DIARIO">Diario</option>
              </Select>
            </div>
            <Button onClick={save} disabled={update.isPending}>
              {update.isPending ? "Guardando…" : "Guardar"}
            </Button>
            {update.isSuccess && <span className="text-sm text-green-600">Guardado</span>}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
