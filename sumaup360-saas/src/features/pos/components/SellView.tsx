"use client";

import { useMemo, useState } from "react";
import type { Product } from "@/features/products/api";
import {
  useCreateSale,
  PAYMENT_METHODS,
  type PaymentMethod,
  type Sale,
} from "@/features/sales/api";
import { useBranchStock } from "@/features/inventory/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { InfoTip } from "@/components/ui/tooltip";
import { ProductPhoto } from "@/components/ui/product-photo";
import { fmt } from "./shared";

const QUICK_CASH = [10, 20, 50, 100, 200];
/** Umbral para avisar "quedan pocos" en la tarjeta del producto. */
const LOW_STOCK = 5;

/**
 * Pantalla de VENTA pura, pensada para pantalla tactil: productos compactos para tocar,
 * carrito con botones amplios, metodo de pago con vuelto rapido. La caja vive en su pestana.
 */
export function SellView({ branchId, products, canSell, canViewProducts, cashOpen, onGoCash, onSold }: {
  branchId: string;
  products: Product[];
  canSell: boolean;
  canViewProducts: boolean;
  cashOpen: boolean;
  onGoCash: () => void;
  onSold: (sale: Sale) => void;
}) {
  const createSale = useCreateSale();

  // Stock de la sede para marcar "sin stock" y topes al vender. Si el usuario no tiene
  // permiso de inventario, la consulta falla y la venta sigue funcionando sin marcas.
  const stock = useBranchStock(branchId || undefined);
  const stockLoaded = !!stock.data;
  const stockMap = useMemo(
    () => new Map((stock.data ?? []).map((s) => [s.productId, Number(s.quantity)])),
    [stock.data]
  );

  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [cart, setCart] = useState<Map<string, number>>(new Map());
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("CASH");
  const [received, setReceived] = useState("");
  const [error, setError] = useState<string | null>(null);

  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const p of products) if (p.active && p.category) set.add(p.category);
    return [...set].sort();
  }, [products]);

  const productMap = useMemo(() => new Map(products.map((p) => [p.id, p])), [products]);

  const visibleProducts = useMemo(() => {
    const q = search.trim().toLowerCase();
    let list = products.filter((p) => p.active);
    if (category) list = list.filter((p) => p.category === category);
    if (q) {
      list = list.filter((p) =>
        p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q));
    }
    return list;
  }, [products, search, category]);

  const cartLines = useMemo(() =>
    [...cart.entries()]
      .map(([productId, quantity]) => {
        const available = stockLoaded ? (stockMap.get(productId) ?? 0) : null;
        return {
          productId,
          quantity,
          product: productMap.get(productId),
          available,
          exceeds: available !== null && quantity > available,
        };
      })
      .filter((l) => l.product),
    [cart, productMap, stockLoaded, stockMap]);

  const total = cartLines.reduce((s, l) => s + Number(l.product!.price) * l.quantity, 0);
  const itemCount = cartLines.reduce((s, l) => s + l.quantity, 0);
  const receivedNum = Number(received) || 0;
  const change = paymentMethod === "CASH" && received !== "" ? receivedNum - total : null;
  const stockConflicts = cartLines.filter((l) => l.exceeds);

  const addToCart = (p: Product) => {
    setError(null);
    setCart((c) => {
      const n = new Map(c);
      n.set(p.id, (n.get(p.id) ?? 0) + 1);
      return n;
    });
  };

  const setQty = (productId: string, qty: number) => {
    setCart((c) => {
      const n = new Map(c);
      if (qty <= 0) n.delete(productId);
      else n.set(productId, qty);
      return n;
    });
  };

  const submitSale = async () => {
    setError(null);
    if (stockConflicts.length > 0) {
      setError("Hay productos marcados en rojo: pides mas de lo que tienes en stock.");
      return;
    }
    if (paymentMethod === "CASH" && received !== "" && receivedNum < total) {
      setError("El monto recibido no alcanza para cubrir el total.");
      return;
    }
    try {
      const s = await createSale.mutateAsync({
        branchId,
        paymentMethod,
        items: cartLines.map((l) => ({ productId: l.productId, quantity: l.quantity })),
      });
      setCart(new Map());
      setReceived("");
      onSold(s);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_400px]">
      {/* PRODUCTOS: compactos, grandes de tocar */}
      <Card>
        <CardContent className="p-4">
          <div className="mb-3 space-y-2">
            <Input value={search} onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar o escanear codigo..." className="h-11 text-base" />
            {categories.length > 0 && (
              <div className="flex gap-1.5 overflow-x-auto pb-1">
                <CategoryChip label="Todos" active={category === ""} onClick={() => setCategory("")} />
                {categories.map((c) => (
                  <CategoryChip key={c} label={c} active={category === c} onClick={() => setCategory(c)} />
                ))}
              </div>
            )}
          </div>

          {visibleProducts.length === 0 ? (
            <p className="py-10 text-center text-sm text-muted-foreground">
              {search || category
                ? "Nada coincide con tu busqueda."
                : !canViewProducts
                  ? "Tu cuenta no tiene permiso para ver el catalogo de productos."
                  : "Aun no tienes productos. Crealos en la seccion Productos."}
            </p>
          ) : (
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6">
              {visibleProducts.map((p) => {
                const inCart = cart.get(p.id) ?? 0;
                const available = stockLoaded ? (stockMap.get(p.id) ?? 0) : null;
                const noStock = available !== null && available <= 0;
                const lowStock = available !== null && available > 0 && available <= LOW_STOCK;
                return (
                  <button
                    key={p.id}
                    type="button"
                    disabled={!canSell || noStock}
                    onClick={() => addToCart(p)}
                    className={
                      "relative flex select-none flex-col items-center gap-1 rounded-xl border bg-white p-2 pb-1.5 text-center " +
                      "transition touch-manipulation active:scale-95 disabled:cursor-default " +
                      (noStock
                        ? "border-red-300 bg-red-50/40"
                        : inCart > 0
                          ? "border-primary shadow-sm"
                          : "border-border hover:border-primary")
                    }
                  >
                    {inCart > 0 && !noStock && (
                      <span className="absolute right-1.5 top-1.5 z-10 flex size-6 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
                        {inCart}
                      </span>
                    )}
                    <div className={noStock ? "opacity-50 grayscale" : undefined}>
                      <ProductPhoto
                        externalUrl={p.photoExternalUrl}
                        uploadedUrl={p.photoUrl}
                        name={p.name}
                        className="size-14"
                      />
                    </div>
                    <span className={"line-clamp-2 min-h-7 w-full text-[11px] font-medium leading-tight " +
                      (noStock ? "text-muted-foreground" : "")}>
                      {p.name}
                    </span>
                    {noStock ? (
                      <span className="rounded-full bg-red-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-red-700">
                        Sin stock
                      </span>
                    ) : (
                      <>
                        <span className="text-sm font-bold text-primary">{fmt(p.price)}</span>
                        {lowStock && (
                          <span className="text-[10px] font-semibold text-amber-600">
                            Quedan {available}
                          </span>
                        )}
                      </>
                    )}
                  </button>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* CARRITO / COBRO */}
      <div className="lg:sticky lg:top-4">
        <Card>
          <CardContent className="p-4">
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-sm font-semibold">Venta actual</h2>
              {itemCount > 0 && (
                <span className="text-xs text-muted-foreground">{itemCount} producto(s)</span>
              )}
            </div>

            {!cashOpen && (
              <div className="mb-3 flex items-center justify-between gap-2 rounded-lg bg-amber-50 px-3 py-2">
                <p className="text-xs font-medium text-amber-700">La caja esta cerrada: abre caja para vender.</p>
                <Button size="sm" variant="outline" className="shrink-0" onClick={onGoCash}>
                  Ir a Caja
                </Button>
              </div>
            )}

            {cartLines.length === 0 ? (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Toca un producto para agregarlo a la venta.
              </p>
            ) : (
              <ul className="max-h-64 space-y-0.5 overflow-y-auto text-sm">
                {cartLines.map((l) => (
                  <li key={l.productId}
                    className={"flex flex-wrap items-center gap-2 rounded-lg border px-2 py-2 " +
                      (l.exceeds
                        ? "border-destructive bg-red-50/60"
                        : "border-transparent border-b-border")}>
                    <div className="min-w-0 flex-1">
                      <p className={"truncate text-xs font-medium " + (l.exceeds ? "text-destructive" : "")}>
                        {l.product!.name}
                      </p>
                      <p className="text-xs text-muted-foreground">{fmt(l.product!.price)} c/u</p>
                    </div>
                    <div className="flex items-center gap-1">
                      <QtyButton label="−" onClick={() => setQty(l.productId, l.quantity - 1)} />
                      <span className={"w-8 text-center text-base font-semibold " +
                        (l.exceeds ? "text-destructive" : "")}>
                        {l.quantity}
                      </span>
                      <QtyButton label="+" onClick={() => setQty(l.productId, l.quantity + 1)} />
                    </div>
                    <span className="w-16 text-right text-sm font-semibold">
                      {fmt(Number(l.product!.price) * l.quantity)}
                    </span>
                    {l.exceeds && (
                      <p className="w-full text-[11px] font-semibold text-destructive">
                        Solo tienes {l.available} en stock: baja la cantidad o repone inventario.
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {/* CALCULADORA DE VENTA: siempre visible, en cero si no hay productos */}
            <div className="mt-2 flex items-center justify-between border-t border-border pt-2">
              <span className="text-sm text-muted-foreground">Total</span>
              <span className={"font-heading text-3xl font-bold " +
                (cartLines.length === 0 ? "text-muted-foreground" : "")}>
                {fmt(total)}
              </span>
            </div>

            {stockConflicts.length > 0 && (
              <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-destructive">
                {stockConflicts.length === 1
                  ? `"${stockConflicts[0].product!.name}" no tiene stock suficiente.`
                  : `${stockConflicts.length} productos no tienen stock suficiente.`} Revisa las lineas en rojo.
              </p>
            )}

            {/* METODO DE PAGO */}
            <div className="mt-3 space-y-2">
                  <Label className="flex items-center gap-1.5">
                    Como paga el cliente
                    <InfoTip align="right" text="Solo el efectivo entra fisicamente a la caja y cuenta para el arqueo. Tarjeta, Yape, Plin y transferencia se cuadran con sus vouchers." />
                  </Label>
                  <div className="grid grid-cols-3 gap-1.5">
                    {PAYMENT_METHODS.map((m) => (
                      <button
                        key={m.code}
                        type="button"
                        onClick={() => setPaymentMethod(m.code)}
                        className={
                          "min-h-11 select-none rounded-lg border px-2 py-2 text-sm font-medium transition touch-manipulation active:scale-95 " +
                          (paymentMethod === m.code
                            ? "border-primary bg-accent text-primary"
                            : "border-border text-muted-foreground hover:border-primary")
                        }
                      >
                        {m.label}
                      </button>
                    ))}
                  </div>

                  {paymentMethod === "CASH" && (
                    <div className="space-y-1.5">
                      <div className="flex flex-wrap gap-1.5">
                        {QUICK_CASH.filter((q) => q >= total).slice(0, 3).map((q) => (
                          <button key={q} type="button"
                            onClick={() => setReceived(String(q))}
                            className="min-h-10 select-none rounded-lg border border-border px-3 text-sm font-medium text-muted-foreground transition touch-manipulation active:scale-95 hover:border-primary hover:text-primary">
                            {fmt(q)}
                          </button>
                        ))}
                        <button type="button"
                          onClick={() => setReceived(total.toFixed(2))}
                          className="min-h-10 select-none rounded-lg border border-border px-3 text-sm font-medium text-muted-foreground transition touch-manipulation active:scale-95 hover:border-primary hover:text-primary">
                          Exacto
                        </button>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="flex-1 space-y-1">
                          <Label className="text-xs">Paga con</Label>
                          <Input type="number" step="0.01" min="0" className="h-11 text-base"
                            placeholder="0.00"
                            value={received}
                            onChange={(e) => setReceived(e.target.value)} />
                        </div>
                        <div className="flex-1 text-right">
                          <p className="text-xs text-muted-foreground">Vuelto</p>
                          <p className={"font-heading text-2xl font-bold " +
                            (change !== null && change < 0 ? "text-destructive" : "text-foreground")}>
                            {change === null ? "—" : fmt(change)}
                          </p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>

            <div className="mt-3 flex gap-2">
              <Button variant="ghost" className="h-12 flex-1"
                disabled={cartLines.length === 0}
                onClick={() => { setCart(new Map()); setReceived(""); }}>
                Vaciar
              </Button>
              <Button className="h-12 flex-[2] text-base font-bold"
                disabled={!canSell || !cashOpen || cartLines.length === 0
                  || stockConflicts.length > 0 || createSale.isPending}
                onClick={submitSale}>
                {createSale.isPending ? "Registrando..." : `Cobrar ${fmt(total)}`}
              </Button>
            </div>

            {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function CategoryChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "h-10 shrink-0 select-none rounded-full border px-4 text-sm font-medium transition touch-manipulation active:scale-95 " +
        (active
          ? "border-primary bg-primary text-primary-foreground"
          : "border-border bg-white text-muted-foreground hover:border-primary")
      }
    >
      {label}
    </button>
  );
}

function QtyButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex size-10 select-none items-center justify-center rounded-lg border border-border text-lg font-bold text-muted-foreground transition touch-manipulation active:scale-95 hover:border-primary hover:text-primary"
    >
      {label}
    </button>
  );
}
