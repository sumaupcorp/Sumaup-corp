/**
 * Tipos centrales del dominio del dashboard SaaS / ERP (Línea Negocios).
 *
 * SOLO tipos/interfaces/enums (contrato). Sin lógica de negocio ni imports.
 * El BACKEND es la autoridad de RBAC, tenant, plan y membresía; estos tipos
 * describen la forma de los datos que el frontend RECIBE y REFLEJA.
 *
 * A futuro, estos tipos deberían generarse desde el OpenAPI del backend.
 * Fuente de dominio: ../../docs/04-saas-erp-modules.md y skill sumaup-domain-model.
 */

/** Rubro del negocio. Extensible: el backend puede añadir más. */
export type BusinessType =
  | "restaurant"
  | "pharmacy"
  | "hardware"
  | "grocery"
  | "minimarket"
  | "bookstore"
  | "beauty"
  | "laundry"
  | "veterinary"
  | "bakery"
  | "pastry"
  | "lodging";

/** Sub-rubro / especialización dentro de un BusinessType. */
export type Vertical =
  // restaurant
  | "cevicheria"
  | "polleria"
  | "chifa"
  | "pasteleria"
  | "cafeteria"
  // salud
  | "botica"
  | "farmacia"
  // hospedaje
  | "hotel"
  | "hostal"
  | "apart-hotel"
  | "albergue"
  | "hospedaje"
  // otros rubros sin sub-rubro definido usan su propio businessType como vertical
  | "generico";

/**
 * Clave de módulo del ERP. Une el core (común a todos los rubros) y los
 * extras por vertical. El menú se construye a partir de los módulos activos.
 */
export type ModuleKey =
  // Core ERP
  | "company"
  | "branch"
  | "customer"
  | "product"
  | "inventory"
  | "sale"
  | "pos"
  | "invoice"
  | "supplier"
  | "report"
  // Extras por vertical (mismo code que catalog.modules del backend)
  | "tables"
  | "kitchen"
  | "menu"
  | "batch-expiry"
  | "prescription"
  | "variants"
  | "bulk-units"
  | "quick-pos"
  | "barcode"
  | "seasonal-catalog"
  | "appointments"
  | "patients"
  | "custom-orders"
  | "services"
  | "service-orders"
  | "tickets"
  | "lodging";

/** Conjunto efectivo de módulos activos de una empresa (lo entrega el backend). */
export type EnabledModules = ModuleKey[];

/** Estado de una membresía empresa ↔ plan. */
export type MembershipStatus = "active" | "trial" | "past_due" | "suspended" | "canceled";

/** Plan comercial: define módulos permitidos y límites. */
export interface Plan {
  id: string;
  /** Nombre comercial (ej. "Emprende", "Crece", "Pro"). */
  name: string;
  /** Módulos que el plan permite como techo. */
  allowedModules: ModuleKey[];
  /** Límites del plan; null = sin límite explícito. */
  limits: {
    branches: number | null;
    users: number | null;
    monthlyTransactions: number | null;
  };
  /** Addons disponibles para este plan (ej. "ai-crm"). */
  addons?: string[];
}

/** Vínculo de una empresa con un plan vigente. */
export interface Membership {
  id: string;
  companyId: string;
  planId: string;
  status: MembershipStatus;
  /** ISO 8601. */
  startsAt: string;
  /** ISO 8601; null si no aplica. */
  renewsAt: string | null;
  billingCycle: "monthly" | "yearly";
}

/** Permiso atómico expresado como `recurso:accion` (ej. "sale:create"). */
export interface Permission {
  /** Recurso del dominio (ej. "sale", "inventory", "report", "module"). */
  resource: string;
  /** Acción sobre el recurso (ej. "create", "read", "update", "delete", "adjust", "enable"). */
  action: string;
}

/** Rol: agrupa permisos. El backend define la composición real. */
export interface Role {
  id: string;
  /** Identificador estable (ej. "admin", "cajero", "almacen"). */
  key: string;
  name: string;
  permissions: Permission[];
}

/** Tenant: cuenta de negocio raíz en el modelo multi-tenant. */
export interface Tenant {
  id: string;
  name: string;
  /** RUC u otro identificador fiscal, si aplica. */
  taxId?: string;
}

/** Sucursal de una empresa. */
export interface Branch {
  id: string;
  companyId: string;
  name: string;
  address?: string;
  isActive: boolean;
}

/** Empresa dentro de un tenant; portadora de businessType/vertical y módulos. */
export interface Company {
  id: string;
  tenantId: string;
  name: string;
  businessType: BusinessType;
  vertical: Vertical;
  branches?: Branch[];
}

/**
 * Sesión resuelta que el backend entrega al frontend. El dashboard se
 * configura completamente a partir de esto: el menú desde `enabledModules`
 * y el gating de acciones desde `permissions`/`roles`.
 */
export interface SessionContext {
  tenant: Tenant;
  company: Company;
  businessType: BusinessType;
  vertical: Vertical;
  plan: Plan;
  membership: Membership;
  enabledModules: EnabledModules;
  roles: Role[];
  permissions: Permission[];
}
