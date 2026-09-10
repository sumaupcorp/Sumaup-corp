"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";
import { useCompany } from "@/features/companies/company-context";

export interface Customer {
  id: string;
  name: string;
  docType?: string | null;
  docNumber?: string | null;
  email?: string | null;
  phone?: string | null;
}

export interface CreateCustomerInput {
  name: string;
  docType?: string;
  docNumber?: string;
  email?: string;
  phone?: string;
}

/** Clientes de la EMPRESA actual (aislado por negocio: la empresa sale del contexto). */
export function useCustomers() {
  const { currentCompany } = useCompany();
  const companyId = currentCompany?.id;
  return useQuery({
    queryKey: ["customers", companyId],
    queryFn: () => apiGet<Customer[]>("/api/v1/customers", { companyId }),
    enabled: !!companyId,
  });
}

export interface AppointmentBrief {
  id: string;
  scheduledAt: string;
  status: string;
  reason?: string | null;
  patientName?: string | null;
}

export interface SaleBrief {
  id: string;
  createdAt: string;
  total: number;
  paymentMethod: string;
  itemCount: number;
}

/** Ficha del cliente: estadisticas de atencion + historial reciente en una llamada. */
export interface CustomerSummary {
  customer: Customer;
  totalAppointments: number;
  attended: number;
  canceled: number;
  noShow: number;
  upcoming: number;
  totalPurchases: number;
  totalSpent: number;
  lastVisitAt: string | null;
  patients: string[];
  recentAppointments: AppointmentBrief[];
  recentSales: SaleBrief[];
}

export function useCustomerSummary(customerId?: string) {
  return useQuery({
    queryKey: ["customer-summary", customerId],
    queryFn: () => apiGet<CustomerSummary>(`/api/v1/customers/${customerId}/summary`),
    enabled: !!customerId,
  });
}

export function useCreateCustomer() {
  const { currentCompany } = useCompany();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateCustomerInput) =>
      apiPost<Customer>("/api/v1/customers", { companyId: currentCompany?.id, ...input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["customers"] }),
  });
}
