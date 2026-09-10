"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

const BASE = "/api/v1/backoffice/persons-admin";

export type OrientationStatus = "PENDING" | "COMPLETED" | "NOT_REQUIRED";

export interface PersonRow {
  userId: string; email: string | null; displayName: string | null;
  dni: string | null; ruc: string | null; clientType: string | null;
  profileCompleted: boolean; planCode: string | null; premium: boolean;
  diagnosisAvailable: boolean;
  orientationStatus: OrientationStatus | null;
}
export interface PersonPlan { planCode: string | null; premium: boolean; }
export interface AiUsage { used: number; limit: number; remaining: number; blocked: boolean; periodo: string | null; premium: boolean; }
export interface AiConfig { freeIniciales: number; freeIa: number; premiumConsultas: number; periodo: string; activo: boolean; }
export interface AiCampaign {
  id: string; nombre: string; clientType: string | null; planCode: string | null;
  consultasExtra: number; fechaInicio: string; fechaFin: string; activo: boolean;
}
export interface PersonPlanOption { code: string; name: string; price: number; }

export function usePersons(query: string) {
  return useQuery({
    queryKey: ["persons", query],
    queryFn: () => apiGet<PersonRow[]>(`${BASE}/users`, { query: query || undefined }),
  });
}

/** Planes de la Linea Personas (taxi/peya). */
export function usePersonPlans() {
  return useQuery({
    queryKey: ["person-plans"],
    queryFn: () => apiGet<PersonPlanOption[]>("/api/v1/billing/plans", { line: "PERSON" }),
  });
}

export function useActivatePremium() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { userId: string; planCode: string; months: number }) =>
      apiPost<PersonPlan>(`${BASE}/${v.userId}/premium`, { planCode: v.planCode, months: v.months }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["persons"] }),
  });
}

export function useCancelPremium() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => apiPost<PersonPlan>(`${BASE}/${userId}/premium/cancel`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["persons"] }),
  });
}

/** Reasigna el rubro/tipo de cliente de una persona (soporte/admin). */
export function useUpdateClientType() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { userId: string; clientType: string }) =>
      apiPost<PersonRow>(`${BASE}/${v.userId}/client-type`, { clientType: v.clientType }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["persons"] }),
  });
}

/** Rehabilita el intento de diagnostico con IA de una persona (soporte). */
export function useResetDiagnosis() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => apiPost<void>(`${BASE}/${userId}/diagnosis/reset`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["persons"] }),
  });
}

/** Reactiva la orientacion tributaria de una persona (soporte). */
export function useResetOrientation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) =>
      apiPost<{ userId: string; orientationStatus: OrientationStatus }>(`${BASE}/${userId}/orientation/reset`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["persons"] }),
  });
}

export function usePersonAiUsage(userId: string | null) {
  return useQuery({
    enabled: !!userId,
    queryKey: ["person-ai-usage", userId],
    queryFn: () => apiGet<AiUsage>(`${BASE}/${userId}/ai-usage`),
  });
}

export function useAiConfig() {
  return useQuery({ queryKey: ["ai-config"], queryFn: () => apiGet<AiConfig>(`${BASE}/ai/config`) });
}

export function useUpdateAiConfig() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: Partial<AiConfig>) => apiPut<AiConfig>(`${BASE}/ai/config`, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ai-config"] }),
  });
}

export interface AiPrompt {
  chatSystem: string; diagnosisSystem: string;
  taxiContext: string | null; peyaContext: string | null; servContext: string | null;
  temperature: number; maxTokens: number;
}

export function useAiPrompt() {
  return useQuery({ queryKey: ["ai-prompt"], queryFn: () => apiGet<AiPrompt>(`${BASE}/ai/prompt`) });
}

export function useUpdateAiPrompt() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: Partial<AiPrompt>) => apiPut<AiPrompt>(`${BASE}/ai/prompt`, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ai-prompt"] }),
  });
}

export function useCampaigns() {
  return useQuery({ queryKey: ["ai-campaigns"], queryFn: () => apiGet<AiCampaign[]>(`${BASE}/ai/campaigns`) });
}

export function useToggleCampaign() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; value: boolean }) =>
      apiPut<AiCampaign>(`${BASE}/ai/campaigns/${v.id}/active?value=${v.value}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ai-campaigns"] }),
  });
}
