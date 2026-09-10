"use client";

import { useState } from "react";
import { useCompanies, useCreateCompany } from "@/features/companies/api";
import { useBusinessTypes, useVerticals } from "@/features/catalog/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner } from "@/components/ui/misc";

export default function CompaniesPage() {
  const hasPerm = useHasPermission();
  const canManage = hasPerm("company:manage");
  const { data: companies = [], isLoading } = useCompanies();
  const { data: businessTypes = [] } = useBusinessTypes();
  const create = useCreateCompany();

  const [legalName, setLegalName] = useState("");
  const [ruc, setRuc] = useState("");
  const [bt, setBt] = useState("");
  const { data: verticals = [] } = useVerticals(bt || undefined);
  const [vertical, setVertical] = useState("");
  const [msg, setMsg] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await create.mutateAsync({
        legalName,
        ruc: ruc || undefined,
        businessTypeCode: bt || undefined,
        verticalCode: vertical || undefined,
      });
      setLegalName(""); setRuc(""); setBt(""); setVertical("");
      setMsg("Empresa creada.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Empresas" subtitle="Negocios de tu cuenta (tenant)" />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardContent className="p-5">
            <h2 className="mb-3 text-sm font-semibold">Tus empresas</h2>
            {isLoading ? (
              <Spinner />
            ) : companies.length === 0 ? (
              <p className="text-sm text-muted-foreground">Aun no tienes empresas.</p>
            ) : (
              <ul className="divide-y divide-border">
                {companies.map((c) => (
                  <li key={c.id} className="py-2">
                    <p className="font-medium text-foreground">{c.legalName}</p>
                    <p className="text-xs text-muted-foreground">
                      {c.ruc ? "RUC " + c.ruc + " · " : ""}
                      {c.businessTypeCode ?? "sin rubro"}
                      {c.verticalCode ? " / " + c.verticalCode : ""}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        {canManage && (
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Nueva empresa</h2>
              <form onSubmit={submit} className="space-y-3">
                <div className="space-y-1">
                  <Label>Razon social</Label>
                  <Input value={legalName} onChange={(e) => setLegalName(e.target.value)} required />
                </div>
                <div className="space-y-1">
                  <Label>RUC</Label>
                  <Input value={ruc} onChange={(e) => setRuc(e.target.value)} maxLength={11} />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <Label>Rubro</Label>
                    <Select value={bt} onChange={(e) => { setBt(e.target.value); setVertical(""); }}>
                      <option value="">—</option>
                      {businessTypes.map((b) => (
                        <option key={b.code} value={b.code}>{b.name}</option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-1">
                    <Label>Sub-rubro</Label>
                    <Select value={vertical} onChange={(e) => setVertical(e.target.value)} disabled={!bt}>
                      <option value="">—</option>
                      {verticals.map((v) => (
                        <option key={v.code} value={v.code}>{v.name}</option>
                      ))}
                    </Select>
                  </div>
                </div>
                {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
                <Button type="submit" disabled={create.isPending}>
                  {create.isPending ? "Guardando..." : "Crear empresa"}
                </Button>
              </form>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
