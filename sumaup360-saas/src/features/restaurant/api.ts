"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPatch } from "@/lib/api";

const RB = "/api/v1/erp/restaurant";

export interface Table {
  id: string; branchId: string; name: string; zone?: string | null; capacity: number; status: string;
}
export interface OrderItem {
  id: string; productId: string; quantity: number; unitPrice: number; lineTotal: number; status: string; notes?: string | null;
}
export interface Order {
  id: string; branchId: string; tableId?: string | null; orderNumber: string; type: string;
  status: string; total: number; saleId?: string | null; items: OrderItem[];
}

export function useTables(branchId?: string) {
  return useQuery({ queryKey: ["r-tables", branchId], queryFn: () => apiGet<Table[]>(`${RB}/tables`, { branchId }), enabled: !!branchId });
}
export function useCreateTable(branchId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { name: string; zone?: string; capacity?: number }) => apiPost<Table>(`${RB}/tables`, { branchId, ...i }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["r-tables", branchId] }),
  });
}

export function useOrders(branchId?: string) {
  return useQuery({ queryKey: ["r-orders", branchId], queryFn: () => apiGet<Order[]>(`${RB}/orders`, { branchId }), enabled: !!branchId });
}
export function useCreateOrder(branchId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { type: string; tableId?: string; notes?: string }) => apiPost<Order>(`${RB}/orders`, { branchId, ...i }),
    onSuccess: () => invalidate(qc, branchId),
  });
}
export function useAddOrderItem(branchId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { orderId: string; productId: string; quantity: number; notes?: string; station?: string }) =>
      apiPost(`${RB}/orders/${i.orderId}/items`, { productId: i.productId, quantity: i.quantity, notes: i.notes, station: i.station }),
    onSuccess: () => invalidate(qc, branchId),
  });
}
export function useOrderAction(branchId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { orderId: string; action: "send-to-kitchen" | "bill" | "cancel" }) =>
      apiPost(`${RB}/orders/${i.orderId}/${i.action}`, {}),
    onSuccess: () => invalidate(qc, branchId),
  });
}

export function useKitchenQueue(branchId?: string) {
  return useQuery({ queryKey: ["r-kitchen", branchId], queryFn: () => apiGet<Order[]>(`${RB}/kitchen/queue`, { branchId }), enabled: !!branchId, refetchInterval: 5000 });
}
export function useSetItemStatus(branchId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { itemId: string; status: string }) => apiPatch(`${RB}/kitchen/items/${i.itemId}/status`, { status: i.status }),
    onSuccess: () => invalidate(qc, branchId),
  });
}

function invalidate(qc: ReturnType<typeof useQueryClient>, branchId: string) {
  qc.invalidateQueries({ queryKey: ["r-orders", branchId] });
  qc.invalidateQueries({ queryKey: ["r-tables", branchId] });
  qc.invalidateQueries({ queryKey: ["r-kitchen", branchId] });
}
