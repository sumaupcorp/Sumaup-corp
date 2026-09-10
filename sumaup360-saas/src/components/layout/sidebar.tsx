"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "@/lib/navigation";
import { useHasPermission, useSession } from "@/features/auth/session";
import { useCompany } from "@/features/companies/company-context";
import { Select } from "@/components/ui/input";

export function Sidebar() {
  const pathname = usePathname();
  const hasPerm = useHasPermission();
  const { data: session } = useSession();
  const { companies, currentCompany, setCurrentCompanyId, enabledModules } = useCompany();

  // Modulos visibles para ESTE usuario: si tiene lista asignada, recorta el menu
  // (la seguridad real siguen siendo los permisos; esto es UX).
  const allowed = session?.allowedModules ?? null;
  const items = NAV_ITEMS.filter(
    (it) =>
      (it.module === null || enabledModules.includes(it.module)) &&
      (it.module === null || allowed === null || allowed.includes(it.module)) &&
      (it.perm === null || hasPerm(it.perm))
  );

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-border bg-brand-soft">
      <div className="flex h-16 items-center gap-2 border-b border-border px-5">
        <span className="font-heading text-lg font-bold text-brand-blue">SUMAUP360</span>
      </div>

      {companies.length > 0 && (
        <div className="border-b border-border p-3">
          <label className="mb-1 block text-xs text-muted-foreground">Empresa</label>
          <Select
            value={currentCompany?.id ?? ""}
            onChange={(e) => setCurrentCompanyId(e.target.value)}
          >
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.legalName}
              </option>
            ))}
          </Select>
        </div>
      )}

      <nav className="flex-1 space-y-1 p-3">
        {items.map((it) => {
          const active = pathname === it.href || (it.href !== "/dashboard" && pathname.startsWith(it.href));
          const Icon = it.icon;
          return (
            <Link
              key={it.href}
              href={it.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                active ? "bg-brand-blue text-white" : "text-foreground hover:bg-accent"
              )}
            >
              <Icon className="size-4" />
              {it.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
