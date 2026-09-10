"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut, apiPatch } from "@/lib/api";

const A = "/api/v1/erp/appointments";
const BP = "/api/v1/erp/booking-page";

export type AppointmentStatus =
  | "REQUESTED" | "SCHEDULED" | "CONFIRMED" | "COMPLETED" | "CANCELED" | "NO_SHOW";

export interface Appointment {
  id: string;
  branchId: string;
  customerId: string;
  customerName?: string | null;
  patientId?: string | null;
  patientName?: string | null;
  reason?: string | null;
  scheduledAt: string;
  durationMinutes: number;
  status: AppointmentStatus;
  notes?: string | null;
  source: "INTERNAL" | "ONLINE";
  formData?: string | null;
  ticketCode?: string | null;
}

/** Busqueda puntual por codigo de ticket (lanza error si no existe). */
export function fetchAppointmentByTicket(code: string) {
  return apiGet<Appointment>(`${A}/ticket/${encodeURIComponent(code.trim().toUpperCase())}`);
}

export interface BookingField {
  key: string;
  label: string;
  type: "text" | "phone" | "email" | "textarea";
  required: boolean;
}

export interface QrStyle {
  preset: "clasico" | "redondeado" | "puntos" | "elegante";
  dotColor: string;
  bgColor: string;
  withLogo: boolean;
}

export interface BookingPage {
  id: string;
  companyId: string;
  token: string;
  enabled: boolean;
  title?: string | null;
  logoUrl?: string | null;
  welcomeText?: string | null;
  formConfig: BookingField[];
  qrStyle?: QrStyle | null;
}

export function useAppointments(branchId?: string, from?: string, to?: string) {
  return useQuery({
    queryKey: ["appointments", branchId ?? "all", from, to],
    queryFn: () => apiGet<Appointment[]>(A, { branchId, from, to }),
  });
}

export function useCreateAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      branchId: string; customerId: string; patientId?: string; reason?: string;
      scheduledAt: string; durationMinutes?: number; notes?: string;
    }) => apiPost<Appointment>(A, i),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments"] }),
  });
}

export function useUpdateAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; scheduledAt?: string; durationMinutes?: number; reason?: string; notes?: string; patientId?: string }) =>
      apiPut<Appointment>(`${A}/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments"] }),
  });
}

export function useSetAppointmentStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; status: AppointmentStatus }) =>
      apiPatch<Appointment>(`${A}/${i.id}/status`, { status: i.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments"] }),
  });
}

// ---- Pagina publica de reserva (QR + formulario dinamico) ----

export function useBookingPage(companyId?: string) {
  return useQuery({
    queryKey: ["booking-page", companyId],
    queryFn: () => apiGet<BookingPage>(BP, { companyId }),
    enabled: !!companyId,
  });
}

export function useUpdateBookingPage(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      enabled?: boolean; title?: string; logoUrl?: string;
      welcomeText?: string; formConfig?: BookingField[]; qrStyle?: QrStyle;
    }) => apiPut<BookingPage>(`${BP}?companyId=${companyId}`, i),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["booking-page", companyId] }),
  });
}

export function useRegenerateBookingToken(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiPost<BookingPage>(`${BP}/regenerate-token?companyId=${companyId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["booking-page", companyId] }),
  });
}
