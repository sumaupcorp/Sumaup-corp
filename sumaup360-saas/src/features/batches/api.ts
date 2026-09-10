"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

const B = "/api/v1/erp/batches";

export interface ProductBatch {
  id: string;
  branchId: string;
  productId: string;
  productName?: string | null;
  batchCode: string;
  expiryDate?: string | null;
  quantity: number;
  status: "ACTIVE" | "DEPLETED" | "EXPIRED" | "RECALLED";
  notes?: string | null;
}

export function useBatches(branchId?: string, productId?: string) {
  return useQuery({
    queryKey: ["batches", branchId, productId],
    queryFn: () => apiGet<ProductBatch[]>(B, { branchId, productId }),
    enabled: !!branchId,
  });
}

export function useExpiringBatches(days = 30) {
  return useQuery({
    queryKey: ["batches-expiring", days],
    queryFn: () => apiGet<ProductBatch[]>(`${B}/expiring`, { days: String(days) }),
  });
}

export function useCreateBatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      branchId: string; productId: string; batchCode: string;
      expiryDate?: string; quantity: number; notes?: string;
    }) => apiPost<ProductBatch>(B, i),
    onSuccess: () => invalidate(qc),
  });
}

export function useUpdateBatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; quantity?: number; expiryDate?: string; status?: string; notes?: string }) =>
      apiPut<ProductBatch>(`${B}/${i.id}`, { quantity: i.quantity, expiryDate: i.expiryDate, status: i.status, notes: i.notes }),
    onSuccess: () => invalidate(qc),
  });
}

function invalidate(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ["batches"] });
  qc.invalidateQueries({ queryKey: ["batches-expiring"] });
}
