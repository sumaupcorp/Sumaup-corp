"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut, apiPatch, apiDelete, type PageResponse } from "@/lib/api";
import { useCompany } from "@/features/companies/company-context";

const E = "/api/v1/erp";

// ---------------------------------------------------------------------------
// Tipos (reflejan LodgingDtos del backend)
// ---------------------------------------------------------------------------

export type RoomStatus = "AVAILABLE" | "OCCUPIED" | "CLEANING" | "MAINTENANCE";
export type StayStatus = "RESERVED" | "CHECKED_IN" | "CHECKED_OUT" | "CANCELED" | "NO_SHOW";
/** NIGHTLY cobra por noches; HOURLY es el alquiler por horas (walk-in, mismo dia). */
export type RentalMode = "NIGHTLY" | "HOURLY";

export interface RoomType {
  id: string;
  name: string;
  capacity: number;
  ratePerNight: number;
  /** null = este tipo no se alquila por horas. */
  ratePerHour?: number | null;
  description?: string | null;
  isActive: boolean;
}

export interface Room {
  id: string;
  branchId: string;
  roomTypeId: string;
  roomTypeName?: string | null;
  ratePerNight?: number | null;
  ratePerHour?: number | null;
  capacity: number;
  number: string;
  floor?: string | null;
  status: RoomStatus;
  notes?: string | null;
  isActive: boolean;
}

export interface StaySummary {
  id: string;
  ticketCode?: string | null;
  customerId: string;
  customerName?: string | null;
  checkInDate: string;
  checkOutDate: string;
  status: StayStatus;
  guestsCount: number;
  rentalMode: RentalMode;
  hours?: number | null;
  checkedInAt?: string | null;
}

/** Habitacion del rack: estado + estadia actual (si esta ocupada) + llegada del dia. */
export interface RackRoom {
  room: Room;
  currentStay?: StaySummary | null;
  arrivingStay?: StaySummary | null;
}

export interface StayGuest {
  id: string;
  fullName: string;
  docType: string;
  docNumber: string;
  nationality: string;
}

export interface StayCharge {
  id: string;
  productId?: string | null;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  createdAt: string;
}

export interface Stay {
  id: string;
  branchId: string;
  roomId: string;
  roomNumber?: string | null;
  customerId: string;
  customerName?: string | null;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  rentalMode: RentalMode;
  hours?: number | null;
  status: StayStatus;
  /** Tarifa pactada por unidad: por noche (NIGHTLY) o por hora (HOURLY). */
  ratePerNight: number;
  guestsCount: number;
  checkedInAt?: string | null;
  checkedOutAt?: string | null;
  saleId?: string | null;
  ticketCode?: string | null;
  source: "INTERNAL" | "ONLINE";
  notes?: string | null;
  chargesTotal: number;
  guests: StayGuest[];
  charges: StayCharge[];
}

export interface GuestInput {
  fullName: string;
  docType?: string;
  docNumber: string;
  nationality?: string;
}

// ---------------------------------------------------------------------------
// Tipos de habitacion
// ---------------------------------------------------------------------------

export function useRoomTypes() {
  const { currentCompany } = useCompany();
  const companyId = currentCompany?.id;
  return useQuery({
    queryKey: ["room-types", companyId],
    queryFn: () => apiGet<RoomType[]>(`${E}/room-types`, { companyId }),
    enabled: !!companyId,
  });
}

export function useCreateRoomType() {
  const { currentCompany } = useCompany();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { name: string; capacity?: number; ratePerNight: number; ratePerHour?: number; description?: string }) =>
      apiPost<RoomType>(`${E}/room-types`, { companyId: currentCompany?.id, ...i }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["room-types"] }),
  });
}

export function useUpdateRoomType() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; name?: string; capacity?: number; ratePerNight?: number; ratePerHour?: number; description?: string; isActive?: boolean }) =>
      apiPut<RoomType>(`${E}/room-types/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["room-types"] });
      qc.invalidateQueries({ queryKey: ["rooms"] });
      qc.invalidateQueries({ queryKey: ["rack"] });
    },
  });
}

// ---------------------------------------------------------------------------
// Habitaciones
// ---------------------------------------------------------------------------

export function useRooms(branchId?: string) {
  const { currentCompany } = useCompany();
  const companyId = currentCompany?.id;
  return useQuery({
    queryKey: ["rooms", companyId, branchId ?? "all"],
    queryFn: () => apiGet<Room[]>(`${E}/rooms`, { companyId, branchId }),
    enabled: !!companyId,
  });
}

export function useCreateRoom() {
  const { currentCompany } = useCompany();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { branchId: string; roomTypeId: string; number: string; floor?: string; notes?: string }) =>
      apiPost<Room>(`${E}/rooms`, { companyId: currentCompany?.id, ...i }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rooms"] });
      qc.invalidateQueries({ queryKey: ["rack"] });
    },
  });
}

export function useUpdateRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; roomTypeId?: string; number?: string; floor?: string; notes?: string; isActive?: boolean }) =>
      apiPut<Room>(`${E}/rooms/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rooms"] });
      qc.invalidateQueries({ queryKey: ["rack"] });
    },
  });
}

export function useSetRoomStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; status: RoomStatus }) =>
      apiPatch<Room>(`${E}/rooms/${i.id}/status`, { status: i.status }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rooms"] });
      qc.invalidateQueries({ queryKey: ["rack"] });
    },
  });
}

/** Rack de la sucursal: cada habitacion con su estadia actual y la llegada del dia. */
export function useRack(branchId?: string, date?: string) {
  return useQuery({
    queryKey: ["rack", branchId, date],
    queryFn: () => apiGet<RackRoom[]>(`${E}/rooms/rack`, { branchId, date }),
    enabled: !!branchId,
    refetchInterval: 60_000,
  });
}

/** Habitaciones libres en el rango [from, to) para reservar sin solaparse. */
export function useAvailability(branchId?: string, from?: string, to?: string) {
  return useQuery({
    queryKey: ["room-availability", branchId, from, to],
    queryFn: () => apiGet<Room[]>(`${E}/rooms/availability`, { branchId, from, to }),
    enabled: !!branchId && !!from && !!to,
  });
}

// ---------------------------------------------------------------------------
// Estadias / reservas
// ---------------------------------------------------------------------------

export function useStays(opts: { branchId?: string; status?: StayStatus | ""; page: number; size?: number }) {
  const { currentCompany } = useCompany();
  const companyId = currentCompany?.id;
  return useQuery({
    queryKey: ["stays", companyId, opts.branchId ?? "all", opts.status ?? "", opts.page, opts.size ?? 20],
    queryFn: () =>
      apiGet<PageResponse<Stay>>(`${E}/stays`, {
        companyId,
        branchId: opts.branchId,
        status: opts.status || undefined,
        page: String(opts.page),
        size: String(opts.size ?? 20),
      }),
    enabled: !!companyId,
  });
}

export function useStay(id?: string) {
  return useQuery({
    queryKey: ["stay", id],
    queryFn: () => apiGet<Stay>(`${E}/stays/${id}`),
    enabled: !!id,
  });
}

/** Busqueda puntual por codigo de ticket (lanza error si no existe). */
export function fetchStayByTicket(code: string) {
  return apiGet<Stay>(`${E}/stays/ticket/${encodeURIComponent(code.trim().toUpperCase())}`);
}

function invalidateStays(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ["stays"] });
  qc.invalidateQueries({ queryKey: ["stay"] });
  qc.invalidateQueries({ queryKey: ["rack"] });
  qc.invalidateQueries({ queryKey: ["rooms"] });
  qc.invalidateQueries({ queryKey: ["room-availability"] });
}

export function useCreateStay() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      branchId: string; roomId: string; customerId: string;
      checkInDate?: string; checkOutDate?: string;
      rentalMode?: RentalMode; hours?: number; checkInNow?: boolean;
      guests?: GuestInput[];
      ratePerNight?: number; guestsCount?: number; notes?: string;
    }) => apiPost<Stay>(`${E}/stays`, i),
    onSuccess: () => invalidateStays(qc),
  });
}

export function useUpdateStay() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: {
      id: string; roomId?: string; checkInDate?: string; checkOutDate?: string;
      hours?: number; ratePerNight?: number; guestsCount?: number; notes?: string;
    }) => apiPut<Stay>(`${E}/stays/${i.id}`, { ...i, id: undefined }),
    onSuccess: () => invalidateStays(qc),
  });
}

export function useCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; guests?: GuestInput[] }) =>
      apiPost<Stay>(`${E}/stays/${i.id}/check-in`, { guests: i.guests ?? [] }),
    onSuccess: () => invalidateStays(qc),
  });
}

export function useAddCharge() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { stayId: string; productId?: string; description?: string; quantity: number; unitPrice: number }) =>
      apiPost<StayCharge>(`${E}/stays/${i.stayId}/charges`, { ...i, stayId: undefined }),
    onSuccess: () => invalidateStays(qc),
  });
}

export function useRemoveCharge() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { stayId: string; chargeId: string }) =>
      apiDelete<void>(`${E}/stays/${i.stayId}/charges/${i.chargeId}`),
    onSuccess: () => invalidateStays(qc),
  });
}

/** Check-out: el backend liquida noches + cargos como venta POS (exige caja abierta). */
export function useCheckOut() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; paymentMethod: string }) =>
      apiPost<Stay>(`${E}/stays/${i.id}/check-out`, { paymentMethod: i.paymentMethod }),
    onSuccess: () => {
      invalidateStays(qc);
      qc.invalidateQueries({ queryKey: ["sales"] });
      qc.invalidateQueries({ queryKey: ["cash"] });
    },
  });
}

/** Solo CANCELED o NO_SHOW; el resto de estados los fijan check-in y check-out. */
export function useSetStayStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (i: { id: string; status: StayStatus; reason?: string }) =>
      apiPatch<Stay>(`${E}/stays/${i.id}/status`, { status: i.status, reason: i.reason }),
    onSuccess: () => invalidateStays(qc),
  });
}
