"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/api";

export interface TenantUser {
  userId: string;
  firebaseUid: string;
  email: string | null;
  displayName: string | null;
  userType: string;
  roles: string[];
  /** Sede asignada (null = todas). */
  branchId: string | null;
  branchName: string | null;
  /** Negocio (empresa) al que pertenece la sede asignada (null = todo el tenant). */
  companyId: string | null;
  companyName: string | null;
  /** Modulos visibles (null = todos los habilitados). */
  allowedModules: string[] | null;
}

export interface CreateWorkerInput {
  email: string;
  password: string;
  name: string;
  roleId?: string;
  branchId?: string;
  allowedModules?: string[];
}

export interface TenantRole {
  id: string;
  code: string;
  name: string;
  tenantId: string;
  permissions: string[];
}

export interface AssignablePermission {
  code: string;
  label: string;
  /** Explicacion en lenguaje del negocio; se muestra en el tooltip de ayuda. */
  description: string;
  /** Permiso sensible: da control administrativo, se resalta en la UI. */
  sensitive?: boolean;
}

export interface PermissionGroup {
  group: string;
  /** Que cubre el grupo; se muestra como ayuda en la cabecera. */
  description: string;
  items: AssignablePermission[];
}

/** Permisos que un rol de tenant puede otorgar (catalogo para la UI). */
export const ASSIGNABLE_PERMISSIONS: PermissionGroup[] = [
  {
    group: "Catalogo y clientes",
    description: "Productos, clientes y proveedores del negocio.",
    items: [
      { code: "product:read", label: "Ver productos", description: "Puede consultar el catalogo de productos, precios y presentaciones, sin modificar nada." },
      { code: "product:manage", label: "Gestionar productos", description: "Puede crear productos, cambiar precios, fotos y presentaciones, y desactivarlos del catalogo." },
      { code: "customer:read", label: "Ver clientes", description: "Puede consultar la lista de clientes y sus datos de contacto." },
      { code: "customer:manage", label: "Gestionar clientes", description: "Puede registrar clientes nuevos y editar o eliminar sus datos." },
      { code: "supplier:read", label: "Ver proveedores", description: "Puede consultar los proveedores registrados y sus datos de contacto." },
      { code: "supplier:manage", label: "Gestionar proveedores", description: "Puede registrar proveedores nuevos y editar o eliminar sus datos." },
    ],
  },
  {
    group: "Operacion",
    description: "El dia a dia: stock, ventas, caja y reportes.",
    items: [
      { code: "inventory:read", label: "Ver inventario", description: "Puede consultar el stock disponible por sede, sin modificarlo." },
      { code: "inventory:adjust", label: "Ajustar inventario", description: "Puede registrar entradas, salidas y ajustes de stock (mermas, conteos, correcciones)." },
      { code: "sale:read", label: "Ver ventas", description: "Puede consultar el historial de ventas y los comprobantes emitidos." },
      { code: "sale:create", label: "Registrar ventas", description: "Puede registrar ventas y emitir comprobantes a clientes." },
      { code: "pos:read", label: "Ver caja", description: "Puede consultar el estado de la caja y sus movimientos, sin operarla." },
      { code: "pos:operate", label: "Operar caja", description: "Puede abrir y cerrar caja, cobrar y registrar ingresos y salidas de dinero." },
      { code: "report:read", label: "Ver reportes", description: "Puede ver los reportes e indicadores del negocio: ventas, productos top, comparativas." },
    ],
  },
  {
    group: "Restaurante",
    description: "Solo aplica si tu rubro es restaurante: mesas, comandas y cocina.",
    items: [
      { code: "table:read", label: "Ver mesas", description: "Puede ver el estado de las mesas del salon (libres, ocupadas, por cobrar)." },
      { code: "table:manage", label: "Gestionar mesas", description: "Puede crear mesas, cambiar su nombre y organizar la distribucion del salon." },
      { code: "order:read", label: "Ver comandas", description: "Puede consultar las comandas del dia y su estado." },
      { code: "order:create", label: "Crear comandas", description: "Puede tomar pedidos de las mesas y enviarlos a cocina." },
      { code: "order:manage", label: "Gestionar comandas", description: "Puede modificar o anular comandas ya enviadas. Util para encargados de salon." },
      { code: "kitchen:read", label: "Ver cocina", description: "Puede ver la cola de pedidos pendientes en cocina." },
      { code: "kitchen:operate", label: "Operar cocina", description: "Puede avanzar los pedidos en cocina: en preparacion, listo, entregado." },
    ],
  },
  {
    group: "Administracion",
    description: "Configuracion del negocio y del equipo. Permisos sensibles.",
    items: [
      { code: "company:read", label: "Ver empresa", description: "Puede consultar los datos del negocio: razon social, RUC, rubro." },
      { code: "company:manage", label: "Gestionar empresa", description: "Puede editar los datos del negocio. Recomendado solo para duenos o administradores.", sensitive: true },
      { code: "branch:read", label: "Ver sucursales", description: "Puede ver la lista de sedes del negocio." },
      { code: "branch:manage", label: "Gestionar sucursales", description: "Puede crear sedes nuevas y editar las existentes.", sensitive: true },
      { code: "user:read", label: "Ver usuarios", description: "Puede ver la lista de usuarios del equipo, sus roles y sedes." },
      { code: "user:manage", label: "Gestionar usuarios", description: "Puede crear usuarios, asignarles roles, sede y modulos. Da mucho control: asignalo con cuidado.", sensitive: true },
      { code: "role:read", label: "Ver roles", description: "Puede consultar los roles del negocio y que permisos tiene cada uno." },
      { code: "role:manage", label: "Gestionar roles", description: "Puede crear roles y cambiar sus permisos. Da mucho control: asignalo con cuidado.", sensitive: true },
    ],
  },
];

/** Indice plano codigo → permiso, para mostrar etiquetas y ayudas fuera del formulario. */
export const PERMISSION_INFO: ReadonlyMap<string, AssignablePermission> = new Map(
  ASSIGNABLE_PERMISSIONS.flatMap((g) => g.items.map((it) => [it.code, it]))
);

export function useTenantUsers() {
  return useQuery({ queryKey: ["tenant-users"], queryFn: () => apiGet<TenantUser[]>("/api/v1/tenant-users") });
}

export function useRoles() {
  return useQuery({ queryKey: ["roles"], queryFn: () => apiGet<TenantRole[]>("/api/v1/roles") });
}

export function useCreateRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { code: string; name: string; permissionCodes: string[] }) =>
      apiPost<TenantRole>("/api/v1/roles", input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roles"] }),
  });
}

/** Alta de un trabajador: cuenta con correo+contrasena + rol/sede/modulos opcionales. */
export function useCreateWorker() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateWorkerInput) => apiPost<TenantUser>("/api/v1/tenant-users", input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tenant-users"] }),
  });
}

/** Cambia sede asignada y modulos visibles (null = sin restriccion). */
export function useUpdateAssignment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { userId: string; branchId: string | null; allowedModules: string[] | null }) =>
      apiPut<TenantUser>(`/api/v1/tenant-users/${vars.userId}/assignment`,
        { branchId: vars.branchId, allowedModules: vars.allowedModules }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tenant-users"] }),
  });
}

export function useAssignRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { userId: string; roleId: string }) =>
      apiPost(`/api/v1/tenant-users/${vars.userId}/roles`, { roleId: vars.roleId }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tenant-users"] }),
  });
}

export function useRevokeRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { userId: string; roleId: string }) =>
      apiDelete(`/api/v1/tenant-users/${vars.userId}/roles/${vars.roleId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tenant-users"] }),
  });
}
