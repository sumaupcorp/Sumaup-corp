"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

const P = "/api/v1/erp/patients";

export interface Patient {
  id: string;
  customerId: string;
  customerName?: string | null;
  name: string;
  species?: string | null;
  breed?: string | null;
  sex?: string | null;
  birthDate?: string | null;
  weightKg?: number | null;
  notes?: string | null;
  photoUrl?: string | null;
  active: boolean;
}

export function usePatients(customerId?: string) {
  return useQuery({
    queryKey: ["patients", customerId ?? "all"],
    queryFn: () => apiGet<Patient[]>(P, { customerId }),
  });
}

export function useCreatePatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      customerId: string; name: string; species?: string; breed?: string;
      sex?: string; birthDate?: string; weightKg?: number; notes?: string; photoUrl?: string;
    }) => apiPost<Patient>(P, i),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["patients"] }),
  });
}

export function useUpdatePatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string } & Partial<Omit<Patient, "id" | "customerId" | "customerName">>) =>
      apiPut<Patient>(`${P}/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["patients"] }),
  });
}
