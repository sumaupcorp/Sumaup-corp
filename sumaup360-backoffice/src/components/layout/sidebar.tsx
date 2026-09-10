"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ShieldCheck } from "lucide-react";
import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "@/lib/navigation";
import { useHasPermission } from "@/features/auth/session";

export function Sidebar() {
  const pathname = usePathname();
  const hasPerm = useHasPermission();
  const items = NAV_ITEMS.filter((it) => it.perm === null || hasPerm(it.perm));

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-border bg-brand-soft">
      <div className="flex h-16 items-center gap-2 border-b border-border px-5">
        <div className="flex size-8 items-center justify-center rounded-lg bg-brand-blue text-white">
          <ShieldCheck className="size-4" />
        </div>
        <div className="leading-tight">
          <span className="block font-heading text-sm font-bold text-brand-blue">SUMAUP360</span>
          <span className="block text-xs text-muted-foreground">Backoffice</span>
        </div>
      </div>
      <nav className="flex-1 space-y-1 p-3">
        {items.map((it) => {
          const active = pathname === it.href || (it.href !== "/panel" && pathname.startsWith(it.href));
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
      <div className="border-t border-border p-4 text-xs text-muted-foreground">Panel interno</div>
    </aside>
  );
}
