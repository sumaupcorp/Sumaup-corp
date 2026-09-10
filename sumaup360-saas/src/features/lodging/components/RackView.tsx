"use client";

import { useMemo, useState } from "react";
import {
  useRack, useSetRoomStatus,
  type RackRoom, type RoomStatus,
} from "@/features/lodging/api";
import { useBranches } from "@/features/branches/api";
import { useCompany } from "@/features/companies/company-context";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label, Select } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { CheckInModal, CheckOutModal, ChargesModal } from "./StayActions";
import { WalkInWizard } from "./WalkInWizard";

/** Estilo del semaforo por estado de la habitacion. */
const CARD_STYLE: Record<RoomStatus, string> = {
  AVAILABLE: "border-green-300 bg-green-50",
  OCCUPIED: "border-brand-blue/40 bg-accent",
  CLEANING: "border-amber-300 bg-amber-50",
  MAINTENANCE: "border-border bg-muted",
};

const STATUS_LABEL: Record<RoomStatus, string> = {
  AVAILABLE: "Disponible",
  OCCUPIED: "Ocupada",
  CLEANING: "Limpieza",
  MAINTENANCE: "Mantenimiento",
};

const STATUS_BADGE: Record<RoomStatus, "success" | "default" | "warning" | "muted"> = {
  AVAILABLE: "success",
  OCCUPIED: "default",
  CLEANING: "warning",
  MAINTENANCE: "muted",
};

const dateLabel = (iso: string) =>
  new Date(iso + "T00:00:00").toLocaleDateString("es-PE", { day: "2-digit", month: "short" }).replace(".", "");

/** Hora estimada de salida de una estadia por horas: check-in real + horas pactadas. */
const estimatedExit = (s: { checkedInAt?: string | null; hours?: number | null }) => {
  if (!s.checkedInAt || !s.hours) return null;
  const d = new Date(s.checkedInAt);
  d.setHours(d.getHours() + s.hours);
  return d.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" });
};

/**
 * Rack de habitaciones: quien esta en cual, que esta libre, sucia o en
 * mantenimiento, con las acciones del dia (check-in, check-out, marcar limpia).
 */
export function RackView({ canManage }: { canManage: boolean }) {
  const { currentCompany } = useCompany();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const [branchId, setBranchId] = useState("");
  const effectiveBranch = branchId || branches[0]?.id || "";

  const rack = useRack(effectiveBranch || undefined);
  const setRoomStatus = useSetRoomStatus();

  const [walkIn, setWalkIn] = useState(false);
  const [checkInStay, setCheckInStay] = useState<{ id: string; who: string; room: string } | null>(null);
  const [checkOutStay, setCheckOutStay] = useState<{ id: string; who: string; room: string } | null>(null);
  const [chargesStayId, setChargesStayId] = useState<string | null>(null);
  const [statusChange, setStatusChange] = useState<{ room: RackRoom["room"]; to: RoomStatus; title: string; message: string; label: string } | null>(null);

  const list = rack.data ?? [];

  const todayIso = useMemo(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }, []);

  const arrivals = list.filter((r) => r.arrivingStay);
  const departures = list.filter(
    (r) => r.currentStay?.status === "CHECKED_IN" && r.currentStay.checkOutDate <= todayIso
  );
  const occupied = list.filter((r) => r.room.status === "OCCUPIED").length;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="w-56 space-y-1">
            <Label>Sucursal</Label>
            <Select value={effectiveBranch} onChange={(e) => setBranchId(e.target.value)}>
              {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </Select>
          </div>
          {list.length > 0 && (
            <p className="pb-2 text-sm text-muted-foreground">
              {occupied} de {list.length} ocupadas
              {arrivals.length > 0 && ` · ${arrivals.length} llegada${arrivals.length === 1 ? "" : "s"} hoy`}
              {departures.length > 0 && ` · ${departures.length} salida${departures.length === 1 ? "" : "s"} hoy`}
            </p>
          )}
        </div>
        {canManage && list.length > 0 && (
          <Button onClick={() => setWalkIn(true)}>Alquilar habitacion</Button>
        )}
      </div>

      {rack.isLoading ? (
        <Spinner />
      ) : rack.isError ? (
        <Card><CardContent className="p-5 text-sm text-destructive">No pudimos cargar el rack. Intenta de nuevo.</CardContent></Card>
      ) : list.length === 0 ? (
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-sm font-medium">Sin habitaciones en esta sucursal</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Registra tus tipos y habitaciones en la pestana Habitaciones para ver el rack.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {list.map((r) => (
            <RackCard
              key={r.room.id}
              rackRoom={r}
              canManage={canManage}
              onCheckIn={(s) => setCheckInStay({ id: s.id, who: s.customerName ?? "el huesped", room: r.room.number })}
              onCheckOut={(s) => setCheckOutStay({ id: s.id, who: s.customerName ?? "el huesped", room: r.room.number })}
              onCharges={(s) => setChargesStayId(s.id)}
              onMarkClean={() => setStatusChange({
                room: r.room, to: "AVAILABLE",
                title: `Marcar limpia la habitacion ${r.room.number}`,
                message: "La habitacion volvera a estar disponible para reservar.",
                label: "Si, esta limpia",
              })}
              onMaintenance={() => setStatusChange({
                room: r.room, to: "MAINTENANCE",
                title: `Enviar a mantenimiento la ${r.room.number}`,
                message: "La habitacion dejara de estar disponible hasta que la marques operativa.",
                label: "Si, a mantenimiento",
              })}
              onBackAvailable={() => setStatusChange({
                room: r.room, to: "AVAILABLE",
                title: `Reactivar la habitacion ${r.room.number}`,
                message: "La habitacion volvera a estar disponible para reservar.",
                label: "Si, disponible",
              })}
            />
          ))}
        </div>
      )}

      {statusChange && (
        <ConfirmDialog
          title={statusChange.title}
          message={statusChange.message}
          confirmLabel={statusChange.label}
          loading={setRoomStatus.isPending}
          onConfirm={async () => {
            await setRoomStatus.mutateAsync({ id: statusChange.room.id, status: statusChange.to });
            setStatusChange(null);
          }}
          onCancel={() => setStatusChange(null)}
        />
      )}

      {checkInStay && (
        <CheckInModal
          stayId={checkInStay.id}
          who={checkInStay.who}
          roomNumber={checkInStay.room}
          onClose={() => setCheckInStay(null)}
        />
      )}

      {checkOutStay && (
        <CheckOutModal
          stayId={checkOutStay.id}
          onClose={() => setCheckOutStay(null)}
        />
      )}

      {chargesStayId && (
        <ChargesModal stayId={chargesStayId} canManage={canManage} onClose={() => setChargesStayId(null)} />
      )}

      {walkIn && effectiveBranch && (
        <WalkInWizard branchId={effectiveBranch} onClose={() => setWalkIn(false)} />
      )}
    </div>
  );
}

function RackCard({ rackRoom: r, canManage, onCheckIn, onCheckOut, onCharges, onMarkClean, onMaintenance, onBackAvailable }: {
  rackRoom: RackRoom;
  canManage: boolean;
  onCheckIn: (s: NonNullable<RackRoom["arrivingStay"]>) => void;
  onCheckOut: (s: NonNullable<RackRoom["currentStay"]>) => void;
  onCharges: (s: NonNullable<RackRoom["currentStay"]>) => void;
  onMarkClean: () => void;
  onMaintenance: () => void;
  onBackAvailable: () => void;
}) {
  const { room, currentStay, arrivingStay } = r;

  return (
    <div className={`rounded-xl border p-3.5 ${CARD_STYLE[room.status]}`}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-heading text-lg font-bold leading-tight">{room.number}</p>
          <p className="text-xs text-muted-foreground">
            {room.roomTypeName ?? "Habitacion"}{room.floor ? ` · Piso ${room.floor}` : ""}
          </p>
        </div>
        <Badge variant={STATUS_BADGE[room.status]}>{STATUS_LABEL[room.status]}</Badge>
      </div>

      {currentStay ? (
        <div className="mt-2 text-sm">
          <p className="truncate font-medium">{currentStay.customerName ?? "Huesped"}</p>
          <p className="text-xs text-muted-foreground">
            {currentStay.rentalMode === "HOURLY"
              ? `Por horas (${currentStay.hours ?? 1} h)${estimatedExit(currentStay) ? ` · sale ~${estimatedExit(currentStay)}` : ""}`
              : `${dateLabel(currentStay.checkInDate)} — ${dateLabel(currentStay.checkOutDate)}`}
            {" · "}{currentStay.guestsCount} huesped{currentStay.guestsCount === 1 ? "" : "es"}
            {currentStay.ticketCode ? ` · ${currentStay.ticketCode}` : ""}
          </p>
        </div>
      ) : arrivingStay ? (
        <div className="mt-2 text-sm">
          <p className="truncate font-medium">Llega hoy: {arrivingStay.customerName ?? "Huesped"}</p>
          <p className="text-xs text-muted-foreground">
            Hasta el {dateLabel(arrivingStay.checkOutDate)}
            {arrivingStay.ticketCode ? ` · ${arrivingStay.ticketCode}` : ""}
          </p>
        </div>
      ) : (
        <p className="mt-2 text-xs text-muted-foreground">
          {room.ratePerNight != null ? `S/ ${Number(room.ratePerNight).toFixed(2)} / noche` : "Sin estadia"}
        </p>
      )}

      {canManage && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {arrivingStay && room.status !== "OCCUPIED" && (
            <Button size="sm" onClick={() => onCheckIn(arrivingStay)}>Check-in</Button>
          )}
          {currentStay?.status === "CHECKED_IN" && (
            <>
              <Button size="sm" variant="outline" onClick={() => onCharges(currentStay)}>Consumos</Button>
              <Button size="sm" onClick={() => onCheckOut(currentStay)}>Check-out</Button>
            </>
          )}
          {room.status === "CLEANING" && (
            <Button size="sm" variant="outline" onClick={onMarkClean}>Marcar limpia</Button>
          )}
          {room.status === "AVAILABLE" && (
            <Button size="sm" variant="ghost" onClick={onMaintenance}>Mantenimiento</Button>
          )}
          {room.status === "MAINTENANCE" && (
            <Button size="sm" variant="outline" onClick={onBackAvailable}>Operativa</Button>
          )}
        </div>
      )}
    </div>
  );
}
