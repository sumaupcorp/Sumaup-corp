"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";
import { auth } from "@/lib/firebase";

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export interface Account {
  id: string;
  code: string;
  name: string;
  accountClass: number | null;
  nature: string | null;
  global: boolean;
}

export interface AccountingConfig {
  companyId: string;
  subdiarioVentas: string;
  subdiarioCompras: string;
  subdiarioDiario: string;
  cuentaPorCobrar: string;
  cuentaVentas: string;
  cuentaVentasServ: string;
  cuentaIgv: string;
  cuentaCaja: string;
  moneda: string;
  concarSeparator: string;
}

export interface JournalEntry {
  id: string;
  entryDate: string;
  subdiario: string;
  correlativo: string | null;
  glosa: string | null;
  moneda: string;
  source: string;
  totalDebe: number;
  totalHaber: number;
}

export function useAccounts(companyId?: string) {
  return useQuery({
    queryKey: ["accounting-accounts", companyId],
    queryFn: () => apiGet<Account[]>("/api/v1/erp/accounting/accounts", { companyId }),
    enabled: !!companyId,
  });
}

export function useAccountingConfig(companyId?: string) {
  return useQuery({
    queryKey: ["accounting-config", companyId],
    queryFn: () => apiGet<AccountingConfig>("/api/v1/erp/accounting/config", { companyId }),
    enabled: !!companyId,
  });
}

export function useSaveAccountingConfig(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Partial<AccountingConfig>) =>
      apiPut<AccountingConfig>("/api/v1/erp/accounting/config", { companyId, ...input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["accounting-config", companyId] }),
  });
}

export function useJournalEntries(companyId?: string, from?: string, to?: string) {
  return useQuery({
    queryKey: ["accounting-entries", companyId, from, to],
    queryFn: () =>
      apiGet<JournalEntry[]>("/api/v1/erp/accounting/entries", { companyId, from, to }),
    enabled: !!companyId && !!from && !!to,
  });
}

/** Genera el asiento contable (12/40/70) de una venta. */
export function useGenerateEntryFromSale() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (saleId: string) =>
      apiPost<JournalEntry>(`/api/v1/erp/accounting/entries/from-sale/${saleId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["accounting-entries"] }),
  });
}

/** Descarga el archivo de importacion a CONCAR del periodo (dispara la descarga en el navegador). */
export async function downloadConcarExport(
  companyId: string,
  year: number,
  month: number,
  format: "txt" | "csv",
): Promise<void> {
  const u = auth.currentUser;
  const headers: Record<string, string> = u ? { Authorization: `Bearer ${await u.getIdToken()}` } : {};
  const qs = new URLSearchParams({
    companyId,
    year: String(year),
    month: String(month),
    format,
  });
  const res = await fetch(`${BASE}/api/v1/erp/accounting/concar/export?${qs.toString()}`, { headers });
  if (!res.ok) throw new Error(`No se pudo generar la exportacion (error ${res.status}).`);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `concar_${year}${String(month).padStart(2, "0")}.${format}`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
