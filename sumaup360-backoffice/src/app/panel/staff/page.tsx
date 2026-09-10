"use client";

import { useMemo, useState } from "react";
import { useStaff, useCreateStaff, STAFF_ROLES } from "@/features/staff/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { SearchBox, Pagination, useTableData } from "@/components/ui/table-tools";

export default function StaffPage() {
  const hasPerm = useHasPermission();
  const canManage = hasPerm("staff:manage");
  const { data: staff = [], isLoading } = useStaff();
  const create = useCreateStaff();

  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [roleCode, setRoleCode] = useState("contador");
  const [msg, setMsg] = useState<string | null>(null);
  const [roleFilter, setRoleFilter] = useState("");

  const byRole = useMemo(
    () => (roleFilter ? staff.filter((s) => s.roles.includes(roleFilter)) : staff),
    [staff, roleFilter]
  );
  const list = useTableData(
    byRole,
    (s, q) => (s.name ?? "").toLowerCase().includes(q) || (s.email ?? "").toLowerCase().includes(q),
    8
  );

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await create.mutateAsync({ email, name, password, roleCode });
      setEmail(""); setName(""); setPassword("");
      setMsg("Cuenta de staff creada. Ya puede iniciar sesion.");
    } catch (err) { setMsg("Error: " + (err as Error).message); }
  };

  return (
    <div>
      <PageHeader title="Personal interno" subtitle="Cuentas de staff (no hay auto-registro)" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="p-0">
            <div className="flex flex-wrap items-center gap-3 p-5 pb-3">
              <SearchBox value={list.query} onChange={list.setQuery} placeholder="Buscar por nombre o correo…" />
              <Select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} className="h-9 w-40">
                <option value="">Todos los roles</option>
                {STAFF_ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </Select>
            </div>
            {isLoading ? <div className="p-5"><Spinner /></div> : (
              <>
                <ul className="divide-y divide-border px-5">
                  {list.view.map((s) => (
                    <li key={s.userId} className="flex items-center justify-between py-2">
                      <div>
                        <p className="font-medium text-foreground">{s.name ?? s.email}</p>
                        <p className="text-xs text-muted-foreground">{s.email}</p>
                      </div>
                      <div className="flex gap-1">{s.roles.map((r) => <Badge key={r} variant="warning">{r}</Badge>)}</div>
                    </li>
                  ))}
                  {list.view.length === 0 && <li className="py-6 text-center text-sm text-muted-foreground">Sin resultados.</li>}
                </ul>
                <Pagination page={list.page} pageCount={list.pageCount} total={list.total} onPage={list.setPage} />
              </>
            )}
          </CardContent>
        </Card>
        {canManage && (
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Nueva cuenta de staff</h2>
              <form onSubmit={submit} className="space-y-3">
                <div className="space-y-1"><Label>Nombre</Label><Input value={name} onChange={(e) => setName(e.target.value)} required /></div>
                <div className="space-y-1"><Label>Correo</Label><Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
                <div className="space-y-1"><Label>Contrasena temporal</Label><Input type="text" value={password} onChange={(e) => setPassword(e.target.value)} minLength={6} required /></div>
                <div className="space-y-1">
                  <Label>Rol</Label>
                  <Select value={roleCode} onChange={(e) => setRoleCode(e.target.value)}>
                    {STAFF_ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                  </Select>
                </div>
                {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
                <Button type="submit" disabled={create.isPending}>{create.isPending ? "Creando..." : "Crear cuenta"}</Button>
              </form>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
