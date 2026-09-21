"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useHasPermission } from "@/features/auth/session";
import {
  useAccountingConfig,
  useSaveAccountingConfig,
  useJournalEntries,
  downloadConcarExport,
  type AccountingConfig,
} from "@/features/accounting/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

const MONTHS = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
];

function monthRange(year: number, month: number): { from: string; to: string } {
  const first = `${year}-${String(month).padStart(2, "0")}-01`;
  const lastDay = new Date(year, month, 0).getDate();
  const to = `${year}-${String(month).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}`;
  return { from: first, to };
}

export default function ContabilidadPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const companyId = currentCompany?.id;

  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);

  if (!currentCompany) {
    return (
      <div>
        <PageHeader title="Contabilidad" />
        <p className="text-sm text-muted-foreground">Selecciona una empresa para continuar.</p>
      </div>
    );
  }

  const { from, to } = monthRange(year, month);

  return (
    <div>
      <PageHeader
        title="Contabilidad"
        subtitle={`${currentCompany.legalName} · Asientos contables y exportacion a CONCAR`}
      />

      <div className="mb-6 grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-1">
          <ConcarExport
            companyId={companyId!}
            year={year}
            month={month}
            onYear={setYear}
            onMonth={setMonth}
            canExport={hasPerm("accounting:export")}
          />
        </div>
        {hasPerm("accounting:manage") && (
          <div className="lg:col-span-2">
            <ConfigCard companyId={companyId!} />
          </div>
        )}
      </div>

      <EntriesCard companyId={companyId!} from={from} to={to} periodLabel={`${MONTHS[month - 1]} ${year}`} />
    </div>
  );
}

function ConcarExport({
  companyId, year, month, onYear, onMonth, canExport,
}: {
  companyId: string; year: number; month: number;
  onYear: (y: number) => void; onMonth: (m: number) => void; canExport: boolean;
}) {
  const [format, setFormat] = useState<"txt" | "csv">("txt");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const download = async () => {
    setMsg(null);
    setBusy(true);
    try {
      await downloadConcarExport(companyId, year, month, format);
    } catch (e) {
      setMsg((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 text-sm font-semibold text-foreground">Exportar a CONCAR</h2>
        <p className="mb-4 text-xs text-muted-foreground">
          Descarga los asientos del periodo en el formato de importacion de CONCAR.
        </p>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>Mes</Label>
              <Select value={month} onChange={(e) => onMonth(Number(e.target.value))}>
                {MONTHS.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Ano</Label>
              <Input type="number" value={year} onChange={(e) => onYear(Number(e.target.value))} />
            </div>
          </div>
          <div className="space-y-1">
            <Label>Formato</Label>
            <Select value={format} onChange={(e) => setFormat(e.target.value as "txt" | "csv")}>
              <option value="txt">TXT (separador configurable)</option>
              <option value="csv">CSV (separado por comas)</option>
            </Select>
          </div>
          {msg && <p className="text-sm text-destructive">{msg}</p>}
          <Button onClick={download} disabled={busy || !canExport} className="w-full">
            {busy ? "Generando..." : "Descargar archivo"}
          </Button>
          {!canExport && (
            <p className="text-xs text-muted-foreground">No tienes permiso para exportar.</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function ConfigCard({ companyId }: { companyId: string }) {
  const config = useAccountingConfig(companyId);
  if (config.isLoading) {
    return (
      <Card><CardContent className="flex justify-center p-10"><Spinner /></CardContent></Card>
    );
  }
  return <ConfigForm companyId={companyId} initial={config.data} />;
}

function ConfigForm({ companyId, initial }: { companyId: string; initial?: AccountingConfig }) {
  const save = useSaveAccountingConfig(companyId);
  const [form, setForm] = useState<AccountingConfig>(
    initial ?? {
      companyId,
      subdiarioVentas: "14", subdiarioCompras: "02", subdiarioDiario: "01",
      cuentaPorCobrar: "1212", cuentaVentas: "7011", cuentaVentasServ: "7041",
      cuentaIgv: "40111", cuentaCaja: "101", moneda: "MN", concarSeparator: "|",
    },
  );
  const [msg, setMsg] = useState<string | null>(null);
  const set = (k: keyof AccountingConfig) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(null);
    try {
      await save.mutateAsync(form);
      setMsg("Configuracion guardada.");
    } catch (err) {
      setMsg("Error: " + (err as Error).message);
    }
  };

  const field = (label: string, k: keyof AccountingConfig) => (
    <div className="space-y-1">
      <Label>{label}</Label>
      <Input value={form[k]} onChange={set(k)} />
    </div>
  );

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 text-sm font-semibold text-foreground">Configuracion contable</h2>
        <p className="mb-4 text-xs text-muted-foreground">
          Cuentas por defecto (PCGE) y subdiarios para los asientos, y el separador del archivo CONCAR.
        </p>
        <form onSubmit={submit} className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {field("Subdiario ventas", "subdiarioVentas")}
          {field("Subdiario compras", "subdiarioCompras")}
          {field("Subdiario diario", "subdiarioDiario")}
          {field("Cuenta por cobrar", "cuentaPorCobrar")}
          {field("Cuenta ventas", "cuentaVentas")}
          {field("Cuenta ventas serv.", "cuentaVentasServ")}
          {field("Cuenta IGV", "cuentaIgv")}
          {field("Cuenta caja", "cuentaCaja")}
          {field("Moneda", "moneda")}
          {field("Separador CONCAR", "concarSeparator")}
          <div className="col-span-2 flex items-center gap-3 sm:col-span-3">
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? "Guardando..." : "Guardar configuracion"}
            </Button>
            {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

function EntriesCard({
  companyId, from, to, periodLabel,
}: { companyId: string; from: string; to: string; periodLabel: string }) {
  const entries = useJournalEntries(companyId, from, to);
  const items = entries.data ?? [];

  return (
    <Card>
      <CardContent className="p-5">
        <h2 className="mb-1 text-sm font-semibold text-foreground">Asientos · {periodLabel}</h2>
        <p className="mb-4 text-xs text-muted-foreground">
          Asientos del periodo seleccionado arriba. Los de ventas se generan desde Facturacion.
        </p>
        {entries.isLoading ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : entries.isError ? (
          <p className="py-8 text-center text-sm text-destructive">No pudimos cargar los asientos.</p>
        ) : items.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">Sin asientos en este periodo.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs text-muted-foreground">
                  <th className="py-2 pr-3">Fecha</th>
                  <th className="py-2 pr-3">Comprobante</th>
                  <th className="py-2 pr-3">Glosa</th>
                  <th className="py-2 pr-3">Origen</th>
                  <th className="py-2 pr-3 text-right">Debe</th>
                  <th className="py-2 text-right">Haber</th>
                </tr>
              </thead>
              <tbody>
                {items.map((e) => (
                  <tr key={e.id} className="border-b border-border/60">
                    <td className="py-2 pr-3 whitespace-nowrap">{e.entryDate}</td>
                    <td className="py-2 pr-3 whitespace-nowrap">{e.subdiario}-{e.correlativo ?? ""}</td>
                    <td className="py-2 pr-3">{e.glosa}</td>
                    <td className="py-2 pr-3">
                      <Badge variant={e.source === "SALE" ? "success" : "muted"}>
                        {e.source === "SALE" ? "Venta" : "Manual"}
                      </Badge>
                    </td>
                    <td className="py-2 pr-3 text-right">{e.totalDebe.toFixed(2)}</td>
                    <td className="py-2 text-right">{e.totalHaber.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
