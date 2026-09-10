"use client";

import { useState } from "react";
import {
  useRoomTypes, useCreateRoomType, useUpdateRoomType,
  useRooms, useCreateRoom, useUpdateRoom,
  type Room, type RoomType,
} from "@/features/lodging/api";
import { useBranches } from "@/features/branches/api";
import { useCompany } from "@/features/companies/company-context";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";

const fmt = (n: number) => `S/ ${Number(n).toFixed(2)}`;

/**
 * Catalogo del hospedaje: tipos de habitacion (tarifa y capacidad) y
 * habitaciones fisicas numeradas por sucursal.
 */
export function RoomsView({ canManage }: { canManage: boolean }) {
  return (
    <div className="space-y-6">
      <RoomTypesSection canManage={canManage} />
      <RoomsSection canManage={canManage} />
    </div>
  );
}

/* --------------------------- Tipos de habitacion --------------------------- */

function RoomTypesSection({ canManage }: { canManage: boolean }) {
  const types = useRoomTypes();
  const createType = useCreateRoomType();
  const updateType = useUpdateRoomType();

  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<RoomType | null>(null);
  const [deactivating, setDeactivating] = useState<RoomType | null>(null);

  const list = types.data ?? [];

  return (
    <Card>
      <CardContent className="p-5">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold">Tipos de habitacion</h2>
            <p className="text-xs text-muted-foreground">
              Simple, Doble, Matrimonial, Suite... cada tipo define tarifa por noche y capacidad.
            </p>
          </div>
          {canManage && (
            <Button size="sm" variant={showForm ? "outline" : "default"} onClick={() => setShowForm((v) => !v)}>
              {showForm ? "Cerrar" : "Nuevo tipo"}
            </Button>
          )}
        </div>

        {canManage && showForm && (
          <TypeForm
            saving={createType.isPending}
            onSave={async (data) => {
              await createType.mutateAsync(data);
              setShowForm(false);
            }}
          />
        )}

        {types.isLoading ? (
          <Spinner />
        ) : list.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Aun no tienes tipos de habitacion. Crea el primero para poder registrar tus habitaciones.
          </p>
        ) : (
          <ul className="divide-y divide-border text-sm">
            {list.map((t) => (
              <li key={t.id} className="flex flex-wrap items-center gap-3 py-2.5">
                <div className="min-w-0 flex-1">
                  <span className="font-medium">{t.name}</span>
                  {!t.isActive && <Badge variant="muted" className="ml-2">Inactivo</Badge>}
                  {t.description && (
                    <p className="truncate text-xs text-muted-foreground">{t.description}</p>
                  )}
                </div>
                <span className="text-muted-foreground">
                  {fmt(t.ratePerNight)} / noche
                  {t.ratePerHour != null && ` · ${fmt(t.ratePerHour)} / hora`}
                  {" · hasta "}{t.capacity} huesped{t.capacity === 1 ? "" : "es"}
                </span>
                {canManage && (
                  <span className="flex gap-1">
                    <Button size="sm" variant="outline" onClick={() => setEditing(t)}>Editar</Button>
                    {t.isActive && (
                      <Button size="sm" variant="ghost" onClick={() => setDeactivating(t)}>Desactivar</Button>
                    )}
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}

        {editing && (
          <Modal title={`Editar ${editing.name}`} onClose={() => setEditing(null)}>
            <TypeForm
              initial={editing}
              saving={updateType.isPending}
              onSave={async (data) => {
                await updateType.mutateAsync({ id: editing.id, ...data, isActive: true });
                setEditing(null);
              }}
            />
          </Modal>
        )}

        {deactivating && (
          <ConfirmDialog
            title={`Desactivar ${deactivating.name}`}
            message="El tipo dejara de ofrecerse para nuevas habitaciones y reservas. Las habitaciones existentes no se borran."
            confirmLabel="Si, desactivar"
            destructive
            loading={updateType.isPending}
            onConfirm={async () => {
              await updateType.mutateAsync({ id: deactivating.id, isActive: false });
              setDeactivating(null);
            }}
            onCancel={() => setDeactivating(null)}
          />
        )}
      </CardContent>
    </Card>
  );
}

function TypeForm({ initial, saving, onSave }: {
  initial?: RoomType;
  saving: boolean;
  onSave: (data: { name: string; capacity: number; ratePerNight: number; ratePerHour?: number; description?: string }) => Promise<void>;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [capacity, setCapacity] = useState(String(initial?.capacity ?? 2));
  const [rate, setRate] = useState(initial ? String(initial.ratePerNight) : "");
  const [hourRate, setHourRate] = useState(initial?.ratePerHour != null ? String(initial.ratePerHour) : "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await onSave({
        name: name.trim(),
        capacity: Math.max(1, Number(capacity) || 1),
        ratePerNight: Number(rate) || 0,
        // Al editar, 0 le dice al backend que quite la tarifa por hora.
        ratePerHour: hourRate !== "" ? Number(hourRate) : initial ? 0 : undefined,
        description: description.trim() || undefined,
      });
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <form onSubmit={submit} className="mb-4 grid gap-3 rounded-lg border border-border p-3 sm:grid-cols-2 lg:grid-cols-4">
      <div className="space-y-1">
        <Label>Nombre</Label>
        <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Matrimonial" required maxLength={80} />
      </div>
      <div className="space-y-1">
        <Label className="flex items-center gap-1">
          Tarifa por noche
          <InfoTip text="Precio base en soles. Al reservar puedes ajustar la tarifa pactada para esa estadia." />
        </Label>
        <Input type="number" min="0" step="0.5" value={rate} onChange={(e) => setRate(e.target.value)} placeholder="80.00" required />
      </div>
      <div className="space-y-1">
        <Label className="flex items-center gap-1">
          Tarifa por hora (opcional)
          <InfoTip text="Precio del alquiler por horas. Dejalo vacio si este tipo no se alquila por horas." />
        </Label>
        <Input type="number" min="0" step="0.5" value={hourRate} onChange={(e) => setHourRate(e.target.value)} placeholder="25.00" />
      </div>
      <div className="space-y-1">
        <Label className="flex items-center gap-1">
          Capacidad
          <InfoTip text="Cantidad maxima de huespedes que admite este tipo de habitacion." />
        </Label>
        <Input type="number" min="1" max="20" value={capacity} onChange={(e) => setCapacity(e.target.value)} />
      </div>
      <div className="space-y-1">
        <Label>Descripcion (opcional)</Label>
        <Input value={description} onChange={(e) => setDescription(e.target.value)} maxLength={300} placeholder="Cama 2 plazas, bano privado" />
      </div>
      {error && <p className="text-sm text-destructive sm:col-span-2 lg:col-span-4">{error}</p>}
      <div className="flex items-end sm:col-span-2 lg:col-span-4">
        <Button type="submit" disabled={saving || !name.trim() || rate === ""}>
          {saving ? "Guardando..." : initial ? "Guardar cambios" : "Crear tipo"}
        </Button>
      </div>
    </form>
  );
}

/* ------------------------------ Habitaciones ------------------------------ */

const ROOM_STATUS_LABEL: Record<Room["status"], string> = {
  AVAILABLE: "Disponible",
  OCCUPIED: "Ocupada",
  CLEANING: "Limpieza",
  MAINTENANCE: "Mantenimiento",
};

function RoomsSection({ canManage }: { canManage: boolean }) {
  const { currentCompany } = useCompany();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const [branchId, setBranchId] = useState("");
  const rooms = useRooms(branchId || undefined);
  const types = useRoomTypes();
  const createRoom = useCreateRoom();
  const updateRoom = useUpdateRoom();

  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Room | null>(null);
  const [deactivating, setDeactivating] = useState<Room | null>(null);

  const list = rooms.data ?? [];
  const activeTypes = (types.data ?? []).filter((t) => t.isActive);
  const branchName = (id: string) => branches.find((b) => b.id === id)?.name ?? "";

  return (
    <Card>
      <CardContent className="p-5">
        <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold">Habitaciones</h2>
            <p className="text-xs text-muted-foreground">
              Habitaciones fisicas numeradas por sucursal: 101, 102, 2B...
            </p>
          </div>
          <div className="flex items-end gap-2">
            <div className="w-44 space-y-1">
              <Label>Sucursal</Label>
              <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
                <option value="">Todas</option>
                {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </Select>
            </div>
            {canManage && (
              <Button size="sm" variant={showForm ? "outline" : "default"} onClick={() => setShowForm((v) => !v)}
                disabled={activeTypes.length === 0}>
                {showForm ? "Cerrar" : "Nueva habitacion"}
              </Button>
            )}
          </div>
        </div>

        {canManage && activeTypes.length === 0 && !types.isLoading && (
          <p className="mb-3 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
            Primero crea al menos un tipo de habitacion para poder registrar habitaciones.
          </p>
        )}

        {canManage && showForm && (
          <RoomForm
            branches={branches}
            types={activeTypes}
            defaultBranchId={branchId}
            saving={createRoom.isPending}
            onSave={async (data) => {
              await createRoom.mutateAsync(data as { branchId: string; roomTypeId: string; number: string; floor?: string; notes?: string });
              setShowForm(false);
            }}
          />
        )}

        {rooms.isLoading ? (
          <Spinner />
        ) : list.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Sin habitaciones registradas{branchId ? " en esta sucursal" : ""}.
          </p>
        ) : (
          <ul className="divide-y divide-border text-sm">
            {list.map((r) => (
              <li key={r.id} className="flex flex-wrap items-center gap-3 py-2.5">
                <span className="w-14 shrink-0 rounded-lg bg-accent px-2 py-1 text-center font-semibold text-accent-foreground">
                  {r.number}
                </span>
                <div className="min-w-0 flex-1">
                  <span className="font-medium">{r.roomTypeName ?? "Tipo"}</span>
                  {!r.isActive && <Badge variant="muted" className="ml-2">Inactiva</Badge>}
                  <p className="truncate text-xs text-muted-foreground">
                    {branchName(r.branchId)}{r.floor ? ` · Piso ${r.floor}` : ""}
                    {r.ratePerNight != null ? ` · ${fmt(r.ratePerNight)} / noche` : ""}
                  </p>
                </div>
                <Badge variant={r.status === "AVAILABLE" ? "success" : r.status === "OCCUPIED" ? "default" : r.status === "CLEANING" ? "warning" : "muted"}>
                  {ROOM_STATUS_LABEL[r.status]}
                </Badge>
                {canManage && (
                  <span className="flex gap-1">
                    <Button size="sm" variant="outline" onClick={() => setEditing(r)}>Editar</Button>
                    {r.isActive && (
                      <Button size="sm" variant="ghost" onClick={() => setDeactivating(r)}>Desactivar</Button>
                    )}
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}

        {editing && (
          <Modal title={`Editar habitacion ${editing.number}`} onClose={() => setEditing(null)}>
            <RoomForm
              branches={branches}
              types={activeTypes}
              initial={editing}
              saving={updateRoom.isPending}
              onSave={async (data) => {
                await updateRoom.mutateAsync({
                  id: editing.id,
                  roomTypeId: data.roomTypeId,
                  number: data.number,
                  floor: data.floor,
                  notes: data.notes,
                  isActive: true,
                });
                setEditing(null);
              }}
            />
          </Modal>
        )}

        {deactivating && (
          <ConfirmDialog
            title={`Desactivar habitacion ${deactivating.number}`}
            message="La habitacion dejara de aparecer en el rack y no aceptara reservas nuevas. Las estadias registradas no se borran."
            confirmLabel="Si, desactivar"
            destructive
            loading={updateRoom.isPending}
            onConfirm={async () => {
              await updateRoom.mutateAsync({ id: deactivating.id, isActive: false });
              setDeactivating(null);
            }}
            onCancel={() => setDeactivating(null)}
          />
        )}
      </CardContent>
    </Card>
  );
}

function RoomForm({ branches, types, initial, defaultBranchId, saving, onSave }: {
  branches: { id: string; name: string }[];
  types: RoomType[];
  initial?: Room;
  defaultBranchId?: string;
  saving: boolean;
  onSave: (data: { branchId?: string; roomTypeId: string; number: string; floor?: string; notes?: string }) => Promise<void>;
}) {
  const [branchId, setBranchId] = useState(initial?.branchId ?? defaultBranchId ?? branches[0]?.id ?? "");
  const [roomTypeId, setRoomTypeId] = useState(initial?.roomTypeId ?? types[0]?.id ?? "");
  const [number, setNumber] = useState(initial?.number ?? "");
  const [floor, setFloor] = useState(initial?.floor ?? "");
  const [notes, setNotes] = useState(initial?.notes ?? "");
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await onSave({
        branchId: initial ? undefined : branchId,
        roomTypeId,
        number: number.trim(),
        floor: floor.trim() || undefined,
        notes: notes.trim() || undefined,
      });
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <form onSubmit={submit} className="mb-4 grid gap-3 rounded-lg border border-border p-3 sm:grid-cols-2 lg:grid-cols-4">
      {!initial && (
        <div className="space-y-1">
          <Label>Sucursal</Label>
          <Select value={branchId} onChange={(e) => setBranchId(e.target.value)} required>
            {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </Select>
        </div>
      )}
      <div className="space-y-1">
        <Label>Tipo</Label>
        <Select value={roomTypeId} onChange={(e) => setRoomTypeId(e.target.value)} required>
          {types.map((t) => (
            <option key={t.id} value={t.id}>{t.name} · {fmt(t.ratePerNight)}</option>
          ))}
        </Select>
      </div>
      <div className="space-y-1">
        <Label className="flex items-center gap-1">
          Numero
          <InfoTip text="Identificador unico de la habitacion en la sucursal: 101, 102, 2B..." />
        </Label>
        <Input value={number} onChange={(e) => setNumber(e.target.value)} placeholder="101" required maxLength={20} />
      </div>
      <div className="space-y-1">
        <Label>Piso (opcional)</Label>
        <Input value={floor} onChange={(e) => setFloor(e.target.value)} maxLength={20} placeholder="1" />
      </div>
      <div className="space-y-1 sm:col-span-2">
        <Label>Notas (opcional)</Label>
        <Input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={300} placeholder="Vista a la calle" />
      </div>
      {error && <p className="text-sm text-destructive sm:col-span-2 lg:col-span-4">{error}</p>}
      <div className="flex items-end sm:col-span-2 lg:col-span-4">
        <Button type="submit" disabled={saving || !number.trim() || !roomTypeId || (!initial && !branchId)}>
          {saving ? "Guardando..." : initial ? "Guardar cambios" : "Crear habitacion"}
        </Button>
      </div>
    </form>
  );
}

/* --------------------------------- Modal --------------------------------- */

export function Modal({ title, onClose, children, wide }: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  wide?: boolean;
}) {
  return (
    <div className="fixed inset-0 z-[60] flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-20" onClick={onClose}>
      <div
        className={`w-full ${wide ? "max-w-2xl" : "max-w-lg"} rounded-xl bg-white p-5 shadow-xl`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">{title}</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>
        {children}
      </div>
    </div>
  );
}
