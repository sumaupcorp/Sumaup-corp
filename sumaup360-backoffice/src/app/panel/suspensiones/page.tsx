"use client";

import { useState } from "react";
import {
  useSuspensiones, useSetSuspensionStatus, useSuspensionAttach, useSuspensionAttachments, SUSPENSION_STATES,
  type SuspensionRow,
} from "@/features/honorarios/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";

export default function SuspensionesPage() {
  const [estado, setEstado] = useState("");
  const { data: rows = [], isLoading } = useSuspensiones(estado);
  const [selected, setSelected] = useState<SuspensionRow | null>(null);

  return (
    <div>
      <PageHeader title="Suspension de 4ta" subtitle="Solicitudes de suspension de 4ta categoria (anual)" />

      <div className="mb-4">
        <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-9 w-64">
          <option value="">Todos los estados</option>
          {SUSPENSION_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
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
                    <th className="px-4 py-3">Año</th>
                    <th className="px-4 py-3">Estado</th>
                    <th className="px-4 py-3">Constancia</th>
                    <th className="px-4 py-3 text-right">Accion</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((s) => (
                    <tr key={s.id} className="border-b border-border/60">
                      <td className="px-4 py-3">{s.anio}</td>
                      <td className="px-4 py-3"><EstadoBadge estado={s.estado} /></td>
                      <td className="px-4 py-3">
                        {s.constanciaUrl
                          ? <a href={s.constanciaUrl} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline">Ver</a>
                          : "—"}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button size="sm" variant="outline" onClick={() => setSelected(s)}>Gestionar</Button>
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

function ManageModal({ row, onClose }: { row: SuspensionRow; onClose: () => void }) {
  const setStatus = useSetSuspensionStatus();
  const attach = useSuspensionAttach();
  const { data: attachments = [] } = useSuspensionAttachments(row.id);
  const [estado, setEstado] = useState(row.estado || "EN_PROCESO");
  const [obs, setObs] = useState("");
  const [fileUrl, setFileUrl] = useState("");
  const [fileName, setFileName] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="w-full max-w-lg rounded-2xl bg-white p-5 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h2 className="font-heading text-base font-semibold">Suspension de 4ta · {row.anio}</h2>
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">✕</button>
        </div>

        <div className="mb-4 space-y-2">
          <Label>Cambiar estado</Label>
          <div className="flex flex-wrap items-end gap-2">
            <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-10 w-48">
              {SUSPENSION_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
            </Select>
            <Input value={obs} onChange={(e) => setObs(e.target.value)} placeholder="Observacion (opcional)" className="w-48" />
            <Button disabled={setStatus.isPending}
              onClick={() => setStatus.mutate({ id: row.id, estado, observacion: obs || undefined }, { onSuccess: onClose })}>
              Actualizar
            </Button>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Adjuntar constancia de suspension</Label>
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
    estado === "TRAMITADA" ? "success"
      : estado === "OBSERVADA" || estado === "CANCELADA" ? "warning"
      : estado === "SOLICITADA" ? "muted" : "default";
  return <Badge variant={v}>{label(estado)}</Badge>;
}

function label(s: string): string {
  switch (s) {
    case "SOLICITADA": return "Solicitada";
    case "EN_PROCESO": return "En proceso";
    case "TRAMITADA": return "Tramitada";
    case "OBSERVADA": return "Observada";
    case "CANCELADA": return "Cancelada";
    default: return s;
  }
}
