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
import type { Product } from "@/features/products/api";
import {
  useOpenCashAction,
  useSales,
  useWeeklySalesFlow,
  PAYMENT_LABELS,
  EXPENSE_CATEGORIES,
  INCOME_CATEGORIES,
  type CashSession,
  type CashSummary,
  type DailySales,
  type Sale,
} from "@/features/sales/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { Paginator } from "@/components/ui/pagination";
import { fmt, hourOf, dateOf, SummaryRow } from "./shared";
import { MovementModal, CloseCashModal } from "./PosModals";

/**
 * Pestana CAJA: el flujo completo del dia — estado, resumen del efectivo, ventas por
 * metodo, movimientos y ventas de la sesion con su detalle. Todo con lenguaje claro.
 */
export function CashView({ cash, cashLoading, summary, branchId, canOperate, productMap, onTicket }: {
  cash: CashSession | null;
  cashLoading: boolean;
  summary: CashSummary | null;
  branchId: string;
  canOperate: boolean;
  productMap: Map<string, Product>;
  onTicket: (sale: Sale) => void;
}) {
  const openCash = useOpenCashAction();
  const [opening, setOpening] = useState("0");
  const [movementOpen, setMovementOpen] = useState(false);
  const [closing, setClosing] = useState(false);
  const [expandedSale, setExpandedSale] = useState<string | null>(null);
  const [salesPage, setSalesPage] = useState(0);

  // Ventas de ESTA caja, paginadas desde el backend (filtro indexado por sesion).
  const sessionSales = useSales({
    cashSessionId: cash?.id,
    page: salesPage,
    size: 8,
    enabled: !!cash,
  });

  const categoryLabels = useMemo(() => new Map(
    [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES].map((c) => [c.code, c.label])
  ), []);

  const flow = useWeeklySalesFlow(branchId);

  if (cashLoading) {
    return <div className="flex justify-center py-12"><Spinner /></div>;
  }

  // ------------------------------------------------------------------ cerrada
  if (!cash) {
    return (
      <div className="space-y-4">
        <SalesWeeklyChart loading={flow.isLoading} data={flow.data ?? []} />
        <Card className="mx-auto max-w-md">
          <CardContent className="space-y-4 p-6 text-center">
            <Badge variant="muted">Caja cerrada</Badge>
            <p className="text-sm text-muted-foreground">
              Para vender primero abre la caja del dia con su fondo inicial.
            </p>
            {canOperate ? (
              <div className="space-y-3 text-left">
                <div className="space-y-1">
                  <Label className="flex items-center gap-1.5">
                    Fondo inicial (sencillo)
                    <InfoTip text="Efectivo con el que empiezas el dia para dar vuelto. Ejemplo: S/ 50 en monedas y billetes chicos. Se cuenta al cierre como parte del arqueo." />
                  </Label>
                  <Input type="number" step="0.01" min="0" className="h-11 text-base" value={opening}
                    onChange={(e) => setOpening(e.target.value)} />
                </div>
                <Button className="h-12 w-full text-base font-bold" disabled={openCash.isPending}
                  onClick={() => openCash.mutate({ branchId, openingAmount: Number(opening) || 0 })}>
                  {openCash.isPending ? "Abriendo..." : "Abrir caja"}
                </Button>
              </div>
            ) : (
              <p className="text-xs text-muted-foreground">
                Solo alguien con permiso de caja puede abrirla.
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  // ------------------------------------------------------------------ abierta
  return (
    <div className="space-y-4">
      {/* ESTADO + ACCIONES */}
      <Card>
        <CardContent className="flex flex-wrap items-center justify-between gap-3 p-4">
          <div className="flex items-center gap-3">
            <Badge variant="success">Caja abierta</Badge>
            <div className="text-sm">
              <p className="font-medium">
                {dateOf(cash.openedAt)} · desde las {hourOf(cash.openedAt)}
              </p>
              <p className="text-xs text-muted-foreground">
                {summary?.openedByName ? `Abierta por ${summary.openedByName} · ` : ""}
                Fondo inicial {fmt(cash.openingAmount)}
              </p>
            </div>
          </div>
          {canOperate && (
            <div className="flex gap-2">
              <Button variant="outline" className="h-11" onClick={() => setMovementOpen(true)}>
                Registrar movimiento
              </Button>
              <Button className="h-11" onClick={() => setClosing(true)}>
                Cerrar caja
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <SalesWeeklyChart loading={flow.isLoading} data={flow.data ?? []} />

      {!summary ? (
        <div className="flex justify-center py-8"><Spinner /></div>
      ) : (
        <div className="grid items-start gap-4 lg:grid-cols-3">
          {/* EFECTIVO */}
          <Card>
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-1.5">
                <h3 className="text-sm font-semibold">Efectivo en caja</h3>
                <InfoTip align="left" text="Lo que deberia haber fisicamente en la caja ahora: fondo + ventas en efectivo + ingresos − salidas. Al cerrar se compara contra lo contado." />
              </div>
              <p className="mb-3 font-heading text-3xl font-bold text-primary">{fmt(summary.expectedCash)}</p>
              <div className="space-y-1.5">
                <SummaryRow label="Fondo inicial" value={fmt(summary.openingAmount)} />
                <SummaryRow label="Ventas en efectivo" value={"+ " + fmt(summary.cashSales)} />
                <SummaryRow label="Otros ingresos" value={"+ " + fmt(summary.incomesTotal)}
                  tip="Sencillo agregado, cobranzas y otros ingresos que no son ventas." />
                <SummaryRow label="Salidas de caja" value={"− " + fmt(summary.expensesTotal)}
                  tip="Gastos menores, pagos, retiros del dueno y depositos al banco hechos con dinero de la caja." />
              </div>
            </CardContent>
          </Card>

          {/* VENTAS DEL DIA */}
          <Card>
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-1.5">
                <h3 className="text-sm font-semibold">Ventas de esta caja</h3>
                <InfoTip align="left" text="Total vendido desde la apertura, separado por como pago el cliente. Solo el efectivo entra a la caja fisica; el resto se cuadra con vouchers." />
              </div>
              <p className="mb-1 font-heading text-3xl font-bold">{fmt(summary.salesTotal)}</p>
              <p className="mb-3 text-xs text-muted-foreground">
                {summary.salesCount} venta(s) desde la apertura
              </p>
              <div className="space-y-1.5">
                {Object.entries(summary.salesByMethod).length === 0 ? (
                  <p className="text-xs text-muted-foreground">Aun no hay ventas.</p>
                ) : (
                  Object.entries(summary.salesByMethod).map(([m, v]) => (
                    <SummaryRow key={m} label={PAYMENT_LABELS.get(m) ?? m} value={fmt(v)} />
                  ))
                )}
              </div>
            </CardContent>
          </Card>

          {/* MOVIMIENTOS */}
          <Card>
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-1.5">
                <h3 className="text-sm font-semibold">Movimientos de efectivo</h3>
                <InfoTip align="left" text="Dinero que entro o salio de la caja fuera de las ventas, con su motivo y hora. Todo queda registrado con el usuario que lo hizo." />
              </div>
              {summary.movements.length === 0 ? (
                <p className="text-xs text-muted-foreground">
                  Sin movimientos. Registra aqui los gastos, retiros o ingresos de sencillo del dia.
                </p>
              ) : (
                <ul className="max-h-56 space-y-2 overflow-y-auto">
                  {summary.movements.map((m) => (
                    <li key={m.id} className="flex items-start justify-between gap-2 text-xs">
                      <div className="min-w-0">
                        <p className="font-medium text-foreground">
                          {categoryLabels.get(m.category) ?? m.category}
                        </p>
                        <p className="truncate text-muted-foreground">
                          {hourOf(m.createdAt)} · {m.concept}
                        </p>
                      </div>
                      <span className={"shrink-0 font-semibold " + (m.type === "EXPENSE" ? "text-destructive" : "text-green-700")}>
                        {m.type === "EXPENSE" ? "−" : "+"} {fmt(m.amount)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* DETALLE DE VENTAS */}
      <Card>
        <CardContent className="p-4">
          <h3 className="mb-3 text-sm font-semibold">Detalle de ventas</h3>
          {sessionSales.isLoading ? (
            <div className="flex justify-center py-6"><Spinner /></div>
          ) : (sessionSales.data?.items ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">Aun no hay ventas en esta caja.</p>
          ) : (
            <ul className="divide-y divide-border">
              {sessionSales.data!.items.map((s) => (
                <li key={s.id} className="py-2">
                  <div className="flex items-center justify-between gap-2">
                    <button
                      type="button"
                      className="min-w-0 flex-1 text-left"
                      onClick={() => setExpandedSale(expandedSale === s.id ? null : s.id)}
                    >
                      <p className="text-sm font-medium">{fmt(s.total)}</p>
                      <p className="text-xs text-muted-foreground">
                        {hourOf(s.createdAt)} · {PAYMENT_LABELS.get(s.paymentMethod) ?? s.paymentMethod} · {s.items.length} item(s)
                      </p>
                    </button>
                    <Button size="sm" variant="outline" onClick={() => onTicket(s)}>
                      Ticket
                    </Button>
                  </div>
                  {expandedSale === s.id && (
                    <ul className="mt-1.5 space-y-0.5 rounded-lg bg-muted/50 p-2.5">
                      {s.items.map((i, idx) => (
                        <li key={idx} className="flex justify-between text-xs">
                          <span className="text-muted-foreground">
                            {Number(i.quantity)} x {productMap.get(i.productId)?.name ?? "Producto"}
                          </span>
                          <span className="font-medium">{fmt(i.lineTotal)}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              ))}
            </ul>
          )}
          {sessionSales.data && (
            <Paginator
              page={sessionSales.data.page}
              totalPages={sessionSales.data.totalPages}
              totalItems={sessionSales.data.totalItems}
              onPage={setSalesPage}
            />
          )}
        </CardContent>
      </Card>

      {movementOpen && (
        <MovementModal sessionId={cash.id} onClose={() => setMovementOpen(false)} />
      )}
      {closing && summary && (
        <CloseCashModal session={cash} summary={summary} branchId={branchId}
          onClose={() => setClosing(false)} />
      )}
    </div>
  );
}

/** Grafico de area con curvas suaves: ventas por dia de la semana, efectivo vs digital. */
function SalesWeeklyChart({ loading, data }: { loading: boolean; data: DailySales[] }) {
  const chartData = useMemo(() => data.map((d) => {
    const day = new Date(d.date + "T00:00:00");
    const label = day.toLocaleDateString("es-PE", { weekday: "short", day: "2-digit" });
    return {
      day: label.charAt(0).toUpperCase() + label.slice(1),
      Efectivo: Number(d.cashTotal),
      Digital: Number(d.digitalTotal),
    };
  }), [data]);

  const empty = chartData.every((d) => d.Efectivo === 0 && d.Digital === 0);

  return (
    <Card>
      <CardContent className="p-4">
        <div className="mb-1 flex items-center gap-1.5">
          <h3 className="text-sm font-semibold">Ventas de la semana</h3>
          <InfoTip align="left" text="Cuanto vendio esta sucursal cada dia de los ultimos 7 dias, separando el efectivo (que pasa por la caja) de los pagos digitales: tarjeta, Yape, Plin y transferencias." />
        </div>
        <div className="mb-2 flex items-center gap-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <span className="size-2.5 rounded-full bg-green-500" /> Efectivo
          </span>
          <span className="flex items-center gap-1.5">
            <span className="size-2.5 rounded-full bg-sky-400" /> Digital
          </span>
        </div>

        {loading ? (
          <div className="flex h-56 items-center justify-center"><Spinner /></div>
        ) : empty ? (
          <div className="flex h-56 items-center justify-center">
            <p className="text-sm text-muted-foreground">Sin ventas en los ultimos 7 dias.</p>
          </div>
        ) : (
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                <defs>
                  <linearGradient id="salesCash" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#22c55e" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#22c55e" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="salesDigital" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#38bdf8" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#38bdf8" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 6" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="day" tickLine={false} axisLine={false}
                  tick={{ fontSize: 11, fill: "#64748b" }} dy={6} />
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
                  strokeLinecap="round" fill="url(#salesCash)" dot={false}
                  activeDot={{ r: 4, strokeWidth: 0 }} />
                <Area type="monotone" dataKey="Digital" stroke="#38bdf8" strokeWidth={2.5}
                  strokeLinecap="round" fill="url(#salesDigital)" dot={false}
                  activeDot={{ r: 4, strokeWidth: 0 }} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
