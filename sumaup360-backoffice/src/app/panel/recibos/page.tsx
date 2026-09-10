"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { CheckCircle2, Circle } from "lucide-react";
import { useReceiptQueue } from "@/features/receipts/api";
import { Card, CardContent } from "@/components/ui/card";
import { Input, Select } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { PageHeader, Spinner } from "@/components/ui/misc";
import { SearchBox, Pagination, useTableData } from "@/components/ui/table-tools";

const STATUSES = ["PENDING", "IN_PROCESS", "PROCESSED", "OBSERVED"];
const STATUS_LABEL: Record<string, string> = {
  PENDING: "Pendientes", IN_PROCESS: "En proceso", PROCESSED: "Procesados", OBSERVED: "Observados",
};

function DeclFlag({ on, label }: { on: boolean; label: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1 text-xs", on ? "text-emerald-600" : "text-muted-foreground")}>
      {on ? <CheckCircle2 className="size-3.5" /> : <Circle className="size-3.5" />} {label}
    </span>
  );
}

export default function RecibosPage() {
  const [status, setStatus] = useState("PENDING");
  const [sireF, setSireF] = useState("");
  const [sunatF, setSunatF] = useState("");
  const [period, setPeriod] = useState("");
  const queue = useReceiptQueue(status);

  const filtered = useMemo(() => {
    const p = period.trim();
    return (queue.data ?? []).filter((r) => {
      if (sireF === "yes" && !r.declaredSire) return false;
      if (sireF === "no" && r.declaredSire) return false;
      if (sunatF === "yes" && !r.declaredSunat) return false;
      if (sunatF === "no" && r.declaredSunat) return false;
      if (p && r.sirePeriod !== p && r.sunatPeriod !== p && (r.issueDate ?? "").slice(0, 7) !== p) return false;
      return true;
    });
  }, [queue.data, sireF, sunatF, period]);

  const list = useTableData(
    filtered,
    (r, q) => (`${r.type} ${r.docNumber ?? ""} ${r.userName ?? ""} ${r.userRuc ?? ""}`).toLowerCase().includes(q),
    10
  );

  return (
    <div>
      <PageHeader title="Recibos" subtitle="Comprobantes que suben los usuarios de la app movil" />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        {STATUSES.map((s) => (
          <button key={s} onClick={() => setStatus(s)}
            className={cn("rounded-lg border px-3 py-1.5 text-sm",
              status === s ? "border-brand-blue bg-blue-50 text-brand-blue" : "border-border text-muted-foreground")}>
            {STATUS_LABEL[s]}
          </button>
        ))}
        <div className="ml-auto"><SearchBox value={list.query} onChange={list.setQuery} placeholder="Buscar usuario, RUC o doc…" /></div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Select value={sireF} onChange={(e) => setSireF(e.target.value)} className="h-9 w-40">
          <option value="">SIRE: todos</option>
          <option value="yes">SIRE: declarado</option>
          <option value="no">SIRE: pendiente</option>
        </Select>
        <Select value={sunatF} onChange={(e) => setSunatF(e.target.value)} className="h-9 w-44">
          <option value="">SUNAT: todos</option>
          <option value="yes">SUNAT: declarado</option>
          <option value="no">SUNAT: pendiente</option>
        </Select>
        <Input value={period} onChange={(e) => setPeriod(e.target.value)} placeholder="Periodo AAAA-MM" className="h-9 w-40" />
        {(sireF || sunatF || period) && (
          <button onClick={() => { setSireF(""); setSunatF(""); setPeriod(""); }} className="text-sm text-brand-blue hover:underline">
            Limpiar filtros
          </button>
        )}
      </div>

      <Card>
        <CardContent className="p-0">
          {queue.isLoading ? <div className="p-5"><Spinner /></div> : list.total === 0 ? (
            <p className="p-5 text-sm text-muted-foreground">Sin recibos en {STATUS_LABEL[status]}.</p>
          ) : (
            <>
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-muted-foreground">
                  <tr>
                    <th className="px-5 py-3">Usuario</th>
                    <th className="px-5 py-3">Comprobante</th>
                    <th className="px-5 py-3 text-right">Monto</th>
                    <th className="px-5 py-3">Declaracion</th>
                    <th className="px-5 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {list.view.map((r) => (
                    <tr key={r.id} className="border-b border-border/60">
                      <td className="px-5 py-3">
                        <p className="font-medium text-foreground">{r.userName ?? "Usuario"}</p>
                        <p className="text-xs text-muted-foreground">RUC {r.userRuc ?? "—"}</p>
                      </td>
                      <td className="px-5 py-3">
                        <p className="text-foreground">{r.type} {r.docNumber ?? ""}</p>
                        <p className="text-xs text-muted-foreground">{r.issueDate ?? "sin fecha"}</p>
                      </td>
                      <td className="px-5 py-3 text-right font-medium">{r.amount != null ? `${r.currency} ${r.amount}` : "—"}</td>
                      <td className="px-5 py-3">
                        <div className="flex flex-col gap-0.5">
                          <DeclFlag on={r.declaredSire} label="SIRE" />
                          <DeclFlag on={r.declaredSunat} label="SUNAT" />
                        </div>
                      </td>
                      <td className="px-5 py-3 text-right">
                        <Link href={`/panel/recibos/${r.id}`} className="text-brand-blue hover:underline">Ver detalle</Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pagination page={list.page} pageCount={list.pageCount} total={list.total} onPage={list.setPage} />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
