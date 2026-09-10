"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type Tipo = "BOLETA" | "FACTURA";

export default function QrTaxiPage() {
  const params = useParams<{ token: string }>();
  const token = params.token;

  const [valid, setValid] = useState<boolean | null>(null);
  const [taxistaName, setTaxistaName] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [tipo, setTipo] = useState<Tipo>("BOLETA");
  const [monto, setMonto] = useState("");
  const [docNumber, setDocNumber] = useState("");
  const [name, setName] = useState("");
  const [whatsapp, setWhatsapp] = useState("");
  const [email, setEmail] = useState("");
  const [observacion, setObservacion] = useState("");

  // El tipo de documento lo define el comprobante: la factura siempre pide RUC,
  // la boleta pide DNI. No se elige a mano para evitar combinaciones invalidas.
  const docType = tipo === "FACTURA" ? "RUC" : "DNI";

  // Al cambiar de comprobante cambia el documento requerido: limpiamos numero y nombre
  // para no arrastrar un DNI donde ahora va un RUC (o viceversa).
  function changeTipo(t: Tipo) {
    if (t === tipo) return;
    setTipo(t);
    setDocNumber("");
    setName("");
  }

  useEffect(() => {
    fetch(`${BASE}/api/v1/public/qr/taxi/${token}`)
      .then((r) => r.json())
      .then((d) => {
        setValid(Boolean(d?.valid));
        setTaxistaName(d?.taxistaName ?? null);
      })
      .catch(() => setValid(false));
  }, [token]);

  // Precarga solo el nombre del cliente por documento (sin datos de contacto de terceros).
  async function lookup(doc: string) {
    const d = doc.trim();
    if (d.length < 8) return;
    try {
      const r = await fetch(`${BASE}/api/v1/public/qr/taxi/${token}/customers/lookup?doc=${encodeURIComponent(d)}`);
      const data = await r.json();
      if (data?.found && data.name) setName(data.name);
    } catch {
      /* silencioso */
    }
  }

  function validate(): string | null {
    const montoNum = Number(monto.replace(",", "."));
    if (!montoNum || montoNum <= 0) return "Ingresa el monto.";
    const dn = docNumber.trim();
    if (!dn) return "Ingresa tu documento.";
    if (!/^\d+$/.test(dn)) return "El documento debe ser numerico.";
    if (docType === "DNI" && dn.length !== 8) return "El DNI debe tener 8 digitos.";
    if (docType === "RUC" && dn.length !== 11) return "El RUC debe tener 11 digitos.";
    if (!name.trim()) return docType === "RUC" ? "Ingresa la razon social." : "Ingresa tu nombre.";
    const wa = whatsapp.replace(/\D/g, "");
    if (wa.length < 6) return "Ingresa un WhatsApp valido.";
    return null;
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const problem = validate();
    if (problem) return setError(problem);
    const montoNum = Number(monto.replace(",", "."));
    setSending(true);
    try {
      const res = await fetch(`${BASE}/api/v1/public/qr/taxi/${token}/request`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tipo, monto: montoNum, docType, docNumber: docNumber.trim(),
          name: name.trim(), whatsapp: whatsapp.trim(),
          email: email.trim() || null, observacion: observacion.trim() || null,
        }),
      });
      if (!res.ok) {
        let msg = "No pudimos enviar tu solicitud. Intenta de nuevo.";
        try {
          const d = await res.json();
          if (d?.message) msg = d.message;
        } catch {
          /* sin cuerpo */
        }
        throw new Error(msg);
      }
      setDone(true);
    } catch (err) {
      setError(err instanceof Error && err.message ? err.message : "No pudimos enviar tu solicitud.");
    } finally {
      setSending(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-md">
        <div className="mb-5 text-center">
          <span className="font-heading text-xl font-extrabold text-blue-700">SUMAUP360</span>
        </div>

        {valid === null ? (
          <Card><p className="text-center text-sm text-slate-500">Cargando…</p></Card>
        ) : valid === false ? (
          <Card>
            <h1 className="mb-1 text-lg font-bold">Codigo no valido</h1>
            <p className="text-sm text-slate-600">Este enlace no esta disponible. Pidele a tu taxista que te comparta su QR de nuevo.</p>
          </Card>
        ) : done ? (
          <Card>
            <div className="text-center">
              <div className="mx-auto mb-3 flex size-12 items-center justify-center rounded-full bg-green-100 text-green-700">
                <CheckIcon />
              </div>
              <h1 className="mb-1 text-lg font-bold">¡Solicitud enviada!</h1>
              <p className="text-sm text-slate-600">
                {taxistaName ? <>{taxistaName} </> : "Tu taxista "}
                te enviara tu comprobante por WhatsApp o correo en un plazo maximo de <b>24 horas</b>.
              </p>
            </div>
          </Card>
        ) : (
          <Card>
            {taxistaName && (
              <div className="mb-4 flex items-center gap-3 rounded-xl bg-blue-50 px-3 py-2.5">
                <div className="flex size-9 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">
                  {initials(taxistaName)}
                </div>
                <div className="min-w-0">
                  <p className="text-xs text-slate-500">Solicitas tu comprobante a</p>
                  <p className="truncate text-sm font-semibold text-slate-800">{taxistaName}</p>
                </div>
              </div>
            )}
            <h1 className="text-lg font-bold">Solicita tu boleta o factura</h1>
            <p className="mb-4 text-sm text-slate-600">
              Completa tus datos y recibiras tu comprobante por WhatsApp o correo en un plazo maximo de 24 horas.
            </p>
            <form onSubmit={submit} className="space-y-3">
              <div className="grid grid-cols-2 gap-2">
                {(["BOLETA", "FACTURA"] as Tipo[]).map((t) => (
                  <button key={t} type="button" onClick={() => changeTipo(t)}
                    className={`rounded-lg border py-2 text-sm font-semibold ${tipo === t ? "border-blue-600 bg-blue-50 text-blue-700" : "border-slate-200 text-slate-600"}`}>
                    {t === "BOLETA" ? "Boleta" : "Factura"}
                  </button>
                ))}
              </div>
              <Field label="Monto (S/)">
                <input value={monto} onChange={(e) => setMonto(e.target.value)} inputMode="decimal"
                  placeholder="0.00" className={inputCls} />
              </Field>
              <Field label={docType === "RUC" ? "RUC" : "DNI"}>
                <input value={docNumber} onChange={(e) => setDocNumber(e.target.value)} onBlur={() => lookup(docNumber)}
                  inputMode="numeric" maxLength={docType === "RUC" ? 11 : 8}
                  placeholder={docType === "RUC" ? "11 digitos" : "8 digitos"} className={inputCls} />
              </Field>
              <Field label={docType === "RUC" ? "Razon social" : "Nombre"}>
                <input value={name} onChange={(e) => setName(e.target.value)} className={inputCls} />
              </Field>
              <Field label="WhatsApp">
                <input value={whatsapp} onChange={(e) => setWhatsapp(e.target.value)} inputMode="tel"
                  placeholder="999 999 999" className={inputCls} />
              </Field>
              <Field label="Correo (opcional)">
                <input value={email} onChange={(e) => setEmail(e.target.value)} inputMode="email" className={inputCls} />
              </Field>
              <Field label="Observacion (opcional)">
                <input value={observacion} onChange={(e) => setObservacion(e.target.value)} className={inputCls} />
              </Field>

              {error && <p className="text-sm text-red-600">{error}</p>}

              <button type="submit" disabled={sending}
                className="w-full rounded-lg bg-blue-600 py-3 text-sm font-semibold text-white disabled:opacity-50">
                {sending ? "Enviando…" : "Solicitar comprobante"}
              </button>
              <p className="text-center text-xs text-slate-400">Plazo maximo de atencion: 24 horas.</p>
            </form>
          </Card>
        )}
      </div>
    </main>
  );
}

const inputCls = "w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-500";

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? "").join("");
  return letters || "T";
}

function CheckIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M20 6 9 17l-5-5" />
    </svg>
  );
}

function Card({ children }: { children: React.ReactNode }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">{children}</div>;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-medium text-slate-500">{label}</span>
      {children}
    </label>
  );
}
