"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

export interface Client {
  tenantId: string; code: string; name: string; status: string;
  companies: number; branches: number; users: number; planCode: string | null;
}
export interface Credentials { ruc: string | null; solUser: string | null; hasPassword: boolean; }
export interface Reveal { solUser: string | null; solPass: string | null; }
export interface Plan { code: string; name: string; price: number; maxBranches?: number | null; maxUsers?: number | null; }
export interface License { id: string; planCode: string; status: string; startDate?: string | null; endDate?: string | null; }
export interface HistoryEvent { id: string; eventType: string; description: string; actorUserId: string | null; actorName: string | null; at: string; }

export function useClients() {
  return useQuery({ queryKey: ["clients"], queryFn: () => apiGet<Client[]>("/api/v1/backoffice/clients") });
}
export function useCredentials(tenantId: string) {
  return useQuery({ queryKey: ["credentials", tenantId], queryFn: () => apiGet<Credentials>(`/api/v1/backoffice/clients/${tenantId}/credentials`) });
}
export function useSaveCredentials(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { ruc?: string; solUser?: string; solPass?: string }) =>
      apiPut<Credentials>(`/api/v1/backoffice/clients/${tenantId}/credentials`, b),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["credentials", tenantId] });
      qc.invalidateQueries({ queryKey: ["history", tenantId] });
    },
  });
}
export function useRevealCredentials(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiPost<Reveal>(`/api/v1/backoffice/clients/${tenantId}/credentials/reveal`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["history", tenantId] }),
  });
}
export function useBusinessPlans() {
  return useQuery({ queryKey: ["biz-plans"], queryFn: () => apiGet<Plan[]>("/api/v1/billing/plans", { line: "BUSINESS" }) });
}
export function useTenantLicenses(tenantId: string) {
  return useQuery({ queryKey: ["licenses", tenantId], queryFn: () => apiGet<License[]>("/api/v1/backoffice/licenses", { tenantId }) });
}
export function useAssignLicense(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (planCode: string) => apiPost<License>("/api/v1/backoffice/licenses", { tenantId, planCode }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["licenses", tenantId] });
      qc.invalidateQueries({ queryKey: ["clients"] });
      qc.invalidateQueries({ queryKey: ["history", tenantId] });
    },
  });
}
export function useClientHistory(tenantId: string) {
  return useQuery({ queryKey: ["history", tenantId], queryFn: () => apiGet<HistoryEvent[]>(`/api/v1/backoffice/clients/${tenantId}/history`) });
}
