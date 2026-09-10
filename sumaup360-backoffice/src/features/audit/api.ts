"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api";

export interface AuditEvent {
  id: string; occurredAt: string; actorUserId: string | null;
  actorType: string | null; action: string | null; status: number | null; tenantId: string | null;
}

export function useAudit() {
  return useQuery({ queryKey: ["audit"], queryFn: () => apiGet<AuditEvent[]>("/api/v1/backoffice/audit") });
}
