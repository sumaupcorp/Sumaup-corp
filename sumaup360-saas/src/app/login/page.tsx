"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { AuthShell } from "@/components/layout/auth-shell";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";

export default function LoginPage() {
  const { user, loading, loginWithEmail, loginWithGoogle, resetPassword } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [resetMode, setResetMode] = useState(false);
  const [resetSent, setResetSent] = useState(false);

  useEffect(() => {
    if (!loading && user) router.replace("/dashboard");
  }, [loading, user, router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await loginWithEmail(email, password);
      router.replace("/dashboard");
    } catch {
      setError("Correo o contrasena incorrectos.");
    } finally {
      setBusy(false);
    }
  };

  const google = async () => {
    setError(null);
    setBusy(true);
    try {
      await loginWithGoogle();
      router.replace("/dashboard");
    } catch {
      setError("No se pudo iniciar sesion con Google.");
    } finally {
      setBusy(false);
    }
  };

  const sendReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await resetPassword(email.trim());
      setResetSent(true);
    } catch (err) {
      const code = (err as { code?: string }).code ?? "";
      if (code === "auth/invalid-email") {
        setError("Ingresa un correo valido.");
      } else if (code === "auth/user-not-found") {
        // Mismo mensaje que el exito: no revelamos si el correo existe o no.
        setResetSent(true);
      } else {
        setError("No pudimos enviar el correo. Intenta de nuevo.");
      }
    } finally {
      setBusy(false);
    }
  };

  const backToLogin = () => {
    setResetMode(false);
    setResetSent(false);
    setError(null);
  };

  if (resetMode) {
    return (
      <AuthShell title="Recupera tu contrasena" subtitle="Te enviaremos un enlace a tu correo">
        {resetSent ? (
          <div className="space-y-4">
            <div className="rounded-xl border border-border bg-brand-soft p-4 text-sm">
              <p className="font-medium text-foreground">Revisa tu correo</p>
              <p className="mt-1 text-muted-foreground">
                Si <span className="font-medium">{email}</span> tiene una cuenta, te llegara un
                enlace para crear una contrasena nueva. Revisa tambien la carpeta de spam.
              </p>
            </div>
            <Button variant="outline" className="w-full" onClick={backToLogin}>
              Volver a iniciar sesion
            </Button>
          </div>
        ) : (
          <form onSubmit={sendReset} className="space-y-4">
            <div className="space-y-1">
              <Label htmlFor="reset-email">Correo de tu cuenta</Label>
              <Input id="reset-email" type="email" autoComplete="email" autoFocus
                value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={busy}>
              {busy ? "Enviando..." : "Enviar enlace de recuperacion"}
            </Button>
            <button type="button" onClick={backToLogin}
              className="w-full text-center text-sm font-medium text-brand-blue hover:underline">
              Volver a iniciar sesion
            </button>
          </form>
        )}
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Inicia sesion" subtitle="Accede al panel de tu negocio">
      <form onSubmit={submit} className="space-y-4">
        <div className="space-y-1">
          <Label htmlFor="email">Correo</Label>
          <Input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <Label htmlFor="password">Contrasena</Label>
            <button type="button"
              onClick={() => { setResetMode(true); setError(null); }}
              className="text-xs font-medium text-brand-blue hover:underline">
              Olvidaste tu contrasena?
            </button>
          </div>
          <Input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <Button type="submit" className="w-full" disabled={busy}>
          {busy ? "Ingresando..." : "Iniciar sesion"}
        </Button>
      </form>

      <div className="my-5 flex items-center gap-3 text-xs text-muted-foreground">
        <div className="h-px flex-1 bg-border" /> o <div className="h-px flex-1 bg-border" />
      </div>

      <Button variant="outline" className="w-full" onClick={google} disabled={busy}>
        Continuar con Google
      </Button>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        No tienes cuenta?{" "}
        <Link href="/register" className="font-medium text-brand-blue hover:underline">
          Crea tu negocio gratis
        </Link>
      </p>
    </AuthShell>
  );
}
