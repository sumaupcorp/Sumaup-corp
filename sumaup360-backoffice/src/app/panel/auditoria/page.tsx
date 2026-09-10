"use client";

import { useMemo, useState } from "react";
import { useAudit } from "@/features/audit/api";
import { Card, CardContent } from "@/components/ui/card";
import { Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { SearchBox, Pagination, useTableData } from "@/components/ui/table-tools";

export default function AuditoriaPage() {
  const { data: events = [], isLoading } = useAudit();
  const [actor, setActor] = useState("");

  const actorTypes = useMemo(
    () => Array.from(new Set(events.map((e) => e.actorType).filter(Boolean))) as string[],
    [events]
  );
  const byActor = useMemo(
    () => (actor ? events.filter((e) => e.actorType === actor) : events),
    [events, actor]
  );

  const { query, setQuery, page, setPage, view, pageCount, total } = useTableData(
    byActor,
    (e, q) => (e.action ?? "").toLowerCase().includes(q),
    12
  );

  return (
    <div>
      <PageHeader title="Auditoria" subtitle="Acciones del sistema" />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <SearchBox value={query} onChange={setQuery} placeholder="Buscar por accion…" />
        <Select value={actor} onChange={(e) => setActor(e.target.value)} className="h-9 w-44">
          <option value="">Todos los actores</option>
          {actorTypes.map((a) => <option key={a} value={a}>{a}</option>)}
        </Select>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? <div className="p-5"><Spinner /></div> : (
            <>
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-muted-foreground">
                  <tr>
                    <th className="px-5 py-3">Fecha</th>
                    <th className="px-5 py-3">Actor</th>
                    <th className="px-5 py-3">Accion</th>
                    <th className="px-5 py-3 text-center">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {view.map((e) => (
                    <tr key={e.id} className="border-b border-border/60">
                      <td className="px-5 py-2 text-xs text-muted-foreground">{new Date(e.occurredAt).toLocaleString("es-PE")}</td>
                      <td className="px-5 py-2"><Badge variant="muted">{e.actorType ?? "—"}</Badge></td>
                      <td className="px-5 py-2 font-mono text-xs">{e.action}</td>
                      <td className="px-5 py-2 text-center">{e.status}</td>
                    </tr>
                  ))}
                  {view.length === 0 && <tr><td colSpan={4} className="px-5 py-6 text-center text-muted-foreground">Sin eventos.</td></tr>}
                </tbody>
              </table>
              <Pagination page={page} pageCount={pageCount} total={total} onPage={setPage} />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
