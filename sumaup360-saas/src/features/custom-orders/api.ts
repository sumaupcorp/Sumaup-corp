"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut, apiPatch } from "@/lib/api";

const C = "/api/v1/erp/custom-orders";

export type CustomOrderStatus = "PENDING" | "IN_PROGRESS" | "READY" | "DELIVERED" | "CANCELED";

export interface CustomOrder {
  id: string;
  branchId: string;
  customerId: string;
  customerName?: string | null;
  description: string;
  deliveryAt: string;
  totalAmount: number;
  advanceAmount: number;
  status: CustomOrderStatus;
  saleId?: string | null;
  notes?: string | null;
}

export function useCustomOrders(branchId?: string, status?: string) {
  return useQuery({
    queryKey: ["custom-orders", branchId, status ?? "all"],
    queryFn: () => apiGet<CustomOrder[]>(C, { branchId, status }),
    enabled: !!branchId,
  });
}

export function useCreateCustomOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      branchId: string; customerId: string; description: string; deliveryAt: string;
      totalAmount: number; advanceAmount?: number; notes?: string;
    }) => apiPost<CustomOrder>(C, i),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["custom-orders"] }),
  });
}

export function useUpdateCustomOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; description?: string; deliveryAt?: string; totalAmount?: number; advanceAmount?: number; notes?: string }) =>
      apiPut<CustomOrder>(`${C}/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["custom-orders"] }),
  });
}

export function useSetCustomOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; status: CustomOrderStatus }) =>
      apiPatch<CustomOrder>(`${C}/${i.id}/status`, { status: i.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["custom-orders"] }),
  });
}
