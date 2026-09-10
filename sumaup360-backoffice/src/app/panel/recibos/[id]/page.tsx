"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Eye, EyeOff, KeyRound, FileText, CheckCircle2, Circle } from "lucide-react";
import {
  useReceiptDetail, useReceiptsByUser, useReceiptAction, useDeclare,
  useUpsertUserFiscal, useRevealUserSol, useReceiptFile,
} from "@/features/receipts/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { cn } from "@/lib/utils";

const statusVariant = (s: string) =>
  s === "PROCESSED" ? "success" : s === "OBSERVED" ? "warning" : s === "IN_PROCESS" ? "default" : "muted";

export default function ReceiptDetailPage() {
  const { id } = useParams<{ id: string }>();
  const hasPerm = useHasPermission();
  const detail = useReceiptDetail(id);
  const r = detail.data;
  const userReceipts = useReceiptsByUser(r?.userId);
  const action = useReceiptAction();
  const declare = useDeclare();
  const saveFiscal = useUpsertUserFiscal();
  const revealSol = useRevealUserSol();
  const loadFile = useReceiptFile();
  const [fileUrl, setFileUrl] = useState<string | null>(null);
  const [fileErr, setFileErr] = useState<string | null>(null);

  const [sirePeriod, setSirePeriod] = useState("");
  const [sunatPeriod, setSunatPeriod] = useState("");
  const [revealed, setRevealed] = useState<string | null>(null);
  const [editFiscal, setEditFiscal] = useState(false);
  const [ruc, setRuc] = useState("");
  const [regime, setRegime] = useState("");
  const [solUser, setSolUser] = useState("");
  const [solPass, setSolPass] = useState("");

  if (detail.isLoading || !r) {
    return <div className="flex h-64 items-center justify-center"><Spinner className="size-7" /></div>;
  }

  const reveal = async () => {
    if (revealed !== null) { setRevealed(null); return; }
    const res = await revealSol.mutateAsync(r.userId);
    setRevealed(res.solPass ?? "");
  };
  const saveF = async () => {
    await saveFiscal.mutateAsync({ userId: r.userId, ruc: ruc || undefined, regime: regime || undefined, solUser: solUser || undefined, solPass: solPass || undefined });
    setEditFiscal(false); setSolPass(""); setRevealed(null);
  };
  const viewFile = async () => {
    setFileErr(null);
    try { const res = await loadFile.mutateAsync(r.id); setFileUrl(res.url); }
    catch (e) { setFileErr("No se pudo cargar el archivo: " + (e as Error).message); }
  };

  return (
    <div>
      <Link href="/panel/recibos" className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:underline">
        <ArrowLeft className="size-4" /> Recibos
      </Link>
      <PageHeader title={`${r.type} ${r.docNumber ?? ""}`} subtitle={`Recibo de ${r.userName ?? "usuario"}`} />

      <div className="grid gap-6 lg:grid-cols-3">
        {/* DETALLE DEL RECIBO */}
        <Card className="lg:col-span-2">
          <CardContent className="p-5">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-semibold">Detalle del comprobante</h2>
              <Badge variant={statusVariant(r.status)}>{r.status}</Badge>
            </div>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <Field label="Tipo" value={r.type} />
              <Field label="Numero" value={r.docNumber ?? "—"} />
              <Field label="Fecha emision" value={r.issueDate ?? "—"} />
              <Field label="Monto" value={r.amount != null ? `${r.currency} ${r.amount}` : "—"} />
              <Field label="RUC emisor" value={r.issuerRuc ?? "—"} />
              <Field label="Procesado" value={r.processedAt ? new Date(r.processedAt).toLocaleString("es-PE") : "—"} />
            </dl>
            {r.notes && <p className="mt-3 rounded-lg bg-brand-soft p-3 text-sm">Nota: {r.notes}</p>}

            <div className="mt-3">
              {r.hasFile ? (
                <>
                  {!fileUrl ? (
                    <Button size="sm" variant="outline" onClick={viewFile} disabled={loadFile.isPending}>
                      <FileText className="size-4" /> {loadFile.isPending ? "Cargando..." : "Ver comprobante"}
                    </Button>
                  ) : (
                    <div className="space-y-2">
                      <img src={fileUrl} alt="Comprobante" className="max-h-96 rounded-lg border border-border" />
                      <a href={fileUrl} target="_blank" rel="noreferrer" className="block text-sm text-brand-blue hover:underline">Abrir en pestana nueva</a>
                    </div>
                  )}
                  {fileErr && <p className="text-sm text-destructive">{fileErr}</p>}
                </>
              ) : r.fileUrl ? (
                <a href={r.fileUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-sm text-brand-blue hover:underline">
                  <FileText className="size-4" /> Ver archivo adjunto
                </a>
              ) : (
                <p className="text-sm text-muted-foreground">Sin archivo adjunto.</p>
              )}
            </div>

            {hasPerm("receipt:process") && (
              <div className="mt-4 flex flex-wrap gap-2 border-t border-border pt-4">
                {r.status === "PENDING" && <Button size="sm" variant="outline" onClick={() => action.mutate({ id: r.id, action: "take" })}>Tomar</Button>}
                {(r.status === "PENDING" || r.status === "IN_PROCESS") && (
                  <>
                    <Button size="sm" onClick={() => action.mutate({ id: r.id, action: "process", note: "Cargado en SUMAUP360" })}>Procesar</Button>
                    <Button size="sm" variant="outline" onClick={() => action.mutate({ id: r.id, action: "observe", note: "Falta informacion" })}>Observar</Button>
                  </>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* DATOS FISCALES DEL USUARIO */}
        <Card>
          <CardContent className="p-5">
            <div className="mb-3 flex items-center gap-2">
              <KeyRound className="size-4 text-brand-blue" />
              <h2 className="text-sm font-semibold">Datos fiscales del usuario</h2>
            </div>
            <div className="space-y-1 text-sm">
              <p><span className="text-muted-foreground">Usuario:</span> {r.userName ?? "—"}</p>
              <p><span className="text-muted-foreground">RUC:</span> {r.userRuc ?? "—"}</p>
              <p><span className="text-muted-foreground">Regimen:</span> {r.userRegime ?? "—"}</p>
              <p className="flex items-center gap-2">
                <span className="text-muted-foreground">Clave SOL:</span>
                {r.userHasSol
                  ? (revealed !== null ? <span className="font-mono">{revealed}</span> : <Badge variant="muted">•••••• (cifrada)</Badge>)
                  : <span className="text-muted-foreground">no registrada</span>}
                {r.userHasSol && hasPerm("sol:reveal") && (
                  <Button size="sm" variant="ghost" onClick={reveal} disabled={revealSol.isPending}>
                    {revealed !== null ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </Button>
                )}
              </p>
            </div>

            {hasPerm("sol:manage") && (
              <>
                {!editFiscal ? (
                  <Button size="sm" variant="outline" className="mt-3" onClick={() => { setEditFiscal(true); setRuc(r.userRuc ?? ""); setRegime(r.userRegime ?? ""); }}>
                    Editar datos fiscales
                  </Button>
                ) : (
                  <div className="mt-3 space-y-2 border-t border-border pt-3">
                    <div className="space-y-1"><Label>RUC</Label><Input value={ruc} onChange={(e) => setRuc(e.target.value)} maxLength={11} /></div>
                    <div className="space-y-1"><Label>Regimen</Label><Input value={regime} onChange={(e) => setRegime(e.target.value)} placeholder="NRUS / RER / RG…" /></div>
                    <div className="space-y-1"><Label>Usuario SOL</Label><Input value={solUser} onChange={(e) => setSolUser(e.target.value)} /></div>
                    <div className="space-y-1"><Label>Clave SOL</Label><Input type="password" value={solPass} onChange={(e) => setSolPass(e.target.value)} placeholder="(vacio = no cambiar)" /></div>
                    <div className="flex gap-2">
                      <Button size="sm" onClick={saveF} disabled={saveFiscal.isPending}>Guardar</Button>
                      <Button size="sm" variant="ghost" onClick={() => setEditFiscal(false)}>Cancelar</Button>
                    </div>
                  </div>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* DECLARACION SIRE / SUNAT */}
      <Card className="mt-6">
        <CardContent className="p-5">
          <h2 className="mb-4 text-sm font-semibold">Declaracion tributaria</h2>
          <div className="grid gap-6 sm:grid-cols-2">
            <DeclareBlock
              title="SIRE" done={r.declaredSire} period={r.sirePeriod} at={r.sireDeclaredAt}
              canDeclare={hasPerm("receipt:declare")} value={sirePeriod} onValue={setSirePeriod}
              onDeclare={() => declare.mutate({ id: r.id, which: "sire", period: sirePeriod })}
              pending={declare.isPending}
            />
            <DeclareBlock
              title="SUNAT" done={r.declaredSunat} period={r.sunatPeriod} at={r.sunatDeclaredAt}
              canDeclare={hasPerm("receipt:declare")} value={sunatPeriod} onValue={setSunatPeriod}
              onDeclare={() => declare.mutate({ id: r.id, which: "sunat", period: sunatPeriod })}
              pending={declare.isPending}
            />
          </div>
        </CardContent>
      </Card>

      {/* HISTORIAL ACUMULADO DEL USUARIO */}
      <Card className="mt-6">
        <CardContent className="p-5">
          <h2 className="mb-3 text-sm font-semibold">Otros recibos de este usuario</h2>
          {userReceipts.isLoading ? <Spinner /> : (
            <ul className="divide-y divide-border text-sm">
              {(userReceipts.data ?? []).map((u) => (
                <li key={u.id} className="flex items-center justify-between py-2">
                  <div>
                    <p className="font-medium text-foreground">{u.type} {u.docNumber ?? ""}</p>
                    <p className="text-xs text-muted-foreground">{u.issueDate ?? "—"} · {u.amount != null ? `${u.currency} ${u.amount}` : "—"}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={cn("inline-flex items-center gap-1 text-xs", u.declaredSire ? "text-emerald-600" : "text-muted-foreground")}>
                      {u.declaredSire ? <CheckCircle2 className="size-3.5" /> : <Circle className="size-3.5" />} SIRE
                    </span>
                    <span className={cn("inline-flex items-center gap-1 text-xs", u.declaredSunat ? "text-emerald-600" : "text-muted-foreground")}>
                      {u.declaredSunat ? <CheckCircle2 className="size-3.5" /> : <Circle className="size-3.5" />} SUNAT
                    </span>
                    <Badge variant={statusVariant(u.status)}>{u.status}</Badge>
                    {u.id !== r.id && <Link href={`/panel/recibos/${u.id}`} className="text-brand-blue hover:underline">Ver</Link>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="font-medium text-foreground">{value}</dd>
    </div>
  );
}

function DeclareBlock({ title, done, period, at, canDeclare, value, onValue, onDeclare, pending }: {
  title: string; done: boolean; period: string | null; at: string | null;
  canDeclare: boolean; value: string; onValue: (v: string) => void; onDeclare: () => void; pending: boolean;
}) {
  return (
    <div className="rounded-lg border border-border p-4">
      <div className="mb-2 flex items-center justify-between">
        <span className="font-medium">{title}</span>
        <Badge variant={done ? "success" : "muted"}>{done ? "Declarado" : "Pendiente"}</Badge>
      </div>
      {done ? (
        <p className="text-sm text-muted-foreground">
          Periodo {period} · {at ? new Date(at).toLocaleDateString("es-PE") : ""}
        </p>
      ) : canDeclare ? (
        <div className="flex items-end gap-2">
          <div className="flex-1 space-y-1">
            <Label>Periodo (AAAA-MM)</Label>
            <Input value={value} onChange={(e) => onValue(e.target.value)} placeholder="2026-06" />
          </div>
          <Button size="sm" disabled={!value || pending} onClick={onDeclare}>Declarar</Button>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Sin declarar.</p>
      )}
    </div>
  );
}
