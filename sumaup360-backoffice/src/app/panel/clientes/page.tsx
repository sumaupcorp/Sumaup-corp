"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useClients } from "@/features/clients/api";
import { Card, CardContent } from "@/components/ui/card";
import { Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { SearchBox, Pagination, useTableData } from "@/components/ui/table-tools";

export default function ClientesPage() {
  const { data: clients = [], isLoading } = useClients();
  const [planFilter, setPlanFilter] = useState("");

  const plans = useMemo(
    () => Array.from(new Set(clients.map((c) => c.planCode).filter(Boolean))) as string[],
    [clients]
  );
  const byPlan = useMemo(
    () => (planFilter ? clients.filter((c) => c.planCode === planFilter) : clients),
    [clients, planFilter]
  );

  const { query, setQuery, page, setPage, view, pageCount, total } = useTableData(
    byPlan,
    (c, q) => c.name.toLowerCase().includes(q) || c.code.toLowerCase().includes(q)
  );

  return (
    <div>
      <PageHeader title="Clientes" subtitle="Todos los negocios (tenants) del SaaS" />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <SearchBox value={query} onChange={setQuery} placeholder="Buscar por nombre o codigo…" />
        <Select value={planFilter} onChange={(e) => setPlanFilter(e.target.value)} className="h-9 w-44">
          <option value="">Todos los planes</option>
          {plans.map((p) => <option key={p} value={p}>{p}</option>)}
        </Select>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5"><Spinner /></div>
          ) : (
            <>
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-muted-foreground">
                  <tr>
                    <th className="px-5 py-3">Negocio</th>
                    <th className="px-5 py-3">Plan</th>
                    <th className="px-5 py-3 text-center">Empresas</th>
                    <th className="px-5 py-3 text-center">Sucursales</th>
                    <th className="px-5 py-3 text-center">Usuarios</th>
                    <th className="px-5 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {view.map((c) => (
                    <tr key={c.tenantId} className="border-b border-border/60">
                      <td className="px-5 py-3 font-medium text-foreground">{c.name}</td>
                      <td className="px-5 py-3"><Badge variant={c.planCode ? "success" : "muted"}>{c.planCode ?? "sin plan"}</Badge></td>
                      <td className="px-5 py-3 text-center">{c.companies}</td>
                      <td className="px-5 py-3 text-center">{c.branches}</td>
                      <td className="px-5 py-3 text-center">{c.users}</td>
                      <td className="px-5 py-3 text-right">
                        <Link href={`/panel/clientes/${c.tenantId}`} className="text-brand-blue hover:underline">Administrar</Link>
                      </td>
                    </tr>
                  ))}
                  {view.length === 0 && (
                    <tr><td colSpan={6} className="px-5 py-6 text-center text-muted-foreground">Sin resultados.</td></tr>
                  )}
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
