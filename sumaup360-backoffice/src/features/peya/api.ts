"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";

const BASE = "/api/v1/backoffice/peya";

export interface PeyaUpload {
  id: string; ruc: string | null; periodo: string; pdfUrl: string; estado: string;
  observacionUsuario: string | null; observacionBackoffice: string | null; codigoNps: string | null;
  createdAt: string | null; completedAt: string | null;
}
export interface Attachment { id: string; fileUrl: string; fileName: string | null; createdAt: string | null; }

export const PEYA_STATES = [
  "PDF_SUBIDO", "PENDIENTE_BACKOFFICE", "EN_PROCESO", "OBSERVADO", "COMPLETADO", "CANCELADO",
] as const;

export function usePeyaUploads(estado: string) {
  return useQuery({
    queryKey: ["peya", estado],
    queryFn: () => apiGet<PeyaUpload[]>(BASE, { estado: estado || undefined }),
  });
}

export function useSetPeyaStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; estado: string; observacion?: string }) =>
      apiPost<PeyaUpload>(`${BASE}/${v.id}/status`, { estado: v.estado, observacion: v.observacion }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["peya"] }),
  });
}

export function useSetNps() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; codigoNps: string }) => apiPost<PeyaUpload>(`${BASE}/${v.id}/nps`, { codigoNps: v.codigoNps }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["peya"] }),
  });
}

export function usePeyaAttachments(id: string | null) {
  return useQuery({
    enabled: !!id,
    queryKey: ["peya-att", id],
    queryFn: () => apiGet<Attachment[]>(`${BASE}/${id}/attachments`),
  });
}

export function usePeyaAttach() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; fileUrl: string; fileName?: string }) =>
      apiPost<Attachment>(`${BASE}/${v.id}/attach`, { fileUrl: v.fileUrl, fileName: v.fileName }),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["peya-att", v.id] }),
  });
}
