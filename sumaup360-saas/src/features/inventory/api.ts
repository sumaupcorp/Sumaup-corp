"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, type PageResponse } from "@/lib/api";

export interface Stock {
  productId: string;
  branchId: string;
  quantity: number;
}

export interface StockMovement {
  id: string;
  productId: string;
  type: "IN" | "OUT" | "ADJUST";
  quantity: number;
  reason: string | null;
  createdAt: string;
}

/** Motivos rapidos de INGRESO de stock (delta positivo). */
export const STOCK_IN_REASONS = [
  "Compra a proveedor",
  "Reposicion desde almacen",
  "Devolucion de cliente",
  "Carga inicial",
];

/** Motivos rapidos de RETIRO de stock (delta negativo). */
export const STOCK_OUT_REASONS = [
  "Merma / vencido",
  "Producto danado",
  "Uso interno",
  "Correccion de conteo",
];

export function useStock(productId?: string, branchId?: string) {
  return useQuery({
    queryKey: ["stock", productId, branchId],
    queryFn: () => apiGet<Stock>("/api/v1/inventory/stock", { productId, branchId }),
    enabled: !!productId && !!branchId,
  });
}

/** Stock de todos los productos de una sucursal (los que no aparecen tienen 0). */
export function useBranchStock(branchId?: string) {
  return useQuery({
    queryKey: ["stock", "branch", branchId],
    queryFn: () => apiGet<Stock[]>("/api/v1/inventory/stock/by-branch", { branchId }),
    enabled: !!branchId,
  });
}

/** Kardex paginado de la sucursal (ventas, ingresos, ajustes), mas reciente primero. */
export function useStockMovements(branchId?: string, page = 0, size = 8) {
  return useQuery({
    queryKey: ["inventory-movements", branchId, page, size],
    queryFn: () => apiGet<PageResponse<StockMovement>>("/api/v1/inventory/movements", {
      branchId,
      page: String(page),
      size: String(size),
    }),
    enabled: !!branchId,
  });
}

export interface DailyFlow {
  date: string;
  inQty: number;
  outQty: number;
}

/** Flujo de los ultimos 7 dias: unidades que entraron y salieron por dia. */
export function useWeeklyFlow(branchId?: string) {
  return useQuery({
    queryKey: ["inventory-flow", branchId],
    queryFn: () => apiGet<DailyFlow[]>("/api/v1/inventory/flow/weekly", { branchId }),
    enabled: !!branchId,
  });
}

export function useAdjustStock() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { productId: string; branchId: string; quantity: number; reason?: string }) =>
      apiPost<Stock>("/api/v1/inventory/adjust", input),
    onSuccess: (_d, vars) => {
      qc.invalidateQueries({ queryKey: ["stock"] });
      qc.invalidateQueries({ queryKey: ["inventory-movements", vars.branchId] });
      qc.invalidateQueries({ queryKey: ["inventory-flow", vars.branchId] });
    },
  });
}
