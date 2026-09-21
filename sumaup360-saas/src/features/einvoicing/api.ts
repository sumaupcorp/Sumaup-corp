"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

/** Config de facturacion electronica (NubeFact) por empresa. El token nunca se devuelve. */
export interface EinvoicingConfig {
  companyId: string;
  provider: string;
  ruta: string | null;
  enabled: boolean;
  hasToken: boolean;
}

/** Resultado de emitir un comprobante ante SUNAT via NubeFact. */
export interface EmitResult {
  fullNumber: string | null;
  documentType: string;
  sunatStatus: string | null;
  pdfUrl: string | null;
  xmlUrl: string | null;
  cdrUrl: string | null;
  qrValue: string | null;
  hashValue: string | null;
  accepted: boolean;
  message: string | null;
}

export function useEinvoicingConfig(companyId?: string) {
  return useQuery({
    queryKey: ["einvoicing-config", companyId],
    queryFn: () => apiGet<EinvoicingConfig>("/api/v1/erp/einvoicing/config", { companyId }),
    enabled: !!companyId,
  });
}

export function useSaveEinvoicingConfig(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { ruta?: string; token?: string; enabled: boolean }) =>
      apiPut<EinvoicingConfig>("/api/v1/erp/einvoicing/config", { companyId, ...input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["einvoicing-config", companyId] }),
  });
}

/** Emite el comprobante electronico de una venta. */
export function useEmitSale() {
  return useMutation({
    mutationFn: (saleId: string) =>
      apiPost<EmitResult>(`/api/v1/erp/einvoicing/sales/${saleId}/emit`),
  });
}
