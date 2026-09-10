"use client";

import { useState } from "react";
import {
  useCampaigns, useCreateCampaign, useCancelCampaign,
  useNotificationSettings, useUpdateSetting,
  type CampaignAudience, type CampaignStatus, type NotificationSetting,
} from "@/features/notifications/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";

const AUDIENCE_LABELS: Record<CampaignAudience, string> = {
  ALL: "Todos",
  TAXISTA: "Taxistas",
  DELIVERY_PEYA: "Repartidores",
  SERVICIOS_PROFESIONALES: "Serv. profesionales",
  ONBOARDING_INCOMPLETE: "Onboarding incompleto",
  ORIENTATION_PENDING: "Orientacion pendiente",
};

export default function NotificacionesPage() {
  const canManage = useHasPermission()("person:profile:manage");

  return (
    <div>
      <PageHeader
        title="Notificaciones"
        subtitle="Campanas push a usuarios de la app y notificaciones automaticas"
      />

      {canManage && <CampaignForm />}

      <CampaignsTable canManage={canManage} />

      <SettingsSection canManage={canManage} />
    </div>
  );
}

function CampaignForm() {
  const create = useCreateCampaign();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [route, setRoute] = useState("");
  const [audience, setAudience] = useState<CampaignAudience>("ALL");
  const [mode, setMode] = useState<"now" | "scheduled">("now");
  const [scheduledAt, setScheduledAt] = useState("");

  const scheduled = mode === "scheduled";
  const disabled =
    create.isPending || !title.trim() || !body.trim() || (scheduled && !scheduledAt);

  function submit() {
    create.mutate(
      {
        title: title.trim(),
        body: body.trim(),
        route: route.trim() || undefined,
        audience,
        scheduledAt: scheduled ? new Date(scheduledAt).toISOString() : undefined,
      },
      {
        onSuccess: () => {
          setTitle(""); setBody(""); setRoute("");
          setAudience("ALL"); setMode("now"); setScheduledAt("");
        },
      }
    );
  }

  return (
    <Card className="mb-6">
      <CardContent className="p-5">
        <h2 className="mb-1 font-heading text-base font-semibold">Enviar notificacion / campana</h2>
        <p className="mb-4 text-sm text-muted-foreground">
          Envia una notificacion push a la audiencia elegida, ahora o programada.
        </p>
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <Label htmlFor="camp-title">Titulo</Label>
            <Input
              id="camp-title"
              maxLength={120}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="mt-1"
              placeholder="Titulo de la notificacion"
            />
          </div>
          <div>
            <Label htmlFor="camp-route">Ruta destino (opcional)</Label>
            <Input
              id="camp-route"
              value={route}
              onChange={(e) => setRoute(e.target.value)}
              className="mt-1"
              placeholder="/home"
            />
          </div>
          <div className="md:col-span-2">
            <Label htmlFor="camp-body">Mensaje</Label>
            <textarea
              id="camp-body"
              rows={3}
              maxLength={500}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              className="mt-1 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              placeholder="Texto que vera el usuario"
            />
          </div>
          <div>
            <Label htmlFor="camp-audience">Audiencia</Label>
            <Select
              id="camp-audience"
              value={audience}
              onChange={(e) => setAudience(e.target.value as CampaignAudience)}
              className="mt-1"
            >
              {(Object.keys(AUDIENCE_LABELS) as CampaignAudience[]).map((a) => (
                <option key={a} value={a}>{AUDIENCE_LABELS[a]}</option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Programacion</Label>
            <div className="mt-1 flex h-10 flex-wrap items-center gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="camp-mode"
                  className="accent-brand-blue"
                  checked={mode === "now"}
                  onChange={() => setMode("now")}
                />
                Enviar ahora
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="camp-mode"
                  className="accent-brand-blue"
                  checked={scheduled}
                  onChange={() => setMode("scheduled")}
                />
                Programar
              </label>
              {scheduled && (
                <Input
                  type="datetime-local"
                  value={scheduledAt}
                  onChange={(e) => setScheduledAt(e.target.value)}
                  className="w-56"
                  aria-label="Fecha y hora de envio"
                />
              )}
            </div>
          </div>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <Button onClick={submit} disabled={disabled}>
            {create.isPending ? "Enviando…" : scheduled ? "Programar" : "Enviar"}
          </Button>
          {create.isError && (
            <span className="text-sm text-destructive">No se pudo crear la campana.</span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function CampaignsTable({ canManage }: { canManage: boolean }) {
  const { data: campaigns = [], isLoading, isError } = useCampaigns();
  const cancel = useCancelCampaign();

  return (
    <Card className="mb-6">
      <CardContent className="p-0">
        <div className="border-b border-border px-5 py-4">
          <h2 className="font-heading text-base font-semibold">Campanas</h2>
        </div>
        {isLoading ? (
          <div className="p-5"><Spinner /></div>
        ) : isError ? (
          <div className="p-5 text-sm text-muted-foreground">No se pudieron cargar las campanas.</div>
        ) : campaigns.length === 0 ? (
          <div className="p-5 text-sm text-muted-foreground">Aun no hay campanas.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-border text-left text-xs text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Titulo</th>
                  <th className="px-4 py-3">Audiencia</th>
                  <th className="px-4 py-3">Programada para</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3">Enviados / Fallidos</th>
                  <th className="px-4 py-3 text-right">Accion</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map((c) => (
                  <tr key={c.id} className="border-b border-border/60">
                    <td className="px-4 py-3">
                      <div className="font-medium">{c.title}</div>
                      <div className="max-w-md truncate text-xs text-muted-foreground">{c.body}</div>
                    </td>
                    <td className="px-4 py-3">{AUDIENCE_LABELS[c.audience] ?? c.audience}</td>
                    <td className="px-4 py-3">{formatSchedule(c.scheduledAt)}</td>
                    <td className="px-4 py-3"><CampaignStatusBadge status={c.status} /></td>
                    <td className="px-4 py-3">
                      {c.sentCount} / {c.failedCount}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {canManage && c.status === "SCHEDULED" ? (
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={cancel.isPending}
                          onClick={() => cancel.mutate(c.id)}
                        >
                          Cancelar
                        </Button>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function CampaignStatusBadge({ status }: { status: CampaignStatus }) {
  if (status === "SCHEDULED") return <Badge variant="warning">Programada</Badge>;
  if (status === "SENDING") return <Badge variant="default">Enviando</Badge>;
  if (status === "SENT") return <Badge variant="success">Enviada</Badge>;
  return <Badge variant="muted">Cancelada</Badge>;
}

function formatSchedule(scheduledAt: string | null): string {
  if (!scheduledAt) return "Inmediata";
  return new Date(scheduledAt).toLocaleString("es-PE", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

function SettingsSection({ canManage }: { canManage: boolean }) {
  const { data: settings = [], isLoading, isError } = useNotificationSettings();

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 font-heading text-base font-semibold">Notificaciones automaticas</h2>
        <p className="mb-4 text-sm text-muted-foreground">
          Mensajes que la app envia sola ante eventos del usuario. Puedes activarlos y editar su texto.
        </p>
        {isLoading ? (
          <Spinner />
        ) : isError ? (
          <div className="text-sm text-muted-foreground">No se pudieron cargar las notificaciones automaticas.</div>
        ) : settings.length === 0 ? (
          <div className="text-sm text-muted-foreground">No hay notificaciones automaticas configuradas.</div>
        ) : (
          <div className="space-y-4">
            {settings.map((s) => (
              // key con updatedAt: al guardarse en el backend la fila se remonta con los datos frescos
              <SettingRow key={`${s.key}-${s.updatedAt}`} setting={s} canManage={canManage} />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function SettingRow({ setting, canManage }: { setting: NotificationSetting; canManage: boolean }) {
  const update = useUpdateSetting();
  const [enabled, setEnabled] = useState(setting.enabled);
  const [title, setTitle] = useState(setting.title);
  const [body, setBody] = useState(setting.body);

  const disabled = !canManage || update.isPending || !title.trim() || !body.trim();

  function save() {
    update.mutate({ key: setting.key, enabled, title: title.trim(), body: body.trim() });
  }

  return (
    <div className="rounded-lg border border-border p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div>
          <div className="text-sm font-medium">{setting.description}</div>
          <div className="text-xs text-muted-foreground">{setting.key}</div>
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="accent-brand-blue"
            checked={enabled}
            disabled={!canManage}
            onChange={(e) => setEnabled(e.target.checked)}
          />
          Activo
        </label>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        <div>
          <Label htmlFor={`setting-title-${setting.key}`}>Titulo</Label>
          <Input
            id={`setting-title-${setting.key}`}
            maxLength={120}
            value={title}
            disabled={!canManage}
            onChange={(e) => setTitle(e.target.value)}
            className="mt-1"
          />
        </div>
        <div>
          <Label htmlFor={`setting-body-${setting.key}`}>Mensaje</Label>
          <textarea
            id={`setting-body-${setting.key}`}
            rows={2}
            maxLength={500}
            value={body}
            disabled={!canManage}
            onChange={(e) => setBody(e.target.value)}
            className="mt-1 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
          />
        </div>
      </div>
      {canManage && (
        <div className="mt-3 flex items-center gap-3">
          <Button size="sm" onClick={save} disabled={disabled}>
            {update.isPending ? "Guardando…" : "Guardar"}
          </Button>
          {update.isSuccess && <span className="text-sm text-green-600">Guardado</span>}
          {update.isError && <span className="text-sm text-destructive">No se pudo guardar.</span>}
        </div>
      )}
    </div>
  );
}
