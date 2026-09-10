"use client";

import { useMemo, useState } from "react";
import {
  useTenantUsers,
  useRoles,
  useCreateRole,
  useAssignRole,
  useRevokeRole,
  useCreateWorker,
  useUpdateAssignment,
  ASSIGNABLE_PERMISSIONS,
  PERMISSION_INFO,
  type TenantUser,
  type TenantRole,
} from "@/features/team/api";
import { useHasPermission } from "@/features/auth/session";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { NAV_ITEMS } from "@/lib/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";

/** Etiquetas legibles de los modulos (desde el mapa de navegacion). */
const MODULE_LABELS = new Map(
  NAV_ITEMS.filter((it) => it.module !== null).map((it) => [it.module as string, it.label])
);

interface CompanyOption {
  id: string;
  legalName: string;
}

export default function TeamPage() {
  const hasPerm = useHasPermission();
  const canManageRoles = hasPerm("role:manage");
  const canManageUsers = hasPerm("user:manage");
  const { companies, currentCompany, enabledModules } = useCompany();

  const users = useTenantUsers();
  const roles = useRoles();

  const [creatingWorker, setCreatingWorker] = useState(false);
  const [assigning, setAssigning] = useState<TenantUser | null>(null);

  /** Usuarios separados por negocio: cada empresa con su gente + los de acceso total. */
  const groups = useMemo(() => {
    const list = users.data ?? [];
    const byCompany = new Map<string, TenantUser[]>();
    const everywhere: TenantUser[] = [];
    for (const u of list) {
      if (u.companyId) {
        const arr = byCompany.get(u.companyId) ?? [];
        arr.push(u);
        byCompany.set(u.companyId, arr);
      } else {
        everywhere.push(u);
      }
    }
    const result: { key: string; title: string; users: TenantUser[]; help?: string }[] = [];
    for (const c of companies) {
      const arr = byCompany.get(c.id);
      if (arr) {
        result.push({ key: c.id, title: c.legalName, users: arr });
        byCompany.delete(c.id);
      }
    }
    // Negocios referenciados por usuarios pero fuera de la lista visible.
    for (const [id, arr] of byCompany) {
      result.push({ key: id, title: arr[0].companyName ?? "Otro negocio", users: arr });
    }
    if (everywhere.length > 0) {
      result.push({
        key: "all",
        title: "Acceso a todos los negocios",
        users: everywhere,
        help: "Estos usuarios no tienen sede fija: pueden operar en cualquier negocio y sede de tu cuenta. Ideal para duenos y administradores generales.",
      });
    }
    return result;
  }, [users.data, companies]);

  return (
    <div>
      <PageHeader
        title="Equipo"
        subtitle="Usuarios de tu negocio: roles, sede y modulos, separados por negocio"
        action={canManageUsers ? (
          <Button onClick={() => setCreatingWorker(true)}>Crear usuario</Button>
        ) : undefined}
      />

      <div className="grid gap-6 lg:grid-cols-2">
        {/* USUARIOS agrupados por negocio */}
        <Card>
          <CardContent className="p-5">
            <div className="mb-3 flex items-center gap-1.5">
              <h2 className="text-sm font-semibold">Usuarios</h2>
              <InfoTip
                align="left"
                text="Cada usuario aparece bajo el negocio de su sede asignada. Los que no tienen sede fija aparecen al final, con acceso a todo."
              />
            </div>
            {users.isLoading ? (
              <Spinner />
            ) : groups.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Aun no hay usuarios en tu equipo. Crea el primero con el boton de arriba.
              </p>
            ) : (
              <div className="space-y-5">
                {groups.map((g) => (
                  <div key={g.key}>
                    <div className="mb-1 flex items-center gap-1.5 border-b border-border pb-1.5">
                      <p className="text-xs font-semibold uppercase tracking-wide text-primary">{g.title}</p>
                      <Badge variant="muted">{g.users.length}</Badge>
                      {g.help && <InfoTip align="left" text={g.help} />}
                    </div>
                    <ul className="divide-y divide-border">
                      {g.users.map((u) => (
                        <UserRow
                          key={u.userId}
                          user={u}
                          roles={roles.data ?? []}
                          canManageUsers={canManageUsers}
                          onEditAssignment={() => setAssigning(u)}
                        />
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* ROLES */}
        <Card>
          <CardContent className="p-5">
            <div className="mb-3 flex items-center gap-1.5">
              <h2 className="text-sm font-semibold">Roles</h2>
              <InfoTip
                align="left"
                text="Un rol es un paquete de permisos con nombre (por ejemplo Cajero o Mozo). Creas el rol una vez y lo asignas a varios usuarios."
              />
            </div>
            {roles.isLoading ? (
              <Spinner />
            ) : (roles.data ?? []).length === 0 ? (
              <p className="mb-4 text-sm text-muted-foreground">
                Aun no tienes roles. Crea uno abajo para poder asignarlo a tu equipo.
              </p>
            ) : (
              <ul className="mb-4 divide-y divide-border">
                {(roles.data ?? []).map((r) => (
                  <RoleRow key={r.id} role={r} />
                ))}
              </ul>
            )}

            {canManageRoles && <NewRoleForm />}
          </CardContent>
        </Card>
      </div>

      {canManageUsers && creatingWorker && (
        <CreateWorkerModal
          companies={companies}
          defaultCompanyId={currentCompany?.id ?? ""}
          enabledModules={enabledModules}
          onClose={() => setCreatingWorker(false)}
        />
      )}

      {canManageUsers && assigning && (
        <AssignmentModal
          user={assigning}
          companies={companies}
          enabledModules={enabledModules}
          onClose={() => setAssigning(null)}
        />
      )}
    </div>
  );
}

/** Fila de usuario: identidad, sede, modulos y gestion de roles. */
function UserRow({ user: u, roles, canManageUsers, onEditAssignment }: {
  user: TenantUser;
  roles: TenantRole[];
  canManageUsers: boolean;
  onEditAssignment: () => void;
}) {
  const assign = useAssignRole();
  const revoke = useRevokeRole();
  const [roleSel, setRoleSel] = useState("");
  const roleIdByCode = useMemo(() => new Map(roles.map((r) => [r.code, r.id])), [roles]);
  const roleNameByCode = useMemo(() => new Map(roles.map((r) => [r.code, r.name])), [roles]);

  return (
    <li className="py-3">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate font-medium text-foreground">
            {u.displayName || u.email || u.firebaseUid}
          </p>
          {u.displayName && u.email && (
            <p className="truncate text-xs text-muted-foreground">{u.email}</p>
          )}
        </div>
        {canManageUsers && (
          <Button size="sm" variant="outline" onClick={onEditAssignment}>
            Sede y modulos
          </Button>
        )}
      </div>

      <div className="mt-1.5 flex flex-wrap items-center gap-1">
        <Badge variant={u.branchId ? "default" : "muted"}>
          {u.branchName ?? "Todas las sedes"}
        </Badge>
        <Badge variant="muted">
          {u.allowedModules === null
            ? "Todos los modulos"
            : `${u.allowedModules.length} ${u.allowedModules.length === 1 ? "modulo" : "modulos"}`}
        </Badge>
      </div>

      <div className="mt-1.5 flex flex-wrap items-center gap-1">
        {u.roles.length === 0 && (
          <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
            sin roles
            <InfoTip text="Sin un rol, el usuario puede entrar al panel pero no puede hacer casi nada. Asignale uno con el selector de abajo." />
          </span>
        )}
        {u.roles.map((rc) => (
          <Badge key={rc} variant="muted">
            {roleNameByCode.get(rc) ?? rc}
            {canManageUsers && roleIdByCode.get(rc) && (
              <button
                className="ml-1 text-destructive"
                title="Quitar este rol"
                onClick={() => revoke.mutate({ userId: u.userId, roleId: roleIdByCode.get(rc)! })}
              >
                ×
              </button>
            )}
          </Badge>
        ))}
      </div>

      {canManageUsers && roles.length > 0 && (
        <div className="mt-2 flex items-center gap-2">
          <Select
            className="h-8 text-xs"
            value={roleSel}
            onChange={(e) => setRoleSel(e.target.value)}
          >
            <option value="">Asignar rol…</option>
            {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </Select>
          <Button
            size="sm"
            variant="outline"
            disabled={!roleSel || assign.isPending}
            onClick={() => { assign.mutate({ userId: u.userId, roleId: roleSel }); setRoleSel(""); }}
          >
            Asignar
          </Button>
        </div>
      )}
    </li>
  );
}

/** Fila de rol con detalle expandible de sus permisos. */
function RoleRow({ role }: { role: TenantRole }) {
  const [expanded, setExpanded] = useState(false);
  return (
    <li className="py-2">
      <div className="flex items-center justify-between gap-2">
        <p className="font-medium text-foreground">
          {role.name} <span className="text-xs text-muted-foreground">({role.code})</span>
        </p>
        <button
          type="button"
          className="text-xs font-medium text-primary hover:underline"
          onClick={() => setExpanded((v) => !v)}
        >
          {expanded ? "Ocultar permisos" : `Ver ${role.permissions.length} ${role.permissions.length === 1 ? "permiso" : "permisos"}`}
        </button>
      </div>
      {expanded && (
        <div className="mt-1.5 flex flex-wrap gap-1">
          {role.permissions.length === 0 && (
            <span className="text-xs text-muted-foreground">Este rol no tiene permisos todavia.</span>
          )}
          {role.permissions.map((code) => {
            const info = PERMISSION_INFO.get(code);
            return (
              <Badge key={code} variant="muted" title={info?.description ?? code}>
                {info?.label ?? code}
              </Badge>
            );
          })}
        </div>
      )}
    </li>
  );
}

/** Formulario de rol nuevo con permisos agrupados, ayuda por permiso y marcar todo. */
function NewRoleForm() {
  const createRole = useCreateRole();
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [perms, setPerms] = useState<Set<string>>(new Set());
  const [msg, setMsg] = useState<string | null>(null);

  const togglePerm = (c: string) =>
    setPerms((s) => { const n = new Set(s); if (!n.delete(c)) n.add(c); return n; });

  const toggleGroup = (codes: string[], allSelected: boolean) =>
    setPerms((s) => {
      const n = new Set(s);
      codes.forEach((c) => (allSelected ? n.delete(c) : n.add(c)));
      return n;
    });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await createRole.mutateAsync({ code, name, permissionCodes: [...perms] });
      setCode(""); setName(""); setPerms(new Set());
      setMsg("Rol creado.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-3 border-t border-border pt-4">
      <h3 className="text-sm font-semibold">Nuevo rol</h3>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label className="flex items-center gap-1.5">
            Codigo
            <InfoTip text="Identificador corto y unico del rol, en minusculas y sin espacios. Ejemplo: cajero, mozo, almacenero." />
          </Label>
          <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="cajero" required />
        </div>
        <div className="space-y-1">
          <Label className="flex items-center gap-1.5">
            Nombre
            <InfoTip text="Nombre visible del rol, como lo vera tu equipo. Ejemplo: Cajero de tienda." />
          </Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Cajero" required />
        </div>
      </div>

      <div className="space-y-3">
        <Label className="flex items-center gap-1.5">
          Permisos
          <InfoTip text="Marca lo que este rol puede hacer. Pasa el cursor sobre el signo de pregunta de cada permiso para ver que habilita exactamente." />
        </Label>
        {ASSIGNABLE_PERMISSIONS.map((g) => {
          const codes = g.items.map((it) => it.code);
          const selectedCount = codes.filter((c) => perms.has(c)).length;
          const allSelected = selectedCount === codes.length;
          return (
            <div key={g.group} className="rounded-lg border border-border p-3">
              <div className="mb-2 flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5">
                  <p className="text-xs font-semibold text-foreground">{g.group}</p>
                  <InfoTip align="left" text={g.description} />
                  {selectedCount > 0 && (
                    <Badge variant="default">{selectedCount}/{codes.length}</Badge>
                  )}
                </div>
                <label className="flex cursor-pointer items-center gap-1.5 text-xs text-muted-foreground">
                  <input
                    type="checkbox"
                    className="accent-primary"
                    checked={allSelected}
                    onChange={() => toggleGroup(codes, allSelected)}
                  />
                  Todo el grupo
                </label>
              </div>
              <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                {g.items.map((it) => (
                  <div key={it.code} className="flex items-center gap-1.5">
                    <label className="flex cursor-pointer items-center gap-1.5 text-xs">
                      <input
                        type="checkbox"
                        className="accent-primary"
                        checked={perms.has(it.code)}
                        onChange={() => togglePerm(it.code)}
                      />
                      <span className={it.sensitive ? "font-medium text-amber-700" : undefined}>
                        {it.label}
                      </span>
                    </label>
                    <InfoTip
                      text={it.sensitive ? `${it.description} (Permiso sensible.)` : it.description}
                    />
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      <div className="flex items-center justify-between gap-2">
        <p className="text-xs text-muted-foreground">
          {perms.size === 0
            ? "Ningun permiso seleccionado"
            : `${perms.size} ${perms.size === 1 ? "permiso seleccionado" : "permisos seleccionados"}`}
        </p>
        <Button type="submit" disabled={createRole.isPending}>
          {createRole.isPending ? "Guardando..." : "Crear rol"}
        </Button>
      </div>
      {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
    </form>
  );
}

/** Selector en cascada: negocio → sede de ese negocio. */
function CompanyBranchPicker({ companies, companyId, branchId, onChangeCompany, onChangeBranch }: {
  companies: CompanyOption[];
  companyId: string;
  branchId: string;
  onChangeCompany: (id: string) => void;
  onChangeBranch: (id: string) => void;
}) {
  const { data: branches = [], isLoading } = useBranches(companyId || undefined);

  return (
    <div className="grid grid-cols-2 gap-3">
      <div className="space-y-1">
        <Label className="flex items-center gap-1.5">
          Negocio
          <InfoTip text="Si tienes varios negocios, elige en cual trabajara. Con 'Todos los negocios' podra operar en cualquiera." />
        </Label>
        <Select
          value={companyId}
          onChange={(e) => { onChangeCompany(e.target.value); onChangeBranch(""); }}
        >
          <option value="">Todos los negocios</option>
          {companies.map((c) => <option key={c.id} value={c.id}>{c.legalName}</option>)}
        </Select>
      </div>
      <div className="space-y-1">
        <Label className="flex items-center gap-1.5">
          Sede
          <InfoTip text="El usuario solo podra vender y abrir caja en la sede elegida. Es obligatoria cuando eliges un negocio." />
        </Label>
        <Select
          value={branchId}
          onChange={(e) => onChangeBranch(e.target.value)}
          disabled={!companyId}
          required={!!companyId}
        >
          <option value="">
            {!companyId ? "Todas las sedes" : isLoading ? "Cargando sedes..." : "Elige una sede…"}
          </option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>
    </div>
  );
}

/** Selector de modulos visibles: vacio = todos los habilitados. */
function ModulePicker({ enabledModules, selected, onToggle }: {
  enabledModules: string[];
  selected: Set<string>;
  onToggle: (m: string) => void;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="flex items-center gap-1.5">
        Modulos que vera
        <InfoTip text="Controla que secciones del menu vera este usuario. Si no marcas ninguno, vera todos los modulos habilitados de tu plan." />
      </Label>
      <div className="grid grid-cols-2 gap-1">
        {enabledModules.map((m) => (
          <label key={m} className="flex cursor-pointer items-center gap-1.5 text-xs">
            <input type="checkbox" className="accent-primary" checked={selected.has(m)}
              onChange={() => onToggle(m)} />
            {MODULE_LABELS.get(m) ?? m}
          </label>
        ))}
      </div>
      <p className="text-xs text-muted-foreground">
        {selected.size === 0 ? "Vera todos los modulos habilitados." : `Vera ${selected.size} ${selected.size === 1 ? "modulo" : "modulos"}.`}
      </p>
    </div>
  );
}

/** Alta de trabajador: cuenta (correo+contrasena) + rol + negocio/sede + modulos en un paso. */
function CreateWorkerModal({ companies, defaultCompanyId, enabledModules, onClose }: {
  companies: CompanyOption[];
  defaultCompanyId: string;
  enabledModules: string[];
  onClose: () => void;
}) {
  const createWorker = useCreateWorker();
  const { data: roles = [] } = useRoles();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [roleId, setRoleId] = useState("");
  const [companyId, setCompanyId] = useState(defaultCompanyId);
  const [branchId, setBranchId] = useState("");
  const [modules, setModules] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);

  const toggleModule = (m: string) =>
    setModules((s) => { const n = new Set(s); if (!n.delete(m)) n.add(m); return n; });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (companyId && !branchId) {
      setError("Elige la sede del negocio seleccionado, o cambia a 'Todos los negocios'.");
      return;
    }
    try {
      await createWorker.mutateAsync({
        name, email, password,
        roleId: roleId || undefined,
        branchId: branchId || undefined,
        allowedModules: modules.size > 0 ? [...modules] : undefined,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-12">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">Crear usuario</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        <form onSubmit={submit} className="space-y-3">
          <div className="space-y-1">
            <Label>Nombre completo</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} required
              placeholder="Ej. Maria Lopez" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>Correo</Label>
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required
                placeholder="maria@negocio.com" />
            </div>
            <div className="space-y-1">
              <Label className="flex items-center gap-1.5">
                Contrasena inicial
                <InfoTip align="right" text="Contrasena temporal para su primer ingreso. Compartela con tu trabajador; podra cambiarla despues." />
              </Label>
              <Input type="text" value={password} onChange={(e) => setPassword(e.target.value)}
                required minLength={6} placeholder="Min. 6 caracteres" />
            </div>
          </div>

          <div className="space-y-1">
            <Label className="flex items-center gap-1.5">
              Rol
              <InfoTip text="El rol define que puede hacer: vender, ver reportes, gestionar productos. Si aun no tienes roles, crealos en la seccion Roles." />
            </Label>
            <Select value={roleId} onChange={(e) => setRoleId(e.target.value)}>
              <option value="">Sin rol (asignar luego)</option>
              {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </Select>
          </div>

          <CompanyBranchPicker
            companies={companies}
            companyId={companyId}
            branchId={branchId}
            onChangeCompany={setCompanyId}
            onChangeBranch={setBranchId}
          />

          <ModulePicker enabledModules={enabledModules} selected={modules} onToggle={toggleModule} />

          <p className="rounded-lg bg-muted/60 px-3 py-2 text-xs text-muted-foreground">
            Comparte el correo y la contrasena inicial con tu trabajador; con ellos entra a este panel.
          </p>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" disabled={createWorker.isPending}>
              {createWorker.isPending ? "Creando..." : "Crear usuario"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

/** Edicion de la asignacion operativa: negocio/sede + modulos visibles. */
function AssignmentModal({ user, companies, enabledModules, onClose }: {
  user: TenantUser;
  companies: CompanyOption[];
  enabledModules: string[];
  onClose: () => void;
}) {
  const update = useUpdateAssignment();
  const [companyId, setCompanyId] = useState(user.companyId ?? "");
  const [branchId, setBranchId] = useState(user.branchId ?? "");
  const [modules, setModules] = useState<Set<string>>(new Set(user.allowedModules ?? []));
  const [error, setError] = useState<string | null>(null);

  const toggleModule = (m: string) =>
    setModules((s) => { const n = new Set(s); if (!n.delete(m)) n.add(m); return n; });

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (companyId && !branchId) {
      setError("Elige la sede del negocio seleccionado, o cambia a 'Todos los negocios'.");
      return;
    }
    try {
      await update.mutateAsync({
        userId: user.userId,
        branchId: branchId || null,
        allowedModules: modules.size > 0 ? [...modules] : null,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-16">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-sm font-semibold">Sede y modulos</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>
        <p className="mb-3 text-sm text-muted-foreground">
          {user.displayName || user.email}
        </p>

        <form onSubmit={save} className="space-y-3">
          <CompanyBranchPicker
            companies={companies}
            companyId={companyId}
            branchId={branchId}
            onChangeCompany={setCompanyId}
            onChangeBranch={setBranchId}
          />

          <ModulePicker enabledModules={enabledModules} selected={modules} onToggle={toggleModule} />

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" disabled={update.isPending}>
              {update.isPending ? "Guardando..." : "Guardar"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
