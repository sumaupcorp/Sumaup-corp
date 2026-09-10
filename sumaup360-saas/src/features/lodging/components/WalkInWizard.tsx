"use client";

import { useMemo, useState } from "react";
import {
  useRoomTypes, useRack, useAvailability, useCreateStay,
  type RentalMode, type Room,
} from "@/features/lodging/api";
import { useCustomers, useCreateCustomer } from "@/features/customers/api";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";
import { Modal } from "./RoomsView";

const fmt = (n: number) => `S/ ${Number(n).toFixed(2)}`;

const todayIso = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

const addDays = (iso: string, days: number) => {
  const d = new Date(iso + "T00:00:00");
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

/**
 * Asistente de recepcion (walk-in): el cliente esta en el mostrador. Flujo
 * real del hospedaje peruano: ¿por horas o por noche? → tarifario por tipo con
 * cuantas quedan libres → habitacion puntual → datos del huesped → ocupar ya.
 */
export function WalkInWizard({ branchId, onClose }: {
  branchId: string;
  onClose: () => void;
}) {
  const types = useRoomTypes();
  const rack = useRack(branchId);
  const createStay = useCreateStay();
  const { data: customers = [] } = useCustomers();
  const createCustomer = useCreateCustomer();

  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [mode, setMode] = useState<RentalMode>("HOURLY");
  const [hours, setHours] = useState("2");
  const [nights, setNights] = useState("1");
  const [typeId, setTypeId] = useState("");
  const [roomId, setRoomId] = useState("");
  const [rate, setRate] = useState("");
  const [error, setError] = useState<string | null>(null);

  // Huesped titular: en walk-in lo normal es registrarlo al vuelo.
  const [newCustomer, setNewCustomer] = useState(true);
  const [custName, setCustName] = useState("");
  const [custDoc, setCustDoc] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [guestsCount, setGuestsCount] = useState("1");

  const nightsNum = Math.max(1, Number(nights) || 1);
  const from = todayIso();
  const to = addDays(from, nightsNum);
  // Para noches el walk-in tambien reserva las fechas: valida contra reservas futuras.
  const availability = useAvailability(mode === "NIGHTLY" ? branchId : undefined, from, to);

  const rackRooms = useMemo(() => rack.data ?? [], [rack.data]);
  const activeTypes = (types.data ?? []).filter((t) => t.isActive);
  const anyHourly = activeTypes.some((t) => t.ratePerHour != null);

  /** Habitaciones alquilables AHORA del tipo dado (libres fisicamente y sin choque de reservas). */
  const availableRooms = useMemo(() => {
    const freeNow = rackRooms
      .filter((r) => r.room.status === "AVAILABLE" && !r.arrivingStay)
      .map((r) => r.room);
    if (mode === "HOURLY") return freeNow;
    const inRange = new Set((availability.data ?? []).map((r) => r.id));
    return freeNow.filter((r) => inRange.has(r.id));
  }, [rackRooms, availability.data, mode]);

  const roomsOfType = (tid: string) => availableRooms.filter((r) => r.roomTypeId === tid);
  const selectedType = activeTypes.find((t) => t.id === typeId);

  const units = mode === "HOURLY" ? Math.max(1, Number(hours) || 1) : nightsNum;
  const unitRate = rate !== ""
    ? Number(rate)
    : mode === "HOURLY" ? selectedType?.ratePerHour ?? 0 : selectedType?.ratePerNight ?? 0;

  // Panorama para no sobrevender un dia lleno (San Valentin y feriados).
  const totalRooms = rackRooms.filter((r) => r.room.isActive).length;
  const freeRooms = availableRooms.length;

  const pickType = (tid: string) => {
    setTypeId(tid);
    setRoomId("");
    const t = activeTypes.find((x) => x.id === tid);
    setRate(String((mode === "HOURLY" ? t?.ratePerHour : t?.ratePerNight) ?? ""));
    setStep(3);
  };

  const submit = async () => {
    setError(null);
    try {
      let cid = customerId;
      const name = custName.trim();
      if (newCustomer) {
        const created = await createCustomer.mutateAsync({
          name,
          docType: custDoc.trim() ? "DNI" : undefined,
          docNumber: custDoc.trim() || undefined,
        });
        cid = created.id;
      }
      const titularName = newCustomer ? name : customers.find((c) => c.id === cid)?.name ?? "";
      const titularDoc = newCustomer ? custDoc.trim() : customers.find((c) => c.id === cid)?.docNumber ?? "";
      await createStay.mutateAsync({
        branchId,
        roomId,
        customerId: cid,
        rentalMode: mode,
        checkInNow: true,
        hours: mode === "HOURLY" ? units : undefined,
        checkInDate: mode === "NIGHTLY" ? from : undefined,
        checkOutDate: mode === "NIGHTLY" ? to : undefined,
        ratePerNight: rate !== "" ? Number(rate) : undefined,
        guestsCount: Math.max(1, Number(guestsCount) || 1),
        // Registro legal: el titular queda en la ficha de huespedes si dio documento.
        guests: titularDoc ? [{ fullName: titularName, docType: "DNI", docNumber: titularDoc }] : undefined,
      });
      onClose();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const saving = createStay.isPending || createCustomer.isPending;
  const customerReady = newCustomer ? custName.trim().length > 1 : !!customerId;

  return (
    <Modal title="Alquilar habitacion (cliente en recepcion)" onClose={onClose} wide>
      <p className="text-sm text-muted-foreground">
        {rack.isLoading ? "Cargando disponibilidad..." : `${freeRooms} de ${totalRooms} habitaciones alquilables ahora.`}
        {freeRooms === 0 && !rack.isLoading && " Revisa el rack: puede haber salidas o limpiezas pendientes."}
      </p>

      {/* Paso 1: modalidad */}
      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        <button type="button"
          onClick={() => { setMode("HOURLY"); setTypeId(""); setRoomId(""); setRate(""); setStep(2); }}
          disabled={!anyHourly}
          className={
            "rounded-xl border p-4 text-left transition touch-manipulation active:scale-[0.98] disabled:opacity-40 " +
            (mode === "HOURLY" && step > 1 ? "border-primary bg-accent" : "border-border hover:border-primary/50")
          }>
          <p className="font-heading text-base font-bold">Por horas</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {anyHourly ? "Entra y sale hoy mismo. Elige cuantas horas." : "Ningun tipo tiene tarifa por hora (configurala en Habitaciones)."}
          </p>
        </button>
        <button type="button"
          onClick={() => { setMode("NIGHTLY"); setTypeId(""); setRoomId(""); setRate(""); setStep(2); }}
          className={
            "rounded-xl border p-4 text-left transition touch-manipulation active:scale-[0.98] " +
            (mode === "NIGHTLY" && step > 1 ? "border-primary bg-accent" : "border-border hover:border-primary/50")
          }>
          <p className="font-heading text-base font-bold">Por noche</p>
          <p className="mt-0.5 text-xs text-muted-foreground">Se queda a dormir. Elige cuantas noches.</p>
        </button>
      </div>

      {step >= 2 && (
        <>
          <div className="mt-3 flex items-end gap-3">
            <div className="w-36 space-y-1">
              <Label>{mode === "HOURLY" ? "Horas" : "Noches"}</Label>
              <Input type="number" min="1" max={mode === "HOURLY" ? 24 : 60}
                value={mode === "HOURLY" ? hours : nights}
                onChange={(e) => {
                  if (mode === "HOURLY") setHours(e.target.value);
                  else { setNights(e.target.value); setRoomId(""); }
                }} />
            </div>
            {mode === "NIGHTLY" && (
              <p className="pb-2 text-xs text-muted-foreground">
                Hoy → {new Date(to + "T00:00:00").toLocaleDateString("es-PE", { day: "2-digit", month: "short" }).replace(".", "")}
              </p>
            )}
          </div>

          {/* Paso 2: tarifario por tipo con disponibles */}
          <div className="mt-3">
            <Label>Tarifario: elige el tipo</Label>
            {types.isLoading || rack.isLoading || (mode === "NIGHTLY" && availability.isLoading) ? (
              <div className="py-4"><Spinner /></div>
            ) : (
              <div className="mt-1.5 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {activeTypes.map((t) => {
                  const price = mode === "HOURLY" ? t.ratePerHour : t.ratePerNight;
                  const free = roomsOfType(t.id).length;
                  const disabled = price == null || free === 0;
                  return (
                    <button key={t.id} type="button" disabled={disabled}
                      onClick={() => pickType(t.id)}
                      className={
                        "rounded-xl border p-3 text-left transition touch-manipulation active:scale-[0.98] disabled:opacity-40 " +
                        (typeId === t.id ? "border-primary bg-accent" : "border-border hover:border-primary/50")
                      }>
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-medium">{t.name}</p>
                        <Badge variant={free > 0 ? "success" : "muted"}>
                          {free > 0 ? `${free} libre${free === 1 ? "" : "s"}` : "Lleno"}
                        </Badge>
                      </div>
                      <p className="mt-1 text-sm font-semibold text-brand-blue">
                        {price != null ? `${fmt(price)} / ${mode === "HOURLY" ? "hora" : "noche"}` : "Sin tarifa por hora"}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        Hasta {t.capacity} huesped{t.capacity === 1 ? "" : "es"}
                        {t.description ? ` · ${t.description}` : ""}
                      </p>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}

      {/* Paso 3: habitacion puntual + huesped */}
      {step === 3 && selectedType && (
        <div className="mt-3 space-y-3 rounded-lg border border-border p-3">
          <div className="space-y-1">
            <Label>Habitacion ({selectedType.name})</Label>
            <div className="flex flex-wrap gap-1.5">
              {roomsOfType(selectedType.id).map((r: Room) => (
                <button key={r.id} type="button" onClick={() => setRoomId(r.id)}
                  className={
                    "min-h-10 rounded-lg border px-3.5 text-sm font-semibold transition touch-manipulation active:scale-95 " +
                    (roomId === r.id ? "border-primary bg-accent text-accent-foreground" : "border-border hover:border-primary/50")
                  }>
                  {r.number}{r.floor ? ` · P${r.floor}` : ""}
                </button>
              ))}
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1 sm:col-span-2">
              <div className="flex items-center justify-between">
                <Label>Huesped titular</Label>
                <button type="button" className="text-xs font-medium text-brand-blue hover:underline"
                  onClick={() => setNewCustomer((v) => !v)}>
                  {newCustomer ? "Elegir cliente existente" : "Cliente nuevo"}
                </button>
              </div>
              {newCustomer ? (
                <div className="grid gap-2 sm:grid-cols-2">
                  <Input placeholder="Nombre completo" value={custName} maxLength={120}
                    onChange={(e) => setCustName(e.target.value)} />
                  <Input placeholder="DNI (registro de huespedes)" value={custDoc} maxLength={20}
                    onChange={(e) => setCustDoc(e.target.value)} />
                </div>
              ) : (
                <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                  <option value="">Elige un cliente</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}{c.docNumber ? ` · ${c.docNumber}` : ""}</option>
                  ))}
                </Select>
              )}
            </div>
            <div className="space-y-1">
              <Label>Tarifa / {mode === "HOURLY" ? "hora" : "noche"}</Label>
              <Input type="number" min="0" step="0.5" value={rate} onChange={(e) => setRate(e.target.value)} />
            </div>
            <div className="space-y-1">
              <Label>Personas</Label>
              <Input type="number" min="1" max={selectedType.capacity} value={guestsCount}
                onChange={(e) => setGuestsCount(e.target.value)} />
            </div>
          </div>

          <p className="rounded-lg bg-muted/60 px-3 py-2 text-sm">
            {units} {mode === "HOURLY" ? (units === 1 ? "hora" : "horas") : (units === 1 ? "noche" : "noches")}
            {" x "}{fmt(unitRate)} = <span className="font-semibold">{fmt(units * unitRate)}</span>
            <span className="text-muted-foreground"> · se cobra al check-out junto con los consumos</span>
          </p>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={onClose} disabled={saving}>Volver</Button>
            <Button onClick={submit} disabled={saving || !roomId || !customerReady}>
              {saving ? "Ocupando..." : "Alquilar y ocupar ahora"}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
}
