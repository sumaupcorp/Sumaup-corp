"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { useSession } from "@/features/auth/session";
import { CompanyProvider } from "@/features/companies/company-context";
import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { Spinner } from "@/components/ui/misc";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { user, emailVerified, loading } = useAuth();
  const router = useRouter();
  const { data: session, isLoading: sessionLoading } = useSession();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  const isStaff = session?.userType === "STAFF";
  // Correo sin verificar (clientes) => verificar primero; luego, sin negocio => onboarding.
  const needsVerify = !!user && !emailVerified && !!session && !isStaff;
  const needsOnboarding = !needsVerify && !!session && !session.tenantId && !isStaff;
  useEffect(() => {
    if (needsVerify) router.replace("/verify-email");
    else if (needsOnboarding) router.replace("/onboarding");
  }, [needsVerify, needsOnboarding, router]);

  if (loading || !user || sessionLoading || needsVerify || needsOnboarding) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner className="size-8" />
      </div>
    );
  }

  return (
    <CompanyProvider>
      <div className="flex h-screen overflow-hidden">
        <Sidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <Topbar />
          <main className="flex-1 overflow-auto bg-brand-soft/40 p-6">{children}</main>
        </div>
      </div>
    </CompanyProvider>
  );
}
