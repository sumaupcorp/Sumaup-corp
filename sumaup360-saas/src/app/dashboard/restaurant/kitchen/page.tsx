"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useProducts } from "@/features/products/api";
import { useKitchenQueue, useSetItemStatus } from "@/features/restaurant/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

const NEXT: Record<string, string> = { PENDING: "PREPARING", PREPARING: "READY", READY: "SERVED" };
const variant = (s: string) => (s === "READY" ? "success" : s === "SERVED" ? "muted" : "warning");

export default function KitchenPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const canOperate = hasPerm("kitchen:operate");
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const { data: products = [] } = useProducts();
  const [branchId, setBranchId] = useState("");
  const queue = useKitchenQueue(branchId || undefined);
  const setStatus = useSetItemStatus(branchId);

  return (
    <div>
      <PageHeader title="Cocina" subtitle="Comandas en preparacion (se actualiza solo)" />

      <div className="mb-4 max-w-xs">
        <Label>Sucursal</Label>
        <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
          <option value="">Selecciona…</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>

      {!branchId ? (
        <p className="text-sm text-muted-foreground">Selecciona una sucursal.</p>
      ) : queue.isLoading ? (
        <Spinner />
      ) : (queue.data ?? []).length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay comandas en cocina.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {queue.data!.map((o) => (
            <Card key={o.id}>
              <CardContent className="p-4">
                <p className="mb-2 font-semibold">{o.orderNumber}</p>
                <ul className="space-y-2">
                  {o.items.map((it) => (
                    <li key={it.id} className="flex items-center justify-between gap-2 text-sm">
                      <span>{products.find((p) => p.id === it.productId)?.name ?? "item"} x{it.quantity}</span>
                      <div className="flex items-center gap-1">
                        <Badge variant={variant(it.status)}>{it.status}</Badge>
                        {canOperate && NEXT[it.status] && (
                          <Button size="sm" variant="outline"
                            onClick={() => setStatus.mutate({ itemId: it.id, status: NEXT[it.status] })}>
                            {NEXT[it.status]}
                          </Button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
