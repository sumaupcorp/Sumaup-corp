"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { MailCheck } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { AuthShell } from "@/components/layout/auth-shell";
import { Button } from "@/components/ui/button";

export default function VerifyEmailPage() {
  const { user, emailVerified, loading, resendVerification, reloadUser, logout } = useAuth();
  const router = useRouter();
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  useEffect(() => {
    if (emailVerified) router.replace("/onboarding");
  }, [emailVerified, router]);

  const check = async () => {
    setBusy(true); setInfo(null);
    const ok = await reloadUser();
    setBusy(false);
    if (ok) router.replace("/onboarding");
    else setInfo("Aun no detectamos la verificacion. Confirma desde el correo y vuelve a intentar.");
  };

  const resend = async () => {
    setBusy(true); setInfo(null);
    try {
      await resendVerification();
      setInfo("Te reenviamos el correo de verificacion.");
    } catch {
      setInfo("Espera un momento antes de reenviar.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell title="Verifica tu correo" subtitle="Es el ultimo paso antes de empezar">
      <div className="space-y-5">
        <div className="flex items-center gap-3 rounded-lg bg-accent p-4">
          <MailCheck className="size-6 text-brand-blue" />
          <p className="text-sm text-foreground">
            Enviamos un enlace de verificacion a{" "}
            <span className="font-medium">{user?.email}</span>. Abrelo para activar tu cuenta
            (revisa tambien spam).
          </p>
        </div>

        {info && <p className="text-sm text-muted-foreground">{info}</p>}

        <Button className="w-full" onClick={check} disabled={busy}>
          {busy ? "Verificando..." : "Ya verifique mi correo"}
        </Button>
        <Button variant="outline" className="w-full" onClick={resend} disabled={busy}>
          Reenviar correo
        </Button>
        <button onClick={() => logout()} className="block w-full text-center text-sm text-muted-foreground hover:underline">
          Usar otra cuenta
        </button>
      </div>
    </AuthShell>
  );
}
