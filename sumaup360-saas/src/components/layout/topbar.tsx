"use client";

import { LogOut } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useSession, type Session } from "@/features/auth/session";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/misc";

function roleLabel(s?: Session): string {
  if (!s) return "";
  if (s.userType === "STAFF") return "Staff · Admin global";
  if (s.roles.includes("tenant-admin")) return "Administrador del negocio";
  if (s.roles.length > 0) return s.roles[0];
  return "Usuario";
}

export function Topbar() {
  const { user, logout } = useAuth();
  const { data: session } = useSession();

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-white px-6">
      <div className="text-sm text-muted-foreground">Panel de gestion</div>
      <div className="flex items-center gap-3">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium leading-tight text-foreground">{user?.displayName || user?.email}</p>
          <p className="text-xs leading-tight text-muted-foreground">{user?.email}</p>
        </div>
        {session && <Badge variant={session.userType === "STAFF" ? "warning" : "default"}>{roleLabel(session)}</Badge>}
        <Button variant="outline" size="sm" onClick={() => logout()}>
          <LogOut className="size-4" />
          Salir
        </Button>
      </div>
    </header>
  );
}
