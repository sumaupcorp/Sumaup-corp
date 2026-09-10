"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api";

/** Respuestas del wizard sobre como opera el negocio (null/undefined = default del rubro). */
export interface OperationAnswers {
  sellsOnTables?: boolean;
  tracksExpiry?: boolean;
  takesAppointments?: boolean;
  takesCustomOrders?: boolean;
  handlesPrescriptions?: boolean;
  /** Hospedaje con restaurante o cafeteria adentro: activa mesas, comandas y carta. */
  sellsFood?: boolean;
}

export interface OnboardInput {
  businessName: string;
  businessTypeCode: string;
  verticalCode?: string;
  ruc?: string;
  branchNames: string[];
  operation?: OperationAnswers;
}

export interface OnboardResult {
  tenantId: string;
  companyId: string;
}

export interface RucLookupResult {
  found: boolean;
  ruc?: string | null;
  razonSocial?: string | null;
  estado?: string | null;
  condicion?: string | null;
  tipoContribuyente?: string | null;
  actividadPrincipal?: string | null;
  ciiu?: string | null;
  domicilioFiscal?: string | null;
  suggestedBusinessTypeCode?: string | null;
  detail?: string | null;
}

export function useOnboard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: OnboardInput) => apiPost<OnboardResult>("/api/v1/onboarding", input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["session"] });
      qc.invalidateQueries({ queryKey: ["companies"] });
    },
  });
}

export function useRucLookup() {
  return useMutation({
    mutationFn: (ruc: string) => apiGet<RucLookupResult>("/api/v1/onboarding/ruc-lookup", { ruc }),
  });
}
