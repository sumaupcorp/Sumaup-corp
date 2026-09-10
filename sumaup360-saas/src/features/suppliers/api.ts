"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";
import { useCompany } from "@/features/companies/company-context";

export interface Supplier {
  id: string;
  name: string;
  ruc?: string | null;
  phone?: string | null;
  email?: string | null;
  contactName?: string | null;
  address?: string | null;
  notes?: string | null;
  active: boolean;
}

export interface SupplierInput {
  name?: string;
  ruc?: string;
  phone?: string;
  email?: string;
  contactName?: string;
  address?: string;
  notes?: string;
}

/** Proveedores de la EMPRESA actual (aislado por negocio: la empresa sale del contexto). */
export function useSuppliers() {
  const { currentCompany } = useCompany();
  const companyId = currentCompany?.id;
  return useQuery({
    queryKey: ["suppliers", companyId],
    queryFn: () => apiGet<Supplier[]>("/api/v1/suppliers", { companyId }),
    enabled: !!companyId,
  });
}

export function useCreateSupplier() {
  const { currentCompany } = useCompany();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: SupplierInput & { name: string }) =>
      apiPost<Supplier>("/api/v1/suppliers", { companyId: currentCompany?.id, ...input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["suppliers"] }),
  });
}

export function useUpdateSupplier() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...input }: SupplierInput & { id: string; active?: boolean }) =>
      apiPut<Supplier>(`/api/v1/suppliers/${id}`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["suppliers"] }),
  });
}
