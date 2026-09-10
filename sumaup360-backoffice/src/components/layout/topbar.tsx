"use client";

import { LogOut } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useSession } from "@/features/auth/session";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/misc";

export function Topbar() {
  const { user, logout } = useAuth();
  const { data: session } = useSession();
  const role = session?.roles.find((r) => r !== undefined) ?? "staff";

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-white px-6">
      <div className="text-sm text-muted-foreground">Panel interno SUMAUP360</div>
      <div className="flex items-center gap-3">
        <span className="text-sm text-foreground">{user?.email}</span>
        <Badge variant="warning">{role}</Badge>
        <Button variant="outline" size="sm" onClick={() => logout()}>
          <LogOut className="size-4" /> Salir
        </Button>
      </div>
    </header>
  );
}
