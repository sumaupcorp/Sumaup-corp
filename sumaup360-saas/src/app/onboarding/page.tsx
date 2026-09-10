"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { useSession } from "@/features/auth/session";
import { useBusinessTypes, useVerticals } from "@/features/catalog/api";
import { useOnboard, useRucLookup, type OperationAnswers } from "@/features/onboarding/api";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";

const TOTAL_STEPS = 4;

/** Defaults de las preguntas segun el rubro elegido (reflejan los defaults del backend). */
const RUBRO_DEFAULTS: Record<string, OperationAnswers> = {
  restaurant: { sellsOnTables: true },
  pharmacy: { tracksExpiry: true, handlesPrescriptions: true },
  veterinary: { takesAppointments: true },
  bakery: { takesCustomOrders: true, tracksExpiry: true },
  pastry: { takesCustomOrders: true },
  minimarket: { tracksExpiry: true },
  beauty: { takesAppointments: true },
};

interface Question {
  key: keyof OperationAnswers;
  label: string;
  hint: string;
}

const QUESTIONS: Question[] = [
  { key: "sellsOnTables", label: "Atiendo en mesas", hint: "Activa mesas, comandas y cocina" },
  { key: "tracksExpiry", label: "Manejo productos con vencimiento o lotes", hint: "Activa lotes y alertas de vencimiento" },
  { key: "takesAppointments", label: "Atiendo con citas", hint: "Activa la agenda y la reserva online por QR" },
  { key: "takesCustomOrders", label: "Recibo pedidos por encargo", hint: "Activa encargos con fecha de entrega y adelanto" },
  { key: "handlesPrescriptions", label: "Registro recetas medicas", hint: "Activa el registro de recetas" },
];

/**
 * Preguntas segun el rubro. En hospedaje las habitaciones vienen por defecto y
 * la pregunta de mesas se reformula como restaurante/cafeteria del hotel.
 */
const questionsFor = (rubro: string): Question[] =>
  rubro === "lodging"
    ? QUESTIONS.map((q) =>
        q.key === "sellsOnTables"
          ? { key: "sellsFood" as const, label: "Atiendo restaurante o cafeteria dentro del hospedaje", hint: "Activa mesas, comandas y cocina" }
          : q
      )
    : QUESTIONS;

export default function OnboardingPage() {
  const { user, emailVerified, loading, logout } = useAuth();
  const router = useRouter();
  const { data: session, isLoading: sessLoading } = useSession();
  const { data: businessTypes = [] } = useBusinessTypes();
  const onboard = useOnboard();
  const rucLookup = useRucLookup();

  const [step, setStep] = useState(1);
  // Paso 1: RUC opcional
  const [hasRuc, setHasRuc] = useState<boolean | null>(null);
  const [ruc, setRuc] = useState("");
  const [rucChecked, setRucChecked] = useState(false);
  // Paso 2: datos del negocio
  const [businessName, setBusinessName] = useState("");
  const [bt, setBt] = useState("");
  const { data: verticals = [] } = useVerticals(bt || undefined);
  const [vertical, setVertical] = useState("");
  const [suggested, setSuggested] = useState<string | null>(null);
  // Paso 3: preguntas de operacion
  const [operation, setOperation] = useState<OperationAnswers>({});
  // Paso 4: sucursales
  const [branchCount, setBranchCount] = useState(1);
  const [branchNames, setBranchNames] = useState<string[]>(["Local Principal"]);
  const [error, setError] = useState<string | null>(null);

  // Guards
  useEffect(() => { if (!loading && !user) router.replace("/login"); }, [loading, user, router]);
  useEffect(() => { if (session?.tenantId) router.replace("/dashboard"); }, [session, router]);
  useEffect(() => {
    if (user && !emailVerified && session && session.userType !== "STAFF") router.replace("/verify-email");
  }, [user, emailVerified, session, router]);

  const lookupRuc = async () => {
    setError(null);
    setRucChecked(false);
    try {
      const r = await rucLookup.mutateAsync(ruc.trim());
      setRucChecked(true);
      if (r.found) {
        if (r.razonSocial && !businessName) setBusinessName(r.razonSocial);
        if (r.suggestedBusinessTypeCode) {
          setBt(r.suggestedBusinessTypeCode);
          setSuggested(r.suggestedBusinessTypeCode);
        }
      } else if (r.detail) {
        setError(r.detail);
      }
    } catch (e) {
      setError("No pudimos consultar SUNAT: " + (e as Error).message);
    }
  };

  const chooseRubro = (code: string) => {
    setBt(code);
    setVertical("");
    setOperation(RUBRO_DEFAULTS[code] ?? {});
  };

  const setCount = (n: number) => {
    const c = Math.max(1, Math.min(10, n));
    setBranchCount(c);
    setBranchNames((prev) => {
      const next = [...prev];
      while (next.length < c) next.push(`Local ${next.length + 1}`);
      next.length = c;
      return next;
    });
  };

  const submit = async () => {
    setError(null);
    try {
      await onboard.mutateAsync({
        businessName,
        businessTypeCode: bt,
        verticalCode: vertical || undefined,
        ruc: hasRuc && ruc ? ruc.trim() : undefined,
        branchNames: branchNames.filter((n) => n.trim()),
        operation,
      });
      router.replace("/dashboard");
    } catch (e) {
      setError("No se pudo configurar tu negocio: " + (e as Error).message);
    }
  };

  if (loading || sessLoading || !user) {
    return <div className="flex h-screen items-center justify-center"><Spinner className="size-8" /></div>;
  }

  const lookup = rucLookup.data;

  return (
    <div className="min-h-screen bg-brand-soft">
      <header className="flex h-16 items-center justify-between border-b border-border bg-white px-6">
        <span className="font-heading text-lg font-bold text-brand-blue">SUMAUP360</span>
        <button onClick={() => logout()} className="text-sm text-muted-foreground hover:underline">Salir</button>
      </header>

      <div className="mx-auto max-w-xl p-6">
        <div className="mb-6 flex items-center gap-2">
          <Badge variant="success">Plan Free</Badge>
          <span className="text-sm text-muted-foreground">Paso {step} de {TOTAL_STEPS}</span>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
          {/* PASO 1: RUC opcional */}
          {step === 1 && (
            <div className="space-y-4">
              <div>
                <h1 className="font-heading text-xl font-bold">¿Tu negocio tiene RUC?</h1>
                <p className="text-sm text-muted-foreground">
                  Si lo tienes, lo consultamos en SUNAT y llenamos tus datos por ti. Si no, puedes continuar sin RUC.
                </p>
              </div>
              <div className="flex gap-2">
                <Button variant={hasRuc === true ? "default" : "outline"} onClick={() => setHasRuc(true)}>Si, tengo RUC</Button>
                <Button variant={hasRuc === false ? "default" : "outline"}
                  onClick={() => { setHasRuc(false); setRuc(""); setStep(2); }}>
                  No, continuar sin RUC
                </Button>
              </div>
              {hasRuc && (
                <div className="space-y-3">
                  <div className="flex items-end gap-2">
                    <div className="flex-1 space-y-1">
                      <Label>RUC</Label>
                      <Input value={ruc} onChange={(e) => setRuc(e.target.value.replace(/\D/g, ""))} maxLength={11} placeholder="20123456789" />
                    </div>
                    <Button variant="outline" disabled={ruc.length !== 11 || rucLookup.isPending} onClick={lookupRuc}>
                      {rucLookup.isPending ? "Consultando..." : "Buscar en SUNAT"}
                    </Button>
                  </div>
                  {rucChecked && lookup?.found && (
                    <div className="rounded-lg border border-border bg-brand-soft p-3 text-sm">
                      <p className="font-medium">{lookup.razonSocial}</p>
                      <p className="text-muted-foreground">
                        {[lookup.estado, lookup.condicion].filter(Boolean).join(" · ")}
                        {lookup.actividadPrincipal ? ` · ${lookup.actividadPrincipal}` : ""}
                      </p>
                      {suggested && (
                        <p className="mt-1 text-brand-blue">
                          Rubro sugerido: {businessTypes.find((b) => b.code === suggested)?.name ?? suggested}
                        </p>
                      )}
                    </div>
                  )}
                  {error && <p className="text-sm text-destructive">{error}</p>}
                  <div className="flex justify-end">
                    <Button disabled={ruc.length !== 11} onClick={() => { if (suggested) setOperation(RUBRO_DEFAULTS[suggested] ?? {}); setStep(2); }}>
                      Continuar
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* PASO 2: datos del negocio */}
          {step === 2 && (
            <div className="space-y-4">
              <div>
                <h1 className="font-heading text-xl font-bold">¿A que se dedica tu negocio?</h1>
                <p className="text-sm text-muted-foreground">Con esto configuramos los modulos que necesitas.</p>
              </div>
              <div className="space-y-1">
                <Label>Nombre de tu negocio</Label>
                <Input value={businessName} onChange={(e) => setBusinessName(e.target.value)} placeholder="Mi Negocio SAC" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Rubro {suggested && bt === suggested ? "(sugerido por SUNAT)" : ""}</Label>
                  <Select value={bt} onChange={(e) => chooseRubro(e.target.value)}>
                    <option value="">Selecciona…</option>
                    {businessTypes.map((b) => <option key={b.code} value={b.code}>{b.name}</option>)}
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>Especialidad</Label>
                  <Select value={vertical} onChange={(e) => setVertical(e.target.value)} disabled={!bt || verticals.length === 0}>
                    <option value="">{verticals.length ? "Opcional…" : "—"}</option>
                    {verticals.map((v) => <option key={v.code} value={v.code}>{v.name}</option>)}
                  </Select>
                </div>
              </div>
              <div className="flex justify-between">
                <Button variant="ghost" onClick={() => setStep(1)}>Atras</Button>
                <Button disabled={!businessName || !bt} onClick={() => setStep(3)}>Continuar</Button>
              </div>
            </div>
          )}

          {/* PASO 3: como opera el negocio */}
          {step === 3 && (
            <div className="space-y-4">
              <div>
                <h1 className="font-heading text-xl font-bold">¿Como trabaja tu negocio?</h1>
                <p className="text-sm text-muted-foreground">
                  Marcamos lo tipico de tu rubro; ajusta lo que haga falta. Esto define los modulos de tu panel
                  (puedes cambiarlos despues en Modulos).
                </p>
              </div>
              <ul className="space-y-2">
                {questionsFor(bt).map((q) => (
                  <li key={q.key} className="rounded-lg border border-border p-3">
                    <label className="flex cursor-pointer items-start gap-3">
                      <input
                        type="checkbox"
                        className="mt-1"
                        checked={operation[q.key] === true}
                        onChange={(e) => setOperation((prev) => ({ ...prev, [q.key]: e.target.checked }))}
                      />
                      <span>
                        <span className="block text-sm font-medium">{q.label}</span>
                        <span className="block text-xs text-muted-foreground">{q.hint}</span>
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
              <div className="flex justify-between">
                <Button variant="ghost" onClick={() => setStep(2)}>Atras</Button>
                <Button onClick={() => setStep(4)}>Continuar</Button>
              </div>
            </div>
          )}

          {/* PASO 4: sucursales */}
          {step === 4 && (
            <div className="space-y-4">
              <div>
                <h1 className="font-heading text-xl font-bold">¿Cuantos locales tienes?</h1>
                <p className="text-sm text-muted-foreground">Puedes agregar mas despues.</p>
              </div>
              <div className="flex gap-2">
                <Button variant={branchCount === 1 ? "default" : "outline"} onClick={() => setCount(1)}>Solo 1</Button>
                <Button variant={branchCount > 1 ? "default" : "outline"} onClick={() => setCount(Math.max(2, branchCount))}>Varios</Button>
                {branchCount > 1 && (
                  <Input type="number" min={2} max={10} value={branchCount} onChange={(e) => setCount(Number(e.target.value))} className="w-20" />
                )}
              </div>
              <div className="space-y-2">
                {branchNames.map((n, i) => (
                  <div key={i} className="space-y-1">
                    <Label>Local {i + 1}</Label>
                    <Input value={n} onChange={(e) => setBranchNames((p) => p.map((x, j) => (j === i ? e.target.value : x)))} />
                  </div>
                ))}
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <div className="flex justify-between">
                <Button variant="ghost" onClick={() => setStep(3)}>Atras</Button>
                <Button onClick={submit} disabled={onboard.isPending}>
                  {onboard.isPending ? "Configurando..." : "Crear mi negocio"}
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
