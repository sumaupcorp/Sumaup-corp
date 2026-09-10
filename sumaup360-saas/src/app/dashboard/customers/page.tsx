"use client";

import { useMemo, useState } from "react";
import {
  useCustomers, useCreateCustomer, useCustomerSummary, type Customer,
} from "@/features/customers/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Paginator } from "@/components/ui/pagination";
import { InfoTip } from "@/components/ui/tooltip";

const PAGE_SIZE = 10;

const fmtMoney = (n: number | string | null | undefined) =>
  "S/ " + Number(n ?? 0).toFixed(2);

const fmtDate = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit", year: "numeric" }) : "—";

const fmtDateTime = (iso: string) =>
  new Date(iso).toLocaleString("es-PE", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });

const APPT_LABELS: Record<string, string> = {
  REQUESTED: "Solicitada", SCHEDULED: "Programada", CONFIRMED: "Confirmada",
  COMPLETED: "Atendida", CANCELED: "Cancelada", NO_SHOW: "No asistio",
};

const PAY_LABELS: Record<string, string> = {
  CASH: "Efectivo", CARD: "Tarjeta", YAPE: "Yape", PLIN: "Plin", TRANSFER: "Transferencia",
};

export default function CustomersPage() {
  const hasPerm = useHasPermission();
  const canManage = hasPerm("customer:manage");
  const { data: customers = [], isLoading } = useCustomers();
  const create = useCreateCustomer();

  const [name, setName] = useState("");
  const [docNumber, setDocNumber] = useState("");
  const [phone, setPhone] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Customer | null>(null);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return customers;
    return customers.filter((c) =>
      c.name.toLowerCase().includes(q) || (c.docNumber ?? "").toLowerCase().includes(q));
  }, [customers, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageItems = filtered.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await create.mutateAsync({ name, docNumber: docNumber || undefined, phone: phone || undefined });
      setName(""); setDocNumber(""); setPhone("");
      setMsg("Cliente creado.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Clientes" subtitle="Clientes de tu negocio" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="p-5">
            <div className="mb-3 max-w-72">
              <Input value={search}
                onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                placeholder="Buscar por nombre o documento..." />
            </div>
            {isLoading ? (
              <Spinner />
            ) : filtered.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                {search ? "Ningun cliente coincide con la busqueda." : "Aun no hay clientes."}
              </p>
            ) : (
              <>
                <ul className="divide-y divide-border">
                  {pageItems.map((c) => (
                    <li key={c.id}>
                      <button
                        type="button"
                        onClick={() => setSelected(c)}
                        className="flex w-full select-none items-center justify-between gap-3 rounded-lg px-2 py-2.5 text-left transition touch-manipulation hover:bg-muted/60 active:scale-[0.995]"
                      >
                        <div className="min-w-0">
                          <p className="font-medium text-foreground">{c.name}</p>
                          <p className="text-xs text-muted-foreground">
                            {c.docNumber ?? "sin doc"}
                            {c.phone ? " · " + c.phone : ""}
                          </p>
                        </div>
                        <span className="shrink-0 text-xs font-medium text-primary">Ver ficha</span>
                      </button>
                    </li>
                  ))}
                </ul>
                <Paginator page={safePage} totalPages={totalPages}
                  totalItems={filtered.length} onPage={setPage} />
              </>
            )}
          </CardContent>
        </Card>

        {canManage && (
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Nuevo cliente</h2>
              <form onSubmit={submit} className="space-y-3">
                <div className="space-y-1">
                  <Label>Nombre / Razon social</Label>
                  <Input value={name} onChange={(e) => setName(e.target.value)} required />
                </div>
                <div className="space-y-1">
                  <Label>Documento</Label>
                  <Input value={docNumber} onChange={(e) => setDocNumber(e.target.value)} />
                </div>
                <div className="space-y-1">
                  <Label>Telefono</Label>
                  <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
                </div>
                {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
                <Button type="submit" disabled={create.isPending}>
                  {create.isPending ? "Guardando..." : "Crear cliente"}
                </Button>
              </form>
            </CardContent>
          </Card>
        )}
      </div>

      {selected && (
        <CustomerFichaModal customer={selected} onClose={() => setSelected(null)} />
      )}
    </div>
  );
}

/** Tarjeta de estadistica de la ficha del cliente. */
function StatTile({ label, value, tip, accent }: {
  label: string;
  value: string;
  tip?: string;
  accent?: boolean;
}) {
  return (
    <div className={"rounded-xl p-3 " + (accent ? "bg-accent" : "bg-muted/60")}>
      <p className={"font-heading text-xl font-bold " + (accent ? "text-primary" : "text-foreground")}>
        {value}
      </p>
      <p className="mt-0.5 flex items-center gap-1 text-xs text-muted-foreground">
        {label}
        {tip && <InfoTip align="left" text={tip} />}
      </p>
    </div>
  );
}

/**
 * Ficha del cliente: estadisticas de atencion (citas, visitas, compras, gasto),
 * sus mascotas y el historial reciente de citas y compras.
 */
function CustomerFichaModal({ customer, onClose }: { customer: Customer; onClose: () => void }) {
  const summary = useCustomerSummary(customer.id);
  const s = summary.data;

  const apptBadge = (status: string) =>
    status === "COMPLETED" || status === "CONFIRMED" ? "success"
      : status === "REQUESTED" ? "warning" : "muted";

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-10"
      onClick={onClose}>
      <div className="w-full max-w-2xl rounded-xl bg-white p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}>
        {/* Cabecera */}
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="truncate font-heading text-lg font-bold">{customer.name}</h3>
            <p className="text-sm text-muted-foreground">
              {[customer.docNumber ? `${customer.docType ?? "Doc"}: ${customer.docNumber}` : null,
                customer.phone, customer.email].filter(Boolean).join(" · ") || "Sin datos de contacto"}
            </p>
          </div>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        {summary.isLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : summary.isError ? (
          <p className="py-8 text-center text-sm text-destructive">
            No pudimos cargar la ficha. Intenta de nuevo.
          </p>
        ) : s && (
          <div className="space-y-4">
            {/* ESTADISTICAS */}
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              <StatTile accent label="Atenciones" value={String(s.attended)}
                tip="Citas marcadas como atendidas: las veces que realmente vino y fue atendido." />
              <StatTile label="Citas totales" value={String(s.totalAppointments)}
                tip="Todas sus citas registradas, en cualquier estado." />
              <StatTile label="Proximas citas" value={String(s.upcoming)} />
              <StatTile accent label="Compras" value={String(s.totalPurchases)}
                tip="Ventas registradas a su nombre en el POS." />
              <StatTile label="Gasto total" value={fmtMoney(s.totalSpent)} />
              <StatTile label="Ultima visita" value={fmtDate(s.lastVisitAt)}
                tip="Su ultima cita atendida o su ultima compra, lo que sea mas reciente." />
            </div>

            {(s.canceled > 0 || s.noShow > 0) && (
              <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-700">
                Historial de inasistencias: {s.canceled} cancelada{s.canceled === 1 ? "" : "s"} y {s.noShow} sin asistir.
              </p>
            )}

            {/* MASCOTAS */}
            {s.patients.length > 0 && (
              <div>
                <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Mascotas / pacientes
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {s.patients.map((p) => <Badge key={p} variant="muted">{p}</Badge>)}
                </div>
              </div>
            )}

            {/* HISTORIAL */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Ultimas citas
                </p>
                {s.recentAppointments.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Sin citas registradas.</p>
                ) : (
                  <ul className="max-h-56 space-y-1.5 overflow-y-auto pr-1">
                    {s.recentAppointments.map((a) => (
                      <li key={a.id} className="rounded-lg border border-border p-2">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-xs font-medium">{fmtDateTime(a.scheduledAt)}</span>
                          <Badge variant={apptBadge(a.status)}>
                            {APPT_LABELS[a.status] ?? a.status}
                          </Badge>
                        </div>
                        <p className="mt-0.5 truncate text-xs text-muted-foreground">
                          {[a.patientName, a.reason].filter(Boolean).join(" · ") || "Sin detalle"}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div>
                <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Ultimas compras
                </p>
                {s.recentSales.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Sin compras a su nombre. Las ventas del POS sin cliente no se asocian.
                  </p>
                ) : (
                  <ul className="max-h-56 space-y-1.5 overflow-y-auto pr-1">
                    {s.recentSales.map((v) => (
                      <li key={v.id} className="flex items-center justify-between gap-2 rounded-lg border border-border p-2">
                        <div className="min-w-0">
                          <p className="text-xs font-medium">{fmtDateTime(v.createdAt)}</p>
                          <p className="truncate text-xs text-muted-foreground">
                            {PAY_LABELS[v.paymentMethod] ?? v.paymentMethod} · {v.itemCount} item{v.itemCount === 1 ? "" : "s"}
                          </p>
                        </div>
                        <span className="shrink-0 text-sm font-semibold">{fmtMoney(v.total)}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
