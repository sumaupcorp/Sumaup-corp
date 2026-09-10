"use client";

import { useMemo, useRef, useState } from "react";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage } from "@/lib/firebase";
import { useCustomers, useCreateCustomer } from "@/features/customers/api";
import { usePatients, useCreatePatient, useUpdatePatient, type Patient } from "@/features/patients/api";
import { useHasPermission, useSession } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Paginator } from "@/components/ui/pagination";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";

const PAGE_SIZE = 12;

/** Especies frecuentes para registro rapido; siempre se puede escribir otra. */
const COMMON_SPECIES = ["Perro", "Gato", "Ave", "Conejo", "Hamster"];

/** Colores del avatar cuando no hay foto, estables por nombre. */
const AVATAR_COLORS = [
  "bg-sky-100 text-sky-700",
  "bg-emerald-100 text-emerald-700",
  "bg-amber-100 text-amber-700",
  "bg-violet-100 text-violet-700",
  "bg-rose-100 text-rose-700",
];

const avatarColor = (name: string) => {
  let h = 0;
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) % AVATAR_COLORS.length;
  return AVATAR_COLORS[h];
};

/** Edad legible desde la fecha de nacimiento: "2 anos 3 m", "8 meses", "3 semanas". */
function ageLabel(birthDate?: string | null): string | null {
  if (!birthDate) return null;
  const birth = new Date(birthDate + "T00:00:00");
  if (isNaN(birth.getTime()) || birth.getTime() > Date.now()) return null;
  const days = Math.floor((Date.now() - birth.getTime()) / 86_400_000);
  if (days < 60) {
    const weeks = Math.floor(days / 7);
    return weeks < 2 ? `${days} dia${days === 1 ? "" : "s"}` : `${weeks} semanas`;
  }
  const months = Math.floor(days / 30.44);
  if (months < 12) return `${months} meses`;
  const years = Math.floor(months / 12);
  const rest = months % 12;
  return rest === 0 ? `${years} ano${years === 1 ? "" : "s"}` : `${years} a ${rest} m`;
}

export default function PatientsPage() {
  const hasPerm = useHasPermission();
  const canManage = hasPerm("patient:manage");
  const { data: customers = [] } = useCustomers();
  const patients = usePatients();

  const [search, setSearch] = useState("");
  const [speciesFilter, setSpeciesFilter] = useState("");
  const [ownerFilter, setOwnerFilter] = useState("");
  const [showInactive, setShowInactive] = useState(false);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Patient | null>(null);
  const [creating, setCreating] = useState(false);

  const list = useMemo(() => patients.data ?? [], [patients.data]);

  const speciesOptions = useMemo(() => {
    const set = new Set<string>();
    for (const p of list) if (p.species) set.add(p.species.trim());
    return [...set].sort();
  }, [list]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return list.filter((p) => {
      if (!showInactive && !p.active) return false;
      if (speciesFilter && (p.species ?? "").trim() !== speciesFilter) return false;
      if (ownerFilter && p.customerId !== ownerFilter) return false;
      if (q) {
        const hay = [p.name, p.customerName, p.breed, p.species].filter(Boolean)
          .join(" ").toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [list, search, speciesFilter, ownerFilter, showInactive]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageItems = filtered.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE);
  const inactiveCount = list.filter((p) => !p.active).length;

  return (
    <div>
      <PageHeader
        title="Pacientes"
        subtitle="Mascotas y pacientes asociados a tus clientes"
        action={canManage ? (
          <Button className="min-h-11" onClick={() => setCreating(true)}>Registrar mascota</Button>
        ) : undefined}
      />

      {/* Filtros */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="w-full max-w-64 space-y-1">
          <Label>Buscar</Label>
          <Input value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            placeholder="Nombre, dueno o raza..." />
        </div>
        <div className="w-full max-w-52 space-y-1">
          <Label>Dueno</Label>
          <Select value={ownerFilter}
            onChange={(e) => { setOwnerFilter(e.target.value); setPage(0); }}>
            <option value="">Todos</option>
            {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </Select>
        </div>
        {speciesOptions.length > 0 && (
          <div className="flex gap-1.5 pb-0.5">
            <SpeciesChip label="Todas" active={speciesFilter === ""}
              onClick={() => { setSpeciesFilter(""); setPage(0); }} />
            {speciesOptions.map((s) => (
              <SpeciesChip key={s} label={s} active={speciesFilter === s}
                onClick={() => { setSpeciesFilter(s); setPage(0); }} />
            ))}
          </div>
        )}
        {inactiveCount > 0 && (
          <label className="flex cursor-pointer items-center gap-1.5 pb-2 text-xs text-muted-foreground">
            <input type="checkbox" className="accent-primary" checked={showInactive}
              onChange={(e) => { setShowInactive(e.target.checked); setPage(0); }} />
            Ver inactivos ({inactiveCount})
          </label>
        )}
      </div>

      {/* Galeria de fichas */}
      {patients.isLoading ? (
        <div className="flex justify-center py-12"><Spinner /></div>
      ) : filtered.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-sm font-medium">
              {list.length === 0 ? "Aun no hay mascotas registradas" : "Nada coincide con tu filtro"}
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              {list.length === 0
                ? "Registra la primera con el boton de arriba."
                : "Prueba con otro nombre o quita los filtros."}
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            {pageItems.map((p) => (
              <PatientCard key={p.id} patient={p} canManage={canManage}
                onOpen={() => canManage && setEditing(p)} />
            ))}
          </div>
          <Paginator page={safePage} totalPages={totalPages}
            totalItems={filtered.length} onPage={setPage} />
        </>
      )}

      {creating && (
        <PatientModal customers={customers} onClose={() => setCreating(false)} />
      )}
      {editing && (
        <PatientModal customers={customers} patient={editing} onClose={() => setEditing(null)} />
      )}
    </div>
  );
}

function SpeciesChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={"h-10 select-none rounded-full border px-3.5 text-xs font-medium transition touch-manipulation active:scale-95 " +
        (active ? "border-primary bg-accent text-primary" : "border-border bg-white text-muted-foreground hover:border-primary")}
    >
      {label}
    </button>
  );
}

/** Avatar de la mascota: foto o inicial con color estable. */
function PetAvatar({ patient: p, className }: { patient: Patient; className: string }) {
  if (p.photoUrl) {
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={p.photoUrl} alt={p.name}
      className={className + " rounded-full border border-border object-cover"} />;
  }
  return (
    <div className={className + " flex items-center justify-center rounded-full font-heading font-bold " + avatarColor(p.name)}>
      {p.name.charAt(0).toUpperCase()}
    </div>
  );
}

/** Ficha de mascota: foto, datos clave y dueno. Toda la tarjeta abre la edicion. */
function PatientCard({ patient: p, canManage, onOpen }: {
  patient: Patient;
  canManage: boolean;
  onOpen: () => void;
}) {
  const age = ageLabel(p.birthDate);
  const details = [
    p.species,
    p.breed,
    p.sex === "MACHO" ? "Macho" : p.sex === "HEMBRA" ? "Hembra" : null,
  ].filter(Boolean).join(" · ");

  return (
    <button
      type="button"
      onClick={onOpen}
      disabled={!canManage}
      className={
        "flex select-none items-center gap-3 rounded-xl border border-border bg-white p-3.5 text-left " +
        "transition touch-manipulation disabled:cursor-default " +
        (canManage ? "hover:border-primary hover:shadow-sm active:scale-[0.99]" : "") +
        (p.active ? "" : " opacity-60")
      }
    >
      <PetAvatar patient={p} className="size-14 shrink-0 text-xl" />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="truncate text-sm font-semibold">{p.name}</p>
          {!p.active && <Badge variant="muted">Inactivo</Badge>}
        </div>
        <p className="truncate text-xs text-muted-foreground">{details || "Sin detalle"}</p>
        <p className="truncate text-xs text-muted-foreground">
          {[age, p.weightKg ? `${Number(p.weightKg)} kg` : null].filter(Boolean).join(" · ")}
        </p>
        {p.customerName && (
          <p className="mt-0.5 truncate text-xs font-medium text-primary">{p.customerName}</p>
        )}
      </div>
    </button>
  );
}

/**
 * Alta y edicion de mascota en un mismo modal. En recepcion el alta es UN solo
 * formulario: datos del dueno (existente o nuevo) + datos de la mascota; al guardar
 * se crean en sus tablas (customer y patient) ya asociados.
 */
function PatientModal({ customers, patient, onClose }: {
  customers: { id: string; name: string }[];
  patient?: Patient;
  onClose: () => void;
}) {
  const createPatient = useCreatePatient();
  const createCustomer = useCreateCustomer();
  const updatePatient = useUpdatePatient();
  const { data: session } = useSession();
  const fileRef = useRef<HTMLInputElement>(null);

  // Dueno: cliente existente o registrar uno nuevo en el mismo paso.
  const [ownerMode, setOwnerMode] = useState<"existing" | "new">(
    customers.length === 0 ? "new" : "existing");
  const [ownerName, setOwnerName] = useState("");
  const [ownerPhone, setOwnerPhone] = useState("");
  const [ownerDoc, setOwnerDoc] = useState("");

  const [customerId, setCustomerId] = useState(patient?.customerId ?? "");
  const [name, setName] = useState(patient?.name ?? "");
  const [species, setSpecies] = useState(patient?.species ?? "");
  const [breed, setBreed] = useState(patient?.breed ?? "");
  const [sex, setSex] = useState(patient?.sex ?? "");
  const [birthDate, setBirthDate] = useState(patient?.birthDate ?? "");
  const [weightKg, setWeightKg] = useState(patient?.weightKg ? String(patient.weightKg) : "");
  const [notes, setNotes] = useState(patient?.notes ?? "");
  const [photoUrl, setPhotoUrl] = useState(patient?.photoUrl ?? "");
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmingToggle, setConfirmingToggle] = useState(false);

  const isEdit = !!patient;
  const saving = createPatient.isPending || createCustomer.isPending || updatePatient.isPending;
  const age = ageLabel(birthDate || null);
  const ownerReady = isEdit || (ownerMode === "existing" ? !!customerId : !!ownerName.trim());

  const uploadPhoto = async (file: File) => {
    setError(null);
    if (!file.type.startsWith("image/")) { setError("El archivo debe ser una imagen."); return; }
    if (file.size > 5 * 1024 * 1024) { setError("La imagen no debe superar los 5MB."); return; }
    setUploading(true);
    try {
      // Ruta permitida por las reglas de Storage: patients/{tenantId}/...
      const tenant = session?.tenantId ?? "tenant";
      const dest = ref(storage, `patients/${tenant}/${Date.now()}-${file.name.replace(/[^a-zA-Z0-9.]/g, "")}`);
      await uploadBytes(dest, file, { contentType: file.type });
      setPhotoUrl(await getDownloadURL(dest));
    } catch {
      setError("No pudimos subir la foto. Intenta de nuevo.");
    } finally {
      setUploading(false);
    }
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      if (isEdit) {
        await updatePatient.mutateAsync({
          id: patient.id,
          name,
          species: species || undefined,
          breed: breed || undefined,
          sex: sex || undefined,
          birthDate: birthDate || undefined,
          weightKg: weightKg ? Number(weightKg) : undefined,
          notes: notes || undefined,
          photoUrl,
        });
        onClose();
        return;
      }

      // Alta en un solo paso: primero el dueno (si es nuevo), luego la mascota asociada.
      let ownerId = customerId;
      if (ownerMode === "new") {
        const created = await createCustomer.mutateAsync({
          name: ownerName.trim(),
          phone: ownerPhone.trim() || undefined,
          docNumber: ownerDoc.trim() || undefined,
        });
        ownerId = created.id;
        // Si la mascota fallara, el dueno ya existe: dejamos el modal apuntando a el.
        setCustomerId(created.id);
        setOwnerMode("existing");
      }
      await createPatient.mutateAsync({
        customerId: ownerId, name,
        species: species || undefined,
        breed: breed || undefined,
        sex: sex || undefined,
        birthDate: birthDate || undefined,
        weightKg: weightKg ? Number(weightKg) : undefined,
        notes: notes || undefined,
        photoUrl: photoUrl || undefined,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const toggleActive = async () => {
    if (!patient) return;
    try {
      await updatePatient.mutateAsync({ id: patient.id, active: !patient.active });
      setConfirmingToggle(false);
      onClose();
    } catch (err) {
      setConfirmingToggle(false);
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-10">
      <div className="w-full max-w-lg rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">{isEdit ? `Ficha de ${patient.name}` : "Registrar mascota"}</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        <form onSubmit={submit} className="space-y-3">
          {/* Foto */}
          <div className="flex items-center gap-4">
            {photoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={photoUrl} alt="Foto" className="size-20 rounded-full border border-border object-cover" />
            ) : (
              <div className="flex size-20 items-center justify-center rounded-full border-2 border-dashed border-border text-xs text-muted-foreground">
                Sin foto
              </div>
            )}
            <div className="space-y-1.5">
              <input ref={fileRef} type="file" accept="image/*" className="hidden"
                onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadPhoto(f); e.target.value = ""; }} />
              <div className="flex gap-2">
                <Button type="button" size="sm" variant="outline" disabled={uploading}
                  onClick={() => fileRef.current?.click()}>
                  {uploading ? "Subiendo..." : photoUrl ? "Cambiar foto" : "Subir foto"}
                </Button>
                {photoUrl && !uploading && (
                  <Button type="button" size="sm" variant="ghost" onClick={() => setPhotoUrl("")}>
                    Quitar
                  </Button>
                )}
              </div>
              <p className="text-xs text-muted-foreground">La foto ayuda a reconocer a la mascota al toque.</p>
            </div>
          </div>

          {!isEdit && (
            <div className="space-y-2 rounded-xl border border-border bg-muted/40 p-3">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-semibold">Dueno</p>
                <div className="flex rounded-lg border border-border bg-white p-0.5">
                  <button type="button"
                    onClick={() => setOwnerMode("existing")}
                    className={"min-h-9 select-none rounded-md px-3 text-xs font-medium transition touch-manipulation " +
                      (ownerMode === "existing" ? "bg-accent text-primary" : "text-muted-foreground")}>
                    Cliente existente
                  </button>
                  <button type="button"
                    onClick={() => setOwnerMode("new")}
                    className={"min-h-9 select-none rounded-md px-3 text-xs font-medium transition touch-manipulation " +
                      (ownerMode === "new" ? "bg-accent text-primary" : "text-muted-foreground")}>
                    Cliente nuevo
                  </button>
                </div>
              </div>

              {ownerMode === "existing" ? (
                <div className="space-y-1">
                  <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                    <option value="">Selecciona al dueno…</option>
                    {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Select>
                  {customers.length === 0 && (
                    <p className="text-xs text-muted-foreground">
                      Aun no tienes clientes: usa &quot;Cliente nuevo&quot;.
                    </p>
                  )}
                </div>
              ) : (
                <div className="space-y-2">
                  <div className="space-y-1">
                    <Label>Nombre del dueno</Label>
                    <Input value={ownerName} onChange={(e) => setOwnerName(e.target.value)}
                      placeholder="Ej. Maria Lopez" required={ownerMode === "new"} />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <Label>Celular</Label>
                      <Input type="tel" value={ownerPhone} onChange={(e) => setOwnerPhone(e.target.value)}
                        placeholder="9xxxxxxxx" />
                    </div>
                    <div className="space-y-1">
                      <Label>DNI (opcional)</Label>
                      <Input value={ownerDoc} onChange={(e) => setOwnerDoc(e.target.value)}
                        maxLength={20} />
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Se registra como cliente del negocio y queda asociado a la mascota.
                  </p>
                </div>
              )}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>Nombre</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} required placeholder="Firulais" />
            </div>
            <div className="space-y-1">
              <Label>Raza</Label>
              <Input value={breed} onChange={(e) => setBreed(e.target.value)} placeholder="Criollo" />
            </div>
          </div>

          <div className="space-y-1">
            <Label>Especie</Label>
            <div className="flex flex-wrap gap-1.5">
              {COMMON_SPECIES.map((s) => (
                <button key={s} type="button"
                  onClick={() => setSpecies(species === s ? "" : s)}
                  className={"h-9 select-none rounded-full border px-3 text-xs font-medium transition touch-manipulation active:scale-95 " +
                    (species === s ? "border-primary bg-accent text-primary" : "border-border text-muted-foreground hover:border-primary")}>
                  {s}
                </button>
              ))}
              <Input className="h-9 w-28 text-xs" value={COMMON_SPECIES.includes(species) ? "" : species}
                onChange={(e) => setSpecies(e.target.value)} placeholder="Otra..." />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="space-y-1">
              <Label>Sexo</Label>
              <Select value={sex} onChange={(e) => setSex(e.target.value)}>
                <option value="">—</option>
                <option value="MACHO">Macho</option>
                <option value="HEMBRA">Hembra</option>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Nacimiento{age ? ` (${age})` : ""}</Label>
              <Input type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} />
            </div>
            <div className="space-y-1">
              <Label>Peso (kg)</Label>
              <Input type="number" min="0" step="0.1" value={weightKg}
                onChange={(e) => setWeightKg(e.target.value)} />
            </div>
          </div>

          <div className="space-y-1">
            <Label>Notas</Label>
            <Input value={notes} onChange={(e) => setNotes(e.target.value)}
              maxLength={500} placeholder="Alergias, antecedentes, caracter..." />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex items-center justify-between gap-2 pt-1">
            {isEdit ? (
              <Button type="button" variant="ghost" size="sm"
                className={patient.active ? "text-destructive" : ""}
                disabled={saving}
                onClick={() => setConfirmingToggle(true)}>
                {patient.active ? "Desactivar" : "Reactivar"}
              </Button>
            ) : <span />}
            <div className="flex gap-2">
              <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
              <Button type="submit" disabled={saving || uploading || !ownerReady || !name}>
                {saving ? "Guardando..." : isEdit ? "Guardar cambios" : "Registrar"}
              </Button>
            </div>
          </div>
        </form>
      </div>

      {confirmingToggle && patient && (
        <ConfirmDialog
          title={patient.active ? "Desactivar mascota" : "Reactivar mascota"}
          message={patient.active
            ? `${patient.name} dejara de aparecer en la lista y no podra usarse en citas nuevas. Su historial se conserva.`
            : `${patient.name} volvera a estar disponible para citas y registros.`}
          confirmLabel={patient.active ? "Si, desactivar" : "Si, reactivar"}
          destructive={patient.active}
          loading={updatePatient.isPending}
          onConfirm={toggleActive}
          onCancel={() => setConfirmingToggle(false)}
        />
      )}
    </div>
  );
}
