"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api";

const BASE = "/api/v1/backoffice/notifications";

export type CampaignStatus = "SCHEDULED" | "SENDING" | "SENT" | "CANCELLED";

export type CampaignAudience =
  | "ALL"
  | "TAXISTA"
  | "DELIVERY_PEYA"
  | "SERVICIOS_PROFESIONALES"
  | "ONBOARDING_INCOMPLETE"
  | "ORIENTATION_PENDING";

export interface NotificationCampaign {
  id: string;
  title: string;
  body: string;
  route: string | null;
  audience: CampaignAudience;
  scheduledAt: string | null;
  status: CampaignStatus;
  sentCount: number;
  failedCount: number;
  createdAt: string;
  sentAt: string | null;
}

export interface CreateCampaignBody {
  title: string;
  body: string;
  route?: string;
  audience: CampaignAudience;
  scheduledAt?: string;
}

export type NotificationSettingKey =
  | "RECEIPT_PROCESSED"
  | "RECEIPT_OBSERVED"
  | "REMINDER_ONBOARDING"
  | "REMINDER_ORIENTATION"
  | "REMINDER_RECEIPTS_OBSERVED";

export interface NotificationSetting {
  key: NotificationSettingKey;
  enabled: boolean;
  title: string;
  body: string;
  description: string;
  updatedAt: string;
}

export function useCampaigns() {
  return useQuery({
    queryKey: ["notification-campaigns"],
    queryFn: () => apiGet<NotificationCampaign[]>(`${BASE}/campaigns`),
  });
}

export function useCreateCampaign() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: CreateCampaignBody) => apiPost<NotificationCampaign>(`${BASE}/campaigns`, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notification-campaigns"] }),
  });
}

export function useCancelCampaign() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiPost<NotificationCampaign>(`${BASE}/campaigns/${id}/cancel`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notification-campaigns"] }),
  });
}

export function useNotificationSettings() {
  return useQuery({
    queryKey: ["notification-settings"],
    queryFn: () => apiGet<NotificationSetting[]>(`${BASE}/settings`),
  });
}

export function useUpdateSetting() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { key: NotificationSettingKey; enabled: boolean; title: string; body: string }) =>
      apiPut<NotificationSetting>(`${BASE}/settings/${v.key}`, {
        enabled: v.enabled,
        title: v.title,
        body: v.body,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notification-settings"] }),
  });
}
