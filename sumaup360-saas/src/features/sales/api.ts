"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, apiGet, apiPost, type PageResponse } from "@/lib/api";
import { auth } from "@/lib/firebase";

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

// ---------------------------------------------------------------------------
// Tipos
// ---------------------------------------------------------------------------

export type PaymentMethod = "CASH" | "CARD" | "YAPE" | "PLIN" | "TRANSFER";

export const PAYMENT_METHODS: { code: PaymentMethod; label: string }[] = [
  { code: "CASH", label: "Efectivo" },
  { code: "CARD", label: "Tarjeta" },
  { code: "YAPE", label: "Yape" },
  { code: "PLIN", label: "Plin" },
  { code: "TRANSFER", label: "Transferencia" },
];

export const PAYMENT_LABELS = new Map(PAYMENT_METHODS.map((m) => [m.code as string, m.label]));

export interface CashSession {
  id: string;
  branchId: string;
  status: string;
  openingAmount: number;
  closingAmount?: number | null;
  expectedAmount?: number | null;
  difference?: number | null;
  notes?: string | null;
  openedAt?: string | null;
  closedAt?: string | null;
}

export interface CashMovement {
  id: string;
  type: "INCOME" | "EXPENSE";
  category: string;
  concept: string;
  amount: number;
  createdAt: string;
}

/** Resumen vivo de la caja: lo que se muestra en el panel y en el arqueo. */
export interface CashSummary {
  sessionId: string;
  status: string;
  openedAt: string | null;
  openedByName: string | null;
  openingAmount: number;
  salesTotal: number;
  salesCount: number;
  cashSales: number;
  salesByMethod: Record<string, number>;
  incomesTotal: number;
  expensesTotal: number;
  expectedCash: number;
  closingAmount: number | null;
  difference: number | null;
  movements: CashMovement[];
}

/** Categorias de salida de efectivo (modelo de caja chica peruana). */
export const EXPENSE_CATEGORIES: { code: string; label: string; description: string }[] = [
  { code: "COMPRA", label: "Compra menor", description: "Cosas compradas con dinero de la caja: bolsas, utiles, insumos de emergencia." },
  { code: "SERVICIO", label: "Pago de servicio", description: "Luz, agua, internet u otro servicio pagado en efectivo desde la caja." },
  { code: "PROVEEDOR", label: "Pago a proveedor", description: "Pago en efectivo a un proveedor que cobra contra entrega." },
  { code: "MOVILIDAD", label: "Movilidad / pasajes", description: "Taxi, mototaxi o pasajes pagados con dinero de la caja." },
  { code: "RETIRO", label: "Retiro del dueno", description: "Dinero que el dueno o administrador retira de la caja. Queda registrado con fecha y responsable." },
  { code: "REMESA", label: "Deposito al banco", description: "Efectivo que se saca de la caja para llevarlo al banco o a la boveda por seguridad." },
  { code: "OTRO", label: "Otra salida", description: "Cualquier otra salida de efectivo. Describe bien el motivo." },
];

/** Categorias de ingreso de efectivo distinto de ventas. */
export const INCOME_CATEGORIES: { code: string; label: string; description: string }[] = [
  { code: "SENCILLO", label: "Ingreso de sencillo", description: "Efectivo que se agrega a la caja para poder dar vuelto." },
  { code: "COBRANZA", label: "Cobranza", description: "Cobro en efectivo de una deuda o credito de un cliente." },
  { code: "OTRO", label: "Otro ingreso", description: "Cualquier otro ingreso de efectivo. Describe bien el origen." },
];

export interface SaleItemInput {
  productId: string;
  quantity: number;
}

export interface Sale {
  id: string;
  branchId: string;
  cashSessionId: string | null;
  total: number;
  status: string;
  paymentMethod: PaymentMethod;
  createdAt: string;
  items: { productId: string; quantity: number; unitPrice: number; lineTotal: number }[];
}

// ---------------------------------------------------------------------------
// Caja
// ---------------------------------------------------------------------------

export function useOpenCash(branchId?: string) {
  return useQuery({
    queryKey: ["cash-open", branchId],
    queryFn: async () => {
      try {
        return await apiGet<CashSession>("/api/v1/cash-sessions/open", { branchId });
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) return null;
        throw e;
      }
    },
    enabled: !!branchId,
    retry: false,
  });
}

export function useCashSummary(sessionId?: string) {
  return useQuery({
    queryKey: ["cash-summary", sessionId],
    queryFn: () => apiGet<CashSummary>(`/api/v1/cash-sessions/${sessionId}/summary`),
    enabled: !!sessionId,
    refetchInterval: 30_000,
  });
}

export function useOpenCashAction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { branchId: string; openingAmount: number }) =>
      apiPost<CashSession>("/api/v1/cash-sessions/open", input),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["cash-open", v.branchId] }),
  });
}

/** Cierre con arqueo: efectivo contado + notas; el backend calcula la diferencia. */
export function useCloseCashAction(branchId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: string; countedAmount: number; notes?: string }) =>
      apiPost<CashSession>(`/api/v1/cash-sessions/${input.id}/close`,
        { countedAmount: input.countedAmount, notes: input.notes }),
    onSuccess: (_d, v) => {
      qc.invalidateQueries({ queryKey: ["cash-open", branchId] });
      qc.invalidateQueries({ queryKey: ["cash-summary", v.id] });
    },
  });
}

/** Registra un ingreso o salida de efectivo en la caja abierta. */
export function useAddCashMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      sessionId: string;
      type: "INCOME" | "EXPENSE";
      category: string;
      concept: string;
      amount: number;
    }) =>
      apiPost<CashMovement>(`/api/v1/cash-sessions/${input.sessionId}/movements`, {
        type: input.type,
        category: input.category,
        concept: input.concept,
        amount: input.amount,
      }),
    onSuccess: (_d, v) => qc.invalidateQueries({ queryKey: ["cash-summary", v.sessionId] }),
  });
}

// ---------------------------------------------------------------------------
// Ventas
// ---------------------------------------------------------------------------

/** Ventas paginadas, mas recientes primero; filtrables por sucursal o sesion de caja. */
export function useSales(params: {
  branchId?: string;
  cashSessionId?: string;
  page?: number;
  size?: number;
  enabled?: boolean;
} = {}) {
  const { branchId, cashSessionId, page = 0, size = 20, enabled = true } = params;
  return useQuery({
    queryKey: ["sales", branchId ?? null, cashSessionId ?? null, page, size],
    queryFn: () => apiGet<PageResponse<Sale>>("/api/v1/sales", {
      branchId,
      cashSessionId,
      page: String(page),
      size: String(size),
    }),
    enabled,
  });
}

export function useCreateSale() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      branchId: string;
      customerId?: string;
      paymentMethod: PaymentMethod;
      items: SaleItemInput[];
    }) => apiPost<Sale>("/api/v1/sales", input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["sales"] });
      qc.invalidateQueries({ queryKey: ["stock"] });
      qc.invalidateQueries({ queryKey: ["cash-summary"] });
      qc.invalidateQueries({ queryKey: ["sales-flow"] });
    },
  });
}

export interface DailySales {
  date: string;
  total: number;
  cashTotal: number;
  digitalTotal: number;
  count: number;
}

/** Ventas de los ultimos 7 dias por dia: total, efectivo y digital. */
export function useWeeklySalesFlow(branchId?: string) {
  return useQuery({
    queryKey: ["sales-flow", branchId],
    queryFn: () => apiGet<DailySales[]>("/api/v1/sales/flow/weekly", { branchId }),
    enabled: !!branchId,
  });
}

// ---------------------------------------------------------------------------
// Ticket de venta (HTML/PDF renderizados por el backend con la plantilla configurada)
// ---------------------------------------------------------------------------

async function authHeader(): Promise<Record<string, string>> {
  const u = auth.currentUser;
  return u ? { Authorization: `Bearer ${await u.getIdToken()}` } : {};
}

export async function fetchTicketHtml(saleId: string): Promise<string> {
  const res = await fetch(`${BASE}/api/v1/sales/${saleId}/ticket`, { headers: await authHeader() });
  if (!res.ok) throw new Error(`No se pudo generar el ticket (error ${res.status}).`);
  return res.text();
}

export async function fetchTicketPdf(saleId: string): Promise<Blob> {
  const res = await fetch(`${BASE}/api/v1/sales/${saleId}/ticket.pdf`, { headers: await authHeader() });
  if (!res.ok) throw new Error(`No se pudo generar el PDF (error ${res.status}).`);
  return res.blob();
}
