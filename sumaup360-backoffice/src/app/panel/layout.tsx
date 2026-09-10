"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { useSession } from "@/features/auth/session";
import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { Spinner } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";

export default function PanelLayout({ children }: { children: React.ReactNode }) {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const { data: session, isLoading } = useSession();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  if (loading || !user || isLoading) {
    return <div className="flex h-screen items-center justify-center"><Spinner className="size-8" /></div>;
  }

  // Solo personal interno (STAFF) puede entrar al Backoffice.
  if (session && session.userType !== "STAFF") {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-brand-soft p-6 text-center">
        <h1 className="font-heading text-xl font-bold text-foreground">Acceso restringido</h1>
        <p className="max-w-md text-sm text-muted-foreground">
          Este panel es solo para personal interno de SUMAUP360. Tu cuenta no tiene acceso.
        </p>
        <Button variant="outline" onClick={() => logout()}>Cerrar sesion</Button>
      </div>
    );
  }

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-auto bg-brand-soft/40 p-6">{children}</main>
      </div>
    </div>
  );
}
