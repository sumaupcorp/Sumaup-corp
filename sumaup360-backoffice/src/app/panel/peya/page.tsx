"use client";

import { useState } from "react";
import {
  usePeyaUploads, useSetPeyaStatus, useSetNps, usePeyaAttach, usePeyaAttachments, PEYA_STATES,
  type PeyaUpload,
} from "@/features/peya/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";

export default function PeyaPage() {
  const [estado, setEstado] = useState("");
  const { data: uploads = [], isLoading } = usePeyaUploads(estado);
  const [selected, setSelected] = useState<PeyaUpload | null>(null);

  return (
    <div>
      <PageHeader title="Peya" subtitle="Cargas mensuales de ventas de repartidores" />

      <div className="mb-4">
        <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-9 w-64">
          <option value="">Todos los estados</option>
          {PEYA_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </Select>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5"><Spinner /></div>
          ) : uploads.length === 0 ? (
            <div className="p-5 text-sm text-muted-foreground">No hay cargas.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-xs text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">RUC</th>
                    <th className="px-4 py-3">Periodo</th>
                    <th className="px-4 py-3">PDF</th>
                    <th className="px-4 py-3">NPS</th>
                    <th className="px-4 py-3">Estado</th>
                    <th className="px-4 py-3 text-right">Accion</th>
                  </tr>
                </thead>
                <tbody>
                  {uploads.map((u) => (
                    <tr key={u.id} className="border-b border-border/60">
                      <td className="px-4 py-3">{u.ruc || "—"}</td>
                      <td className="px-4 py-3">{u.periodo}</td>
                      <td className="px-4 py-3">
                        <a href={u.pdfUrl} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline">Ver PDF</a>
                      </td>
                      <td className="px-4 py-3">{u.codigoNps || "—"}</td>
                      <td className="px-4 py-3"><EstadoBadge estado={u.estado} /></td>
                      <td className="px-4 py-3 text-right">
                        <Button size="sm" variant="outline" onClick={() => setSelected(u)}>Gestionar</Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {selected && <ManageModal upload={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}

function ManageModal({ upload, onClose }: { upload: PeyaUpload; onClose: () => void }) {
  const setStatus = useSetPeyaStatus();
  const setNps = useSetNps();
  const attach = usePeyaAttach();
  const { data: attachments = [] } = usePeyaAttachments(upload.id);
  const [estado, setEstado] = useState(upload.estado || "EN_PROCESO");
  const [obs, setObs] = useState("");
  const [nps, setNpsCode] = useState(upload.codigoNps ?? "");
  const [fileUrl, setFileUrl] = useState("");
  const [fileName, setFileName] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="w-full max-w-lg rounded-2xl bg-white p-5 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h2 className="font-heading text-base font-semibold">Carga Peya · {upload.periodo}</h2>
            <p className="text-sm text-muted-foreground">RUC {upload.ruc || "—"}</p>
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">✕</button>
        </div>

        <a href={upload.pdfUrl} target="_blank" rel="noreferrer"
          className="mb-4 inline-block text-sm text-blue-600 hover:underline">Descargar PDF de ventas</a>

        <div className="mb-4 space-y-2">
          <Label>Cambiar estado</Label>
          <div className="flex flex-wrap items-end gap-2">
            <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-10 w-48">
              {PEYA_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
            </Select>
            <Input value={obs} onChange={(e) => setObs(e.target.value)} placeholder="Observacion (opcional)" className="w-48" />
            <Button disabled={setStatus.isPending}
              onClick={() => setStatus.mutate({ id: upload.id, estado, observacion: obs || undefined }, { onSuccess: onClose })}>
              Actualizar
            </Button>
          </div>
        </div>

        <div className="mb-4 space-y-2">
          <Label>Codigo NPS</Label>
          <div className="flex items-end gap-2">
            <Input value={nps} onChange={(e) => setNpsCode(e.target.value)} placeholder="Codigo NPS" className="w-48" />
            <Button variant="outline" disabled={setNps.isPending || !nps.trim()}
              onClick={() => setNps.mutate({ id: upload.id, codigoNps: nps.trim() })}>Guardar NPS</Button>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Adjuntar reporte / declaracion</Label>
          <div className="flex flex-wrap items-end gap-2">
            <Input value={fileUrl} onChange={(e) => setFileUrl(e.target.value)} placeholder="URL del archivo" className="w-64" />
            <Input value={fileName} onChange={(e) => setFileName(e.target.value)} placeholder="Nombre" className="w-32" />
            <Button variant="outline" disabled={attach.isPending || !fileUrl.trim()}
              onClick={() => attach.mutate({ id: upload.id, fileUrl: fileUrl.trim(), fileName: fileName.trim() || undefined },
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
    estado === "COMPLETADO" ? "success"
      : estado === "OBSERVADO" || estado === "CANCELADO" ? "warning"
      : estado === "PDF_SUBIDO" ? "muted" : "default";
  return <Badge variant={v}>{label(estado)}</Badge>;
}

function label(s: string): string {
  switch (s) {
    case "PDF_SUBIDO": return "PDF subido";
    case "PENDIENTE_BACKOFFICE": return "Pendiente";
    case "EN_PROCESO": return "En proceso";
    case "OBSERVADO": return "Observado";
    case "COMPLETADO": return "Completado";
    case "CANCELADO": return "Cancelado";
    default: return s;
  }
}
