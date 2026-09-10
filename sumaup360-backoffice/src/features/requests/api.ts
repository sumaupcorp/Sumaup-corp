"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiDelete } from "@/lib/api";

const BASE = "/api/v1/backoffice/requests";

export interface ReceiptRequest {
  id: string; tipo: string | null; monto: number | null; montoEditado: number | null;
  docType: string | null; docNumber: string | null; customerName: string | null;
  whatsapp: string | null; email: string | null; observacion: string | null;
  estado: string | null; motivo: string | null; createdAt: string | null; completedAt: string | null;
}
export interface Attachment { id: string; fileUrl: string; fileName: string | null; createdAt: string | null; }

export const REQUEST_STATES = [
  "PENDIENTE_TAXISTA", "CONFIRMADO_TAXISTA", "MONTO_EDITADO_CONFIRMADO", "RECHAZADO_TAXISTA",
  "PENDIENTE_BACKOFFICE", "EN_PROCESO_BACKOFFICE", "COMPLETADO", "OBSERVADO", "CANCELADO",
] as const;

export function useRequests(estado: string) {
  return useQuery({
    queryKey: ["requests", estado],
    queryFn: () => apiGet<ReceiptRequest[]>(BASE, { estado: estado || undefined }),
  });
}

export function useSetStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; estado: string; motivo?: string }) =>
      apiPost<ReceiptRequest>(`${BASE}/${v.id}/status`, { estado: v.estado, motivo: v.motivo }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["requests"] }),
  });
}

export function useAttachments(id: string | null) {
  return useQuery({
    enabled: !!id,
    queryKey: ["attachments", id],
    queryFn: () => apiGet<Attachment[]>(`${BASE}/${id}/attachments`),
  });
}

export function useAttach() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; fileUrl: string; fileName?: string }) =>
      apiPost<Attachment>(`${BASE}/${v.id}/attach`, { fileUrl: v.fileUrl, fileName: v.fileName }),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["attachments", v.id] }),
  });
}

export function useDetach() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; attachmentId: string }) =>
      apiDelete<void>(`${BASE}/${v.id}/attachments/${v.attachmentId}`),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["attachments", v.id] }),
  });
}
