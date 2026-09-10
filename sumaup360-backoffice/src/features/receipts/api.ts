"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

export interface Receipt {
  id: string;
  userId: string;
  userName: string | null;
  userEmail: string | null;
  userRuc: string | null;
  userRegime: string | null;
  userHasSol: boolean;
  type: string;
  docNumber: string | null;
  issueDate: string | null;
  amount: number | null;
  currency: string;
  fileUrl: string | null;
  filePath: string | null;
  hasFile: boolean;
  issuerRuc: string | null;
  status: string;
  assignedStaffId: string | null;
  notes: string | null;
  processedAt: string | null;
  declaredSire: boolean;
  sirePeriod: string | null;
  sireDeclaredAt: string | null;
  declaredSunat: boolean;
  sunatPeriod: string | null;
  sunatDeclaredAt: string | null;
}

export interface RevealSol { solUser: string | null; solPass: string | null; }

export function useReceiptQueue(status: string) {
  return useQuery({ queryKey: ["bo-receipts", status], queryFn: () => apiGet<Receipt[]>("/api/v1/backoffice/receipts", { status }) });
}

export function useReceiptDetail(id: string) {
  return useQuery({ queryKey: ["bo-receipt", id], queryFn: () => apiGet<Receipt>(`/api/v1/backoffice/receipts/${id}`), enabled: !!id });
}

export function useReceiptsByUser(userId?: string) {
  return useQuery({ queryKey: ["bo-receipts-user", userId], queryFn: () => apiGet<Receipt[]>(`/api/v1/backoffice/receipts/by-user/${userId}`), enabled: !!userId });
}

function useInvalidate() {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ["bo-receipts"] });
    qc.invalidateQueries({ queryKey: ["bo-receipt"] });
    qc.invalidateQueries({ queryKey: ["bo-receipts-user"] });
  };
}

export function useReceiptAction() {
  const inv = useInvalidate();
  return useMutation({
    mutationFn: (v: { id: string; action: "take" | "process" | "observe"; note?: string }) =>
      apiPost(`/api/v1/backoffice/receipts/${v.id}/${v.action}`, v.note ? { note: v.note } : {}),
    onSuccess: inv,
  });
}

export function useDeclare() {
  const inv = useInvalidate();
  return useMutation({
    mutationFn: (v: { id: string; which: "sire" | "sunat"; period: string }) =>
      apiPost(`/api/v1/backoffice/receipts/${v.id}/declare-${v.which}`, { period: v.period }),
    onSuccess: inv,
  });
}

export function useUpsertUserFiscal() {
  const inv = useInvalidate();
  return useMutation({
    mutationFn: (v: { userId: string; ruc?: string; regime?: string; solUser?: string; solPass?: string }) =>
      apiPut(`/api/v1/backoffice/receipts/users/${v.userId}/fiscal`, v),
    onSuccess: inv,
  });
}

export function useRevealUserSol() {
  return useMutation({
    mutationFn: (userId: string) => apiPost<RevealSol>(`/api/v1/backoffice/receipts/users/${userId}/reveal-sol`, {}),
  });
}

export function useReceiptFile() {
  return useMutation({
    mutationFn: (id: string) => apiGet<{ url: string; expiresInSeconds: number }>(`/api/v1/backoffice/receipts/${id}/file`),
  });
}
