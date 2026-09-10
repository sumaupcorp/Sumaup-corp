"use client";

import Link from "next/link";
import {
  Building2, Store, Users, UserCog, FileClock, CreditCard, Boxes, ArrowRight,
} from "lucide-react";
import { useOverview } from "@/features/stats/api";
import { useAudit } from "@/features/audit/api";
import { useSession } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

function Stat({ icon, label, value, hint, accent }: {
  icon: React.ReactNode; label: string; value: string | number; hint?: string; accent?: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 p-5">
        <div className={`flex size-11 items-center justify-center rounded-xl ${accent ? "bg-brand-blue text-white" : "bg-accent text-brand-blue"}`}>
          {icon}
        </div>
        <div>
          <p className="text-xs text-muted-foreground">{label}</p>
          <p className="font-heading text-2xl font-bold leading-tight text-foreground">{value}</p>
          {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
        </div>
      </CardContent>
    </Card>
  );
}

const PLAN_COLORS = ["bg-brand-blue", "bg-brand-electric", "bg-brand-sky", "bg-slate-400"];

export default function PanelHome() {
  const { data: session } = useSession();
  const ov = useOverview();
  const audit = useAudit();
  const o = ov.data;
  const maxPlan = Math.max(1, ...(o?.byPlan.map((p) => p.count) ?? [1]));
  const totalReceipts = o ? o.pendingReceipts + o.inProcessReceipts + o.processedReceipts : 0;

  return (
    <div>
      <PageHeader
        title={`Hola${session?.email ? ", " + session.email.split("@")[0] : ""}`}
        subtitle="Panorama del ecosistema SUMAUP360"
      />

      {ov.isLoading || !o ? (
        <Spinner />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
            <Stat icon={<Building2 className="size-5" />} label="Clientes" value={o.clients} accent />
            <Stat icon={<Boxes className="size-5" />} label="Empresas" value={o.companies} />
            <Stat icon={<Store className="size-5" />} label="Sucursales" value={o.branches} />
            <Stat icon={<Users className="size-5" />} label="Usuarios" value={o.users} />
            <Stat icon={<UserCog className="size-5" />} label="Staff" value={o.staff} />
            <Stat icon={<CreditCard className="size-5" />} label="Suscripciones" value={o.activeSubscriptions} />
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-3">
            {/* Clientes por plan */}
            <Card className="lg:col-span-2">
              <CardContent className="p-5">
                <h2 className="mb-4 text-sm font-semibold">Clientes por plan</h2>
                {o.byPlan.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aun no hay suscripciones activas.</p>
                ) : (
                  <div className="space-y-3">
                    {o.byPlan.map((p, i) => (
                      <div key={p.plan}>
                        <div className="mb-1 flex justify-between text-sm">
                          <span className="font-medium">{p.plan}</span>
                          <span className="text-muted-foreground">{p.count}</span>
                        </div>
                        <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                          <div className={`h-full ${PLAN_COLORS[i % PLAN_COLORS.length]}`} style={{ width: `${(p.count / maxPlan) * 100}%` }} />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Recibos */}
            <Card>
              <CardContent className="p-5">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-sm font-semibold">Recibos (intake)</h2>
                  <FileClock className="size-4 text-muted-foreground" />
                </div>
                <div className="space-y-2 text-sm">
                  <Row label="Pendientes" value={o.pendingReceipts} variant="warning" />
                  <Row label="En proceso" value={o.inProcessReceipts} variant="muted" />
                  <Row label="Procesados" value={o.processedReceipts} variant="success" />
                </div>
                <Link href="/panel/recibos" className="mt-4 inline-flex items-center gap-1 text-sm text-brand-blue hover:underline">
                  Ir a la bandeja <ArrowRight className="size-3.5" />
                </Link>
                <p className="mt-1 text-xs text-muted-foreground">{totalReceipts} recibos en total</p>
              </CardContent>
            </Card>
          </div>

          {/* Actividad reciente */}
          <Card className="mt-6">
            <CardContent className="p-5">
              <h2 className="mb-3 text-sm font-semibold">Actividad reciente</h2>
              {audit.isLoading ? <Spinner /> : (audit.data ?? []).length === 0 ? (
                <p className="text-sm text-muted-foreground">Sin actividad.</p>
              ) : (
                <ul className="divide-y divide-border text-sm">
                  {audit.data!.slice(0, 8).map((e) => (
                    <li key={e.id} className="flex items-center justify-between py-2">
                      <span className="font-mono text-xs">{e.action}</span>
                      <span className="flex items-center gap-2">
                        <Badge variant="muted">{e.actorType}</Badge>
                        <span className="text-xs text-muted-foreground">{new Date(e.occurredAt).toLocaleTimeString("es-PE")}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}

function Row({ label, value, variant }: { label: string; value: number; variant: "warning" | "muted" | "success" }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <Badge variant={variant}>{value}</Badge>
    </div>
  );
}
