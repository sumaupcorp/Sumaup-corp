"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet, type PageResponse } from "@/lib/api";

export interface SalesSummary {
  salesCount: number;
  totalAmount: number;
}
export interface TopProduct {
  productId: string;
  sku: string | null;
  name: string | null;
  quantitySold: number;
  amount: number;
}
export interface SalesByBranch {
  branchId: string;
  branchName: string | null;
  salesCount: number;
  totalAmount: number;
}
export interface DailySales {
  date: string;
  total: number;
  cashTotal: number;
  digitalTotal: number;
  count: number;
}
export interface PaymentMethodStat {
  method: string;
  count: number;
  total: number;
}
export interface LowStockItem {
  productId: string;
  sku: string | null;
  name: string | null;
  branchId: string;
  quantity: number;
}
export interface CashClosure {
  id: string;
  branchId: string;
  branchName: string | null;
  openedAt: string | null;
  closedAt: string | null;
  openingAmount: number;
  expectedAmount: number | null;
  closingAmount: number | null;
  difference: number | null;
  notes: string | null;
}

/** branchId opcional: sin el = consolidado general. from/to acotan el rango (ISO). */
export function useSalesSummary(branchId?: string, from?: string, to?: string) {
  return useQuery({
    queryKey: ["report", "sales-summary", branchId ?? "all", from ?? "", to ?? ""],
    queryFn: () => apiGet<SalesSummary>("/api/v1/reports/sales-summary", { branchId, from, to }),
  });
}

export function useTopProducts(branchId?: string, limit = 5) {
  return useQuery({
    queryKey: ["report", "top-products", branchId ?? "all", limit],
    queryFn: () => apiGet<TopProduct[]>("/api/v1/reports/top-products",
      { limit: String(limit), branchId }),
  });
}

export function useSalesByBranch() {
  return useQuery({
    queryKey: ["report", "sales-by-branch"],
    queryFn: () => apiGet<SalesByBranch[]>("/api/v1/reports/sales-by-branch"),
  });
}

/** Serie diaria de los ultimos N dias (efectivo vs digital), para el grafico. */
export function useDailySales(branchId?: string, days = 30) {
  return useQuery({
    queryKey: ["report", "sales-daily", branchId ?? "all", days],
    queryFn: () => apiGet<DailySales[]>("/api/v1/reports/sales-daily",
      { branchId, days: String(days) }),
  });
}

export function usePaymentMethods(branchId?: string, days = 30) {
  return useQuery({
    queryKey: ["report", "payment-methods", branchId ?? "all", days],
    queryFn: () => apiGet<PaymentMethodStat[]>("/api/v1/reports/payment-methods",
      { branchId, days: String(days) }),
  });
}

/** Stock bajo de UNA sucursal (el endpoint requiere branchId). */
export function useLowStock(branchId?: string, threshold = 5) {
  return useQuery({
    queryKey: ["report", "low-stock", branchId, threshold],
    queryFn: () => apiGet<LowStockItem[]>("/api/v1/reports/low-stock",
      { branchId, threshold: String(threshold) }),
    enabled: !!branchId,
  });
}

/** Historial paginado de cierres de caja (arqueos) con su diferencia. */
export function useCashClosures(branchId?: string, page = 0, size = 8) {
  return useQuery({
    queryKey: ["cash-closures", branchId ?? "all", page, size],
    queryFn: () => apiGet<PageResponse<CashClosure>>("/api/v1/cash-sessions/history",
      { branchId, page: String(page), size: String(size) }),
  });
}
