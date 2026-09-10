"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useSession } from "@/features/auth/session";
import { useCompanies, useEnabledModules, type Company } from "./api";

const STORAGE_KEY = "sumaup.companyId";

interface CompanyState {
  companies: Company[];
  currentCompany: Company | null;
  setCurrentCompanyId: (id: string) => void;
  enabledModules: string[];
  hasModule: (code: string) => boolean;
  loading: boolean;
}

const CompanyContext = createContext<CompanyState | undefined>(undefined);

export function CompanyProvider({ children }: { children: React.ReactNode }) {
  const { data: session } = useSession();
  const hasTenant = !!session?.tenantId;
  const { data: companies = [], isLoading } = useCompanies(hasTenant);
  const [currentId, setCurrentId] = useState<string | null>(null);

  // Selecciona empresa: la guardada o la primera disponible.
  useEffect(() => {
    if (companies.length === 0) return;
    const saved = typeof window !== "undefined" ? window.localStorage.getItem(STORAGE_KEY) : null;
    const valid = saved && companies.some((c) => c.id === saved) ? saved : companies[0].id;
    setCurrentId(valid);
  }, [companies]);

  const setCurrentCompanyId = (id: string) => {
    setCurrentId(id);
    if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, id);
  };

  const currentCompany = useMemo(
    () => companies.find((c) => c.id === currentId) ?? null,
    [companies, currentId]
  );

  const { data: enabledModules = [] } = useEnabledModules(currentCompany?.id);

  const value: CompanyState = {
    companies,
    currentCompany,
    setCurrentCompanyId,
    enabledModules,
    hasModule: (code) => enabledModules.includes(code),
    loading: hasTenant && isLoading,
  };

  return <CompanyContext.Provider value={value}>{children}</CompanyContext.Provider>;
}

export function useCompany(): CompanyState {
  const ctx = useContext(CompanyContext);
  if (!ctx) throw new Error("useCompany debe usarse dentro de CompanyProvider");
  return ctx;
}
