"use client";

import { useState } from "react";
import {
  useStay, useCheckIn, useCheckOut, useAddCharge, useRemoveCharge, useSetStayStatus, useUpdateStay,
  type GuestInput, type Stay, type StayStatus,
} from "@/features/lodging/api";
import { useProducts } from "@/features/products/api";
import { PAYMENT_METHODS, type PaymentMethod } from "@/features/sales/api";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { Spinner } from "@/components/ui/misc";
import { Modal } from "./RoomsView";

const fmt = (n: number) => `S/ ${Number(n).toFixed(2)}`;

const DOC_TYPES = ["DNI", "CE", "PASAPORTE"];

/** Unidades a cobrar del hospedaje: horas pactadas o noches. */
const stayUnits = (s: Stay) => (s.rentalMode === "HOURLY" ? Math.max(1, s.hours ?? 1) : Math.max(1, s.nights));

const unitsLabel = (s: Stay) => {
  const u = stayUnits(s);
  return s.rentalMode === "HOURLY" ? `${u} hora${u === 1 ? "" : "s"}` : `${u} noche${u === 1 ? "" : "s"}`;
};

/* -------------------------------- Check-in -------------------------------- */

/**
 * Check-in con registro de huespedes (ficha obligatoria en Peru: nombre y
 * documento de cada persona que se aloja).
 */
export function CheckInModal({ stayId, who, roomNumber, onClose }: {
  stayId: string;
  who: string;
  roomNumber: string;
  onClose: () => void;
}) {
  const checkIn = useCheckIn();
  const [guests, setGuests] = useState<GuestInput[]>([
    { fullName: "", docType: "DNI", docNumber: "", nationality: "Peru" },
  ]);
  const [error, setError] = useState<string | null>(null);

  const setGuest = (idx: number, patch: Partial<GuestInput>) =>
    setGuests((prev) => prev.map((g, i) => (i === idx ? { ...g, ...patch } : g)));

  const validGuests = guests.filter((g) => g.fullName.trim() && g.docNumber.trim());

  const submit = async () => {
    setError(null);
    try {
      await checkIn.mutateAsync({
        id: stayId,
        guests: validGuests.map((g) => ({
          fullName: g.fullName.trim(),
          docType: g.docType,
          docNumber: g.docNumber.trim(),
          nationality: g.nationality?.trim() || "Peru",
        })),
      });
      onClose();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Modal title={`Check-in habitacion ${roomNumber}`} onClose={onClose} wide>
      <p className="text-sm text-muted-foreground">
        Reserva de {who}. Registra a cada huesped con su documento: es el registro
        de huespedes que exige la norma peruana de hospedaje.
      </p>

      <div className="mt-3 space-y-2">
        {guests.map((g, i) => (
          <div key={i} className="grid gap-2 rounded-lg border border-border p-2.5 sm:grid-cols-[1fr_110px_1fr_110px_auto]">
            <Input placeholder="Nombre completo" value={g.fullName} maxLength={120}
              onChange={(e) => setGuest(i, { fullName: e.target.value })} />
            <Select value={g.docType} onChange={(e) => setGuest(i, { docType: e.target.value })}>
              {DOC_TYPES.map((d) => <option key={d} value={d}>{d}</option>)}
            </Select>
            <Input placeholder="Numero de documento" value={g.docNumber} maxLength={20}
              onChange={(e) => setGuest(i, { docNumber: e.target.value })} />
            <Input placeholder="Nacionalidad" value={g.nationality ?? ""} maxLength={60}
              onChange={(e) => setGuest(i, { nationality: e.target.value })} />
            <Button type="button" size="sm" variant="ghost" disabled={guests.length === 1}
              onClick={() => setGuests((prev) => prev.filter((_, x) => x !== i))}>
              Quitar
            </Button>
          </div>
        ))}
      </div>

      <div className="mt-2">
        <Button type="button" size="sm" variant="outline"
          onClick={() => setGuests((prev) => [...prev, { fullName: "", docType: "DNI", docNumber: "", nationality: "Peru" }])}>
          Agregar huesped
        </Button>
      </div>

      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}

      <div className="mt-4 flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose} disabled={checkIn.isPending}>Volver</Button>
        <Button onClick={submit} disabled={checkIn.isPending || validGuests.length === 0}>
          {checkIn.isPending ? "Registrando..." : "Confirmar check-in"}
        </Button>
      </div>
    </Modal>
  );
}

/* --------------------------------- Cargos --------------------------------- */

/** Cargos a la habitacion durante la estadia (minibar, lavanderia, desayuno...). */
export function ChargesModal({ stayId, canManage, onClose }: {
  stayId: string;
  canManage: boolean;
  onClose: () => void;
}) {
  const stay = useStay(stayId);
  const { data: products = [] } = useProducts();
  const addCharge = useAddCharge();
  const removeCharge = useRemoveCharge();

  const [productId, setProductId] = useState("");
  const [description, setDescription] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [unitPrice, setUnitPrice] = useState("");
  const [error, setError] = useState<string | null>(null);

  const s = stay.data;
  const activeProducts = products.filter((p) => p.active);
  const canAdd = canManage && s?.status === "CHECKED_IN";

  const pickProduct = (id: string) => {
    setProductId(id);
    const p = activeProducts.find((x) => x.id === id);
    if (p) setUnitPrice(String(p.price));
  };

  const submit = async () => {
    setError(null);
    try {
      await addCharge.mutateAsync({
        stayId,
        productId: productId || undefined,
        description: description.trim() || undefined,
        quantity: Number(quantity) || 1,
        unitPrice: Number(unitPrice) || 0,
      });
      setProductId("");
      setDescription("");
      setQuantity("1");
      setUnitPrice("");
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Modal title={`Cargos a la habitacion ${s?.roomNumber ?? ""}`} onClose={onClose} wide>
      {stay.isLoading || !s ? (
        <Spinner />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            {s.customerName ?? "Huesped"} · {unitsLabel(s)} a {fmt(s.ratePerNight)}.
            Los consumos (minibar, bebidas, comida, lavanderia...) se cobran juntos en el check-out.
          </p>

          {s.charges.length === 0 ? (
            <p className="mt-3 rounded-lg bg-muted/60 px-3 py-2 text-sm text-muted-foreground">
              Sin cargos registrados en esta estadia.
            </p>
          ) : (
            <ul className="mt-3 divide-y divide-border text-sm">
              {s.charges.map((c) => (
                <li key={c.id} className="flex items-center gap-3 py-2">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{c.description}</p>
                    <p className="text-xs text-muted-foreground">
                      {c.quantity} x {fmt(c.unitPrice)}
                    </p>
                  </div>
                  <span className="font-medium">{fmt(c.lineTotal)}</span>
                  {canAdd && (
                    <Button size="sm" variant="ghost" disabled={removeCharge.isPending}
                      onClick={() => removeCharge.mutate({ stayId, chargeId: c.id })}>
                      Quitar
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}

          <p className="mt-2 text-right text-sm font-semibold">
            Cargos: {fmt(s.chargesTotal)} · Total estimado: {fmt(stayUnits(s) * s.ratePerNight + s.chargesTotal)}
          </p>

          {canAdd && (
            <div className="mt-3 rounded-lg border border-border p-3">
              <p className="mb-2 text-xs font-medium text-muted-foreground">
                Nuevo cargo: elige un producto del inventario o describe un servicio libre.
              </p>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                <div className="space-y-1 sm:col-span-2">
                  <Label>Producto (opcional)</Label>
                  <Select value={productId} onChange={(e) => pickProduct(e.target.value)}>
                    <option value="">Servicio libre (sin producto)</option>
                    {activeProducts.map((p) => (
                      <option key={p.id} value={p.id}>{p.name} · {fmt(p.price)}</option>
                    ))}
                  </Select>
                </div>
                <div className="space-y-1 sm:col-span-2">
                  <Label>{productId ? "Descripcion (opcional)" : "Descripcion"}</Label>
                  <Input value={description} maxLength={160} placeholder="Lavanderia, desayuno..."
                    onChange={(e) => setDescription(e.target.value)} />
                </div>
                <div className="space-y-1">
                  <Label>Cantidad</Label>
                  <Input type="number" min="0.5" step="0.5" value={quantity}
                    onChange={(e) => setQuantity(e.target.value)} />
                </div>
                <div className="space-y-1">
                  <Label>Precio unitario</Label>
                  <Input type="number" min="0" step="0.1" value={unitPrice}
                    onChange={(e) => setUnitPrice(e.target.value)} />
                </div>
                <div className="flex items-end sm:col-span-2">
                  <Button onClick={submit}
                    disabled={addCharge.isPending || (!productId && (!description.trim() || unitPrice === ""))}>
                    {addCharge.isPending ? "Agregando..." : "Agregar cargo"}
                  </Button>
                </div>
              </div>
              {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
            </div>
          )}

          {!canAdd && s.status !== "CHECKED_IN" && (
            <p className="mt-3 text-xs text-muted-foreground">
              Los cargos solo se registran mientras la estadia tiene check-in activo.
            </p>
          )}
        </>
      )}
    </Modal>
  );
}

/* -------------------------------- Check-out ------------------------------- */

/**
 * Check-out: liquida noches + cargos como una venta del POS (necesita caja
 * abierta en la sucursal). La habitacion pasa a limpieza.
 */
export function CheckOutModal({ stayId, onClose }: {
  stayId: string;
  onClose: () => void;
}) {
  const stay = useStay(stayId);
  const checkOut = useCheckOut();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("CASH");
  const [error, setError] = useState<string | null>(null);

  const s = stay.data;
  const lodgingTotal = s ? stayUnits(s) * s.ratePerNight : 0;
  const total = s ? lodgingTotal + s.chargesTotal : 0;

  const submit = async () => {
    setError(null);
    try {
      await checkOut.mutateAsync({ id: stayId, paymentMethod });
      onClose();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Modal title={`Check-out habitacion ${s?.roomNumber ?? ""}`} onClose={onClose}>
      {stay.isLoading || !s ? (
        <Spinner />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            {s.customerName ?? "Huesped"} · {s.ticketCode ?? ""}
          </p>

          <div className="mt-3 space-y-1.5 rounded-lg bg-muted/60 p-3 text-sm">
            <div className="flex justify-between">
              <span>Hospedaje: {unitsLabel(s)} x {fmt(s.ratePerNight)}</span>
              <span className="font-medium">{fmt(lodgingTotal)}</span>
            </div>
            {s.charges.map((c) => (
              <div key={c.id} className="flex justify-between text-muted-foreground">
                <span className="truncate pr-2">{c.description} ({c.quantity} x {fmt(c.unitPrice)})</span>
                <span>{fmt(c.lineTotal)}</span>
              </div>
            ))}
            <div className="flex justify-between border-t border-border pt-1.5 text-base font-semibold">
              <span>Total a cobrar</span>
              <span>{fmt(total)}</span>
            </div>
          </div>

          <div className="mt-3 space-y-1">
            <Label>Como paga el huesped</Label>
            <div className="grid grid-cols-3 gap-1.5">
              {PAYMENT_METHODS.map((m) => (
                <button key={m.code} type="button" onClick={() => setPaymentMethod(m.code)}
                  className={
                    "min-h-10 rounded-lg border px-2 text-sm font-medium transition touch-manipulation active:scale-95 " +
                    (paymentMethod === m.code
                      ? "border-primary bg-accent text-accent-foreground"
                      : "border-border text-muted-foreground hover:border-primary/50")
                  }>
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          <p className="mt-3 text-xs text-muted-foreground">
            El cobro se registra como una venta de la caja abierta de la sucursal, con su
            ticket y serie. La habitacion pasa a limpieza.
          </p>

          {error && <p className="mt-2 text-sm text-destructive">{error}</p>}

          <div className="mt-4 flex justify-end gap-2">
            <Button variant="ghost" onClick={onClose} disabled={checkOut.isPending}>Volver</Button>
            <Button onClick={submit} disabled={checkOut.isPending}>
              {checkOut.isPending ? "Cobrando..." : `Cobrar ${fmt(total)} y cerrar`}
            </Button>
          </div>
        </>
      )}
    </Modal>
  );
}

/* ----------------------------- Extender horas ----------------------------- */

/** El huesped por horas pide quedarse mas tiempo: se ajustan las horas pactadas. */
export function ExtendHoursModal({ stay, onClose }: { stay: Stay; onClose: () => void }) {
  const updateStay = useUpdateStay();
  const current = Math.max(1, stay.hours ?? 1);
  const [hours, setHours] = useState(String(current));
  const [error, setError] = useState<string | null>(null);

  const next = Math.max(1, Number(hours) || 1);

  const submit = async () => {
    setError(null);
    try {
      await updateStay.mutateAsync({ id: stay.id, hours: next });
      onClose();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Modal title={`Extender estadia hab. ${stay.roomNumber ?? ""}`} onClose={onClose}>
      <p className="text-sm text-muted-foreground">
        {stay.customerName ?? "Huesped"} · por horas ({current} h a {fmt(stay.ratePerNight)}).
      </p>
      <div className="mt-3 space-y-1">
        <Label>Total de horas de la estadia</Label>
        <Input type="number" min="1" max="24" value={hours} onChange={(e) => setHours(e.target.value)} />
      </div>
      <p className="mt-2 rounded-lg bg-muted/60 px-3 py-2 text-sm">
        Nuevo hospedaje: {next} hora{next === 1 ? "" : "s"} x {fmt(stay.ratePerNight)} ={" "}
        <span className="font-semibold">{fmt(next * stay.ratePerNight)}</span>
      </p>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose} disabled={updateStay.isPending}>Volver</Button>
        <Button onClick={submit} disabled={updateStay.isPending || next === current}>
          {updateStay.isPending ? "Guardando..." : "Guardar horas"}
        </Button>
      </div>
    </Modal>
  );
}

/* ------------------------------- Cancelacion ------------------------------ */

/** Cancela o marca no show una reserva pendiente, con motivo opcional. */
export function CancelStayModal({ stayId, who, mode, onClose }: {
  stayId: string;
  who: string;
  mode: Extract<StayStatus, "CANCELED" | "NO_SHOW">;
  onClose: () => void;
}) {
  const setStatus = useSetStayStatus();
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const isCancel = mode === "CANCELED";

  const submit = async () => {
    setError(null);
    try {
      await setStatus.mutateAsync({ id: stayId, status: mode, reason: reason.trim() || undefined });
      onClose();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Modal title={isCancel ? "Cancelar reserva" : "Marcar no show"} onClose={onClose}>
      <p className="text-sm text-muted-foreground">
        {isCancel
          ? `La reserva de ${who} se cancelara y la habitacion quedara libre para esas fechas. No se puede deshacer.`
          : `Se registrara que ${who} no llego. La habitacion queda libre para esas fechas.`}
      </p>
      <div className="mt-3 space-y-1">
        <Label>Motivo (opcional)</Label>
        <Input value={reason} maxLength={300} placeholder="Cliente aviso que no viene"
          onChange={(e) => setReason(e.target.value)} />
      </div>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose} disabled={setStatus.isPending}>Volver</Button>
        <Button variant={isCancel ? "destructive" : "default"} onClick={submit} disabled={setStatus.isPending}>
          {setStatus.isPending ? "Guardando..." : isCancel ? "Si, cancelar reserva" : "Si, no llego"}
        </Button>
      </div>
    </Modal>
  );
}
