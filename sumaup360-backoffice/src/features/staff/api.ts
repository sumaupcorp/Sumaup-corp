"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";

export interface Staff { userId: string; email: string | null; name: string | null; roles: string[]; }

export const STAFF_ROLES = ["admin", "gerencia", "contador", "desarrollador", "soporte", "logistica"];

export function useStaff() {
  return useQuery({ queryKey: ["staff"], queryFn: () => apiGet<Staff[]>("/api/v1/backoffice/staff") });
}

export function useCreateStaff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { email: string; password: string; name: string; roleCode: string }) =>
      apiPost<Staff>("/api/v1/backoffice/staff", b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["staff"] }),
  });
}
