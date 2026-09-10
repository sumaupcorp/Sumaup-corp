"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api";

export interface CodeName {
  code: string;
  name: string;
}
export interface VerticalView {
  code: string;
  name: string;
  businessTypeCode: string;
}

export function useBusinessTypes() {
  return useQuery({
    queryKey: ["catalog", "business-types"],
    queryFn: () => apiGet<CodeName[]>("/api/v1/catalog/business-types"),
    staleTime: 5 * 60_000,
  });
}

export function useVerticals(businessType?: string) {
  return useQuery({
    queryKey: ["catalog", "verticals", businessType],
    queryFn: () => apiGet<VerticalView[]>("/api/v1/catalog/verticals", { businessType }),
    enabled: !!businessType,
    staleTime: 5 * 60_000,
  });
}

/** Producto del catalogo maestro SUMAUP (solo lectura para el tenant). */
export interface CatalogProduct {
  id: string;
  ean: string | null;
  name: string;
  brand: string | null;
  category: string | null;
  presentation: string | null;
  photoUrl: string | null;
  photoExternalUrl: string | null;
}

/** Busca/navega el catalogo maestro segun el rubro de la empresa (q vacio = lista inicial). */
export function useCatalogSearch(companyId: string | undefined, q: string, enabled = true) {
  const query = q.trim();
  return useQuery({
    queryKey: ["catalog", "master-search", companyId, query],
    queryFn: () => apiGet<CatalogProduct[]>("/api/v1/erp/catalog/products",
      { companyId, q: query || undefined }),
    enabled: enabled && !!companyId,
    staleTime: 60_000,
  });
}
