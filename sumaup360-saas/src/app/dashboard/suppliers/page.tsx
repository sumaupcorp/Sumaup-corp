"use client";

import { useState } from "react";
import { useSuppliers, useCreateSupplier } from "@/features/suppliers/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { PageHeader, Spinner } from "@/components/ui/misc";

export default function SuppliersPage() {
  const hasPerm = useHasPermission();
  const canManage = hasPerm("supplier:manage");
  const { data: suppliers = [], isLoading } = useSuppliers();
  const create = useCreateSupplier();
  const [name, setName] = useState("");
  const [ruc, setRuc] = useState("");
  const [phone, setPhone] = useState("");
  const [msg, setMsg] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await create.mutateAsync({ name, ruc: ruc || undefined, phone: phone || undefined });
      setName(""); setRuc(""); setPhone("");
      setMsg("Proveedor creado.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Proveedores" subtitle="Proveedores de tu negocio" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="p-5">
            {isLoading ? (
              <Spinner />
            ) : suppliers.length === 0 ? (
              <p className="text-sm text-muted-foreground">Aun no hay proveedores.</p>
            ) : (
              <ul className="divide-y divide-border">
                {suppliers.map((s) => (
                  <li key={s.id} className="py-2">
                    <p className="font-medium text-foreground">{s.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {s.ruc ? "RUC " + s.ruc : "sin RUC"}{s.phone ? " · " + s.phone : ""}
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
              <h2 className="mb-3 text-sm font-semibold">Nuevo proveedor</h2>
              <form onSubmit={submit} className="space-y-3">
                <div className="space-y-1"><Label>Nombre</Label><Input value={name} onChange={(e) => setName(e.target.value)} required /></div>
                <div className="space-y-1"><Label>RUC</Label><Input value={ruc} onChange={(e) => setRuc(e.target.value)} maxLength={11} /></div>
                <div className="space-y-1"><Label>Telefono</Label><Input value={phone} onChange={(e) => setPhone(e.target.value)} /></div>
                {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
                <Button type="submit" disabled={create.isPending}>{create.isPending ? "Guardando..." : "Crear proveedor"}</Button>
              </form>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
