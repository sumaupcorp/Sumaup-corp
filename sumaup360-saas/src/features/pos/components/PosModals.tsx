"use client";

import { useEffect, useRef, useState } from "react";
import {
  useCloseCashAction,
  useAddCashMovement,
  fetchTicketHtml,
  fetchTicketPdf,
  PAYMENT_LABELS,
  EXPENSE_CATEGORIES,
  INCOME_CATEGORIES,
  type CashSession,
  type CashSummary,
  type Sale,
} from "@/features/sales/api";
import type { Product } from "@/features/products/api";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { Spinner, Badge } from "@/components/ui/misc";
import { InfoTip } from "@/components/ui/tooltip";
import { fmt, hourOf, dateOf, SummaryRow } from "./shared";

/** Registro de un ingreso o salida de efectivo con categoria peruana y motivo. */
export function MovementModal({ sessionId, onClose }: { sessionId: string; onClose: () => void }) {
  const addMovement = useAddCashMovement();
  const [type, setType] = useState<"EXPENSE" | "INCOME">("EXPENSE");
  const [category, setCategory] = useState("");
  const [concept, setConcept] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);

  const categories = type === "EXPENSE" ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
  const selected = categories.find((c) => c.code === category);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await addMovement.mutateAsync({
        sessionId, type, category, concept, amount: Number(amount),
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-16">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="flex items-center gap-1.5 text-sm font-semibold">
            Movimiento de caja
            <InfoTip align="left" text="Registra todo efectivo que entra o sale de la caja fuera de las ventas. Asi el arqueo del cierre cuadra y se sabe en que se uso el dinero." />
          </h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        <form onSubmit={submit} className="space-y-3">
          <div className="grid grid-cols-2 gap-1.5">
            <button type="button"
              onClick={() => { setType("EXPENSE"); setCategory(""); }}
              className={"min-h-11 rounded-lg border px-3 py-2 text-sm font-medium transition touch-manipulation " +
                (type === "EXPENSE" ? "border-destructive bg-red-50 text-destructive" : "border-border text-muted-foreground")}>
              Salida de dinero
            </button>
            <button type="button"
              onClick={() => { setType("INCOME"); setCategory(""); }}
              className={"min-h-11 rounded-lg border px-3 py-2 text-sm font-medium transition touch-manipulation " +
                (type === "INCOME" ? "border-green-600 bg-green-50 text-green-700" : "border-border text-muted-foreground")}>
              Ingreso de dinero
            </button>
          </div>

          <div className="space-y-1">
            <Label>Motivo</Label>
            <Select value={category} onChange={(e) => setCategory(e.target.value)} required>
              <option value="">Elige el motivo…</option>
              {categories.map((c) => <option key={c.code} value={c.code}>{c.label}</option>)}
            </Select>
            {selected && (
              <p className="text-xs text-muted-foreground">{selected.description}</p>
            )}
          </div>

          <div className="space-y-1">
            <Label>Descripcion</Label>
            <Input value={concept} onChange={(e) => setConcept(e.target.value)} required
              maxLength={200} placeholder={type === "EXPENSE" ? "Ej. compra de bolsas en el mercado" : "Ej. sencillo del banco"} />
          </div>

          <div className="space-y-1">
            <Label>Monto</Label>
            <Input type="number" step="0.01" min="0.01" value={amount}
              onChange={(e) => setAmount(e.target.value)} required placeholder="0.00" />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" disabled={addMovement.isPending}>
              {addMovement.isPending ? "Guardando..." : "Registrar"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

/** Cierre de caja con arqueo: esperado vs contado, diferencia en vivo y resultado. */
export function CloseCashModal({ session, summary, branchId, onClose }: {
  session: CashSession;
  summary: CashSummary;
  branchId: string;
  onClose: () => void;
}) {
  const closeCash = useCloseCashAction(branchId);
  const [counted, setCounted] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<CashSession | null>(null);

  const countedNum = Number(counted);
  const diff = counted === "" ? null : countedNum - summary.expectedCash;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const r = await closeCash.mutateAsync({
        id: session.id,
        countedAmount: countedNum,
        notes: notes || undefined,
      });
      setResult(r);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const diffLabel = (d: number) =>
    d === 0 ? "Caja cuadrada" : d > 0 ? `Sobrante de ${fmt(d)}` : `Faltante de ${fmt(Math.abs(d))}`;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-12">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="flex items-center gap-1.5 text-sm font-semibold">
            Cierre de caja (arqueo)
            <InfoTip align="left" text="Cuenta todo el efectivo fisico de la caja (billetes y monedas) y escribelo abajo. El sistema lo compara con lo que deberia haber y registra la diferencia." />
          </h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        {result ? (
          <div className="space-y-3">
            <div className="space-y-1.5 rounded-lg bg-muted/50 p-3">
              <SummaryRow label="Efectivo esperado" value={fmt(result.expectedAmount ?? 0)} />
              <SummaryRow label="Efectivo contado" value={fmt(result.closingAmount ?? 0)} />
              <div className="border-t border-border pt-1.5">
                <SummaryRow strong negative={(result.difference ?? 0) < 0}
                  label="Resultado" value={diffLabel(result.difference ?? 0)} />
              </div>
            </div>
            {(result.difference ?? 0) < 0 && (
              <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-destructive">
                Falta dinero en la caja. Revisa los movimientos del dia y confirma con el responsable del turno. El faltante quedo registrado con fecha y usuario.
              </p>
            )}
            <Button className="w-full" onClick={onClose}>Entendido</Button>
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1.5 rounded-lg bg-muted/50 p-3">
              <SummaryRow label="Fondo inicial" value={fmt(summary.openingAmount)} />
              <SummaryRow label="Ventas en efectivo" value={"+ " + fmt(summary.cashSales)} />
              <SummaryRow label="Otros ingresos" value={"+ " + fmt(summary.incomesTotal)} />
              <SummaryRow label="Salidas de caja" value={"− " + fmt(summary.expensesTotal)} />
              <div className="border-t border-border pt-1.5">
                <SummaryRow strong label="Efectivo esperado" value={fmt(summary.expectedCash)}
                  tip="Lo que deberia haber en la caja segun el sistema. No incluye tarjeta, Yape, Plin ni transferencias." />
              </div>
              {Object.entries(summary.salesByMethod).filter(([m]) => m !== "CASH").length > 0 && (
                <p className="pt-1 text-xs text-muted-foreground">
                  Ademas del efectivo: {Object.entries(summary.salesByMethod)
                    .filter(([m]) => m !== "CASH")
                    .map(([m, v]) => `${PAYMENT_LABELS.get(m) ?? m} ${fmt(v)}`)
                    .join(" · ")} (cuadrar con vouchers).
                </p>
              )}
            </div>

            <div className="space-y-1">
              <Label className="flex items-center gap-1.5">
                Efectivo contado
                <InfoTip text="Suma de todos los billetes y monedas que hay fisicamente en la caja en este momento." />
              </Label>
              <Input type="number" step="0.01" min="0" value={counted}
                onChange={(e) => setCounted(e.target.value)} required placeholder="0.00" autoFocus />
            </div>

            {diff !== null && (
              <p className={"rounded-lg px-3 py-2 text-sm font-medium " +
                (diff === 0 ? "bg-green-50 text-green-700" : diff > 0 ? "bg-amber-50 text-amber-700" : "bg-red-50 text-destructive")}>
                {diffLabel(diff)}
              </p>
            )}

            <div className="space-y-1">
              <Label>Notas (opcional)</Label>
              <Input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={300}
                placeholder="Ej. faltante por vuelto mal dado en la manana" />
            </div>

            {error && <p className="text-sm text-destructive">{error}</p>}

            <div className="flex justify-end gap-2">
              <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
              <Button type="submit" disabled={closeCash.isPending}>
                {closeCash.isPending ? "Cerrando..." : "Cerrar caja"}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

/** Ticket de la venta: vista previa con la plantilla configurada, imprimir, PDF y WhatsApp. */
export function TicketModal({ sale, companyName, productMap, onClose }: {
  sale: Sale;
  companyName: string;
  productMap: Map<string, Product>;
  onClose: () => void;
}) {
  const [html, setHtml] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [phone, setPhone] = useState("");
  const [downloading, setDownloading] = useState(false);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    let active = true;
    fetchTicketHtml(sale.id)
      .then((h) => { if (active) setHtml(h); })
      .catch((e) => { if (active) setError((e as Error).message); });
    return () => { active = false; };
  }, [sale.id]);

  const print = () => {
    iframeRef.current?.contentWindow?.focus();
    iframeRef.current?.contentWindow?.print();
  };

  const downloadPdf = async () => {
    setDownloading(true);
    setError(null);
    try {
      const blob = await fetchTicketPdf(sale.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `ticket-${sale.id.slice(0, 8)}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setDownloading(false);
    }
  };

  const whatsappText = () => {
    const lines = sale.items.map((i) => {
      const name = productMap.get(i.productId)?.name ?? "Producto";
      return `${Number(i.quantity)} x ${name}  ${fmt(i.lineTotal)}`;
    });
    return [
      `*${companyName || "Su compra"}*`,
      `${dateOf(sale.createdAt)} ${hourOf(sale.createdAt)}`,
      "------------------------",
      ...lines,
      "------------------------",
      `*TOTAL: ${fmt(sale.total)}*`,
      `Pago: ${PAYMENT_LABELS.get(sale.paymentMethod) ?? sale.paymentMethod}`,
      "Gracias por su compra.",
    ].join("\n");
  };

  const sendWhatsapp = () => {
    const digits = phone.replace(/\D/g, "");
    const target = digits ? `51${digits.replace(/^51/, "")}` : "";
    const url = target
      ? `https://wa.me/${target}?text=${encodeURIComponent(whatsappText())}`
      : `https://wa.me/?text=${encodeURIComponent(whatsappText())}`;
    window.open(url, "_blank", "noopener");
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-8">
      <div className="w-full max-w-lg rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-semibold">Ticket de venta</h3>
            <Badge variant="success">Venta registrada</Badge>
          </div>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        {/* Vista previa con la plantilla configurada en Documentos */}
        <div className="mb-3 h-80 overflow-hidden rounded-lg border border-border bg-muted/30">
          {error ? (
            <p className="p-4 text-sm text-destructive">{error}</p>
          ) : html === null ? (
            <div className="flex h-full items-center justify-center"><Spinner /></div>
          ) : (
            <iframe ref={iframeRef} srcDoc={html} title="Ticket"
              className="h-full w-full bg-white" />
          )}
        </div>

        <div className="grid grid-cols-2 gap-2">
          <Button className="h-11" onClick={print} disabled={html === null}>Imprimir</Button>
          <Button className="h-11" variant="outline" onClick={downloadPdf} disabled={downloading}>
            {downloading ? "Generando..." : "Descargar PDF"}
          </Button>
        </div>

        <div className="mt-3 space-y-1.5 border-t border-border pt-3">
          <Label className="flex items-center gap-1.5">
            Enviar por WhatsApp
            <InfoTip align="left" text="Abre WhatsApp con el resumen de la compra listo para enviar. Si escribes el celular del cliente, va directo a su chat; si lo dejas vacio, eliges el contacto en WhatsApp." />
          </Label>
          <div className="flex gap-2">
            <Input type="tel" className="h-11" placeholder="Celular del cliente (opcional)"
              value={phone} onChange={(e) => setPhone(e.target.value)} />
            <Button variant="outline" className="h-11 shrink-0" onClick={sendWhatsapp}>
              Enviar
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
