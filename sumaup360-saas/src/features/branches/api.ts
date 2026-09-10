"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";

export interface Branch {
  id: string;
  companyId: string;
  name: string;
  address?: string | null;
  main: boolean;
}

export function useBranches(companyId?: string) {
  return useQuery({
    queryKey: ["branches", companyId],
    queryFn: () => apiGet<Branch[]>(`/api/v1/companies/${companyId}/branches`),
    enabled: !!companyId,
  });
}

export function useCreateBranch(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; address?: string; main?: boolean }) =>
      apiPost<Branch>(`/api/v1/companies/${companyId}/branches`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["branches", companyId] }),
  });
}
