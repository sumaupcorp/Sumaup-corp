"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";

const P = "/api/v1/erp/prescriptions";

export interface Prescription {
  id: string;
  branchId: string;
  customerId?: string | null;
  customerName?: string | null;
  saleId?: string | null;
  doctorName: string;
  doctorLicense?: string | null;
  issuedDate: string;
  diagnosis?: string | null;
  medications: string;
  notes?: string | null;
}

export function usePrescriptions(branchId?: string) {
  return useQuery({
    queryKey: ["prescriptions", branchId ?? "all"],
    queryFn: () => apiGet<Prescription[]>(P, { branchId }),
  });
}

export function useCreatePrescription() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      branchId: string; customerId?: string; doctorName: string; doctorLicense?: string;
      issuedDate: string; diagnosis?: string; medications: string; notes?: string;
    }) => apiPost<Prescription>(P, i),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["prescriptions"] }),
  });
}
