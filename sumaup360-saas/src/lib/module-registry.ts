/**
 * Registro de módulos del ERP.
 *
 * Mapea cada `ModuleKey` a su metadata de UI (etiqueta, ruta, grupo, icono).
 * Sirve para construir el menú/navegación a partir de los `enabledModules`
 * que ENTREGA EL BACKEND. Este archivo NO decide qué módulos están activos
 * ni contiene lógica de negocio: es solo el contrato de presentación.
 *
 * Ver docs/MODULE_SYSTEM.md y skill saas-module-registry.
 */

import type { ModuleKey } from "@/types/domain";

/** Agrupación visual del sidebar. */
export type ModuleGroup = "core" | "ventas" | "inventario" | "administracion" | "vertical";

/** Metadata de presentación de un módulo. */
export interface ModuleMeta {
  /** Clave estable del módulo (coincide con la del registro). */
  key: ModuleKey;
  /** Etiqueta visible en español. */
  label: string;
  /** Ruta dentro de (dashboard), sin el prefijo de grupo de rutas. */
  path: string;
  /** Grupo del sidebar donde se ubica. */
  group: ModuleGroup;
  /**
   * Nombre del icono (ej. lucide-react). String para no acoplar el contrato
   * a una librería de iconos en este archivo de tipos.
   */
  icon: string;
  /** Solo backoffice/administración avanzada. La visibilidad final la define el backend. */
  adminOnly?: boolean;
}

/**
 * Registro completo. El menú real = filtrar este registro por `enabledModules`.
 * Nunca mostrar un módulo cuya key no venga en `enabledModules` del backend.
 */
export const MODULE_REGISTRY: Record<ModuleKey, ModuleMeta> = {
  // --- Core ERP (común a todos los rubros) ---
  company: { key: "company", label: "Empresa", path: "/empresa", group: "administracion", icon: "building-2", adminOnly: true },
  branch: { key: "branch", label: "Sucursales", path: "/sucursales", group: "administracion", icon: "store" },
  customer: { key: "customer", label: "Clientes", path: "/clientes", group: "ventas", icon: "users" },
  product: { key: "product", label: "Productos", path: "/productos", group: "inventario", icon: "package" },
  inventory: { key: "inventory", label: "Inventario", path: "/inventario", group: "inventario", icon: "boxes" },
  sale: { key: "sale", label: "Ventas", path: "/ventas", group: "ventas", icon: "shopping-cart" },
  pos: { key: "pos", label: "Caja / POS", path: "/pos", group: "ventas", icon: "monitor" },
  invoice: { key: "invoice", label: "Comprobantes", path: "/comprobantes", group: "ventas", icon: "file-text" },
  supplier: { key: "supplier", label: "Proveedores", path: "/proveedores", group: "inventario", icon: "truck" },
  report: { key: "report", label: "Reportes", path: "/reportes", group: "administracion", icon: "bar-chart-3" },

  // --- Extras por vertical ---
  tables: { key: "tables", label: "Mesas", path: "/restaurant", group: "vertical", icon: "layout-grid" },
  kitchen: { key: "kitchen", label: "Cocina / Comandas", path: "/restaurant/kitchen", group: "vertical", icon: "chef-hat" },
  menu: { key: "menu", label: "Carta", path: "/carta", group: "vertical", icon: "book-open" },
  "batch-expiry": { key: "batch-expiry", label: "Lotes y vencimientos", path: "/batches", group: "vertical", icon: "calendar-clock" },
  prescription: { key: "prescription", label: "Recetas", path: "/prescriptions", group: "vertical", icon: "clipboard-list" },
  variants: { key: "variants", label: "Variantes", path: "/variantes", group: "vertical", icon: "layers" },
  "bulk-units": { key: "bulk-units", label: "Unidades a granel", path: "/granel", group: "vertical", icon: "scale" },
  "quick-pos": { key: "quick-pos", label: "Venta rápida", path: "/venta-rapida", group: "vertical", icon: "zap" },
  barcode: { key: "barcode", label: "Código de barras", path: "/codigos", group: "vertical", icon: "scan-barcode" },
  "seasonal-catalog": { key: "seasonal-catalog", label: "Catálogo de temporada", path: "/temporada", group: "vertical", icon: "calendar-range" },
  appointments: { key: "appointments", label: "Citas", path: "/appointments", group: "vertical", icon: "calendar-check" },
  patients: { key: "patients", label: "Pacientes / Mascotas", path: "/patients", group: "vertical", icon: "paw-print" },
  "custom-orders": { key: "custom-orders", label: "Pedidos por encargo", path: "/custom-orders", group: "vertical", icon: "cake" },
  services: { key: "services", label: "Servicios", path: "/servicios", group: "vertical", icon: "sparkles" },
  "service-orders": { key: "service-orders", label: "Órdenes de servicio", path: "/ordenes", group: "vertical", icon: "receipt" },
  tickets: { key: "tickets", label: "Tickets", path: "/tickets", group: "vertical", icon: "ticket" },
  lodging: { key: "lodging", label: "Hospedaje", path: "/lodging", group: "vertical", icon: "bed-double" },
};

/** Módulos del core ERP, comunes a todos los rubros. */
export const CORE_MODULES: ModuleKey[] = [
  "company",
  "branch",
  "customer",
  "product",
  "inventory",
  "sale",
  "pos",
  "invoice",
  "supplier",
  "report",
];
