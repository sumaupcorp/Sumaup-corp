"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { AuthShell } from "@/components/layout/auth-shell";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";

export default function RegisterPage() {
  const { user, loading, registerWithEmail, loginWithGoogle } = useAuth();
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!loading && user) router.replace(user.emailVerified ? "/onboarding" : "/verify-email");
  }, [loading, user, router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (password.length < 6) return setError("La contrasena debe tener al menos 6 caracteres.");
    if (password !== confirm) return setError("Las contrasenas no coinciden.");
    setBusy(true);
    try {
      await registerWithEmail(name, email, password);
      router.replace("/verify-email");
    } catch (err) {
      const code = (err as { code?: string }).code;
      setError(code === "auth/email-already-in-use" ? "Ese correo ya esta registrado." : "No se pudo crear la cuenta.");
    } finally {
      setBusy(false);
    }
  };

  const google = async () => {
    setError(null);
    setBusy(true);
    try {
      await loginWithGoogle();
      router.replace("/onboarding");
    } catch {
      setError("No se pudo continuar con Google.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell title="Crea tu negocio" subtitle="Empieza gratis. Sin tarjeta.">
      <form onSubmit={submit} className="space-y-4">
        <div className="space-y-1">
          <Label htmlFor="name">Tu nombre</Label>
          <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="space-y-1">
          <Label htmlFor="email">Correo</Label>
          <Input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <Label htmlFor="password">Contrasena</Label>
            <Input id="password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <div className="space-y-1">
            <Label htmlFor="confirm">Confirmar</Label>
            <Input id="confirm" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
          </div>
        </div>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <Button type="submit" className="w-full" disabled={busy}>
          {busy ? "Creando cuenta..." : "Crear cuenta gratis"}
        </Button>
      </form>

      <div className="my-5 flex items-center gap-3 text-xs text-muted-foreground">
        <div className="h-px flex-1 bg-border" /> o <div className="h-px flex-1 bg-border" />
      </div>

      <Button variant="outline" className="w-full" onClick={google} disabled={busy}>
        Continuar con Google
      </Button>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Ya tienes cuenta?{" "}
        <Link href="/login" className="font-medium text-brand-blue hover:underline">
          Inicia sesion
        </Link>
      </p>
    </AuthShell>
  );
}
