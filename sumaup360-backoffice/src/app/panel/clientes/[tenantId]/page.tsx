"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Eye, EyeOff, KeyRound, History, CreditCard, ShieldAlert, UserPlus, FileText } from "lucide-react";
import {
  useCredentials, useSaveCredentials, useRevealCredentials,
  useBusinessPlans, useTenantLicenses, useAssignLicense, useClientHistory,
} from "@/features/clients/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

export default function ClientDetailPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const hasPerm = useHasPermission();

  const creds = useCredentials(tenantId);
  const save = useSaveCredentials(tenantId);
  const reveal = useRevealCredentials(tenantId);
  const plans = useBusinessPlans();
  const licenses = useTenantLicenses(tenantId);
  const assign = useAssignLicense(tenantId);
  const history = useClientHistory(tenantId);

  const [ruc, setRuc] = useState("");
  const [solUser, setSolUser] = useState("");
  const [solPass, setSolPass] = useState("");
  const [revealed, setRevealed] = useState<string | null>(null);
  const [plan, setPlan] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [histType, setHistType] = useState("");

  const histEvents = (history.data ?? []).filter((e) => !histType || e.eventType === histType);
  const histTypes = Array.from(new Set((history.data ?? []).map((e) => e.eventType)));

  const saveCreds = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await save.mutateAsync({ ruc: ruc || undefined, solUser: solUser || undefined, solPass: solPass || undefined });
      setSolPass(""); setRevealed(null);
      setMsg("Credenciales guardadas (cifradas).");
    } catch (err) { setMsg("Error: " + (err as Error).message); }
  };

  const doReveal = async () => {
    setMsg(null);
    try { const r = await reveal.mutateAsync(); setRevealed(r.solPass ?? ""); }
    catch (err) { setMsg("Error: " + (err as Error).message); }
  };

  return (
    <div>
      <Link href="/panel/clientes" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:underline">
        <ArrowLeft className="size-4" /> Clientes
      </Link>
      <PageHeader title="Administrar cliente" subtitle={`Tenant ${tenantId.slice(0, 8)}…`} />

      <div className="grid gap-6 lg:grid-cols-2">
        {/* CREDENCIALES FISCALES */}
        <Card>
          <CardContent className="p-5">
            <div className="mb-3 flex items-center gap-2">
              <KeyRound className="size-4 text-brand-blue" />
              <h2 className="text-sm font-semibold">Credenciales fiscales (Clave SOL)</h2>
            </div>
            {creds.isLoading ? <Spinner /> : (
              <>
                <div className="mb-4 rounded-lg bg-brand-soft p-3 text-sm">
                  <p><span className="text-muted-foreground">RUC:</span> {creds.data?.ruc ?? "—"}</p>
                  <p><span className="text-muted-foreground">Usuario SOL:</span> {creds.data?.solUser ?? "—"}</p>
                  <p className="flex items-center gap-2">
                    <span className="text-muted-foreground">Clave SOL:</span>
                    {creds.data?.hasPassword
                      ? (revealed !== null ? <span className="font-mono">{revealed}</span> : <Badge variant="muted">•••••• (cifrada)</Badge>)
                      : <span className="text-muted-foreground">no registrada</span>}
                    {creds.data?.hasPassword && hasPerm("sol:reveal") && (
                      <Button size="sm" variant="ghost" onClick={revealed !== null ? () => setRevealed(null) : doReveal} disabled={reveal.isPending}>
                        {revealed !== null ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                        {revealed !== null ? "Ocultar" : "Revelar"}
                      </Button>
                    )}
                  </p>
                </div>

                {hasPerm("sol:manage") && (
                  <form onSubmit={saveCreds} className="space-y-3 border-t border-border pt-4">
                    <p className="text-xs text-muted-foreground">La clave se guarda cifrada (AES-GCM); solo se descifra al revelar (auditado).</p>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-1"><Label>RUC</Label><Input value={ruc} onChange={(e) => setRuc(e.target.value)} maxLength={11} placeholder={creds.data?.ruc ?? ""} /></div>
                      <div className="space-y-1"><Label>Usuario SOL</Label><Input value={solUser} onChange={(e) => setSolUser(e.target.value)} placeholder={creds.data?.solUser ?? ""} /></div>
                    </div>
                    <div className="space-y-1"><Label>Clave SOL</Label><Input type="password" value={solPass} onChange={(e) => setSolPass(e.target.value)} placeholder="(dejar vacio para no cambiar)" /></div>
                    <Button type="submit" disabled={save.isPending}>{save.isPending ? "Guardando..." : "Guardar credenciales"}</Button>
                  </form>
                )}
              </>
            )}
          </CardContent>
        </Card>

        {/* LICENCIA / PLAN */}
        <Card>
          <CardContent className="p-5">
            <h2 className="mb-3 text-sm font-semibold">Licencia / Plan</h2>
            {licenses.isLoading ? <Spinner /> : (
              <ul className="mb-4 divide-y divide-border text-sm">
                {(licenses.data ?? []).length === 0 && <li className="py-2 text-muted-foreground">Sin licencias.</li>}
                {(licenses.data ?? []).map((l) => (
                  <li key={l.id} className="flex justify-between py-2">
                    <span>{l.planCode}</span>
                    <Badge variant={l.status === "ACTIVE" ? "success" : "muted"}>{l.status}</Badge>
                  </li>
                ))}
              </ul>
            )}
            {hasPerm("license:manage") && (
              <div className="flex items-end gap-2 border-t border-border pt-4">
                <div className="flex-1 space-y-1">
                  <Label>Asignar plan</Label>
                  <Select value={plan} onChange={(e) => setPlan(e.target.value)}>
                    <option value="">Selecciona…</option>
                    {(plans.data ?? []).map((p) => <option key={p.code} value={p.code}>{p.name} (S/ {p.price})</option>)}
                  </Select>
                </div>
                <Button disabled={!plan || assign.isPending} onClick={() => assign.mutate(plan)}>Asignar</Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* HISTORIAL DEL CLIENTE */}
      <Card className="mt-6">
        <CardContent className="p-5">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <History className="size-4 text-brand-blue" />
              <h2 className="text-sm font-semibold">Historial del cliente</h2>
            </div>
            {histTypes.length > 1 && (
              <Select value={histType} onChange={(e) => setHistType(e.target.value)} className="h-8 w-48 text-xs">
                <option value="">Todos los eventos</option>
                {histTypes.map((t) => <option key={t} value={t}>{t}</option>)}
              </Select>
            )}
          </div>
          {history.isLoading ? <Spinner /> : histEvents.length === 0 ? (
            <p className="text-sm text-muted-foreground">Sin eventos registrados.</p>
          ) : (
            <ol className="relative space-y-4 border-l border-border pl-6">
              {histEvents.map((ev) => (
                <li key={ev.id} className="relative">
                  <span className="absolute -left-[31px] flex size-6 items-center justify-center rounded-full bg-accent text-brand-blue ring-4 ring-card">
                    {eventIcon(ev.eventType)}
                  </span>
                  <p className="text-sm font-medium text-foreground">{ev.description}</p>
                  <p className="text-xs text-muted-foreground">
                    {new Date(ev.at).toLocaleString("es-PE")} · por {ev.actorName ?? "Sistema"}
                  </p>
                </li>
              ))}
            </ol>
          )}
        </CardContent>
      </Card>

      {msg && <p className="mt-4 text-sm text-muted-foreground">{msg}</p>}
    </div>
  );
}

function eventIcon(type: string) {
  const cls = "size-3.5";
  if (type === "PLAN_ASSIGNED" || type === "LICENSE_STATUS") return <CreditCard className={cls} />;
  if (type === "SOL_REVEALED") return <ShieldAlert className={cls} />;
  if (type === "CREDENTIALS_UPDATED") return <KeyRound className={cls} />;
  if (type === "CLIENT_CREATED") return <UserPlus className={cls} />;
  return <FileText className={cls} />;
}
