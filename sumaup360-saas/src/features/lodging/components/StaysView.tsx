"use client";

import { useMemo, useState } from "react";
import {
  useStays, useCreateStay, useUpdateStay, useAvailability,
  type Stay, type StayStatus,
} from "@/features/lodging/api";
import { useBranches } from "@/features/branches/api";
import { useCompany } from "@/features/companies/company-context";
import { useCustomers, useCreateCustomer } from "@/features/customers/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { Paginator } from "@/components/ui/pagination";
import { Modal } from "./RoomsView";
import { CheckInModal, CheckOutModal, ChargesModal, CancelStayModal, ExtendHoursModal } from "./StayActions";

const fmt = (n: number) => `S/ ${Number(n).toFixed(2)}`;

const STATUS_LABEL: Record<StayStatus, string> = {
  RESERVED: "Reservada",
  CHECKED_IN: "Hospedado",
  CHECKED_OUT: "Finalizada",
  CANCELED: "Cancelada",
  NO_SHOW: "No llego",
};

const statusVariant = (s: StayStatus) =>
  s === "RESERVED" ? "warning" : s === "CHECKED_IN" ? "default" : s === "CHECKED_OUT" ? "success" : "muted";

const dateLabel = (iso: string) =>
  new Date(iso + "T00:00:00").toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" }).replace(".", "");

const todayIso = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

/** Reservas y estadias: lista paginada con filtros y todo el ciclo de vida. */
export function StaysView({ canManage }: { canManage: boolean }) {
  const { currentCompany } = useCompany();
  const { data: branches = [] } = useBranches(currentCompany?.id);

  const [branchId, setBranchId] = useState("");
  const [status, setStatus] = useState<StayStatus | "">("");
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);

  const stays = useStays({ branchId: branchId || undefined, status, page });

  const [checkInStay, setCheckInStay] = useState<Stay | null>(null);
  const [chargesStay, setChargesStay] = useState<Stay | null>(null);
  const [checkOutStay, setCheckOutStay] = useState<Stay | null>(null);
  const [cancelStay, setCancelStay] = useState<{ stay: Stay; mode: "CANCELED" | "NO_SHOW" } | null>(null);
  const [rescheduling, setRescheduling] = useState<Stay | null>(null);
  const [extending, setExtending] = useState<Stay | null>(null);

  const data = stays.data;
  const list = data?.items ?? [];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="w-44 space-y-1">
            <Label>Sucursal</Label>
            <Select value={branchId} onChange={(e) => { setBranchId(e.target.value); setPage(0); }}>
              <option value="">Todas</option>
              {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </Select>
          </div>
          <div className="w-44 space-y-1">
            <Label>Estado</Label>
            <Select value={status} onChange={(e) => { setStatus(e.target.value as StayStatus | ""); setPage(0); }}>
              <option value="">Todos</option>
              {Object.entries(STATUS_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </Select>
          </div>
        </div>
        {canManage && (
          <Button onClick={() => setShowForm((v) => !v)} variant={showForm ? "outline" : "default"}>
            {showForm ? "Cerrar formulario" : "Nueva reserva"}
          </Button>
        )}
      </div>

      {canManage && showForm && (
        <NewStayForm branches={branches} defaultBranchId={branchId} onCreated={() => setShowForm(false)} />
      )}

      <Card>
        <CardContent className="p-5">
          {stays.isLoading ? (
            <Spinner />
          ) : stays.isError ? (
            <p className="py-6 text-center text-sm text-destructive">No pudimos cargar las reservas. Intenta de nuevo.</p>
          ) : list.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-sm font-medium">Sin reservas con esos filtros</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Crea una reserva con el boton Nueva reserva.
              </p>
            </div>
          ) : (
            <>
              <ul className="divide-y divide-border text-sm">
                {list.map((s) => (
                  <li key={s.id} className="flex flex-wrap items-center gap-3 py-3">
                    <span className="w-14 shrink-0 rounded-lg bg-accent px-2 py-1 text-center font-semibold text-accent-foreground">
                      {s.roomNumber ?? "—"}
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium">{s.customerName ?? "Cliente"}</span>
                        <Badge variant={statusVariant(s.status)}>{STATUS_LABEL[s.status]}</Badge>
                        {s.rentalMode === "HOURLY" && <Badge variant="default">Por horas</Badge>}
                        {s.ticketCode && <Badge variant="muted">{s.ticketCode}</Badge>}
                      </div>
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        {s.rentalMode === "HOURLY"
                          ? `${dateLabel(s.checkInDate)} · ${s.hours ?? 1} hora${(s.hours ?? 1) === 1 ? "" : "s"} a ${fmt(s.ratePerNight)}`
                          : `${dateLabel(s.checkInDate)} — ${dateLabel(s.checkOutDate)} · ${s.nights} noche${s.nights === 1 ? "" : "s"} a ${fmt(s.ratePerNight)}`}
                        {" · "}{s.guestsCount} huesped{s.guestsCount === 1 ? "" : "es"}
                        {s.chargesTotal > 0 && ` · cargos ${fmt(s.chargesTotal)}`}
                      </p>
                    </div>
                    {canManage && (
                      <span className="flex shrink-0 flex-wrap gap-1">
                        {s.status === "RESERVED" && (
                          <>
                            <Button size="sm" onClick={() => setCheckInStay(s)}>Check-in</Button>
                            <Button size="sm" variant="outline" onClick={() => setRescheduling(s)}>Reprogramar</Button>
                            <Button size="sm" variant="ghost" onClick={() => setCancelStay({ stay: s, mode: "CANCELED" })}>Cancelar</Button>
                            <Button size="sm" variant="ghost" onClick={() => setCancelStay({ stay: s, mode: "NO_SHOW" })}>No llego</Button>
                          </>
                        )}
                        {s.status === "CHECKED_IN" && (
                          <>
                            <Button size="sm" variant="outline" onClick={() => setChargesStay(s)}>Consumos</Button>
                            {s.rentalMode === "HOURLY" && (
                              <Button size="sm" variant="outline" onClick={() => setExtending(s)}>Extender</Button>
                            )}
                            <Button size="sm" onClick={() => setCheckOutStay(s)}>Check-out</Button>
                          </>
                        )}
                        {s.status === "CHECKED_OUT" && (
                          <Button size="sm" variant="outline" onClick={() => setChargesStay(s)}>Detalle</Button>
                        )}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
              <Paginator
                page={data?.page ?? 0}
                totalPages={data?.totalPages ?? 1}
                totalItems={data?.totalItems}
                onPage={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>

      {checkInStay && (
        <CheckInModal
          stayId={checkInStay.id}
          who={checkInStay.customerName ?? "el huesped"}
          roomNumber={checkInStay.roomNumber ?? ""}
          onClose={() => setCheckInStay(null)}
        />
      )}
      {chargesStay && (
        <ChargesModal stayId={chargesStay.id} canManage={canManage} onClose={() => setChargesStay(null)} />
      )}
      {checkOutStay && (
        <CheckOutModal stayId={checkOutStay.id} onClose={() => setCheckOutStay(null)} />
      )}
      {cancelStay && (
        <CancelStayModal
          stayId={cancelStay.stay.id}
          who={cancelStay.stay.customerName ?? "el cliente"}
          mode={cancelStay.mode}
          onClose={() => setCancelStay(null)}
        />
      )}
      {rescheduling && (
        <RescheduleStayModal stay={rescheduling} onClose={() => setRescheduling(null)} />
      )}
      {extending && (
        <ExtendHoursModal stay={extending} onClose={() => setExtending(null)} />
      )}
    </div>
  );
}

/* ------------------------------ Nueva reserva ------------------------------ */

function NewStayForm({ branches, defaultBranchId, onCreated }: {
  branches: { id: string; name: string }[];
  defaultBranchId: string;
  onCreated: () => void;
}) {
  const createStay = useCreateStay();
  const { data: customers = [] } = useCustomers();
  const createCustomer = useCreateCustomer();

  const [branchId, setBranchId] = useState(defaultBranchId || branches[0]?.id || "");
  const [checkInDate, setCheckInDate] = useState(todayIso());
  const [checkOutDate, setCheckOutDate] = useState("");
  const [roomId, setRoomId] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [rate, setRate] = useState("");
  const [guestsCount, setGuestsCount] = useState("1");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);

  // Cliente creado al vuelo (huesped nuevo sin ficha).
  const [newCustomer, setNewCustomer] = useState(false);
  const [custName, setCustName] = useState("");
  const [custDoc, setCustDoc] = useState("");
  const [custPhone, setCustPhone] = useState("");

  const datesValid = !!checkInDate && !!checkOutDate && checkOutDate > checkInDate;
  const availability = useAvailability(
    branchId || undefined,
    datesValid ? checkInDate : undefined,
    datesValid ? checkOutDate : undefined
  );
  const rooms = availability.data ?? [];
  const selectedRoom = rooms.find((r) => r.id === roomId);

  const nights = useMemo(() => {
    if (!datesValid) return 0;
    return Math.round(
      (new Date(checkOutDate + "T00:00:00").getTime() - new Date(checkInDate + "T00:00:00").getTime()) / 86_400_000
    );
  }, [checkInDate, checkOutDate, datesValid]);

  const effectiveRate = rate !== "" ? Number(rate) : selectedRoom?.ratePerNight ?? 0;

  const pickRoom = (id: string) => {
    setRoomId(id);
    const r = rooms.find((x) => x.id === id);
    if (r?.ratePerNight != null) setRate(String(r.ratePerNight));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      let cid = customerId;
      if (newCustomer) {
        const created = await createCustomer.mutateAsync({
          name: custName.trim(),
          docType: custDoc.trim() ? "DNI" : undefined,
          docNumber: custDoc.trim() || undefined,
          phone: custPhone.trim() || undefined,
        });
        cid = created.id;
      }
      await createStay.mutateAsync({
        branchId,
        roomId,
        customerId: cid,
        checkInDate,
        checkOutDate,
        ratePerNight: rate !== "" ? Number(rate) : undefined,
        guestsCount: Math.max(1, Number(guestsCount) || 1),
        notes: notes.trim() || undefined,
      });
      onCreated();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const saving = createStay.isPending || createCustomer.isPending;
  const customerReady = newCustomer ? custName.trim().length > 1 : !!customerId;

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-3 text-sm font-semibold">Nueva reserva</h2>
        <form onSubmit={submit} className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1">
              <Label>Sucursal</Label>
              <Select value={branchId} onChange={(e) => { setBranchId(e.target.value); setRoomId(""); }} required>
                {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Entrada</Label>
              <Input type="date" value={checkInDate} min={todayIso()}
                onChange={(e) => { setCheckInDate(e.target.value); setRoomId(""); }} required />
            </div>
            <div className="space-y-1">
              <Label className="flex items-center gap-1">
                Salida
                <InfoTip text="El dia de salida no se cobra: 2 noches es del lunes al miercoles." />
              </Label>
              <Input type="date" value={checkOutDate} min={checkInDate}
                onChange={(e) => { setCheckOutDate(e.target.value); setRoomId(""); }} required />
            </div>
            <div className="space-y-1">
              <Label>Habitacion</Label>
              <Select value={roomId} onChange={(e) => pickRoom(e.target.value)} required disabled={!datesValid}>
                <option value="">{datesValid ? (availability.isLoading ? "Buscando..." : rooms.length === 0 ? "Sin habitaciones libres" : "Elige") : "Elige las fechas"}</option>
                {rooms.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.number} · {r.roomTypeName}{r.ratePerNight != null ? ` · ${fmt(r.ratePerNight)}` : ""}
                  </option>
                ))}
              </Select>
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
                <div className="grid gap-2 sm:grid-cols-3">
                  <Input placeholder="Nombre completo" value={custName} maxLength={120}
                    onChange={(e) => setCustName(e.target.value)} />
                  <Input placeholder="DNI (opcional)" value={custDoc} maxLength={20}
                    onChange={(e) => setCustDoc(e.target.value)} />
                  <Input placeholder="Telefono (opcional)" value={custPhone} maxLength={20}
                    onChange={(e) => setCustPhone(e.target.value)} />
                </div>
              ) : (
                <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)} required={!newCustomer}>
                  <option value="">Elige un cliente</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}{c.docNumber ? ` · ${c.docNumber}` : ""}
                    </option>
                  ))}
                </Select>
              )}
            </div>
            <div className="space-y-1">
              <Label className="flex items-center gap-1">
                Tarifa por noche
                <InfoTip text="Se propone la tarifa del tipo de habitacion; puedes pactar otra para esta estadia." />
              </Label>
              <Input type="number" min="0" step="0.5" value={rate} onChange={(e) => setRate(e.target.value)}
                placeholder={selectedRoom?.ratePerNight != null ? String(selectedRoom.ratePerNight) : ""} />
            </div>
            <div className="space-y-1">
              <Label>Huespedes</Label>
              <Input type="number" min="1" max={selectedRoom?.capacity || 20} value={guestsCount}
                onChange={(e) => setGuestsCount(e.target.value)} />
              {selectedRoom && (
                <p className="text-xs text-muted-foreground">Admite hasta {selectedRoom.capacity}</p>
              )}
            </div>
          </div>

          <div className="space-y-1">
            <Label>Notas (opcional)</Label>
            <Input value={notes} maxLength={500} placeholder="Llega despues de las 10 pm"
              onChange={(e) => setNotes(e.target.value)} />
          </div>

          {datesValid && roomId && (
            <p className="rounded-lg bg-muted/60 px-3 py-2 text-sm">
              {nights} noche{nights === 1 ? "" : "s"} x {fmt(effectiveRate)} ={" "}
              <span className="font-semibold">{fmt(nights * effectiveRate)}</span>
              <span className="text-muted-foreground"> (mas los cargos que consuma)</span>
            </p>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end">
            <Button type="submit" disabled={saving || !datesValid || !roomId || !customerReady}>
              {saving ? "Reservando..." : "Reservar"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

/* ------------------------------ Reprogramacion ----------------------------- */

function RescheduleStayModal({ stay, onClose }: { stay: Stay; onClose: () => void }) {
  const updateStay = useUpdateStay();
  const [checkInDate, setCheckInDate] = useState(stay.checkInDate);
  const [checkOutDate, setCheckOutDate] = useState(stay.checkOutDate);
  const [roomId, setRoomId] = useState(stay.roomId);
  const [error, setError] = useState<string | null>(null);

  const datesValid = !!checkInDate && !!checkOutDate && checkOutDate > checkInDate;
  const availability = useAvailability(
    stay.branchId,
    datesValid ? checkInDate : undefined,
    datesValid ? checkOutDate : undefined
  );
  // La habitacion actual sigue siendo elegible aunque "ocupe" el rango (es esta misma reserva).
  const rooms = useMemo(() => {
    const base = availability.data ?? [];
    return base.some((r) => r.id === stay.roomId)
      ? base
      : [{ id: stay.roomId, number: stay.roomNumber ?? "actual", roomTypeName: "actual", ratePerNight: stay.ratePerNight, capacity: stay.guestsCount, branchId: stay.branchId, roomTypeId: "", status: "AVAILABLE" as const, isActive: true }, ...base];
  }, [availability.data, stay]);

  const changed = checkInDate !== stay.checkInDate || checkOutDate !== stay.checkOutDate || roomId !== stay.roomId;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await updateStay.mutateAsync({
        id: stay.id,
        roomId: roomId !== stay.roomId ? roomId : undefined,
        checkInDate,
        checkOutDate,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Modal title="Reprogramar reserva" onClose={onClose}>
      <p className="text-sm text-muted-foreground">
        {stay.customerName ?? "Cliente"} · habitacion {stay.roomNumber}
        {" · "}{dateLabel(stay.checkInDate)} — {dateLabel(stay.checkOutDate)}
      </p>
      <form onSubmit={submit} className="mt-3 space-y-3">
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-1">
            <Label>Nueva entrada</Label>
            <Input type="date" value={checkInDate} onChange={(e) => setCheckInDate(e.target.value)} required />
          </div>
          <div className="space-y-1">
            <Label>Nueva salida</Label>
            <Input type="date" value={checkOutDate} min={checkInDate} onChange={(e) => setCheckOutDate(e.target.value)} required />
          </div>
        </div>
        <div className="space-y-1">
          <Label>Habitacion</Label>
          <Select value={roomId} onChange={(e) => setRoomId(e.target.value)} disabled={!datesValid}>
            {rooms.map((r) => (
              <option key={r.id} value={r.id}>
                {r.number}{r.roomTypeName ? ` · ${r.roomTypeName}` : ""}
              </option>
            ))}
          </Select>
        </div>
        <p className="text-xs text-muted-foreground">
          Recuerda avisar el cambio al huesped. La tarifa pactada se mantiene.
        </p>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose} disabled={updateStay.isPending}>Volver</Button>
          <Button type="submit" disabled={!changed || !datesValid || updateStay.isPending}>
            {updateStay.isPending ? "Guardando..." : "Reprogramar"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
