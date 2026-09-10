"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useProducts } from "@/features/products/api";
import {
  useTables, useCreateTable, useOrders, useCreateOrder, useAddOrderItem, useOrderAction,
} from "@/features/restaurant/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

const tableVariant = (s: string) => (s === "FREE" ? "success" : s === "OCCUPIED" ? "warning" : "muted");

export default function RestaurantPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const { data: products = [] } = useProducts();
  const [branchId, setBranchId] = useState("");
  const tables = useTables(branchId || undefined);
  const orders = useOrders(branchId || undefined);
  const createTable = useCreateTable(branchId);
  const createOrder = useCreateOrder(branchId);
  const addItem = useAddOrderItem(branchId);
  const action = useOrderAction(branchId);

  const [tableName, setTableName] = useState("");
  const [selOrder, setSelOrder] = useState<string | null>(null);
  const [pid, setPid] = useState("");
  const [pqty, setPqty] = useState("1");

  const openOrders = (orders.data ?? []).filter((o) => !["BILLED", "CANCELLED"].includes(o.status));
  const current = openOrders.find((o) => o.id === selOrder) ?? null;
  const orderOfTable = (tid: string) => openOrders.find((o) => o.tableId === tid);

  return (
    <div>
      <PageHeader title="Restaurante" subtitle="Mesas y comandas" />

      <div className="mb-4 max-w-xs">
        <Label>Sucursal</Label>
        <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
          <option value="">Selecciona…</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>

      {!branchId ? (
        <p className="text-sm text-muted-foreground">Selecciona una sucursal.</p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-2">
          {/* MESAS */}
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Mesas</h2>
              {tables.isLoading ? <Spinner /> : (
                <div className="grid grid-cols-2 gap-3">
                  {(tables.data ?? []).map((t) => {
                    const ord = orderOfTable(t.id);
                    return (
                      <div key={t.id} className="rounded-lg border border-border p-3">
                        <div className="flex items-center justify-between">
                          <span className="font-medium">{t.name}</span>
                          <Badge variant={tableVariant(t.status)}>{t.status}</Badge>
                        </div>
                        {ord ? (
                          <Button size="sm" variant="outline" className="mt-2 w-full" onClick={() => setSelOrder(ord.id)}>
                            Ver comanda {ord.orderNumber}
                          </Button>
                        ) : hasPerm("order:create") ? (
                          <Button size="sm" className="mt-2 w-full"
                            onClick={async () => { const o = await createOrder.mutateAsync({ type: "DINE_IN", tableId: t.id }); setSelOrder(o.id); }}>
                            Abrir comanda
                          </Button>
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              )}
              {hasPerm("table:manage") && (
                <div className="mt-4 flex items-end gap-2 border-t border-border pt-4">
                  <div className="flex-1 space-y-1"><Label>Nueva mesa</Label><Input value={tableName} onChange={(e) => setTableName(e.target.value)} placeholder="Mesa 1" /></div>
                  <Button variant="outline" disabled={!tableName} onClick={async () => { await createTable.mutateAsync({ name: tableName }); setTableName(""); }}>Crear</Button>
                </div>
              )}
            </CardContent>
          </Card>

          {/* COMANDA */}
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Comanda</h2>
              {!current ? (
                <p className="text-sm text-muted-foreground">Selecciona o abre una comanda.</p>
              ) : (
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{current.orderNumber}</span>
                    <Badge variant="muted">{current.status}</Badge>
                  </div>
                  <ul className="divide-y divide-border text-sm">
                    {current.items.map((it) => (
                      <li key={it.id} className="flex justify-between py-1">
                        <span>{products.find((p) => p.id === it.productId)?.name ?? "item"} x{it.quantity}</span>
                        <span>S/ {it.lineTotal}</span>
                      </li>
                    ))}
                  </ul>
                  <p className="text-right font-semibold">Total: S/ {current.total}</p>

                  {hasPerm("order:create") && current.status === "OPEN" && (
                    <div className="flex items-end gap-2">
                      <div className="flex-1 space-y-1">
                        <Label>Producto</Label>
                        <Select value={pid} onChange={(e) => setPid(e.target.value)}>
                          <option value="">Selecciona…</option>
                          {products.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                        </Select>
                      </div>
                      <div className="w-16 space-y-1"><Label>Cant.</Label><Input type="number" min="1" value={pqty} onChange={(e) => setPqty(e.target.value)} /></div>
                      <Button variant="outline" disabled={!pid}
                        onClick={async () => { await addItem.mutateAsync({ orderId: current.id, productId: pid, quantity: Number(pqty) }); setPid(""); setPqty("1"); }}>
                        Agregar
                      </Button>
                    </div>
                  )}

                  <div className="flex gap-2">
                    {hasPerm("order:manage") && current.status === "OPEN" && (
                      <Button variant="outline" onClick={() => action.mutate({ orderId: current.id, action: "send-to-kitchen" })}>Enviar a cocina</Button>
                    )}
                    {hasPerm("order:manage") && (
                      <Button onClick={async () => { await action.mutateAsync({ orderId: current.id, action: "bill" }); setSelOrder(null); }}>Cobrar</Button>
                    )}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
