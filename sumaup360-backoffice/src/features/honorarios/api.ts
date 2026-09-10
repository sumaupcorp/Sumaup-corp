"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";

export interface Attachment { id: string; fileUrl: string; fileName: string | null; createdAt: string | null; }

// --- Recibos por Honorarios ---

const HON_BASE = "/api/v1/backoffice/honorarios";

export interface HonorarioRow {
  id: string; clienteNombre: string; clienteDocType: string | null; clienteDocNumber: string | null;
  descripcion: string; monto: number; conRetencion: boolean; estado: string;
  reciboUrl: string | null; observacion: string | null; createdAt: string | null; completedAt: string | null;
}

export const HONORARIO_STATES = [
  "PENDIENTE", "EN_PROCESO", "GENERADO", "OBSERVADO", "CANCELADO",
] as const;

export function useHonorarios(estado: string) {
  return useQuery({
    queryKey: ["honorarios", estado],
    queryFn: () => apiGet<HonorarioRow[]>(HON_BASE, { estado: estado || undefined }),
  });
}

export function useSetHonorarioStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; estado: string; observacion?: string }) =>
      apiPost<HonorarioRow>(`${HON_BASE}/${v.id}/status`, { estado: v.estado, observacion: v.observacion }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["honorarios"] }),
  });
}

export function useHonorarioAttachments(id: string | null) {
  return useQuery({
    enabled: !!id,
    queryKey: ["honorario-att", id],
    queryFn: () => apiGet<Attachment[]>(`${HON_BASE}/${id}/attachments`),
  });
}

export function useHonorarioAttach() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; fileUrl: string; fileName?: string }) =>
      apiPost<Attachment>(`${HON_BASE}/${v.id}/attach`, { fileUrl: v.fileUrl, fileName: v.fileName }),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["honorario-att", v.id] }),
  });
}

// --- Suspension de 4ta ---

const SUS_BASE = "/api/v1/backoffice/suspensiones";

export interface SuspensionRow {
  id: string; anio: number; estado: string; observacion: string | null;
  constanciaUrl: string | null; createdAt: string | null; completedAt: string | null;
}

export const SUSPENSION_STATES = [
  "SOLICITADA", "EN_PROCESO", "TRAMITADA", "OBSERVADA", "CANCELADA",
] as const;

export function useSuspensiones(estado: string) {
  return useQuery({
    queryKey: ["suspensiones", estado],
    queryFn: () => apiGet<SuspensionRow[]>(SUS_BASE, { estado: estado || undefined }),
  });
}

export function useSetSuspensionStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; estado: string; observacion?: string }) =>
      apiPost<SuspensionRow>(`${SUS_BASE}/${v.id}/status`, { estado: v.estado, observacion: v.observacion }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["suspensiones"] }),
  });
}

export function useSuspensionAttachments(id: string | null) {
  return useQuery({
    enabled: !!id,
    queryKey: ["suspension-att", id],
    queryFn: () => apiGet<Attachment[]>(`${SUS_BASE}/${id}/attachments`),
  });
}

export function useSuspensionAttach() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; fileUrl: string; fileName?: string }) =>
      apiPost<Attachment>(`${SUS_BASE}/${v.id}/attach`, { fileUrl: v.fileUrl, fileName: v.fileName }),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["suspension-att", v.id] }),
  });
}
