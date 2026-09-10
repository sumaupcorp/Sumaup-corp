"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

export interface Company {
  id: string;
  tenantId: string;
  legalName: string;
  ruc?: string | null;
  businessTypeCode?: string | null;
  verticalCode?: string | null;
}

export interface CreateCompanyInput {
  legalName: string;
  ruc?: string;
  businessTypeCode?: string;
  verticalCode?: string;
}

export interface CompanyModule {
  moduleCode: string;
  name: string;
  enabled: boolean;
  source: string;
  core: boolean;
}

export function useCompanies(enabled = true) {
  return useQuery({
    queryKey: ["companies"],
    queryFn: () => apiGet<Company[]>("/api/v1/companies"),
    enabled,
  });
}

export function useCreateCompany() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateCompanyInput) => apiPost<Company>("/api/v1/companies", input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["companies"] }),
  });
}

export function useEnabledModules(companyId?: string) {
  return useQuery({
    queryKey: ["enabled-modules", companyId],
    queryFn: () => apiGet<string[]>(`/api/v1/erp/companies/${companyId}/modules/enabled`),
    enabled: !!companyId,
  });
}

export function useCompanyModules(companyId?: string) {
  return useQuery({
    queryKey: ["company-modules", companyId],
    queryFn: () => apiGet<CompanyModule[]>(`/api/v1/erp/companies/${companyId}/modules`),
    enabled: !!companyId,
  });
}

export function useSetModule(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { moduleCode: string; enabled: boolean }) =>
      apiPut<CompanyModule>(`/api/v1/erp/companies/${companyId}/modules/${vars.moduleCode}`, {
        enabled: vars.enabled,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["company-modules", companyId] });
      qc.invalidateQueries({ queryKey: ["enabled-modules", companyId] });
    },
  });
}
