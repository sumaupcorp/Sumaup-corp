/**
 * Tipos del RBAC interno del backoffice (staff de SUMAUP).
 *
 * SOLO tipos/interfaces/enums (contrato). Sin lógica de negocio ni imports.
 * El BACKEND es la autoridad de RBAC; estos tipos describen la forma de los
 * datos que el frontend RECIBE y REFLEJA. A futuro deberían generarse desde
 * el OpenAPI del backend.
 *
 * IMPORTANTE: "staff" (interno) ≠ usuario de la app (Personas) ≠ trabajador
 * de un tenant (Negocios). Aquí SOLO se modela al staff interno.
 *
 * Fuente: ../../docs/STAFF_RBAC.md y skill backoffice-staff-rbac.
 */

/** Rol de staff interno. Extensible: el backend puede añadir más. */
export type StaffRole =
  | "gerencia" // dirección: lectura de reportes/licencias, visión global
  | "admin" // super-admin: todo, incluye staff y roles
  | "contador" // procesa comprobantes y apoya soporte
  | "desarrollador" // soporte técnico: logs/auditoría
  | "soporte" // tickets y vista de usuarios de la app
  | "logistica"; // operación: tenants, planes, tareas operativas

/** Módulos navegables del backoffice. */
export type BackofficeModule =
  | "dashboard"
  | "comprobantes"
  | "support"
  | "app-users"
  | "licenses"
  | "tenants"
  | "plans"
  | "staff"
  | "roles"
  | "audit"
  | "reports";

/** Nivel de acceso de un rol a un módulo (para gating de UI; UX, no seguridad). */
export type ModuleAccess =
  | "full" // ver y operar
  | "read" // solo lectura
  | "none"; // sin acceso

/**
 * Matriz conceptual rol × módulo. Refleja la tabla de docs/STAFF_RBAC.md.
 * Es una referencia de UI; el acceso efectivo lo entrega el backend.
 */
export type ModulePermissionMatrix = Record<
  StaffRole,
  Record<BackofficeModule, ModuleAccess>
>;

/**
 * Permiso atómico expresado como `recurso:accion` (ej. "comprobante:process").
 * El backend define la composición real por rol.
 */
export interface StaffPermission {
  /** Recurso del dominio (ej. "comprobante", "license", "staff", "audit"). */
  resource: string;
  /** Acción (ej. "read", "create", "update", "process", "assign", "renew"). */
  action: string;
}

/** Usuario staff interno autenticado contra Firebase y resuelto por el backend. */
export interface StaffUser {
  id: string;
  /** UID de Firebase del empleado. */
  firebaseUid: string;
  email: string;
  fullName: string;
  /** Roles asignados; un staff puede tener más de uno. */
  roles: StaffRole[];
  /** Permisos efectivos que entrega el backend para esta sesión. */
  permissions: StaffPermission[];
  isActive: boolean;
  /** ISO 8601; null si nunca ingresó. */
  lastLoginAt: string | null;
}

/**
 * Contexto de sesión staff que el backend entrega al frontend. El backoffice
 * se configura a partir de esto: el menú desde los módulos accesibles y el
 * gating de acciones desde permissions.
 */
export interface StaffSessionContext {
  user: StaffUser;
  /** Módulos que el backend habilita para mostrar en el sidebar. */
  enabledModules: BackofficeModule[];
}
