import { LayoutDashboard, Building2, FileText, UserCog, ScrollText, Users, ClipboardList, Bike, ReceiptText, CalendarClock, Bot, PackageSearch, Bell, type LucideIcon } from "lucide-react";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  perm: string | null;
}

/** Navegacion del Backoffice. Se muestra segun el permiso del staff. */
export const NAV_ITEMS: NavItem[] = [
  { label: "Inicio", href: "/panel", icon: LayoutDashboard, perm: null },
  { label: "Clientes", href: "/panel/clientes", icon: Building2, perm: "client:read" },
  { label: "Personas", href: "/panel/personas", icon: Users, perm: "ai:usage:read" },
  { label: "Notificaciones", href: "/panel/notificaciones", icon: Bell, perm: "person:profile:manage" },
  { label: "Recibos", href: "/panel/recibos", icon: FileText, perm: "receipt:read" },
  { label: "Solicitudes", href: "/panel/solicitudes", icon: ClipboardList, perm: "receipt:read" },
  { label: "Peya", href: "/panel/peya", icon: Bike, perm: "receipt:read" },
  { label: "Honorarios", href: "/panel/honorarios", icon: ReceiptText, perm: "receipt:read" },
  { label: "Suspension 4ta", href: "/panel/suspensiones", icon: CalendarClock, perm: "receipt:read" },
  { label: "Catalogo", href: "/panel/catalogo", icon: PackageSearch, perm: "catalog:product:manage" },
  { label: "IA", href: "/panel/ia", icon: Bot, perm: "ai:manage" },
  { label: "Staff", href: "/panel/staff", icon: UserCog, perm: "staff:read" },
  { label: "Auditoria", href: "/panel/auditoria", icon: ScrollText, perm: "audit:read" },
];
