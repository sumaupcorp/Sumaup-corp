"use client";

import { useMemo, useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useProducts } from "@/features/products/api";
import {
  useOpenCash,
  useCashSummary,
  type Sale,
} from "@/features/sales/api";
import { useHasPermission, useSession } from "@/features/auth/session";
import { SellView } from "@/features/pos/components/SellView";
import { CashView } from "@/features/pos/components/CashView";
import { TicketModal } from "@/features/pos/components/PosModals";
import { Label, Select } from "@/components/ui/input";

type Tab = "sell" | "cash";

/**
 * POS con dos pestanas: VENDER (interfaz tactil pura de venta) y CAJA (flujo del dia:
 * efectivo, movimientos, arqueo y detalle de ventas). Pensado para pantalla tactil.
 */
export default function PosPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const canSell = hasPerm("sale:create");
  const canOperate = hasPerm("pos:operate");
  const canViewProducts = hasPerm("product:read");

  const { data: session } = useSession();
  const { data: branchesAll = [] } = useBranches(currentCompany?.id);
  const { data: products = [] } = useProducts();

  // Sede asignada: el trabajador con sede fija solo opera en ella (el backend tambien lo
  // valida); sin asignacion, elige entre todas.
  const assignedBranchId = session?.branchId ?? null;
  const branches = assignedBranchId
    ? branchesAll.filter((b) => b.id === assignedBranchId)
    : branchesAll;
  const [branchId, setBranchId] = useState("");
  // Con una sola sucursal se selecciona sola: evita el callejon "Selecciona una sucursal".
  const autoBranchId = !assignedBranchId && branchesAll.length === 1 ? branchesAll[0].id : "";
  const effectiveBranchId = assignedBranchId ?? (branchId || autoBranchId);

  const [tab, setTab] = useState<Tab>("sell");
  const [ticketSale, setTicketSale] = useState<Sale | null>(null);

  const cash = useOpenCash(effectiveBranchId || undefined);
  const summary = useCashSummary(cash.data?.id);

  const productMap = useMemo(() => new Map(products.map((p) => [p.id, p])), [products]);

  return (
    <div>
      {/* CABECERA: pestanas grandes (tactil) + sucursal */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-xl bg-muted p-1">
          <TabButton label="Vender" active={tab === "sell"} onClick={() => setTab("sell")} />
          <TabButton label="Caja" active={tab === "cash"} onClick={() => setTab("cash")}
            dot={!cash.data && !cash.isLoading} />
        </div>

        <div className="flex items-center gap-2">
          <Label className="text-xs text-muted-foreground">Sucursal</Label>
          <Select className="h-10 w-48" value={effectiveBranchId} disabled={!!assignedBranchId}
            onChange={(e) => setBranchId(e.target.value)}>
            {!assignedBranchId && branchesAll.length !== 1 && <option value="">Selecciona…</option>}
            {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </Select>
        </div>
      </div>

      {!effectiveBranchId ? (
        <p className="py-8 text-center text-sm text-muted-foreground">
          {branches.length === 0
            ? "No tienes una sucursal disponible. Pide a tu administrador que te asigne una sede."
            : "Selecciona una sucursal para operar."}
        </p>
      ) : tab === "sell" ? (
        <SellView
          branchId={effectiveBranchId}
          products={products}
          canSell={canSell}
          canViewProducts={canViewProducts}
          cashOpen={!!cash.data}
          onGoCash={() => setTab("cash")}
          onSold={setTicketSale}
        />
      ) : (
        <CashView
          cash={cash.data ?? null}
          cashLoading={cash.isLoading}
          summary={summary.data ?? null}
          branchId={effectiveBranchId}
          canOperate={canOperate}
          productMap={productMap}
          onTicket={setTicketSale}
        />
      )}

      {ticketSale && (
        <TicketModal
          key={ticketSale.id}
          sale={ticketSale}
          companyName={currentCompany?.legalName ?? ""}
          productMap={productMap}
          onClose={() => setTicketSale(null)}
        />
      )}
    </div>
  );
}

function TabButton({ label, active, onClick, dot }: {
  label: string;
  active: boolean;
  onClick: () => void;
  dot?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "relative min-h-11 select-none rounded-lg px-6 text-sm font-semibold transition touch-manipulation active:scale-95 " +
        (active ? "bg-white text-primary shadow-sm" : "text-muted-foreground")
      }
    >
      {label}
      {dot && (
        <span className="absolute right-2 top-2 size-2 rounded-full bg-amber-500"
          title="Caja cerrada" />
      )}
    </button>
  );
}
