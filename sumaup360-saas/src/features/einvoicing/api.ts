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
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (saleId: string) =>
      apiPost<EmitResult>(`/api/v1/erp/einvoicing/sales/${saleId}/emit`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["einvoicing-documents"] }),
  });
}

/** Un comprobante emitido (historial). */
export interface IssuedDocument {
  id: string;
  fullNumber: string | null;
  documentType: string;
  sunatStatus: string | null;
  documentStatus: string | null;
  total: number;
  currency: string;
  customerName: string | null;
  issuedAt: string | null;
  pdfUrl: string | null;
  xmlUrl: string | null;
  saleId: string | null;
}

/** Historial de comprobantes electronicos emitidos por la empresa. */
export function useIssuedDocuments(companyId?: string) {
  return useQuery({
    queryKey: ["einvoicing-documents", companyId],
    queryFn: () => apiGet<IssuedDocument[]>("/api/v1/erp/einvoicing/documents", { companyId }),
    enabled: !!companyId,
  });
}
