"use client";

import { useMemo, useState } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useCompany } from "@/features/companies/company-context";
import { useProducts, type Product } from "@/features/products/api";
import { useBranches } from "@/features/branches/api";
import {
  useBranchStock,
  useStockMovements,
  useWeeklyFlow,
  useAdjustStock,
  STOCK_IN_REASONS,
  STOCK_OUT_REASONS,
} from "@/features/inventory/api";
import { useHasPermission, useSession } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { Paginator } from "@/components/ui/pagination";
import { ProductPhoto } from "@/components/ui/product-photo";

/** Umbral de aviso de stock bajo (solo visual). */
const LOW_STOCK = 5;
/** Filas de producto por pagina en la lista de stock. */
const LIST_PAGE_SIZE = 10;

const timeOf = (iso: string) =>
  new Date(iso).toLocaleString("es-PE", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });

type StockFilter = "all" | "low" | "out";
type Tab = "stock" | "movements";

/**
 * Inventario con dos pestanas: STOCK (lista paginada de productos con foto y ajuste)
 * y MOVIMIENTOS (grafico semanal de entradas/salidas + kardex paginado).
 */
export default function InventoryPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const canAdjust = hasPerm("inventory:adjust");
  const { data: session } = useSession();
  const { data: products = [] } = useProducts();
  const { data: branchesAll = [] } = useBranches(currentCompany?.id);

  // Respeta la sede asignada del trabajador, igual que el POS.
  const assignedBranchId = session?.branchId ?? null;
  const branches = assignedBranchId
    ? branchesAll.filter((b) => b.id === assignedBranchId)
    : branchesAll;
  const [branchSel, setBranchSel] = useState("");
  const autoBranchId = !assignedBranchId && branchesAll.length === 1 ? branchesAll[0].id : "";
  const branchId = assignedBranchId ?? (branchSel || autoBranchId);

  const [tab, setTab] = useState<Tab>("stock");
  const [adjusting, setAdjusting] = useState<Product | null>(null);

  const stock = useBranchStock(branchId || undefined);
  const stockMap = useMemo(
    () => new Map((stock.data ?? []).map((s) => [s.productId, Number(s.quantity)])),
    [stock.data]
  );
  const productMap = useMemo(() => new Map(products.map((p) => [p.id, p])), [products]);

  return (
    <div>
      <PageHeader title="Inventario" subtitle="Stock por sucursal: revisa, ingresa y retira mercaderia" />

      {/* Pestanas + sucursal */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-xl bg-muted p-1">
          <TabButton label="Stock" active={tab === "stock"} onClick={() => setTab("stock")} />
          <TabButton label="Movimientos" active={tab === "movements"} onClick={() => setTab("movements")} />
        </div>
        <div className="flex items-center gap-2">
          <Label className="text-xs text-muted-foreground">Sucursal</Label>
          <Select className="h-10 w-48" value={branchId} disabled={!!assignedBranchId}
            onChange={(e) => setBranchSel(e.target.value)}>
            {!assignedBranchId && branchesAll.length !== 1 && <option value="">Selecciona…</option>}
            {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </Select>
        </div>
      </div>

      {!branchId ? (
        <p className="py-8 text-center text-sm text-muted-foreground">
          Selecciona una sucursal para ver su inventario.
        </p>
      ) : tab === "stock" ? (
        <StockTab
          products={products}
          stockMap={stockMap}
          loading={stock.isLoading}
          canAdjust={canAdjust}
          onAdjust={setAdjusting}
        />
      ) : (
        <MovementsTab branchId={branchId} productMap={productMap} />
      )}

      {adjusting && branchId && (
        <AdjustModal
          product={adjusting}
          branchId={branchId}
          currentQty={stockMap.get(adjusting.id) ?? 0}
          onClose={() => setAdjusting(null)}
        />
      )}
    </div>
  );
}

function TabButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "min-h-11 select-none rounded-lg px-6 text-sm font-semibold transition touch-manipulation active:scale-95 " +
        (active ? "bg-white text-primary shadow-sm" : "text-muted-foreground")
      }
    >
      {label}
    </button>
  );
}

/** Pestana STOCK: buscador, filtros y lista paginada de productos con foto. */
function StockTab({ products, stockMap, loading, canAdjust, onAdjust }: {
  products: Product[];
  stockMap: Map<string, number>;
  loading: boolean;
  canAdjust: boolean;
  onAdjust: (p: Product) => void;
}) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<StockFilter>("all");
  const [page, setPage] = useState(0);

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    let list = products
      .filter((p) => p.active)
      .map((p) => ({ product: p, qty: stockMap.get(p.id) ?? 0 }));
    if (q) {
      list = list.filter(({ product: p }) =>
        p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q));
    }
    if (filter === "low") list = list.filter((r) => r.qty > 0 && r.qty <= LOW_STOCK);
    if (filter === "out") list = list.filter((r) => r.qty === 0);
    return list.sort((a, b) => a.product.name.localeCompare(b.product.name));
  }, [products, stockMap, search, filter]);

  const totalPages = Math.max(1, Math.ceil(rows.length / LIST_PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageRows = rows.slice(safePage * LIST_PAGE_SIZE, (safePage + 1) * LIST_PAGE_SIZE);

  const lowCount = useMemo(
    () => products.filter((p) => p.active && (stockMap.get(p.id) ?? 0) > 0 && (stockMap.get(p.id) ?? 0) <= LOW_STOCK).length,
    [products, stockMap]
  );
  const outCount = useMemo(
    () => products.filter((p) => p.active && (stockMap.get(p.id) ?? 0) === 0).length,
    [products, stockMap]
  );

  return (
    <Card>
      <CardContent className="p-4">
        <div className="mb-3 flex flex-wrap items-center gap-3">
          <div className="w-full max-w-72">
            <Input value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              placeholder="Nombre o codigo del producto..." />
          </div>
          <div className="flex gap-1.5">
            <FilterPill label="Todos" active={filter === "all"}
              onClick={() => { setFilter("all"); setPage(0); }} />
            <FilterPill label={`Stock bajo (${lowCount})`} active={filter === "low"} warn
              onClick={() => { setFilter("low"); setPage(0); }} />
            <FilterPill label={`Sin stock (${outCount})`} active={filter === "out"} danger
              onClick={() => { setFilter("out"); setPage(0); }} />
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : rows.length === 0 ? (
          <p className="py-10 text-center text-sm text-muted-foreground">
            {filter !== "all" || search
              ? "Ningun producto coincide con el filtro."
              : "Aun no tienes productos. Crealos en la seccion Productos."}
          </p>
        ) : (
          <>
            <ul className="divide-y divide-border">
              {pageRows.map(({ product: p, qty }) => (
                <li key={p.id} className="flex items-center gap-3 py-2.5">
                  <ProductPhoto
                    externalUrl={p.photoExternalUrl}
                    uploadedUrl={p.photoUrl}
                    name={p.name}
                    className="size-12 shrink-0"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{p.name}</p>
                    <p className="truncate text-xs text-muted-foreground">
                      {p.sku}{p.category ? ` · ${p.category}` : ""} · {p.unit}
                    </p>
                  </div>
                  <StockBadge qty={qty} />
                  {canAdjust && (
                    <Button size="sm" variant="outline" className="shrink-0"
                      onClick={() => onAdjust(p)}>
                      Ajustar
                    </Button>
                  )}
                </li>
              ))}
            </ul>
            <Paginator page={safePage} totalPages={totalPages}
              totalItems={rows.length} onPage={setPage} />
          </>
        )}
      </CardContent>
    </Card>
  );
}

/** Pestana MOVIMIENTOS: grafico semanal de entradas/salidas + kardex paginado. */
function MovementsTab({ branchId, productMap }: {
  branchId: string;
  productMap: Map<string, Product>;
}) {
  const [page, setPage] = useState(0);
  const movements = useStockMovements(branchId, page);
  const flow = useWeeklyFlow(branchId);

  return (
    <div className="space-y-4">
      <WeeklyFlowChart loading={flow.isLoading} data={flow.data ?? []} />

      <Card>
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-1.5">
            <h2 className="text-sm font-semibold">Historial de movimientos</h2>
            <InfoTip align="left" text="Todo lo que entro y salio del inventario de esta sucursal: ventas, ingresos de mercaderia y ajustes, con su motivo." />
          </div>
          {movements.isLoading ? (
            <Spinner />
          ) : (movements.data?.items ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">Sin movimientos todavia.</p>
          ) : (
            <>
              <ul className="divide-y divide-border">
                {movements.data!.items.map((m) => {
                  const p = productMap.get(m.productId);
                  const isOut = m.type === "OUT" || (m.type === "ADJUST" && Number(m.quantity) < 0);
                  return (
                    <li key={m.id} className="flex items-center gap-3 py-2">
                      <ProductPhoto
                        externalUrl={p?.photoExternalUrl}
                        uploadedUrl={p?.photoUrl}
                        name={p?.name ?? "Producto"}
                        className="size-10 shrink-0"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">{p?.name ?? "Producto"}</p>
                        <p className="truncate text-xs text-muted-foreground">
                          {timeOf(m.createdAt)} · {m.reason ?? (m.type === "OUT" ? "Venta" : "Ajuste")}
                        </p>
                      </div>
                      <span className={"shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold " +
                        (isOut ? "bg-red-50 text-destructive" : "bg-green-50 text-green-700")}>
                        {isOut ? "−" : "+"}{Math.abs(Number(m.quantity))}
                      </span>
                    </li>
                  );
                })}
              </ul>
              {movements.data && (
                <Paginator page={movements.data.page} totalPages={movements.data.totalPages}
                  totalItems={movements.data.totalItems} onPage={setPage} />
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

/** Grafico de area con curvas suaves: unidades que entran y salen cada dia de la semana. */
function WeeklyFlowChart({ loading, data }: {
  loading: boolean;
  data: { date: string; inQty: number; outQty: number }[];
}) {
  const chartData = useMemo(() => data.map((d) => {
    const day = new Date(d.date + "T00:00:00");
    const label = day.toLocaleDateString("es-PE", { weekday: "short", day: "2-digit" });
    return {
      day: label.charAt(0).toUpperCase() + label.slice(1),
      Entradas: Number(d.inQty),
      Salidas: Number(d.outQty),
    };
  }), [data]);

  const empty = chartData.every((d) => d.Entradas === 0 && d.Salidas === 0);

  return (
    <Card>
      <CardContent className="p-4">
        <div className="mb-1 flex items-center gap-1.5">
          <h2 className="text-sm font-semibold">Movimiento de la semana</h2>
          <InfoTip align="left" text="Unidades que entraron (ingresos de mercaderia) y salieron (ventas, mermas, retiros) del inventario cada dia de los ultimos 7 dias." />
        </div>
        <div className="mb-2 flex items-center gap-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <span className="size-2.5 rounded-full bg-green-500" /> Entradas
          </span>
          <span className="flex items-center gap-1.5">
            <span className="size-2.5 rounded-full bg-red-400" /> Salidas
          </span>
        </div>

        {loading ? (
          <div className="flex h-56 items-center justify-center"><Spinner /></div>
        ) : empty ? (
          <div className="flex h-56 items-center justify-center">
            <p className="text-sm text-muted-foreground">Sin movimientos en los ultimos 7 dias.</p>
          </div>
        ) : (
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
                <defs>
                  <linearGradient id="flowIn" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#22c55e" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#22c55e" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="flowOut" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#f87171" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#f87171" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 6" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="day" tickLine={false} axisLine={false}
                  tick={{ fontSize: 11, fill: "#64748b" }} dy={6} />
                <YAxis tickLine={false} axisLine={false} allowDecimals={false}
                  tick={{ fontSize: 11, fill: "#64748b" }} />
                <Tooltip
                  cursor={{ stroke: "#cbd5e1", strokeDasharray: "3 3" }}
                  contentStyle={{
                    borderRadius: 12,
                    border: "1px solid #e2e8f0",
                    boxShadow: "0 4px 12px rgba(15, 23, 42, 0.08)",
                    fontSize: 12,
                  }}
                />
                <Area type="monotone" dataKey="Entradas" stroke="#22c55e" strokeWidth={2.5}
                  strokeLinecap="round" fill="url(#flowIn)" dot={false}
                  activeDot={{ r: 4, strokeWidth: 0 }} />
                <Area type="monotone" dataKey="Salidas" stroke="#f87171" strokeWidth={2.5}
                  strokeLinecap="round" fill="url(#flowOut)" dot={false}
                  activeDot={{ r: 4, strokeWidth: 0 }} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function FilterPill({ label, active, onClick, warn, danger }: {
  label: string;
  active: boolean;
  onClick: () => void;
  warn?: boolean;
  danger?: boolean;
}) {
  const activeStyle = danger
    ? "border-destructive bg-red-50 text-destructive"
    : warn
      ? "border-amber-500 bg-amber-50 text-amber-700"
      : "border-primary bg-accent text-primary";
  return (
    <button
      type="button"
      onClick={onClick}
      className={"h-10 select-none rounded-full border px-3.5 text-xs font-medium transition touch-manipulation active:scale-95 " +
        (active ? activeStyle : "border-border bg-white text-muted-foreground hover:border-primary")}
    >
      {label}
    </button>
  );
}

function StockBadge({ qty }: { qty: number }) {
  if (qty === 0) return <Badge className="shrink-0 bg-red-100 text-red-700">Sin stock</Badge>;
  if (qty <= LOW_STOCK) return <Badge variant="warning" className="shrink-0">{qty} — bajo</Badge>;
  return <Badge variant="success" className="shrink-0">{qty}</Badge>;
}

/** Ajuste de stock con la foto del producto, ingreso/retiro y motivos rapidos. */
function AdjustModal({ product, branchId, currentQty, onClose }: {
  product: Product;
  branchId: string;
  currentQty: number;
  onClose: () => void;
}) {
  const adjust = useAdjustStock();
  const [mode, setMode] = useState<"in" | "out">("in");
  const [qty, setQty] = useState("");
  const [reason, setReason] = useState("");
  const [customReason, setCustomReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const reasons = mode === "in" ? STOCK_IN_REASONS : STOCK_OUT_REASONS;
  const qtyNum = Number(qty) || 0;
  const newQty = mode === "in" ? currentQty + qtyNum : currentQty - qtyNum;
  const effectiveReason = reason === "otro" ? customReason : reason;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (qtyNum <= 0) { setError("Indica una cantidad mayor a cero."); return; }
    if (mode === "out" && qtyNum > currentQty) {
      setError(`No puedes retirar mas de lo que hay (${currentQty}).`);
      return;
    }
    try {
      await adjust.mutateAsync({
        productId: product.id,
        branchId,
        quantity: mode === "in" ? qtyNum : -qtyNum,
        reason: effectiveReason || undefined,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-14">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">Ajustar stock</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        {/* Producto con su foto y stock actual */}
        <div className="mb-4 flex items-center gap-3 rounded-lg bg-muted/50 p-3">
          <ProductPhoto
            externalUrl={product.photoExternalUrl}
            uploadedUrl={product.photoUrl}
            name={product.name}
            className="size-16 shrink-0"
          />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold">{product.name}</p>
            <p className="text-xs text-muted-foreground">{product.sku} · {product.unit}</p>
          </div>
          <div className="text-right">
            <p className="text-xs text-muted-foreground">Stock actual</p>
            <p className="font-heading text-2xl font-bold">{currentQty}</p>
          </div>
        </div>

        <form onSubmit={submit} className="space-y-3">
          <div className="grid grid-cols-2 gap-1.5">
            <button type="button"
              onClick={() => { setMode("in"); setReason(""); }}
              className={"min-h-11 select-none rounded-lg border px-3 py-2 text-sm font-medium transition touch-manipulation active:scale-95 " +
                (mode === "in" ? "border-green-600 bg-green-50 text-green-700" : "border-border text-muted-foreground")}>
              Ingresar mercaderia
            </button>
            <button type="button"
              onClick={() => { setMode("out"); setReason(""); }}
              className={"min-h-11 select-none rounded-lg border px-3 py-2 text-sm font-medium transition touch-manipulation active:scale-95 " +
                (mode === "out" ? "border-destructive bg-red-50 text-destructive" : "border-border text-muted-foreground")}>
              Retirar / merma
            </button>
          </div>

          <div className="space-y-1">
            <Label>Cantidad ({product.unit.toLowerCase()})</Label>
            <Input type="number" step="0.001" min="0.001" value={qty}
              onChange={(e) => setQty(e.target.value)} required autoFocus placeholder="0" />
          </div>

          <div className="space-y-1">
            <Label className="flex items-center gap-1.5">
              Motivo
              <InfoTip text="Queda registrado en el historial de movimientos: sirve para saber por que cambio el stock y detectar perdidas." />
            </Label>
            <Select value={reason} onChange={(e) => setReason(e.target.value)} required>
              <option value="">Elige el motivo…</option>
              {reasons.map((r) => <option key={r} value={r}>{r}</option>)}
              <option value="otro">Otro motivo…</option>
            </Select>
            {reason === "otro" && (
              <Input value={customReason} onChange={(e) => setCustomReason(e.target.value)}
                placeholder="Describe el motivo" required maxLength={160} />
            )}
          </div>

          {qtyNum > 0 && (
            <p className={"rounded-lg px-3 py-2 text-sm font-medium " +
              (newQty < 0 ? "bg-red-50 text-destructive" : "bg-muted/60 text-foreground")}>
              {newQty < 0
                ? "El retiro deja el stock en negativo: revisa la cantidad."
                : <>El stock quedara en <span className="font-bold">{newQty}</span>.</>}
            </p>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" disabled={adjust.isPending}>
              {adjust.isPending ? "Aplicando..." : mode === "in" ? "Ingresar" : "Retirar"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
