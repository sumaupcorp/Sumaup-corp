"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api";

export interface PlanCount { plan: string; count: number; }
export interface Overview {
  clients: number;
  companies: number;
  branches: number;
  users: number;
  staff: number;
  pendingReceipts: number;
  inProcessReceipts: number;
  processedReceipts: number;
  activeSubscriptions: number;
  byPlan: PlanCount[];
}

export function useOverview() {
  return useQuery({ queryKey: ["overview"], queryFn: () => apiGet<Overview>("/api/v1/backoffice/overview") });
}
