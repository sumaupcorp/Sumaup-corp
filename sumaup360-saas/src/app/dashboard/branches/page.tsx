"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches, useCreateBranch } from "@/features/branches/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

export default function BranchesPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const canManage = hasPerm("branch:manage");
  const { data: branches = [], isLoading } = useBranches(currentCompany?.id);
  const create = useCreateBranch(currentCompany?.id ?? "");
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [main, setMain] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  if (!currentCompany) {
    return (
      <div>
        <PageHeader title="Sucursales" />
        <p className="text-sm text-muted-foreground">Selecciona o crea una empresa primero.</p>
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await create.mutateAsync({ name, address: address || undefined, main });
      setName(""); setAddress(""); setMain(false);
      setMsg("Sucursal creada.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Sucursales" subtitle={currentCompany.legalName} />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardContent className="p-5">
            <h2 className="mb-3 text-sm font-semibold">Sucursales</h2>
            {isLoading ? (
              <Spinner />
            ) : branches.length === 0 ? (
              <p className="text-sm text-muted-foreground">Aun no hay sucursales.</p>
            ) : (
              <ul className="divide-y divide-border">
                {branches.map((b) => (
                  <li key={b.id} className="flex items-center justify-between py-2">
                    <div>
                      <p className="font-medium text-foreground">{b.name}</p>
                      <p className="text-xs text-muted-foreground">{b.address ?? "sin direccion"}</p>
                    </div>
                    {b.main && <Badge>principal</Badge>}
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        {canManage && (
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Nueva sucursal</h2>
              <form onSubmit={submit} className="space-y-3">
                <div className="space-y-1">
                  <Label>Nombre</Label>
                  <Input value={name} onChange={(e) => setName(e.target.value)} required />
                </div>
                <div className="space-y-1">
                  <Label>Direccion</Label>
                  <Input value={address} onChange={(e) => setAddress(e.target.value)} />
                </div>
                <label className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={main} onChange={(e) => setMain(e.target.checked)} />
                  Sucursal principal
                </label>
                {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
                <Button type="submit" disabled={create.isPending}>
                  {create.isPending ? "Guardando..." : "Crear sucursal"}
                </Button>
              </form>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
