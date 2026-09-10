"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";
import { useCompany } from "@/features/companies/company-context";

export interface Product {
  id: string;
  sku: string;
  name: string;
  unit: string;
  price: number;
  category?: string | null;
  active: boolean;
  masterProductId?: string | null;
  photoUrl?: string | null;
  photoExternalUrl?: string | null;
}

export interface CreateProductInput {
  sku: string;
  name: string;
  unit?: string;
  price?: number;
  category?: string;
  masterProductId?: string;
  /** Foto propia del negocio (para productos que no estan en el catalogo maestro). */
  photoUrl?: string;
  /** Empresa actual: permite proponer el producto propio al catalogo maestro SUMAUP. */
  companyId?: string;
}

/** Productos de la EMPRESA actual (aislado por negocio: la empresa sale del contexto). */
export function useProducts() {
  const { currentCompany } = useCompany();
  const companyId = currentCompany?.id;
  return useQuery({
    queryKey: ["products", companyId],
    queryFn: () => apiGet<Product[]>("/api/v1/products", { companyId }),
    enabled: !!companyId,
  });
}

export function useCreateProduct() {
  const { currentCompany } = useCompany();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateProductInput) =>
      apiPost<Product>("/api/v1/products", { companyId: currentCompany?.id, ...input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
}

export interface UpdateProductInput {
  id: string;
  name?: string;
  unit?: string;
  price?: number;
  category?: string;
  active?: boolean;
  /** Foto propia: cadena vacia la quita (vuelve a heredar la del maestro). */
  photoUrl?: string;
}

export function useUpdateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...input }: UpdateProductInput) =>
      apiPut<Product>(`/api/v1/products/${id}`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
}
