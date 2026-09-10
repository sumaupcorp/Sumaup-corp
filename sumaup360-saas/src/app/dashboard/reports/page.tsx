"use client";

import { useMemo, useState } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as ChartTooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  useSalesSummary, useTopProducts, useSalesByBranch, useDailySales,
  usePaymentMethods, useLowStock, useCashClosures,
} from "@/features/reports/api";
import { useProducts } from "@/features/products/api";
import { useBranches } from "@/features/branches/api";
import { useCompany } from "@/features/companies/company-context";
import { Card, CardContent } from "@/components/ui/card";
import { Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { Paginator } from "@/components/ui/pagination";
import { ProductPhoto } from "@/components/ui/product-photo";

const fmt = (n: number | string | null | undefined) =>
  "S/ " + Number(n ?? 0).toFixed(2);

const PAY_LABELS: Record<string, string> = {
  CASH: "Efectivo", CARD: "Tarjeta", YAPE: "Yape", PLIN: "Plin", TRANSFER: "Transferencia",
};

/** Inicio del dia de hace N dias, en ISO (hora local del navegador ~ Lima). */
function daysAgoISO(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  d.setHours(0, 0, 0, 0);
  return d.toISOString();
}

/**
 * Centro de reportes del negocio: KPIs de hoy/semana/mes, curva de ventas de 30 dias,
 * metodos de pago, comparativo por sucursal, productos top, cierres de caja y stock bajo.
 */
export default function ReportsPage() {
  const { currentCompany } = useCompany();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const { data: products = [] } = useProducts();
  const [branchId, setBranchId] = useState("");
  const [branchPage, setBranchPage] = useState(0);
  const [stockPage, setStockPage] = useState(0);

  const today = useSalesSummary(branchId || undefined, daysAgoISO(0));
  const week = useSalesSummary(branchId || undefined, daysAgoISO(6));
  const month = useSalesSummary(branchId || undefined, daysAgoISO(29));
  const daily = useDailySales(branchId || undefined, 30);
  const methods = usePaymentMethods(branchId || undefined, 30);
  const top = useTopProducts(branchId || undefined, 8);
  const byBranch = useSalesByBranch();
  const lowStock = useLowStock(branchId || undefined);

  const productMap = useMemo(() => new Map(products.map((p) => [p.id, p])), [products]);

  const branchName = branchId
    ? branches.find((b) => b.id === branchId)?.name ?? "Sucursal"
    : "Todas las sucursales";

  const avgTicket = month.data && month.data.salesCount > 0
    ? Number(month.data.totalAmount) / month.data.salesCount : 0;

  const bestBranchId = useMemo(() => {
    const rows = byBranch.data ?? [];
    if (rows.length < 2) return null;
    return rows.reduce((a, b) => Number(b.totalAmount) > Number(a.totalAmount) ? b : a).branchId;
  }, [byBranch.data]);

  return (
    <div>
      <PageHeader title="Reportes" subtitle="Como va tu negocio: ventas, caja, productos y stock" />

      <div className="mb-4 w-full max-w-xs space-y-1">
        <Label>Ver datos de</Label>
        <Select value={branchId}
          onChange={(e) => { setBranchId(e.target.value); setStockPage(0); }}>
          <option value="">Todas las sucursales (general)</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>

      {/* KPIs */}
      <div className="mb-4 grid grid-cols-2 gap-3 xl:grid-cols-4">
        <KpiCard label="Ventas de hoy" loading={today.isLoading}
          value={fmt(today.data?.totalAmount)}
          detail={`${today.data?.salesCount ?? 0} venta${(today.data?.salesCount ?? 0) === 1 ? "" : "s"}`} accent />
        <KpiCard label="Ultimos 7 dias" loading={week.isLoading}
          value={fmt(week.data?.totalAmount)}
          detail={`${week.data?.salesCount ?? 0} ventas`} />
        <KpiCard label="Ultimos 30 dias" loading={month.isLoading}
          value={fmt(month.data?.totalAmount)}
          detail={`${month.data?.salesCount ?? 0} ventas`} />
        <KpiCard label="Ticket promedio (30d)" loading={month.isLoading}
          value={fmt(avgTicket)}
          detail="por venta"
          tip="Cuanto gasta en promedio cada cliente por compra en los ultimos 30 dias." />
      </div>

      {/* CURVA DE VENTAS 30 DIAS */}
      <Card className="mb-4">
        <CardContent className="p-4">
          <div className="mb-1 flex items-center gap-1.5">
            <h2 className="text-sm font-semibold">Ventas de los ultimos 30 dias</h2>
            <InfoTip align="left" text={`Cuanto vendio ${branchName.toLowerCase()} cada dia, separando el efectivo (pasa por caja) de los pagos digitales (tarjeta, Yape, Plin, transferencia).`} />
          </div>
          <div className="mb-2 flex items-center gap-4 text-xs text-muted-foreground">
            <span className="flex items-center gap-1.5"><span className="size-2.5 rounded-full bg-green-500" /> Efectivo</span>
            <span className="flex items-center gap-1.5"><span className="size-2.5 rounded-full bg-sky-400" /> Digital</span>
          </div>
          <SalesDailyChart loading={daily.isLoading} data={daily.data ?? []} />
        </CardContent>
      </Card>

      <div className="mb-4 grid items-start gap-4 lg:grid-cols-2">
        {/* METODOS DE PAGO */}
        <Card>
          <CardContent className="p-4">
            <div className="mb-3 flex items-center gap-1.5">
              <h2 className="text-sm font-semibold">Como te pagan (30 dias)</h2>
              <InfoTip align="left" text="Distribucion de las ventas por metodo de pago. Te dice cuanto efectivo mueve tu caja y cuanto entra por billeteras y tarjeta." />
            </div>
            {methods.isLoading ? <Spinner /> : (methods.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">Sin ventas en el rango.</p>
            ) : (
              <ul className="space-y-2.5">
                {(() => {
                  const rows = methods.data!;
                  const grand = rows.reduce((s, r) => s + Number(r.total), 0) || 1;
                  return rows.map((r) => {
                    const pct = (Number(r.total) / grand) * 100;
                    return (
                      <li key={r.method}>
                        <div className="mb-1 flex items-center justify-between text-sm">
                          <span className="font-medium">{PAY_LABELS[r.method] ?? r.method}</span>
                          <span className="text-muted-foreground">
                            {fmt(r.total)} · {pct.toFixed(0)}%
                          </span>
                        </div>
                        <MiniBar pct={pct} color={r.method === "CASH" ? "bg-green-500" : "bg-sky-400"} />
                      </li>
                    );
                  });
                })()}
              </ul>
            )}
          </CardContent>
        </Card>

        {/* COMPARATIVO POR SUCURSAL */}
        <Card>
          <CardContent className="p-4">
            <div className="mb-3 flex items-center gap-1.5">
              <h2 className="text-sm font-semibold">Comparativo por sucursal (30 dias)</h2>
              <InfoTip align="left" text="Donde vende mas tu negocio. Siempre muestra todas las sucursales, sin importar el filtro de arriba." />
            </div>
            {byBranch.isLoading ? <Spinner /> : (byBranch.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">Sin ventas en el rango.</p>
            ) : (
              (() => {
                const rows = byBranch.data!;
                const max = Math.max(...rows.map((r) => Number(r.totalAmount)), 1);
                const totalPages = Math.max(1, Math.ceil(rows.length / 8));
                const safePage = Math.min(branchPage, totalPages - 1);
                const pageRows = rows.slice(safePage * 8, (safePage + 1) * 8);
                return (
                  <>
                    <ul className="space-y-2.5">
                      {pageRows.map((b) => (
                        <li key={b.branchId}>
                          <div className="mb-1 flex items-center justify-between text-sm">
                            <span className="flex items-center gap-2 font-medium">
                              {b.branchName ?? "Sucursal"}
                              {b.branchId === bestBranchId && <Badge variant="success">Lider</Badge>}
                            </span>
                            <span className="text-muted-foreground">
                              {b.salesCount} ventas · {fmt(b.totalAmount)}
                            </span>
                          </div>
                          <MiniBar pct={(Number(b.totalAmount) / max) * 100} color="bg-primary" />
                        </li>
                      ))}
                    </ul>
                    <Paginator page={safePage} totalPages={totalPages}
                      totalItems={rows.length} onPage={setBranchPage} />
                  </>
                );
              })()
            )}
          </CardContent>
        </Card>
      </div>

      <div className="mb-4 grid items-start gap-4 lg:grid-cols-2">
        {/* TOP PRODUCTOS */}
        <Card>
          <CardContent className="p-4">
            <div className="mb-3 flex items-center gap-1.5">
              <h2 className="text-sm font-semibold">Productos mas vendidos (30 dias)</h2>
              <InfoTip align="left" text="Lo que mas sale, por unidades e ingresos. Uselo para decidir que reponer primero y que promocionar." />
            </div>
            {top.isLoading ? <Spinner /> : (top.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">Sin ventas en el rango.</p>
            ) : (
              <ul className="space-y-2">
                {(() => {
                  const rows = top.data!;
                  const max = Math.max(...rows.map((r) => Number(r.amount)), 1);
                  return rows.map((p, i) => {
                    const prod = productMap.get(p.productId);
                    return (
                      <li key={p.productId} className="flex items-center gap-2.5">
                        <span className="w-5 shrink-0 text-center text-xs font-bold text-muted-foreground">
                          {i + 1}
                        </span>
                        <ProductPhoto
                          externalUrl={prod?.photoExternalUrl}
                          uploadedUrl={prod?.photoUrl}
                          name={p.name ?? "Producto"}
                          className="size-9 shrink-0"
                        />
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center justify-between gap-2 text-sm">
                            <span className="truncate font-medium">{p.name ?? p.sku ?? "Producto"}</span>
                            <span className="shrink-0 text-xs text-muted-foreground">
                              {Number(p.quantitySold)} und · {fmt(p.amount)}
                            </span>
                          </div>
                          <MiniBar pct={(Number(p.amount) / max) * 100} color="bg-primary" />
                        </div>
                      </li>
                    );
                  });
                })()}
              </ul>
            )}
          </CardContent>
        </Card>

        {/* CIERRES DE CAJA */}
        <CashClosuresCard branchId={branchId || undefined} />
      </div>

      {/* STOCK BAJO */}
      <Card>
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-1.5">
            <h2 className="text-sm font-semibold">Stock bajo (5 o menos)</h2>
            <InfoTip align="left" text="Productos por agotarse en la sucursal elegida: lo que hay que reponer ya. Elige una sucursal en el filtro de arriba." />
          </div>
          {!branchId ? (
            <p className="text-sm text-muted-foreground">
              Elige una sucursal en el filtro de arriba para ver que productos estan por agotarse.
            </p>
          ) : lowStock.isLoading ? <Spinner /> : (lowStock.data ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">Todo con stock suficiente en {branchName}.</p>
          ) : (
            (() => {
              const rows = lowStock.data!;
              const totalPages = Math.max(1, Math.ceil(rows.length / 9));
              const safePage = Math.min(stockPage, totalPages - 1);
              const pageRows = rows.slice(safePage * 9, (safePage + 1) * 9);
              return (
                <>
                  <ul className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                    {pageRows.map((it) => {
                      const prod = productMap.get(it.productId);
                      return (
                        <li key={it.productId}
                          className="flex items-center gap-2.5 rounded-lg border border-amber-200 bg-amber-50/50 p-2">
                          <ProductPhoto
                            externalUrl={prod?.photoExternalUrl}
                            uploadedUrl={prod?.photoUrl}
                            name={it.name ?? "Producto"}
                            className="size-9 shrink-0"
                          />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-xs font-medium">{it.name ?? it.sku}</p>
                            <p className="text-xs text-amber-700">
                              Quedan {Number(it.quantity)}
                            </p>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                  <Paginator page={safePage} totalPages={totalPages}
                    totalItems={rows.length} onPage={setStockPage} />
                </>
              );
            })()
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function KpiCard({ label, value, detail, loading, accent, tip }: {
  label: string;
  value: string;
  detail?: string;
  loading?: boolean;
  accent?: boolean;
  tip?: string;
}) {
  return (
    <Card>
      <CardContent className="p-4">
        <p className="flex items-center gap-1 text-xs text-muted-foreground">
          {label}
          {tip && <InfoTip align="left" text={tip} />}
        </p>
        {loading ? (
          <div className="mt-2"><Spinner /></div>
        ) : (
          <>
            <p className={"mt-1 font-heading text-2xl font-bold " + (accent ? "text-primary" : "")}>
              {value}
            </p>
            {detail && <p className="text-xs text-muted-foreground">{detail}</p>}
          </>
        )}
      </CardContent>
    </Card>
  );
}

/** Barra de proporcion simple para comparativos. */
function MiniBar({ pct, color }: { pct: number; color: string }) {
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
      <div className={"h-full rounded-full " + color}
        style={{ width: `${Math.max(2, Math.min(100, pct))}%` }} />
    </div>
  );
}

/** Curva suave de 30 dias: efectivo vs digital (mismo estilo de inventario y caja). */
function SalesDailyChart({ loading, data }: {
  loading: boolean;
  data: { date: string; cashTotal: number; digitalTotal: number }[];
}) {
  const chartData = useMemo(() => data.map((d) => {
    const day = new Date(d.date + "T00:00:00");
    return {
      day: day.toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit" }),
      Efectivo: Number(d.cashTotal),
      Digital: Number(d.digitalTotal),
    };
  }), [data]);

  const empty = chartData.every((d) => d.Efectivo === 0 && d.Digital === 0);

  if (loading) return <div className="flex h-64 items-center justify-center"><Spinner /></div>;
  if (empty) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-sm text-muted-foreground">Sin ventas en los ultimos 30 dias.</p>
      </div>
    );
  }

  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
          <defs>
            <linearGradient id="repCash" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#22c55e" stopOpacity={0.25} />
              <stop offset="100%" stopColor="#22c55e" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="repDigital" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity={0.25} />
              <stop offset="100%" stopColor="#38bdf8" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 6" vertical={false} stroke="#e2e8f0" />
          <XAxis dataKey="day" tickLine={false} axisLine={false}
            tick={{ fontSize: 11, fill: "#64748b" }} dy={6} interval="preserveStartEnd" minTickGap={24} />
          <YAxis tickLine={false} axisLine={false}
            tick={{ fontSize: 11, fill: "#64748b" }}
            tickFormatter={(v: number) => `S/ ${v}`} width={56} />
          <ChartTooltip
            cursor={{ stroke: "#cbd5e1", strokeDasharray: "3 3" }}
            formatter={(value) => fmt(Number(value ?? 0))}
            contentStyle={{
              borderRadius: 12,
              border: "1px solid #e2e8f0",
              boxShadow: "0 4px 12px rgba(15, 23, 42, 0.08)",
              fontSize: 12,
            }}
          />
          <Area type="monotone" dataKey="Efectivo" stroke="#22c55e" strokeWidth={2.5}
            strokeLinecap="round" fill="url(#repCash)" dot={false}
            activeDot={{ r: 4, strokeWidth: 0 }} />
          <Area type="monotone" dataKey="Digital" stroke="#38bdf8" strokeWidth={2.5}
            strokeLinecap="round" fill="url(#repDigital)" dot={false}
            activeDot={{ r: 4, strokeWidth: 0 }} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Historial de cierres de caja: cuadres, sobrantes y faltantes de cada arqueo. */
function CashClosuresCard({ branchId }: { branchId?: string }) {
  const [page, setPage] = useState(0);
  const closures = useCashClosures(branchId, page);

  const fmtDay = (iso: string | null) =>
    iso ? new Date(iso).toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit" }) : "—";

  return (
    <Card>
      <CardContent className="p-4">
        <div className="mb-3 flex items-center gap-1.5">
          <h2 className="text-sm font-semibold">Cierres de caja</h2>
          <InfoTip align="left" text="Cada arqueo al cerrar caja: lo que debia haber vs lo contado. Los faltantes en rojo merecen revision con el responsable del turno." />
        </div>
        {closures.isLoading ? <Spinner /> : (closures.data?.items ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground">Aun no hay cierres de caja registrados.</p>
        ) : (
          <>
            <ul className="divide-y divide-border">
              {closures.data!.items.map((c) => {
                const diff = Number(c.difference ?? 0);
                return (
                  <li key={c.id} className="flex items-center justify-between gap-2 py-2" title={c.notes ?? undefined}>
                    <div className="min-w-0">
                      <p className="text-sm font-medium">
                        {fmtDay(c.closedAt)} · {c.branchName ?? "Sucursal"}
                      </p>
                      <p className="truncate text-xs text-muted-foreground">
                        Esperado {fmt(c.expectedAmount)} · Contado {fmt(c.closingAmount)}
                        {c.notes ? ` · ${c.notes}` : ""}
                      </p>
                    </div>
                    <span className={"shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold " +
                      (diff === 0 ? "bg-green-50 text-green-700"
                        : diff > 0 ? "bg-amber-50 text-amber-700"
                          : "bg-red-50 text-destructive")}>
                      {diff === 0 ? "Cuadrada" : diff > 0 ? `+${fmt(diff)}` : `−${fmt(Math.abs(diff))}`}
                    </span>
                  </li>
                );
              })}
            </ul>
            {closures.data && (
              <Paginator page={closures.data.page} totalPages={closures.data.totalPages}
                totalItems={closures.data.totalItems} onPage={setPage} />
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
