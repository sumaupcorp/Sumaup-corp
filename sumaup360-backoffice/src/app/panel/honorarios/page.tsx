"use client";

import { useState } from "react";
import {
  useHonorarios, useSetHonorarioStatus, useHonorarioAttach, useHonorarioAttachments, HONORARIO_STATES,
  type HonorarioRow,
} from "@/features/honorarios/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";

export default function HonorariosPage() {
  const [estado, setEstado] = useState("");
  const { data: rows = [], isLoading } = useHonorarios(estado);
  const [selected, setSelected] = useState<HonorarioRow | null>(null);

  return (
    <div>
      <PageHeader title="Honorarios" subtitle="Solicitudes de recibo por honorarios de profesionales" />

      <div className="mb-4">
        <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-9 w-64">
          <option value="">Todos los estados</option>
          {HONORARIO_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </Select>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5"><Spinner /></div>
          ) : rows.length === 0 ? (
            <div className="p-5 text-sm text-muted-foreground">No hay solicitudes.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-xs text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">Cliente</th>
                    <th className="px-4 py-3">Servicio</th>
                    <th className="px-4 py-3">Monto</th>
                    <th className="px-4 py-3">Estado</th>
                    <th className="px-4 py-3 text-right">Accion</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((h) => (
                    <tr key={h.id} className="border-b border-border/60">
                      <td className="px-4 py-3">
                        <div className="font-medium">{h.clienteNombre}</div>
                        <div className="text-xs text-muted-foreground">
                          {h.clienteDocType || ""} {h.clienteDocNumber || ""}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="max-w-xs truncate">{h.descripcion}</div>
                        {h.conRetencion && <div className="text-xs text-muted-foreground">Con retencion</div>}
                      </td>
                      <td className="px-4 py-3">S/ {h.monto.toFixed(2)}</td>
                      <td className="px-4 py-3"><EstadoBadge estado={h.estado} /></td>
                      <td className="px-4 py-3 text-right">
                        <Button size="sm" variant="outline" onClick={() => setSelected(h)}>Gestionar</Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {selected && <ManageModal row={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}

function ManageModal({ row, onClose }: { row: HonorarioRow; onClose: () => void }) {
  const setStatus = useSetHonorarioStatus();
  const attach = useHonorarioAttach();
  const { data: attachments = [] } = useHonorarioAttachments(row.id);
  const [estado, setEstado] = useState(row.estado || "EN_PROCESO");
  const [obs, setObs] = useState("");
  const [fileUrl, setFileUrl] = useState("");
  const [fileName, setFileName] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="w-full max-w-lg rounded-2xl bg-white p-5 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h2 className="font-heading text-base font-semibold">Recibo · {row.clienteNombre}</h2>
            <p className="text-sm text-muted-foreground">{row.descripcion} · S/ {row.monto.toFixed(2)}</p>
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">✕</button>
        </div>

        <div className="mb-4 space-y-2">
          <Label>Cambiar estado</Label>
          <div className="flex flex-wrap items-end gap-2">
            <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-10 w-48">
              {HONORARIO_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
            </Select>
            <Input value={obs} onChange={(e) => setObs(e.target.value)} placeholder="Observacion (opcional)" className="w-48" />
            <Button disabled={setStatus.isPending}
              onClick={() => setStatus.mutate({ id: row.id, estado, observacion: obs || undefined }, { onSuccess: onClose })}>
              Actualizar
            </Button>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Adjuntar recibo generado (PDF)</Label>
          <div className="flex flex-wrap items-end gap-2">
            <Input value={fileUrl} onChange={(e) => setFileUrl(e.target.value)} placeholder="URL del archivo" className="w-64" />
            <Input value={fileName} onChange={(e) => setFileName(e.target.value)} placeholder="Nombre" className="w-32" />
            <Button variant="outline" disabled={attach.isPending || !fileUrl.trim()}
              onClick={() => attach.mutate({ id: row.id, fileUrl: fileUrl.trim(), fileName: fileName.trim() || undefined },
                { onSuccess: () => { setFileUrl(""); setFileName(""); } })}>Adjuntar</Button>
          </div>
          {attachments.length > 0 && (
            <ul className="mt-2 space-y-1 text-sm">
              {attachments.map((a) => (
                <li key={a.id}><a href={a.fileUrl} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline">{a.fileName || a.fileUrl}</a></li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function EstadoBadge({ estado }: { estado: string }) {
  const v: "default" | "muted" | "success" | "warning" =
    estado === "GENERADO" ? "success"
      : estado === "OBSERVADO" || estado === "CANCELADO" ? "warning"
      : estado === "PENDIENTE" ? "muted" : "default";
  return <Badge variant={v}>{label(estado)}</Badge>;
}

function label(s: string): string {
  switch (s) {
    case "PENDIENTE": return "Pendiente";
    case "EN_PROCESO": return "En proceso";
    case "GENERADO": return "Generado";
    case "OBSERVADO": return "Observado";
    case "CANCELADO": return "Cancelado";
    default: return s;
  }
}
