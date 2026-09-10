"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api";
import { useAuth } from "./auth-context";

export type UserType = "PERSON" | "BUSINESS" | "STAFF";

/** Respuesta de GET /api/v1/me — la identidad resuelta por el backend. */
export interface Session {
  userId: string;
  firebaseUid: string;
  email: string | null;
  userType: UserType;
  tenantId: string | null;
  roles: string[];
  permissions: string[];
}

/** Carga la sesion (/me) cuando hay usuario Firebase autenticado. */
export function useSession() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["session", user?.uid],
    queryFn: () => apiGet<Session>("/api/v1/me"),
    enabled: !!user,
    staleTime: 60_000,
  });
}

/** Hook de permisos: true si la sesion incluye el permiso. */
export function useHasPermission() {
  const { data } = useSession();
  const perms = new Set(data?.permissions ?? []);
  return (code: string) => perms.has(code);
}
