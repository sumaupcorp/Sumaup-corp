import {
  LayoutDashboard,
  Building2,
  Store,
  Package,
  Boxes,
  Truck,
  Users,
  UserCog,
  ShoppingCart,
  UtensilsCrossed,
  ChefHat,
  BarChart3,
  FileText,
  SlidersHorizontal,
  CalendarClock,
  CalendarCheck,
  PawPrint,
  Cake,
  ClipboardList,
  BedDouble,
  type LucideIcon,
} from "lucide-react";

/**
 * Mapeo de navegacion del SaaS. Cada item se muestra si:
 *  - el modulo (module) esta en enabledModules de la empresa (o module = null = siempre), y
 *  - el usuario tiene el permiso (perm = null = sin requisito).
 * Asi el menu se construye desde el rubro/plan + RBAC (no se duplica por rubro).
 */
export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  module: string | null;
  perm: string | null;
}

export const NAV_ITEMS: NavItem[] = [
  { label: "Inicio", href: "/dashboard", icon: LayoutDashboard, module: null, perm: null },
  { label: "Empresas", href: "/dashboard/companies", icon: Building2, module: "company", perm: "company:read" },
  { label: "Sucursales", href: "/dashboard/branches", icon: Store, module: "branch", perm: "branch:read" },
  { label: "Productos", href: "/dashboard/products", icon: Package, module: "product", perm: "product:read" },
  { label: "Inventario", href: "/dashboard/inventory", icon: Boxes, module: "inventory", perm: "inventory:read" },
  { label: "Ventas / Caja", href: "/dashboard/pos", icon: ShoppingCart, module: "pos", perm: "sale:read" },
  { label: "Restaurante", href: "/dashboard/restaurant", icon: UtensilsCrossed, module: "tables", perm: "table:read" },
  { label: "Cocina", href: "/dashboard/restaurant/kitchen", icon: ChefHat, module: "kitchen", perm: "kitchen:read" },
  { label: "Lotes y vencimientos", href: "/dashboard/batches", icon: CalendarClock, module: "batch-expiry", perm: "batch:read" },
  { label: "Recetas", href: "/dashboard/prescriptions", icon: ClipboardList, module: "prescription", perm: "prescription:read" },
  { label: "Citas", href: "/dashboard/appointments", icon: CalendarCheck, module: "appointments", perm: "appointment:read" },
  { label: "Pacientes", href: "/dashboard/patients", icon: PawPrint, module: "patients", perm: "patient:read" },
  { label: "Encargos", href: "/dashboard/custom-orders", icon: Cake, module: "custom-orders", perm: "custom-order:read" },
  { label: "Hospedaje", href: "/dashboard/lodging", icon: BedDouble, module: "lodging", perm: "lodging:read" },
  { label: "Clientes", href: "/dashboard/customers", icon: Users, module: "customer", perm: "customer:read" },
  { label: "Proveedores", href: "/dashboard/suppliers", icon: Truck, module: "supplier", perm: "supplier:read" },
  { label: "Reportes", href: "/dashboard/reports", icon: BarChart3, module: "report", perm: "report:read" },
  { label: "Documentos", href: "/dashboard/settings/document-templates", icon: FileText, module: "invoice", perm: "document-template:read" },
  { label: "Equipo", href: "/dashboard/team", icon: UserCog, module: null, perm: "user:read" },
  { label: "Modulos", href: "/dashboard/modules", icon: SlidersHorizontal, module: null, perm: "company:manage" },
];
