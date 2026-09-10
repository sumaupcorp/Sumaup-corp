"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useCustomers } from "@/features/customers/api";
import {
  useCustomOrders, useCreateCustomOrder, useSetCustomOrderStatus, type CustomOrderStatus,
} from "@/features/custom-orders/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

const statusLabel: Record<CustomOrderStatus, string> = {
  PENDING: "Pendiente", IN_PROGRESS: "En preparacion", READY: "Listo",
  DELIVERED: "Entregado", CANCELED: "Cancelado",
};
const nextAction: Partial<Record<CustomOrderStatus, { to: CustomOrderStatus; label: string }>> = {
  PENDING: { to: "IN_PROGRESS", label: "Empezar" },
  IN_PROGRESS: { to: "READY", label: "Listo" },
  READY: { to: "DELIVERED", label: "Entregar" },
};
const statusVariant = (s: CustomOrderStatus) =>
  s === "DELIVERED" ? "success" : s === "CANCELED" ? "muted" : "warning";

export default function CustomOrdersPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const { data: customers = [] } = useCustomers();
  const [branchId, setBranchId] = useState("");
  const orders = useCustomOrders(branchId || undefined);
  const createOrder = useCreateCustomOrder();
  const setStatus = useSetCustomOrderStatus();

  const [customerId, setCustomerId] = useState("");
  const [description, setDescription] = useState("");
  const [deliveryAt, setDeliveryAt] = useState("");
  const [totalAmount, setTotalAmount] = useState("");
  const [advanceAmount, setAdvanceAmount] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setError(null);
    try {
      await createOrder.mutateAsync({
        branchId, customerId, description,
        deliveryAt: new Date(deliveryAt).toISOString(),
        totalAmount: Number(totalAmount),
        advanceAmount: advanceAmount ? Number(advanceAmount) : undefined,
      });
      setDescription(""); setDeliveryAt(""); setTotalAmount(""); setAdvanceAmount("");
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Pedidos por encargo" subtitle="Encargos con fecha de entrega y adelanto" />

      <div className="mb-4 max-w-xs">
        <Label>Sucursal</Label>
        <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
          <option value="">Selecciona…</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>

      {!branchId ? (
        <p className="text-sm text-muted-foreground">Selecciona una sucursal para ver sus encargos.</p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Encargos</h2>
              {orders.isLoading ? <Spinner /> : (orders.data ?? []).length === 0 ? (
                <p className="text-sm text-muted-foreground">Aun no hay encargos registrados.</p>
              ) : (
                <ul className="divide-y divide-border text-sm">
                  {(orders.data ?? []).map((o) => {
                    const action = nextAction[o.status];
                    return (
                      <li key={o.id} className="py-2">
                        <div className="flex items-center justify-between">
                          <span className="font-medium">{o.customerName ?? "Cliente"}</span>
                          <Badge variant={statusVariant(o.status)}>{statusLabel[o.status]}</Badge>
                        </div>
                        <p className="mt-1 text-muted-foreground">{o.description}</p>
                        <div className="mt-1 flex items-center justify-between text-muted-foreground">
                          <span>
                            Entrega: {new Date(o.deliveryAt).toLocaleString("es-PE")} ·
                            S/ {o.totalAmount}{o.advanceAmount > 0 ? ` (adelanto S/ ${o.advanceAmount})` : ""}
                          </span>
                          {hasPerm("custom-order:manage") && (
                            <span className="flex gap-1">
                              {action && (
                                <Button size="sm" variant="outline"
                                  onClick={() => setStatus.mutate({ id: o.id, status: action.to })}>
                                  {action.label}
                                </Button>
                              )}
                              {!["DELIVERED", "CANCELED"].includes(o.status) && (
                                <Button size="sm" variant="ghost"
                                  onClick={() => setStatus.mutate({ id: o.id, status: "CANCELED" })}>
                                  Cancelar
                                </Button>
                              )}
                            </span>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </CardContent>
          </Card>

          {hasPerm("custom-order:manage") && (
            <Card>
              <CardContent className="space-y-3 p-5">
                <h2 className="text-sm font-semibold">Nuevo encargo</h2>
                <div className="space-y-1">
                  <Label>Cliente</Label>
                  <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                    <option value="">Selecciona…</option>
                    {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>Descripcion del pedido</Label>
                  <textarea
                    className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    rows={2}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Torta de chocolate 20 personas, mensaje Feliz Dia..."
                  />
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div className="space-y-1 col-span-1"><Label>Entrega</Label><Input type="datetime-local" value={deliveryAt} onChange={(e) => setDeliveryAt(e.target.value)} /></div>
                  <div className="space-y-1"><Label>Total S/</Label><Input type="number" min="0" step="0.1" value={totalAmount} onChange={(e) => setTotalAmount(e.target.value)} /></div>
                  <div className="space-y-1"><Label>Adelanto S/</Label><Input type="number" min="0" step="0.1" value={advanceAmount} onChange={(e) => setAdvanceAmount(e.target.value)} /></div>
                </div>
                {error && <p className="text-sm text-destructive">{error}</p>}
                <Button disabled={!customerId || !description || !deliveryAt || !totalAmount || createOrder.isPending} onClick={submit}>
                  {createOrder.isPending ? "Guardando..." : "Registrar encargo"}
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
