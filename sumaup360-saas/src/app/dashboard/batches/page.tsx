"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useProducts } from "@/features/products/api";
import { useBatches, useExpiringBatches, useCreateBatch, useUpdateBatch } from "@/features/batches/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

const statusVariant = (s: string) => (s === "ACTIVE" ? "success" : s === "EXPIRED" ? "warning" : "muted");
const statusLabel: Record<string, string> = {
  ACTIVE: "Activo", DEPLETED: "Agotado", EXPIRED: "Vencido", RECALLED: "Retirado",
};

function daysTo(expiry?: string | null) {
  if (!expiry) return null;
  const diff = Math.ceil((new Date(expiry).getTime() - Date.now()) / 86_400_000);
  return diff;
}

export default function BatchesPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const { data: products = [] } = useProducts();
  const [branchId, setBranchId] = useState("");
  const batches = useBatches(branchId || undefined);
  const expiring = useExpiringBatches(30);
  const createBatch = useCreateBatch();
  const updateBatch = useUpdateBatch();

  const [productId, setProductId] = useState("");
  const [batchCode, setBatchCode] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [quantity, setQuantity] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setError(null);
    try {
      await createBatch.mutateAsync({
        branchId, productId, batchCode,
        expiryDate: expiryDate || undefined,
        quantity: Number(quantity),
      });
      setBatchCode(""); setExpiryDate(""); setQuantity("");
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Lotes y vencimientos" subtitle="Controla lotes por producto y anticipa vencimientos" />

      {/* Vencimientos proximos */}
      <Card className="mb-6">
        <CardContent className="p-5">
          <h2 className="mb-3 text-sm font-semibold">Vencen en los proximos 30 dias</h2>
          {expiring.isLoading ? <Spinner /> : (expiring.data ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">Sin lotes por vencer. Todo en orden.</p>
          ) : (
            <ul className="divide-y divide-border text-sm">
              {(expiring.data ?? []).map((b) => {
                const d = daysTo(b.expiryDate);
                return (
                  <li key={b.id} className="flex items-center justify-between py-2">
                    <span>{b.productName ?? b.productId} — lote {b.batchCode}</span>
                    <Badge variant={d != null && d < 0 ? "warning" : "muted"}>
                      {d != null && d < 0 ? `Vencido hace ${-d} dias` : `Vence en ${d} dias`}
                    </Badge>
                  </li>
                );
              })}
            </ul>
          )}
        </CardContent>
      </Card>

      <div className="mb-4 max-w-xs">
        <Label>Sucursal</Label>
        <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
          <option value="">Selecciona…</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>

      {!branchId ? (
        <p className="text-sm text-muted-foreground">Selecciona una sucursal para ver sus lotes.</p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Lotes de la sucursal</h2>
              {batches.isLoading ? <Spinner /> : (batches.data ?? []).length === 0 ? (
                <p className="text-sm text-muted-foreground">Aun no hay lotes registrados.</p>
              ) : (
                <ul className="divide-y divide-border text-sm">
                  {(batches.data ?? []).map((b) => (
                    <li key={b.id} className="py-2">
                      <div className="flex items-center justify-between">
                        <span className="font-medium">{b.productName ?? b.productId}</span>
                        <Badge variant={statusVariant(b.status)}>{statusLabel[b.status] ?? b.status}</Badge>
                      </div>
                      <div className="mt-1 flex items-center justify-between text-muted-foreground">
                        <span>Lote {b.batchCode} · {b.quantity} und · {b.expiryDate ? `vence ${b.expiryDate}` : "sin vencimiento"}</span>
                        {hasPerm("batch:manage") && b.status === "ACTIVE" && (
                          <span className="flex gap-1">
                            <Button size="sm" variant="ghost" onClick={() => updateBatch.mutate({ id: b.id, status: "DEPLETED" })}>Agotado</Button>
                            <Button size="sm" variant="ghost" onClick={() => updateBatch.mutate({ id: b.id, status: "EXPIRED" })}>Vencido</Button>
                          </span>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          {hasPerm("batch:manage") && (
            <Card>
              <CardContent className="space-y-3 p-5">
                <h2 className="text-sm font-semibold">Registrar lote</h2>
                <div className="space-y-1">
                  <Label>Producto</Label>
                  <Select value={productId} onChange={(e) => setProductId(e.target.value)}>
                    <option value="">Selecciona…</option>
                    {products.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </Select>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1"><Label>Codigo de lote</Label><Input value={batchCode} onChange={(e) => setBatchCode(e.target.value)} placeholder="L-2026-001" /></div>
                  <div className="space-y-1"><Label>Vencimiento</Label><Input type="date" value={expiryDate} onChange={(e) => setExpiryDate(e.target.value)} /></div>
                </div>
                <div className="space-y-1 max-w-40"><Label>Cantidad</Label><Input type="number" min="0" value={quantity} onChange={(e) => setQuantity(e.target.value)} /></div>
                {error && <p className="text-sm text-destructive">{error}</p>}
                <Button disabled={!productId || !batchCode || !quantity || createBatch.isPending} onClick={submit}>
                  {createBatch.isPending ? "Guardando..." : "Guardar lote"}
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
