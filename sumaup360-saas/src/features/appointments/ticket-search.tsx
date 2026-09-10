"use client";

import { useState } from "react";
import Link from "next/link";
import { fetchAppointmentByTicket, type Appointment, type AppointmentStatus } from "./api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { Badge } from "@/components/ui/misc";

const statusLabel: Record<AppointmentStatus, string> = {
  REQUESTED: "Solicitada", SCHEDULED: "Programada", CONFIRMED: "Confirmada",
  COMPLETED: "Atendida", CANCELED: "Cancelada", NO_SHOW: "No asistio",
};
const statusVariant = (s: AppointmentStatus) =>
  s === "REQUESTED" ? "warning" : s === "CONFIRMED" || s === "COMPLETED" ? "success" : "muted";

/**
 * Buscador rapido de citas por codigo de ticket (el que recibe el cliente al reservar).
 * Se usa en el inicio del panel y en la pagina de Citas.
 */
export function TicketSearch({ withLink = false }: { withLink?: boolean }) {
  const [code, setCode] = useState("");
  const [result, setResult] = useState<Appointment | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const search = async () => {
    if (!code.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      setResult(await fetchAppointmentByTicket(code));
    } catch (e) {
      setError((e as Error).message || "No hay ninguna cita con ese codigo.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-5">
        <div className="mb-3">
          <p className="text-sm font-semibold">Buscar ticket de cita</p>
          <p className="text-sm text-muted-foreground">
            Ingresa el codigo que muestra el cliente y ubica su cita al instante.
          </p>
        </div>
        <div className="flex items-end gap-2">
          <div className="w-44 space-y-1">
            <Label>Codigo</Label>
            <Input
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              onKeyDown={(e) => { if (e.key === "Enter") void search(); }}
              placeholder="Ej. K7M2QF"
              maxLength={10}
              className="font-mono tracking-widest"
            />
          </div>
          <Button disabled={!code.trim() || loading} onClick={search}>
            {loading ? "Buscando..." : "Buscar"}
          </Button>
        </div>

        {error && <p className="mt-3 text-sm text-destructive">{error}</p>}

        {result && (
          <div className="mt-4 rounded-xl border border-border bg-muted/40 p-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-mono text-sm font-bold tracking-widest">{result.ticketCode}</span>
              <Badge variant={statusVariant(result.status)}>
                {statusLabel[result.status]}{result.source === "ONLINE" ? " · online" : ""}
              </Badge>
            </div>
            <p className="mt-2 text-sm font-medium">
              {result.customerName ?? "Cliente"}{result.patientName ? ` — ${result.patientName}` : ""}
            </p>
            <p className="text-sm text-muted-foreground">
              {new Date(result.scheduledAt).toLocaleString("es-PE", {
                weekday: "long", day: "2-digit", month: "long", hour: "2-digit", minute: "2-digit",
              })}
              {result.reason ? ` · ${result.reason}` : ""}
            </p>
            {withLink && (
              <Link href="/dashboard/appointments" className="mt-2 inline-block text-sm font-medium text-primary hover:underline">
                Ir a Citas
              </Link>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
