"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

const BASE = "/api/v1/backoffice/catalog/products";

export interface MasterProduct {
  id: string;
  ean: string | null;
  name: string;
  brand: string | null;
  category: string | null;
  presentation: string | null;
  photoUrl: string | null;
  photoExternalUrl: string | null;
  photoSource: string | null;
  photoSourceUrl: string | null;
  verified: boolean;
  active: boolean;
  rubros: string[];
}

export interface SaveMasterProduct {
  ean?: string | null;
  name: string;
  brand?: string | null;
  category?: string | null;
  presentation?: string | null;
  photoUrl?: string | null;
  photoExternalUrl?: string | null;
  photoSource?: string | null;
  photoSourceUrl?: string | null;
  verified?: boolean;
  active?: boolean;
  rubros: string[];
}

export const PHOTO_SOURCES = [
  { code: "fabricante", label: "Web del fabricante" },
  { code: "openfoodfacts", label: "Open Food Facts" },
  { code: "staff", label: "Foto propia (staff)" },
  { code: "tenant", label: "Aportada por un negocio" },
] as const;

export function useBusinessTypes() {
  return useQuery({
    queryKey: ["business-types"],
    queryFn: () => apiGet<{ code: string; name: string }[]>("/api/v1/catalog/business-types"),
    staleTime: 5 * 60 * 1000,
  });
}

export function useMasterProducts(q: string, rubro: string) {
  return useQuery({
    queryKey: ["master-products", q, rubro],
    queryFn: () => apiGet<MasterProduct[]>(BASE, { q: q || undefined, rubro: rubro || undefined }),
  });
}

export function useCreateMasterProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: SaveMasterProduct) => apiPost<MasterProduct>(BASE, i),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["master-products"] }),
  });
}

export function useUpdateMasterProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string } & SaveMasterProduct) =>
      apiPut<MasterProduct>(`${BASE}/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["master-products"] }),
  });
}

// ---- Propuestas de los negocios (crowdsourcing con curacion) ----

const PROPOSALS = "/api/v1/backoffice/catalog/proposals";

export interface CatalogProposal {
  id: string;
  name: string;
  ean: string | null;
  category: string | null;
  businessType: string | null;
  createdAt: string;
}

export function useCatalogProposals() {
  return useQuery({
    queryKey: ["catalog-proposals"],
    queryFn: () => apiGet<CatalogProposal[]>(PROPOSALS),
  });
}

export function useApproveProposal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiPost<MasterProduct>(`${PROPOSALS}/${id}/approve`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["catalog-proposals"] });
      qc.invalidateQueries({ queryKey: ["master-products"] });
    },
  });
}

export function useRejectProposal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiPost<void>(`${PROPOSALS}/${id}/reject`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["catalog-proposals"] }),
  });
}
