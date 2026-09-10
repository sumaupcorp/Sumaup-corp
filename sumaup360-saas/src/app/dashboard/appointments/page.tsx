"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Cropper, { type Area } from "react-easy-crop";
import type QRCodeStyling from "qr-code-styling";
import type { Options as QrOptions } from "qr-code-styling";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage } from "@/lib/firebase";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useCustomers } from "@/features/customers/api";
import { usePatients } from "@/features/patients/api";
import {
  useAppointments, useCreateAppointment, useSetAppointmentStatus, useUpdateAppointment,
  useBookingPage, useUpdateBookingPage, useRegenerateBookingToken,
  type Appointment, type AppointmentStatus, type BookingField, type QrStyle,
} from "@/features/appointments/api";
import { TicketSearch } from "@/features/appointments/ticket-search";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";

const BOOKING_WEB_BASE = process.env.NEXT_PUBLIC_BOOKING_WEB_BASE ?? "https://sumaup360.com";

const statusLabel: Record<AppointmentStatus, string> = {
  REQUESTED: "Solicitada", SCHEDULED: "Programada", CONFIRMED: "Confirmada",
  COMPLETED: "Atendida", CANCELED: "Cancelada", NO_SHOW: "No asistio",
};
const statusVariant = (s: AppointmentStatus) =>
  s === "REQUESTED" ? "warning" : s === "CONFIRMED" || s === "COMPLETED" ? "success" : "muted";

/** Colores del bloque en el calendario por estado de la cita. */
const statusBlockStyle: Record<AppointmentStatus, string> = {
  REQUESTED: "border-amber-500 bg-amber-50 text-amber-900",
  SCHEDULED: "border-primary bg-accent text-accent-foreground",
  CONFIRMED: "border-green-600 bg-green-50 text-green-900",
  COMPLETED: "border-slate-400 bg-muted text-muted-foreground",
  CANCELED: "border-red-300 bg-red-50 text-red-400 line-through",
  NO_SHOW: "border-red-400 bg-red-50 text-red-700",
};

const VIEW_STORAGE_KEY = "sumaup.appointments.view";
type AgendaView = "list" | "calendar";

type Tab = "agenda" | "reserva";

export default function AppointmentsPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const [tab, setTab] = useState<Tab>("agenda");
  const canManage = hasPerm("appointment:manage");

  // La pestaña vive en la URL (?tab=reserva) para sobrevivir al refresh y poder compartirse.
  useEffect(() => {
    if (new URLSearchParams(window.location.search).get("tab") === "reserva") setTab("reserva");
  }, []);
  const changeTab = (t: Tab) => {
    setTab(t);
    const url = new URL(window.location.href);
    if (t === "agenda") url.searchParams.delete("tab");
    else url.searchParams.set("tab", t);
    window.history.replaceState(null, "", url);
  };

  return (
    <div>
      <PageHeader title="Citas" subtitle="Agenda de citas y reserva online por QR" />

      {canManage && currentCompany && (
        <div className="mb-6 inline-flex rounded-lg border border-border bg-muted p-1">
          <TabButton active={tab === "agenda"} onClick={() => changeTab("agenda")}>Agenda</TabButton>
          <TabButton active={tab === "reserva"} onClick={() => changeTab("reserva")}>Reserva online (QR)</TabButton>
        </div>
      )}

      {tab === "agenda" || !currentCompany
        ? <AgendaTab canManage={canManage} />
        : <BookingConfig companyId={currentCompany.id} />}
    </div>
  );
}

function TabButton({ active, onClick, children }: {
  active: boolean; onClick: () => void; children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "rounded-md px-4 py-1.5 text-sm font-medium transition " +
        (active ? "bg-white text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground")
      }
    >
      {children}
    </button>
  );
}

/* ------------------------------- AGENDA ------------------------------- */

function AgendaTab({ canManage }: { canManage: boolean }) {
  const { currentCompany, hasModule } = useCompany();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const [branchId, setBranchId] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [view, setView] = useState<AgendaView>("list");

  // La vista elegida (filas o calendario) se recuerda por navegador.
  useEffect(() => {
    if (window.localStorage.getItem(VIEW_STORAGE_KEY) === "calendar") setView("calendar");
  }, []);
  const changeView = (v: AgendaView) => {
    setView(v);
    window.localStorage.setItem(VIEW_STORAGE_KEY, v);
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="w-full max-w-xs space-y-1">
            <Label>Sucursal</Label>
            <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
              <option value="">Todas</option>
              {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </Select>
          </div>
          <div className="flex rounded-lg border border-border bg-muted p-1">
            <TabButton active={view === "list"} onClick={() => changeView("list")}>Lista</TabButton>
            <TabButton active={view === "calendar"} onClick={() => changeView("calendar")}>Calendario</TabButton>
          </div>
        </div>
        {canManage && (
          <Button onClick={() => setShowForm((v) => !v)} variant={showForm ? "outline" : "default"}>
            {showForm ? "Cerrar formulario" : "Nueva cita"}
          </Button>
        )}
      </div>

      {canManage && showForm && (
        <NewAppointmentForm
          branches={branches}
          defaultBranchId={branchId}
          hasPatients={hasModule("patients")}
          onCreated={() => setShowForm(false)}
        />
      )}

      <TicketSearch />

      {view === "list"
        ? <AgendaList branchId={branchId} canManage={canManage} />
        : <WeekCalendar branchId={branchId} canManage={canManage} />}
    </div>
  );
}

/** Vista tradicional por filas: proximos 30 dias. */
function AgendaList({ branchId, canManage }: { branchId: string; canManage: boolean }) {
  const appointments = useAppointments(branchId || undefined);
  const setStatus = useSetAppointmentStatus();
  const list = appointments.data ?? [];

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-3 text-sm font-semibold">Proximos 30 dias</h2>
        {appointments.isLoading ? <Spinner /> : list.length === 0 ? (
          <div className="py-8 text-center">
            <p className="text-sm font-medium">Sin citas en el rango</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Crea una cita con el boton Nueva cita o comparte tu pagina de reserva online.
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-border text-sm">
            {list.map((a) => {
              const d = new Date(a.scheduledAt);
              return (
                <li key={a.id} className="flex items-start gap-4 py-3">
                  <div className="w-16 shrink-0 rounded-lg bg-accent px-2 py-1.5 text-center">
                    <span className="block text-xs text-accent-foreground">
                      {d.toLocaleDateString("es-PE", { day: "2-digit", month: "short" })}
                    </span>
                    <span className="text-sm font-semibold text-accent-foreground">
                      {d.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}
                    </span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-medium">
                        {a.customerName ?? "Cliente"}{a.patientName ? ` — ${a.patientName}` : ""}
                      </span>
                      <Badge variant={statusVariant(a.status)}>
                        {statusLabel[a.status]}{a.source === "ONLINE" ? " · online" : ""}
                      </Badge>
                    </div>
                    {a.reason && <p className="mt-0.5 truncate text-muted-foreground">{a.reason}</p>}
                  </div>
                  {canManage && (
                    <span className="flex shrink-0 gap-1">
                      <AppointmentActions appointment={a} setStatus={setStatus} />
                    </span>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

/** Textos del dialogo de confirmacion por cambio de estado. */
const confirmCopy: Partial<Record<AppointmentStatus, {
  title: string; message: (who: string) => string; confirmLabel: string; destructive?: boolean;
}>> = {
  CONFIRMED: {
    title: "Confirmar cita",
    message: (who) => `La cita de ${who} quedara marcada como confirmada.`,
    confirmLabel: "Si, confirmar",
  },
  COMPLETED: {
    title: "Marcar como atendida",
    message: (who) => `La cita de ${who} quedara registrada como atendida. Ya no podras cambiarla de estado.`,
    confirmLabel: "Si, fue atendida",
  },
  CANCELED: {
    title: "Cancelar cita",
    message: (who) => `La cita de ${who} se cancelara y no se puede deshacer. Recuerda avisarle al cliente.`,
    confirmLabel: "Si, cancelar cita",
    destructive: true,
  },
};

/**
 * Botones de accion de una cita (compartidos entre lista y calendario). Ningun cambio
 * se aplica directo: los estados pasan por un dialogo de confirmacion y reprogramar
 * abre su propio modal con la nueva fecha.
 */
function AppointmentActions({ appointment: a, setStatus, onAfter }: {
  appointment: Appointment;
  setStatus: ReturnType<typeof useSetAppointmentStatus>;
  onAfter?: () => void;
}) {
  const [pending, setPending] = useState<AppointmentStatus | null>(null);
  const [rescheduling, setRescheduling] = useState(false);
  const active = !["COMPLETED", "CANCELED"].includes(a.status);

  const confirm = async () => {
    if (!pending) return;
    try {
      await setStatus.mutateAsync({ id: a.id, status: pending });
    } finally {
      setPending(null);
      onAfter?.();
    }
  };

  const who = a.customerName ?? "el cliente";
  const copy = pending ? confirmCopy[pending] : null;

  return (
    <>
      {["REQUESTED", "SCHEDULED"].includes(a.status) && (
        <Button size="sm" variant="outline" onClick={() => setPending("CONFIRMED")}>Confirmar</Button>
      )}
      {a.status === "CONFIRMED" && (
        <Button size="sm" variant="outline" onClick={() => setPending("COMPLETED")}>Atendida</Button>
      )}
      {active && (
        <Button size="sm" variant="outline" onClick={() => setRescheduling(true)}>Reprogramar</Button>
      )}
      {active && (
        <Button size="sm" variant="ghost" onClick={() => setPending("CANCELED")}>Cancelar</Button>
      )}

      {pending && copy && (
        <ConfirmDialog
          title={copy.title}
          message={copy.message(who)}
          confirmLabel={copy.confirmLabel}
          destructive={copy.destructive}
          loading={setStatus.isPending}
          onConfirm={confirm}
          onCancel={() => setPending(null)}
        />
      )}

      {rescheduling && (
        <RescheduleModal
          appointment={a}
          onClose={() => setRescheduling(false)}
          onDone={() => { setRescheduling(false); onAfter?.(); }}
        />
      )}
    </>
  );
}

/** Fecha en formato datetime-local (hora local del navegador). */
function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Reprogramacion: nueva fecha/hora (y duracion) con confirmacion explicita. */
function RescheduleModal({ appointment: a, onClose, onDone }: {
  appointment: Appointment;
  onClose: () => void;
  onDone: () => void;
}) {
  const update = useUpdateAppointment();
  const [scheduledAt, setScheduledAt] = useState(() => toLocalInput(a.scheduledAt));
  const [duration, setDuration] = useState(String(a.durationMinutes || 30));
  const [error, setError] = useState<string | null>(null);

  const current = new Date(a.scheduledAt);
  const changed = scheduledAt !== toLocalInput(a.scheduledAt)
    || Number(duration) !== (a.durationMinutes || 30);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const next = new Date(scheduledAt);
    if (isNaN(next.getTime())) {
      setError("Elige una fecha y hora validas.");
      return;
    }
    if (next.getTime() < Date.now()) {
      setError("La nueva fecha no puede estar en el pasado.");
      return;
    }
    try {
      await update.mutateAsync({
        id: a.id,
        scheduledAt: next.toISOString(),
        durationMinutes: Number(duration) || undefined,
      });
      onDone();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-24"
      onClick={onClose}>
      <div className="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}>
        <h3 className="text-sm font-semibold">Reprogramar cita</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          {a.customerName ?? "Cliente"}{a.patientName ? ` — ${a.patientName}` : ""}
        </p>
        <p className="mt-1.5 rounded-lg bg-muted/60 px-3 py-2 text-xs text-muted-foreground">
          Fecha actual: {current.toLocaleDateString("es-PE", { weekday: "long", day: "2-digit", month: "long" })}
          {" a las "}{current.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}
        </p>

        <form onSubmit={submit} className="mt-3 space-y-3">
          <div className="space-y-1">
            <Label>Nueva fecha y hora</Label>
            <Input type="datetime-local" className="h-11" value={scheduledAt}
              onChange={(e) => setScheduledAt(e.target.value)} required />
          </div>
          <div className="space-y-1">
            <Label>Duracion (minutos)</Label>
            <Input type="number" min="5" step="5" className="h-11" value={duration}
              onChange={(e) => setDuration(e.target.value)} />
          </div>

          <p className="text-xs text-muted-foreground">
            La cita mantiene su estado actual. Recuerda avisar el cambio al cliente.
          </p>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" className="min-h-11" disabled={update.isPending}
              onClick={onClose}>
              Volver
            </Button>
            <Button type="submit" className="min-h-11" disabled={!changed || update.isPending}>
              {update.isPending ? "Guardando..." : "Reprogramar"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* --------------------------- VISTA CALENDARIO --------------------------- */

const CAL_START_HOUR = 7;
const CAL_END_HOUR = 21;
const HOUR_PX = 48;

/** Lunes de la semana de la fecha dada, a medianoche. */
function startOfWeek(d: Date): Date {
  const x = new Date(d);
  x.setDate(x.getDate() - ((x.getDay() + 6) % 7));
  x.setHours(0, 0, 0, 0);
  return x;
}

/** Vista semanal tipo Teams/Google Calendar: columnas por dia, bloques por hora. */
function WeekCalendar({ branchId, canManage }: { branchId: string; canManage: boolean }) {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [selected, setSelected] = useState<Appointment | null>(null);
  const setStatus = useSetAppointmentStatus();

  const weekEnd = useMemo(() => {
    const e = new Date(weekStart);
    e.setDate(e.getDate() + 7);
    return e;
  }, [weekStart]);

  const appointments = useAppointments(
    branchId || undefined, weekStart.toISOString(), weekEnd.toISOString());

  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart);
    d.setDate(d.getDate() + i);
    return d;
  }), [weekStart]);

  const todayKey = new Date().toDateString();
  const hours = Array.from({ length: CAL_END_HOUR - CAL_START_HOUR }, (_, i) => CAL_START_HOUR + i);
  const weekCount = (appointments.data ?? []).length;

  /** Citas de un dia, con posicion vertical (top/height en px) segun hora y duracion. */
  const dayBlocks = (day: Date) =>
    (appointments.data ?? [])
      .filter((a) => new Date(a.scheduledAt).toDateString() === day.toDateString())
      .sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt))
      .map((a, idx) => {
        const d = new Date(a.scheduledAt);
        const minutes = (d.getHours() - CAL_START_HOUR) * 60 + d.getMinutes();
        const top = Math.max(0, (minutes / 60) * HOUR_PX);
        const height = Math.max(34, ((a.durationMinutes || 60) / 60) * HOUR_PX - 3);
        return { a, top, height, idx };
      });

  const moveWeek = (delta: number) => {
    const d = new Date(weekStart);
    d.setDate(d.getDate() + delta * 7);
    setWeekStart(d);
  };

  // Salto directo a una fecha: el input muestra el lunes de la semana visible.
  const toISODate = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  const jumpToDate = (value: string) => {
    if (!value) return;
    const picked = new Date(value + "T00:00:00");
    if (!isNaN(picked.getTime())) setWeekStart(startOfWeek(picked));
  };

  // Linea de "ahora" sobre la columna de hoy (solo dentro del horario visible).
  const now = new Date();
  const nowMinutes = (now.getHours() - CAL_START_HOUR) * 60 + now.getMinutes();
  const nowTop = (nowMinutes / 60) * HOUR_PX;
  const nowVisible = nowMinutes >= 0 && now.getHours() < CAL_END_HOUR;

  const clean = (s: string) => s.replace(".", "");
  const rangeLabel = `${clean(days[0].toLocaleDateString("es-PE", { day: "2-digit", month: "short" }))} — ${clean(days[6].toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" }))}`;

  return (
    <Card>
      <CardContent className="p-4">
        {/* Barra de navegacion: semana, salto por fecha y resumen */}
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex overflow-hidden rounded-lg border border-border">
              <button type="button" onClick={() => moveWeek(-1)} aria-label="Semana anterior"
                className="min-h-10 border-r border-border px-3 text-base text-muted-foreground transition touch-manipulation hover:bg-muted hover:text-foreground active:scale-95">
                ‹
              </button>
              <button type="button" onClick={() => setWeekStart(startOfWeek(new Date()))}
                className="min-h-10 border-r border-border px-4 text-sm font-medium text-foreground transition touch-manipulation hover:bg-muted active:scale-95">
                Hoy
              </button>
              <button type="button" onClick={() => moveWeek(1)} aria-label="Semana siguiente"
                className="min-h-10 px-3 text-base text-muted-foreground transition touch-manipulation hover:bg-muted hover:text-foreground active:scale-95">
                ›
              </button>
            </div>
            <div className="flex items-center gap-1.5">
              <Label className="text-xs text-muted-foreground">Ir a</Label>
              <Input type="date" className="h-10 w-40"
                value={toISODate(days[0])}
                onChange={(e) => jumpToDate(e.target.value)} />
            </div>
          </div>
          <div className="flex items-center gap-2 text-right">
            <div>
              <p className="text-sm font-semibold capitalize">{rangeLabel}</p>
              <p className="text-xs text-muted-foreground">
                {appointments.isLoading
                  ? "Cargando..."
                  : weekCount === 0
                    ? "Sin citas esta semana"
                    : `${weekCount} cita${weekCount === 1 ? "" : "s"} esta semana`}
              </p>
            </div>
          </div>
        </div>

        {/* Grilla semanal con scroll horizontal en pantallas chicas */}
        <div className="overflow-x-auto rounded-xl border border-border">
          <div className="min-w-[760px]">
            {/* Cabecera de dias */}
            <div className="grid grid-cols-[52px_repeat(7,1fr)] border-b border-border bg-muted/60">
              <div />
              {days.map((d) => {
                const isToday = d.toDateString() === todayKey;
                return (
                  <div key={d.toISOString()}
                    className={"border-l border-border px-1 py-2 text-center " + (isToday ? "bg-accent/50" : "")}>
                    <p className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                      {clean(d.toLocaleDateString("es-PE", { weekday: "short" }))}
                    </p>
                    <p className={
                      "mx-auto mt-0.5 flex size-7 items-center justify-center rounded-full text-sm font-semibold " +
                      (isToday ? "bg-primary text-primary-foreground shadow-sm" : "text-foreground")
                    }>
                      {d.getDate()}
                    </p>
                  </div>
                );
              })}
            </div>

            {/* Cuerpo: gutter de horas + 7 columnas */}
            <div className="grid grid-cols-[52px_repeat(7,1fr)] bg-white">
              {/* Horas */}
              <div className="relative bg-muted/30" style={{ height: hours.length * HOUR_PX }}>
                {hours.map((h, i) => (
                  <span key={h}
                    className="absolute right-1.5 -translate-y-1/2 text-[10px] font-medium tabular-nums text-muted-foreground"
                    style={{ top: i * HOUR_PX }}>
                    {i === 0 ? "" : `${String(h).padStart(2, "0")}:00`}
                  </span>
                ))}
              </div>

              {days.map((day) => {
                const isToday = day.toDateString() === todayKey;
                const isWeekend = day.getDay() === 0 || day.getDay() === 6;
                return (
                  <div key={day.toISOString()}
                    className={
                      "relative border-l border-border " +
                      (isToday ? "bg-accent/25" : isWeekend ? "bg-muted/30" : "")
                    }
                    style={{ height: hours.length * HOUR_PX }}>
                    {/* Lineas de hora y media hora */}
                    {hours.map((h, i) => (
                      <div key={h}>
                        <div className="absolute inset-x-0 border-t border-border/70"
                          style={{ top: i * HOUR_PX }} />
                        <div className="absolute inset-x-0 border-t border-dashed border-border/35"
                          style={{ top: i * HOUR_PX + HOUR_PX / 2 }} />
                      </div>
                    ))}

                    {/* Linea de AHORA en el dia actual */}
                    {isToday && nowVisible && (
                      <div className="absolute inset-x-0 z-20" style={{ top: nowTop }}>
                        <div className="relative border-t-2 border-red-500">
                          <span className="absolute -left-1 -top-[5px] size-2 rounded-full bg-red-500" />
                        </div>
                      </div>
                    )}

                    {/* Bloques de cita */}
                    {dayBlocks(day).map(({ a, top, height, idx }) => (
                      <button
                        key={a.id}
                        type="button"
                        onClick={() => setSelected(a)}
                        title={`${a.customerName ?? "Cliente"}${a.reason ? " · " + a.reason : ""}`}
                        className={
                          "absolute z-10 w-[92%] overflow-hidden rounded-lg border-l-4 px-2 py-1 text-left " +
                          "shadow-sm ring-1 ring-black/5 transition touch-manipulation hover:z-30 hover:shadow-md active:scale-[0.98] " +
                          statusBlockStyle[a.status]
                        }
                        style={{ top, height, left: `${3 + (idx % 3) * 3}%` }}
                      >
                        <p className="truncate text-[11px] font-semibold leading-tight">
                          {a.customerName ?? "Cliente"}{a.patientName ? ` — ${a.patientName}` : ""}
                        </p>
                        <p className="truncate text-[10px] opacity-80">
                          {new Date(a.scheduledAt).toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}
                          {a.reason ? ` · ${a.reason}` : ""}
                        </p>
                      </button>
                    ))}
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Leyenda de estados */}
        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted-foreground">
          <LegendDot className="bg-amber-500" label="Solicitada" />
          <LegendDot className="bg-primary" label="Programada" />
          <LegendDot className="bg-green-600" label="Confirmada" />
          <LegendDot className="bg-slate-400" label="Atendida" />
          <LegendDot className="bg-red-400" label="Cancelada / no asistio" />
          <span className="ml-auto">Toca una cita para ver su detalle · Horario 07:00–21:00</span>
        </div>
      </CardContent>

      {selected && (
        <AppointmentDetailModal
          appointment={selected}
          canManage={canManage}
          setStatus={setStatus}
          onClose={() => setSelected(null)}
        />
      )}
    </Card>
  );
}

function LegendDot({ className, label }: { className: string; label: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className={"size-2.5 rounded-full " + className} />
      {label}
    </span>
  );
}

/** Detalle de una cita desde el calendario, con las mismas acciones de la lista. */
function AppointmentDetailModal({ appointment: a, canManage, setStatus, onClose }: {
  appointment: Appointment;
  canManage: boolean;
  setStatus: ReturnType<typeof useSetAppointmentStatus>;
  onClose: () => void;
}) {
  const d = new Date(a.scheduledAt);
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-20">
      <div className="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">Detalle de la cita</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        <div className="space-y-2 text-sm">
          <p className="font-medium">
            {a.customerName ?? "Cliente"}{a.patientName ? ` — ${a.patientName}` : ""}
          </p>
          <p className="text-muted-foreground">
            {d.toLocaleDateString("es-PE", { weekday: "long", day: "2-digit", month: "long" })}
            {" · "}
            {d.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}
            {" · "}{a.durationMinutes || 60} min
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={statusVariant(a.status)}>{statusLabel[a.status]}</Badge>
            {a.source === "ONLINE" && <Badge variant="muted">Reserva online</Badge>}
            {a.ticketCode && <Badge variant="muted">{a.ticketCode}</Badge>}
          </div>
          {a.reason && <p className="text-muted-foreground">Motivo: {a.reason}</p>}
          {a.notes && <p className="text-muted-foreground">Notas: {a.notes}</p>}
        </div>

        {canManage && (
          <div className="mt-4 flex flex-wrap justify-end gap-1 border-t border-border pt-3">
            <AppointmentActions appointment={a} setStatus={setStatus} onAfter={onClose} />
          </div>
        )}
      </div>
    </div>
  );
}

function NewAppointmentForm({ branches, defaultBranchId, hasPatients, onCreated }: {
  branches: { id: string; name: string }[];
  defaultBranchId: string;
  hasPatients: boolean;
  onCreated: () => void;
}) {
  const { data: customers = [] } = useCustomers();
  const { data: patients = [] } = usePatients();
  const createAppointment = useCreateAppointment();

  const [branchId, setBranchId] = useState(defaultBranchId);
  const [customerId, setCustomerId] = useState("");
  const [patientId, setPatientId] = useState("");
  const [reason, setReason] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setError(null);
    try {
      await createAppointment.mutateAsync({
        branchId, customerId,
        patientId: patientId || undefined,
        reason: reason || undefined,
        scheduledAt: new Date(scheduledAt).toISOString(),
      });
      onCreated();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Card>
      <CardContent className="space-y-3 p-5">
        <h2 className="text-sm font-semibold">Nueva cita</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div className="space-y-1">
            <Label>Sucursal</Label>
            <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
              <option value="">Selecciona…</option>
              {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </Select>
          </div>
          <div className="space-y-1">
            <Label>Cliente</Label>
            <Select value={customerId} onChange={(e) => { setCustomerId(e.target.value); setPatientId(""); }}>
              <option value="">Selecciona…</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </div>
          {hasPatients && (
            <div className="space-y-1">
              <Label>Paciente (opcional)</Label>
              <Select value={patientId} onChange={(e) => setPatientId(e.target.value)} disabled={!customerId}>
                <option value="">—</option>
                {patients.filter((p) => p.customerId === customerId).map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </Select>
            </div>
          )}
          <div className="space-y-1">
            <Label>Fecha y hora</Label>
            <Input type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} />
          </div>
          <div className="space-y-1">
            <Label>Motivo</Label>
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Control, vacuna..." />
          </div>
        </div>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <Button disabled={!branchId || !customerId || !scheduledAt || createAppointment.isPending} onClick={submit}>
          {createAppointment.isPending ? "Guardando..." : "Crear cita"}
        </Button>
      </CardContent>
    </Card>
  );
}

/* --------------------------- RESERVA ONLINE --------------------------- */

/** Configurador de la pagina publica de reserva + vista previa en vivo. */
function BookingConfig({ companyId }: { companyId: string }) {
  const bookingPage = useBookingPage(companyId);
  const updatePage = useUpdateBookingPage(companyId);
  const regenerate = useRegenerateBookingToken(companyId);
  const { data: branches = [] } = useBranches(companyId);
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [newFieldLabel, setNewFieldLabel] = useState("");
  const [cropSrc, setCropSrc] = useState<string | null>(null);
  const [logoError, setLogoError] = useState<string | null>(null);
  const [qrStyle, setQrStyle] = useState<QrStyle>(DEFAULT_QR_STYLE);
  const qrInstance = useRef<QRCodeStyling | null>(null);

  // Estado local de textos para que la vista previa se actualice al escribir;
  // se guarda al salir del campo (onBlur) solo si cambio.
  const [title, setTitle] = useState("");
  const [welcome, setWelcome] = useState("");
  const page = bookingPage.data;
  useEffect(() => {
    if (page) {
      setTitle(page.title ?? "");
      setWelcome(page.welcomeText ?? "");
      if (page.qrStyle) setQrStyle({ ...DEFAULT_QR_STYLE, ...page.qrStyle });
    }
  }, [page]);

  if (bookingPage.isLoading) return <div className="mt-2"><Spinner /></div>;
  if (!page) return null;

  const bookingUrl = `${BOOKING_WEB_BASE}/reservas/${page.token}`;
  const fields = page.formConfig ?? [];
  const saveFields = (next: BookingField[]) => updatePage.mutate({ formConfig: next });

  const pickLogo = (file: File) => {
    setLogoError(null);
    if (!file.type.startsWith("image/")) {
      setLogoError("El archivo debe ser una imagen.");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setLogoError("La imagen no debe superar los 10MB.");
      return;
    }
    setCropSrc(URL.createObjectURL(file));
  };

  const uploadCropped = async (blob: Blob) => {
    setUploading(true);
    try {
      const dest = ref(storage, `booking/${companyId}/${Date.now()}-logo.jpg`);
      await uploadBytes(dest, blob, { contentType: "image/jpeg" });
      const url = await getDownloadURL(dest);
      await updatePage.mutateAsync({ logoUrl: url });
    } catch {
      setLogoError("No pudimos subir la imagen. Intenta de nuevo.");
    } finally {
      setUploading(false);
      if (cropSrc) URL.revokeObjectURL(cropSrc);
      setCropSrc(null);
    }
  };

  return (
    <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
      <div className="space-y-4">
        {/* Estado y enlace */}
        <Card>
          <CardContent className="p-5">
            <div className="mb-4 flex items-start justify-between gap-4">
              <div>
                <h2 className="text-sm font-semibold">Estado y enlace</h2>
                <p className="text-sm text-muted-foreground">
                  Comparte el QR o el enlace y tus clientes reservan desde su celular.
                </p>
              </div>
              <label className="flex shrink-0 cursor-pointer items-center gap-2 text-sm font-medium">
                <input
                  type="checkbox"
                  className="size-4 accent-primary"
                  checked={page.enabled}
                  onChange={(e) => updatePage.mutate({ enabled: e.target.checked })}
                />
                {page.enabled ? "Activa" : "Desactivada"}
              </label>
            </div>

            {!page.enabled && (
              <p className="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-700">
                La pagina esta desactivada: el enlace mostrara &quot;no disponible&quot; hasta que la actives.
              </p>
            )}

            <div className="flex flex-wrap items-center gap-4">
              <div className="rounded-xl border border-border bg-white p-3">
                <StyledQr value={bookingUrl} size={128} style={qrStyle}
                  logoUrl={page.logoUrl} instanceRef={qrInstance} />
              </div>
              <div className="min-w-0 flex-1 space-y-2">
                <p className="break-all text-xs text-muted-foreground">{bookingUrl}</p>
                <div className="flex flex-wrap gap-2">
                  <Button size="sm" variant="outline"
                    onClick={async () => { await navigator.clipboard.writeText(bookingUrl); setCopied(true); setTimeout(() => setCopied(false), 1500); }}>
                    {copied ? "Copiado" : "Copiar enlace"}
                  </Button>
                  <Button size="sm" variant="outline"
                    onClick={() => qrInstance.current?.download({ name: "qr-reservas", extension: "png" })}>
                    Descargar PNG
                  </Button>
                  <Button size="sm" variant="ghost" onClick={() => regenerate.mutate()}>Regenerar QR</Button>
                </div>
                <p className="text-xs text-muted-foreground">
                  Al regenerar, el QR y el enlace anteriores dejan de funcionar.
                </p>
              </div>
            </div>

            <QrDesigner
              value={bookingUrl}
              style={qrStyle}
              hasLogo={!!page.logoUrl}
              onChange={(next) => { setQrStyle(next); updatePage.mutate({ qrStyle: next }); }}
            />
          </CardContent>
        </Card>

        {/* Personalizacion */}
        <Card>
          <CardContent className="space-y-3 p-5">
            <h2 className="text-sm font-semibold">Personalizacion</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1">
                <Label>Nombre visible</Label>
                <Input value={title} onChange={(e) => setTitle(e.target.value)}
                  placeholder="Ej. Veterinaria San Martin"
                  onBlur={() => { if (title !== (page.title ?? "")) updatePage.mutate({ title }); }} />
              </div>
              <div className="space-y-1">
                <Label>Logo / imagen</Label>
                <div className="flex items-center gap-4">
                  {page.logoUrl ? (
                    <div className="rounded-2xl border border-border bg-white p-1.5 shadow-sm">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={page.logoUrl} alt="Logo" className="size-24 rounded-xl object-cover" />
                    </div>
                  ) : (
                    <div className="flex size-24 items-center justify-center rounded-2xl border-2 border-dashed border-border text-xs text-muted-foreground">
                      Tu logo
                    </div>
                  )}
                  <div className="space-y-1.5">
                    <div className="flex flex-wrap gap-2">
                      <input ref={fileRef} type="file" accept="image/*" className="hidden"
                        onChange={(e) => { const f = e.target.files?.[0]; if (f) pickLogo(f); e.target.value = ""; }} />
                      <Button size="sm" variant="outline" disabled={uploading} onClick={() => fileRef.current?.click()}>
                        {uploading ? "Subiendo..." : page.logoUrl ? "Cambiar" : "Subir imagen"}
                      </Button>
                      {page.logoUrl && !uploading && (
                        <Button size="sm" variant="ghost" onClick={() => updatePage.mutate({ logoUrl: "" })}>Quitar</Button>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">Se recorta en cuadrado antes de subir.</p>
                  </div>
                </div>
                {logoError && <p className="text-xs text-destructive">{logoError}</p>}
              </div>
            </div>
            <div className="space-y-1">
              <Label>Mensaje de bienvenida</Label>
              <Input value={welcome} onChange={(e) => setWelcome(e.target.value)}
                placeholder="Ej. Agenda tu cita y te confirmamos por WhatsApp en minutos"
                onBlur={() => { if (welcome !== (page.welcomeText ?? "")) updatePage.mutate({ welcomeText: welcome }); }} />
              <p className="text-xs text-muted-foreground">
                Si lo dejas vacio se muestra: &quot;Completa tus datos y te confirmamos por telefono.&quot;
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Campos del formulario */}
        <Card>
          <CardContent className="space-y-3 p-5">
            <div>
              <h2 className="text-sm font-semibold">Campos del formulario</h2>
              <p className="text-sm text-muted-foreground">
                Nombre y telefono siempre se piden: son la base del contacto.
              </p>
            </div>
            <ul className="divide-y divide-border text-sm">
              {fields.map((f, i) => {
                const fixed = ["nombre", "telefono"].includes(f.key);
                return (
                  <li key={f.key} className="flex items-center justify-between gap-3 py-2">
                    <span className="flex min-w-0 items-center gap-2">
                      <span className="truncate">{f.label}</span>
                      {fixed && <Badge variant="muted">Siempre</Badge>}
                    </span>
                    <span className="flex shrink-0 items-center gap-2">
                      {!fixed && (
                        <>
                          <label className="flex items-center gap-1 text-xs text-muted-foreground">
                            <input type="checkbox" className="accent-primary" checked={f.required}
                              onChange={(e) => saveFields(fields.map((x, j) => j === i ? { ...x, required: e.target.checked } : x))} />
                            Obligatorio
                          </label>
                          <Button size="sm" variant="ghost" onClick={() => saveFields(fields.filter((_, j) => j !== i))}>Quitar</Button>
                        </>
                      )}
                    </span>
                  </li>
                );
              })}
            </ul>
            <div className="flex items-end gap-2 border-t border-border pt-3">
              <div className="flex-1 space-y-1">
                <Label>Nuevo campo</Label>
                <Input value={newFieldLabel} onChange={(e) => setNewFieldLabel(e.target.value)} placeholder="Ej. Edad de la mascota" />
              </div>
              <Button size="sm" variant="outline" disabled={!newFieldLabel.trim()}
                onClick={() => {
                  const key = newFieldLabel.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
                  if (!key || fields.some((f) => f.key === key)) return;
                  saveFields([...fields, { key, label: newFieldLabel.trim(), type: "text", required: false }]);
                  setNewFieldLabel("");
                }}>
                Agregar
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      <BookingPreview
        title={title}
        welcomeText={welcome}
        logoUrl={page.logoUrl}
        fields={fields}
        branchCount={branches.length}
      />

      {cropSrc && (
        <LogoCropModal
          src={cropSrc}
          uploading={uploading}
          onCancel={() => { URL.revokeObjectURL(cropSrc); setCropSrc(null); }}
          onConfirm={uploadCropped}
        />
      )}
    </div>
  );
}

/** Modal de recorte cuadrado del logo: previsualiza, ajusta con zoom/arrastre y sube. */
function LogoCropModal({ src, uploading, onCancel, onConfirm }: {
  src: string;
  uploading: boolean;
  onCancel: () => void;
  onConfirm: (blob: Blob) => void;
}) {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedArea, setCroppedArea] = useState<Area | null>(null);
  const [error, setError] = useState<string | null>(null);

  const confirm = async () => {
    if (!croppedArea) return;
    setError(null);
    try {
      onConfirm(await cropToBlob(src, croppedArea));
    } catch {
      setError("No pudimos procesar la imagen. Prueba con otra.");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-4 shadow-xl">
        <h3 className="mb-1 text-sm font-semibold">Ajusta tu logo</h3>
        <p className="mb-3 text-sm text-muted-foreground">
          Arrastra y usa el zoom para encuadrarlo; se guarda en cuadrado.
        </p>
        <div className="relative h-72 overflow-hidden rounded-lg bg-muted">
          <Cropper
            image={src}
            crop={crop}
            zoom={zoom}
            aspect={1}
            showGrid={false}
            onCropChange={setCrop}
            onZoomChange={setZoom}
            onCropComplete={(_, areaPixels) => setCroppedArea(areaPixels)}
          />
        </div>
        <div className="mt-3 flex items-center gap-3">
          <span className="text-xs text-muted-foreground">Zoom</span>
          <input
            type="range" min={1} max={3} step={0.05} value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
            className="flex-1 accent-primary"
          />
        </div>
        {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" disabled={uploading} onClick={onCancel}>Cancelar</Button>
          <Button disabled={uploading || !croppedArea} onClick={confirm}>
            {uploading ? "Subiendo..." : "Recortar y subir"}
          </Button>
        </div>
      </div>
    </div>
  );
}

/* ---------------------------- DISEÑO DEL QR ---------------------------- */

const DEFAULT_QR_STYLE: QrStyle = {
  preset: "clasico", dotColor: "#05070D", bgColor: "#FFFFFF", withLogo: false,
};

/** Forma de los modulos por preset (estilo WhatsApp: diseños listos para elegir). */
const QR_PRESETS: Record<QrStyle["preset"], {
  label: string;
  defaultColor: string;
  dots: QrOptions["dotsOptions"];
  cornersSquare: QrOptions["cornersSquareOptions"];
  cornersDot: QrOptions["cornersDotOptions"];
}> = {
  clasico: {
    label: "Clasico", defaultColor: "#05070D",
    dots: { type: "square" }, cornersSquare: { type: "square" }, cornersDot: { type: "square" },
  },
  redondeado: {
    label: "Redondeado", defaultColor: "#0B5BFF",
    dots: { type: "rounded" }, cornersSquare: { type: "extra-rounded" }, cornersDot: { type: "dot" },
  },
  puntos: {
    label: "Puntos", defaultColor: "#1E8FFF",
    dots: { type: "dots" }, cornersSquare: { type: "extra-rounded" }, cornersDot: { type: "dot" },
  },
  elegante: {
    label: "Elegante", defaultColor: "#05070D",
    dots: { type: "classy-rounded" }, cornersSquare: { type: "extra-rounded" }, cornersDot: { type: "square" },
  },
};

function qrOptions(value: string, size: number, style: QrStyle, logoUrl?: string | null): QrOptions {
  const preset = QR_PRESETS[style.preset];
  const withLogo = style.withLogo && !!logoUrl;
  return {
    width: size,
    height: size,
    data: value,
    margin: 4,
    qrOptions: { errorCorrectionLevel: withLogo ? "H" : "M" },
    dotsOptions: { ...preset.dots, color: style.dotColor },
    cornersSquareOptions: { ...preset.cornersSquare, color: style.dotColor },
    cornersDotOptions: { ...preset.cornersDot, color: style.dotColor },
    backgroundOptions: { color: style.bgColor },
    image: withLogo ? logoUrl! : undefined,
    imageOptions: { crossOrigin: "anonymous", margin: 3, imageSize: 0.35, hideBackgroundDots: true },
  };
}

/** QR estilizado (qr-code-styling se importa dinamico: usa APIs del navegador). */
function StyledQr({ value, size, style, logoUrl, instanceRef }: {
  value: string;
  size: number;
  style: QrStyle;
  logoUrl?: string | null;
  instanceRef?: React.MutableRefObject<QRCodeStyling | null>;
}) {
  const holder = useRef<HTMLDivElement>(null);
  const qr = useRef<QRCodeStyling | null>(null);

  useEffect(() => {
    let alive = true;
    void (async () => {
      const { default: QRCodeStyling } = await import("qr-code-styling");
      if (!alive || !holder.current) return;
      const opts = qrOptions(value, size, style, logoUrl);
      if (!qr.current) {
        qr.current = new QRCodeStyling(opts);
        holder.current.innerHTML = "";
        qr.current.append(holder.current);
      } else {
        qr.current.update(opts);
      }
      if (instanceRef) instanceRef.current = qr.current;
    })();
    return () => { alive = false; };
  }, [value, size, style, logoUrl, instanceRef]);

  return <div ref={holder} style={{ width: size, height: size }} />;
}

/** Selector de diseño del QR: presets, colores y logo al centro. */
function QrDesigner({ value, style, hasLogo, onChange }: {
  value: string;
  style: QrStyle;
  hasLogo: boolean;
  onChange: (next: QrStyle) => void;
}) {
  return (
    <div className="mt-4 border-t border-border pt-4">
      <p className="mb-2 text-sm font-medium">Diseño del QR</p>
      <div className="flex flex-wrap gap-3">
        {(Object.keys(QR_PRESETS) as QrStyle["preset"][]).map((key) => {
          const p = QR_PRESETS[key];
          const active = style.preset === key;
          return (
            <button
              key={key}
              type="button"
              onClick={() => onChange({ ...style, preset: key, dotColor: p.defaultColor, bgColor: "#FFFFFF" })}
              className={
                "flex flex-col items-center gap-1.5 rounded-xl border p-2 transition " +
                (active ? "border-primary bg-accent" : "border-border bg-white hover:border-muted-foreground/40")
              }
            >
              <div className="pointer-events-none rounded-lg bg-white p-1">
                <StyledQr value={value} size={56}
                  style={{ preset: key, dotColor: p.defaultColor, bgColor: "#FFFFFF", withLogo: false }} />
              </div>
              <span className={"text-xs " + (active ? "font-semibold text-primary" : "text-muted-foreground")}>
                {p.label}
              </span>
            </button>
          );
        })}
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-6 gap-y-2">
        <label className="flex items-center gap-2 text-sm">
          <span className="text-muted-foreground">Color</span>
          <input type="color" value={style.dotColor}
            onChange={(e) => onChange({ ...style, dotColor: e.target.value })}
            className="size-7 cursor-pointer rounded border border-border bg-white p-0.5" />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <span className="text-muted-foreground">Fondo</span>
          <input type="color" value={style.bgColor}
            onChange={(e) => onChange({ ...style, bgColor: e.target.value })}
            className="size-7 cursor-pointer rounded border border-border bg-white p-0.5" />
        </label>
        <label className={"flex items-center gap-2 text-sm " + (hasLogo ? "" : "opacity-50")}>
          <input type="checkbox" className="accent-primary" checked={style.withLogo && hasLogo}
            disabled={!hasLogo}
            onChange={(e) => onChange({ ...style, withLogo: e.target.checked })} />
          Logo al centro
          {!hasLogo && <span className="text-xs text-muted-foreground">(sube un logo en Personalizacion)</span>}
        </label>
      </div>
    </div>
  );
}

/** Recorta el area elegida a un cuadrado de 512px y lo devuelve como JPEG. */
async function cropToBlob(src: string, area: Area): Promise<Blob> {
  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const i = new Image();
    i.onload = () => resolve(i);
    i.onerror = () => reject(new Error("image load"));
    i.src = src;
  });
  const size = 512;
  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("canvas");
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, size, size);
  ctx.drawImage(img, area.x, area.y, area.width, area.height, 0, 0, size, size);
  return new Promise((resolve, reject) =>
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error("blob"))), "image/jpeg", 0.9)
  );
}

/** Replica visual de la web publica de reservas (solo lectura, se actualiza en vivo). */
function BookingPreview({ title, welcomeText, logoUrl, fields, branchCount }: {
  title: string;
  welcomeText: string;
  logoUrl?: string | null;
  fields: BookingField[];
  branchCount: number;
}) {
  return (
    <div className="lg:sticky lg:top-6">
      <p className="mb-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
        Vista previa — asi lo ve tu cliente
      </p>
      <div className="mx-auto w-full max-w-[340px] overflow-hidden rounded-[2rem] border-[6px] border-slate-800 bg-slate-800 shadow-xl">
        <div className="mx-auto mb-1 mt-1.5 h-1.5 w-16 rounded-full bg-slate-600" />
        <div className="max-h-[600px] overflow-y-auto rounded-[1.6rem] bg-muted p-3">
          <div className="rounded-2xl border border-border bg-white p-4 shadow-sm">
            <div className="mb-4 flex items-center gap-3">
              {logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={logoUrl} alt="Logo" className="size-11 shrink-0 rounded-xl border border-border object-cover" />
              ) : (
                <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-accent text-sm font-bold text-accent-foreground">
                  {(title.trim() || "R").charAt(0).toUpperCase()}
                </div>
              )}
              <div className="min-w-0">
                <p className="truncate font-heading text-sm font-bold">{title.trim() || "Reserva tu cita"}</p>
                <p className="line-clamp-2 text-xs text-muted-foreground">
                  {welcomeText.trim() || "Completa tus datos y te confirmamos por telefono."}
                </p>
              </div>
            </div>

            <div className="pointer-events-none space-y-3">
              {branchCount > 1 && <PreviewField label="Local" hint="Elige el local…" />}
              <PreviewField label="Fecha y hora preferida" hint="dd/mm/aaaa --:--" />
              {fields.map((f) => (
                <PreviewField
                  key={f.key}
                  label={`${f.label}${f.required ? "" : " (opcional)"}`}
                  tall={f.type === "textarea"}
                />
              ))}
              <div className="rounded-lg bg-primary py-2 text-center text-xs font-semibold text-primary-foreground">
                Reservar cita
              </div>
              <p className="text-center text-[10px] text-muted-foreground">Con tecnologia de SUMAUP360</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function PreviewField({ label, hint, tall }: { label: string; hint?: string; tall?: boolean }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <div className={
        "flex items-center rounded-lg border border-border bg-white px-2.5 text-xs text-muted-foreground " +
        (tall ? "h-14 items-start pt-2" : "h-8")
      }>
        {hint ?? ""}
      </div>
    </div>
  );
}
