"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useHasPermission } from "@/features/auth/session";
import { useSales, PAYMENT_LABELS, type Sale } from "@/features/sales/api";
import {
  useEinvoicingConfig,
  useSaveEinvoicingConfig,
  useEmitSale,
  useIssuedDocuments,
  type EmitResult,
  type IssuedDocument,
} from "@/features/einvoicing/api";
import { useGenerateEntryFromSale } from "@/features/accounting/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

export default function FacturacionPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const companyId = currentCompany?.id;

  if (!currentCompany) {
    return (
      <div>
        <PageHeader title="Facturacion electronica" />
        <p className="text-sm text-muted-foreground">Selecciona una empresa para continuar.</p>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Facturacion electronica"
        subtitle={`${currentCompany.legalName} · Emite boletas y facturas ante SUNAT (NubeFact)`}
      />
      <div className="mb-6">
        <IssuedDocsHistory companyId={companyId!} />
      </div>
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <SalesToEmit companyId={companyId!} />
        </div>
        {hasPerm("einvoice:config") && (
          <div>
            <NubefactConfig companyId={companyId!} />
          </div>
        )}
      </div>
    </div>
  );
}

const DOC_TYPE_LABEL: Record<string, string> = {
  INVOICE: "Factura",
  SALE_RECEIPT: "Boleta",
  CREDIT_NOTE: "Nota de credito",
  DEBIT_NOTE: "Nota de debito",
};

function statusBadge(d: IssuedDocument): { label: string; variant: "success" | "warning" | "muted" } {
  const s = (d.sunatStatus ?? "").toUpperCase();
  if (s === "ACEPTADO") return { label: "Aceptado", variant: "success" };
  if (s === "ENVIADO") return { label: "Enviado", variant: "warning" };
  return { label: d.sunatStatus ?? d.documentStatus ?? "—", variant: "muted" };
}

function IssuedDocsHistory({ companyId }: { companyId: string }) {
  const docs = useIssuedDocuments(companyId);
  const items = docs.data ?? [];

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 text-sm font-semibold text-foreground">Historial de comprobantes</h2>
        <p className="mb-4 text-xs text-muted-foreground">
          Boletas y facturas electronicas emitidas ante SUNAT.
        </p>
        {docs.isLoading ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : docs.isError ? (
          <p className="py-8 text-center text-sm text-destructive">No pudimos cargar el historial.</p>
        ) : items.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Aun no has emitido comprobantes. Emite desde una venta abajo o en el POS.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs text-muted-foreground">
                  <th className="py-2 pr-3">Comprobante</th>
                  <th className="py-2 pr-3">Tipo</th>
                  <th className="py-2 pr-3">Cliente</th>
                  <th className="py-2 pr-3 text-right">Total</th>
                  <th className="py-2 pr-3">Estado</th>
                  <th className="py-2">PDF</th>
                </tr>
              </thead>
              <tbody>
                {items.map((d) => {
                  const b = statusBadge(d);
                  return (
                    <tr key={d.id} className="border-b border-border/60">
                      <td className="py-2 pr-3 whitespace-nowrap font-medium">{d.fullNumber ?? "—"}</td>
                      <td className="py-2 pr-3 whitespace-nowrap">{DOC_TYPE_LABEL[d.documentType] ?? d.documentType}</td>
                      <td className="py-2 pr-3">{d.customerName ?? "Cliente varios"}</td>
                      <td className="py-2 pr-3 text-right whitespace-nowrap">S/ {d.total.toFixed(2)}</td>
                      <td className="py-2 pr-3"><Badge variant={b.variant}>{b.label}</Badge></td>
                      <td className="py-2">
                        {d.pdfUrl ? (
                          <a href={d.pdfUrl} target="_blank" rel="noreferrer" className="text-primary underline">Ver</a>
                        ) : <span className="text-muted-foreground">—</span>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function NubefactConfig({ companyId }: { companyId: string }) {
  const config = useEinvoicingConfig(companyId);
  const save = useSaveEinvoicingConfig(companyId);
  const [ruta, setRuta] = useState("");
  const [token, setToken] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const cfg = config.data;
  const rutaValue = touched ? ruta : (cfg?.ruta ?? "");
  const enabledValue = touched ? enabled : (cfg?.enabled ?? true);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await save.mutateAsync({
        ruta: rutaValue || undefined,
        token: token || undefined,
        enabled: enabledValue,
      });
      setToken("");
      setTouched(false);
      setMsg("Configuracion guardada.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 text-sm font-semibold text-foreground">Credenciales NubeFact</h2>
        <p className="mb-4 text-xs text-muted-foreground">
          Ruta y token de tu cuenta NubeFact para esta empresa. El token se guarda cifrado y no se
          vuelve a mostrar.
        </p>
        {config.isLoading ? (
          <div className="flex justify-center py-8"><Spinner /></div>
        ) : (
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1">
              <Label>Ruta (URL)</Label>
              <Input
                value={rutaValue}
                onChange={(e) => { setTouched(true); setRuta(e.target.value); }}
                placeholder="https://api.nubefact.com/api/v1/..."
              />
            </div>
            <div className="space-y-1">
              <Label>Token</Label>
              <Input
                type="password"
                value={token}
                onChange={(e) => { setTouched(true); setToken(e.target.value); }}
                placeholder={cfg?.hasToken ? "•••••••• (guardado)" : "Pega tu token"}
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-foreground">
              <input
                type="checkbox"
                checked={enabledValue}
                onChange={(e) => { setTouched(true); setEnabled(e.target.checked); }}
              />
              Emitir con estas credenciales
            </label>
            {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? "Guardando..." : "Guardar credenciales"}
            </Button>
            <p className="text-xs text-muted-foreground">
              Si no configuras credenciales propias, se usa el entorno demo global (si esta activo).
            </p>
          </form>
        )}
      </CardContent>
    </Card>
  );
}

function SalesToEmit({ companyId }: { companyId: string }) {
  const sales = useSales({ size: 20 });
  const items = sales.data?.items ?? [];

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 text-sm font-semibold text-foreground">Ventas recientes</h2>
        <p className="mb-4 text-xs text-muted-foreground">
          Emite el comprobante electronico de una venta o genera su asiento contable.
        </p>
        {sales.isLoading ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : sales.isError ? (
          <p className="py-8 text-center text-sm text-destructive">No pudimos cargar las ventas.</p>
        ) : items.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">Aun no hay ventas registradas.</p>
        ) : (
          <ul className="divide-y divide-border">
            {items.map((s) => <SaleRow key={s.id} sale={s} companyId={companyId} />)}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

function SaleRow({ sale, companyId }: { sale: Sale; companyId: string }) {
  const hasPerm = useHasPermission();
  const emit = useEmitSale();
  const genEntry = useGenerateEntryFromSale();
  const [result, setResult] = useState<EmitResult | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [entryMsg, setEntryMsg] = useState<string | null>(null);

  const doEmit = async () => {
    setErr(null);
    try {
      setResult(await emit.mutateAsync(sale.id));
    } catch (e) {
      setErr((e as Error).message);
    }
  };

  const doEntry = async () => {
    setEntryMsg(null);
    try {
      const entry = await genEntry.mutateAsync(sale.id);
      setEntryMsg(`Asiento ${entry.subdiario}-${entry.correlativo ?? ""} generado.`);
    } catch (e) {
      setEntryMsg("Error: " + (e as Error).message);
    }
  };

  const date = new Date(sale.createdAt).toLocaleDateString("es-PE", {
    day: "2-digit", month: "2-digit", year: "numeric",
  });

  return (
    <li className="py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium text-foreground">
            S/ {sale.total.toFixed(2)}{" "}
            <span className="font-normal text-muted-foreground">
              · {PAYMENT_LABELS.get(sale.paymentMethod) ?? sale.paymentMethod} · {date}
            </span>
          </p>
          {result && (
            <p className="mt-1 text-xs text-foreground">
              <Badge variant={result.accepted ? "success" : "warning"}>
                {result.fullNumber ?? "Emitido"} · {result.sunatStatus ?? ""}
              </Badge>{" "}
              {result.pdfUrl && (
                <a href={result.pdfUrl} target="_blank" rel="noreferrer" className="text-primary underline">
                  Ver PDF
                </a>
              )}
            </p>
          )}
          {err && <p className="mt-1 text-xs text-destructive">{err}</p>}
          {entryMsg && <p className="mt-1 text-xs text-muted-foreground">{entryMsg}</p>}
        </div>
        <div className="flex shrink-0 gap-2">
          {hasPerm("accounting:manage") && (
            <Button size="sm" variant="outline" onClick={doEntry} disabled={genEntry.isPending}>
              {genEntry.isPending ? "..." : "Asiento"}
            </Button>
          )}
          {hasPerm("einvoice:emit") && (
            <Button size="sm" onClick={doEmit} disabled={emit.isPending || !!result}>
              {emit.isPending ? "Emitiendo..." : result ? "Emitido" : "Emitir comprobante"}
            </Button>
          )}
        </div>
      </div>
    </li>
  );
}
