"use client";

import { useEffect, useRef, useState } from "react";
import { ref, uploadBytes, getDownloadURL, deleteObject } from "firebase/storage";
import { storage } from "@/lib/firebase";
import {
  useRequests, useSetStatus, useAttach, useDetach, useAttachments, REQUEST_STATES,
  type ReceiptRequest, type Attachment,
} from "@/features/requests/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";

// Tamano maximo del comprobante. Se mantiene liviano a proposito: el staff debe subir un PDF
// comprimido. El limite se valida en el navegador antes de tocar Firebase.
const MAX_BYTES = 2 * 1024 * 1024; // 2 MB

/** Sube el PDF del comprobante a Firebase Storage con el nombre indicado y devuelve su URL. */
async function uploadComprobante(
  requestId: string, file: File, desiredName: string,
): Promise<{ url: string; name: string }> {
  const safe = desiredName.replace(/[^\w.\-]+/g, "_");
  const path = `comprobantes/${requestId}/${Date.now()}_${safe}`;
  const r = ref(storage, path);
  await uploadBytes(r, file, { contentType: file.type || "application/pdf" });
  return { url: await getDownloadURL(r), name: desiredName };
}

/** Borra el objeto de Storage a partir de su download URL. Best-effort (ignora si no aplica). */
async function deleteFromStorage(url: string) {
  if (!url.includes("firebasestorage")) return; // URLs externas pegadas a mano: nada que borrar
  try {
    await deleteObject(ref(storage, url));
  } catch {
    /* el objeto ya no existe o no es de este bucket: seguimos */
  }
}

/** Nombre legible del archivo: Boleta-correlativo-cliente-anio.pdf */
function buildFileName(request: ReceiptRequest, correlativo: string): string {
  const tipo = request.tipo === "FACTURA" ? "Factura" : "Boleta";
  const corr = (correlativo.trim() || request.id.slice(0, 6)).replace(/[^\w-]+/g, "");
  const cliente =
    (request.customerName || "cliente").trim().replace(/\s+/g, "-").replace(/[^\w-]+/g, "").slice(0, 30) ||
    "cliente";
  const anio = new Date().getFullYear();
  return `${tipo}-${corr}-${cliente}-${anio}.pdf`;
}

function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

/** Enlace wa.me al WhatsApp del cliente (Peru +51) con el mensaje + link del comprobante. */
function waLink(whatsapp: string, message: string): string {
  let digits = (whatsapp || "").replace(/\D/g, "");
  if (digits.length === 9) digits = "51" + digits; // celular peruano sin prefijo
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`;
}

export default function SolicitudesPage() {
  const [estado, setEstado] = useState("");
  const { data: requests = [], isLoading } = useRequests(estado);
  const [selected, setSelected] = useState<ReceiptRequest | null>(null);

  return (
    <div>
      <PageHeader title="Solicitudes" subtitle="Comprobantes solicitados por clientes de taxistas" />

      <div className="mb-4">
        <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-9 w-64">
          <option value="">Todos los estados</option>
          {REQUEST_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </Select>
      </div>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5"><Spinner /></div>
          ) : requests.length === 0 ? (
            <div className="p-5 text-sm text-muted-foreground">No hay solicitudes.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border text-left text-xs text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">Cliente</th>
                    <th className="px-4 py-3">Tipo</th>
                    <th className="px-4 py-3">Monto</th>
                    <th className="px-4 py-3">Contacto</th>
                    <th className="px-4 py-3">Estado</th>
                    <th className="px-4 py-3 text-right">Accion</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((r) => (
                    <tr key={r.id} className="border-b border-border/60">
                      <td className="px-4 py-3">
                        <div className="font-medium">{r.customerName || "—"}</div>
                        <div className="text-xs text-muted-foreground">{r.docType} {r.docNumber}</div>
                      </td>
                      <td className="px-4 py-3">{r.tipo === "FACTURA" ? "Factura" : "Boleta"}</td>
                      <td className="px-4 py-3">S/ {(r.montoEditado ?? r.monto ?? 0).toFixed(2)}</td>
                      <td className="px-4 py-3">
                        <div>{r.whatsapp || "—"}</div>
                        <div className="text-xs text-muted-foreground">{r.email}</div>
                      </td>
                      <td className="px-4 py-3"><EstadoBadge estado={r.estado} /></td>
                      <td className="px-4 py-3 text-right">
                        <Button size="sm" variant="outline" onClick={() => setSelected(r)}>Gestionar</Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {selected && <ManageModal request={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}

function ManageModal({ request, onClose }: { request: ReceiptRequest; onClose: () => void }) {
  const setStatus = useSetStatus();
  const attach = useAttach();
  const detach = useDetach();
  const { data: attachments = [] } = useAttachments(request.id);

  const [estado, setEstado] = useState(request.estado ?? "EN_PROCESO_BACKOFFICE");
  const [motivo, setMotivo] = useState("");

  const [correlativo, setCorrelativo] = useState("");
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>("");
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [replaceTargetId, setReplaceTargetId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const tipoLabel = request.tipo === "FACTURA" ? "factura" : "boleta";
  const targetName = buildFileName(request, correlativo);

  // Libera el objectURL de la previsualizacion cuando cambia o al cerrar.
  useEffect(() => {
    return () => { if (previewUrl) URL.revokeObjectURL(previewUrl); };
  }, [previewUrl]);

  function openPicker(replaceId: string | null) {
    setReplaceTargetId(replaceId);
    setUploadError(null);
    fileInputRef.current?.click();
  }

  function onPickFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // permite volver a elegir el mismo archivo
    if (!file) return;
    setUploadError(null);
    if (file.type && file.type !== "application/pdf") {
      setUploadError("Solo se permite PDF.");
      return;
    }
    if (file.size > MAX_BYTES) {
      setUploadError(`El archivo pesa ${humanSize(file.size)}. El maximo es 2 MB, comprime el PDF.`);
      return;
    }
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  }

  function clearPending() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingFile(null);
    setPreviewUrl("");
    setReplaceTargetId(null);
    setUploadError(null);
  }

  async function confirmUpload() {
    if (!pendingFile) return;
    setUploading(true);
    setUploadError(null);
    try {
      const { url, name } = await uploadComprobante(request.id, pendingFile, targetName);
      await attach.mutateAsync({ id: request.id, fileUrl: url, fileName: name });
      // Si era un reemplazo, recien ahora borramos el anterior (ya subio el nuevo).
      if (replaceTargetId) {
        const old = attachments.find((a) => a.id === replaceTargetId);
        if (old) {
          await deleteFromStorage(old.fileUrl);
          await detach.mutateAsync({ id: request.id, attachmentId: replaceTargetId });
        }
      }
      clearPending();
    } catch (err) {
      const code = (err as { code?: string })?.code;
      const detail = code ? ` (${code})` : "";
      setUploadError(`No se pudo subir el archivo${detail}. Revisa el Storage de Firebase.`);
      console.error("uploadComprobante fallo:", err);
    } finally {
      setUploading(false);
    }
  }

  async function removeAttachment(a: Attachment) {
    setDeletingId(a.id);
    try {
      await deleteFromStorage(a.fileUrl);
      await detach.mutateAsync({ id: request.id, attachmentId: a.id });
    } catch (err) {
      console.error("No se pudo eliminar el adjunto:", err);
    } finally {
      setDeletingId(null);
    }
  }

  function notifyWhatsApp(pdfUrl: string) {
    const msg = `Hola ${request.customerName || ""}, aqui esta tu ${tipoLabel} solicitada en SUMAUP360: ${pdfUrl}`;
    window.open(waLink(request.whatsapp || "", msg), "_blank", "noopener");
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-5 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h2 className="font-heading text-base font-semibold">Solicitud de comprobante</h2>
            <p className="text-sm text-muted-foreground">
              {request.customerName} · {request.docType} {request.docNumber} · S/ {(request.montoEditado ?? request.monto ?? 0).toFixed(2)}
            </p>
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground" aria-label="Cerrar">✕</button>
        </div>

        {request.observacion && (
          <p className="mb-4 rounded-lg bg-muted p-3 text-sm">Observacion del cliente: {request.observacion}</p>
        )}

        <div className="mb-5 space-y-2">
          <Label>Cambiar estado</Label>
          <div className="flex flex-wrap items-end gap-2">
            <Select value={estado} onChange={(e) => setEstado(e.target.value)} className="h-10 w-56">
              {REQUEST_STATES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
            </Select>
            <Input value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Motivo (opcional)" className="w-48" />
            <Button disabled={setStatus.isPending}
              onClick={() => setStatus.mutate({ id: request.id, estado, motivo: motivo || undefined }, { onSuccess: onClose })}>
              {setStatus.isPending ? "Guardando…" : "Actualizar"}
            </Button>
          </div>
        </div>

        {/* --- Comprobante --- */}
        <div className="space-y-3 rounded-xl border border-border p-4">
          <div>
            <Label>Comprobante (PDF)</Label>
            <p className="text-xs text-muted-foreground">
              Sube el PDF de la {tipoLabel} (maximo 2 MB). Se previsualiza antes de enviarlo a Firebase; luego lo compartes por WhatsApp.
            </p>
          </div>

          {/* Correlativo -> define el nombre del archivo */}
          <div className="grid gap-1">
            <Label className="text-xs">Correlativo / N° de comprobante</Label>
            <Input
              value={correlativo}
              onChange={(e) => setCorrelativo(e.target.value)}
              placeholder="Ej. B001-1234"
              className="h-9"
              disabled={uploading}
            />
            <p className="text-[11px] text-muted-foreground">
              Se guardara como <span className="font-medium text-foreground">{targetName}</span>
            </p>
          </div>

          <input ref={fileInputRef} type="file" accept="application/pdf" className="hidden" onChange={onPickFile} />

          {/* Zona de seleccion / previsualizacion */}
          {!pendingFile ? (
            <button
              type="button"
              onClick={() => openPicker(null)}
              disabled={uploading}
              className="flex w-full flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-border py-6 text-sm text-muted-foreground hover:border-blue-500 hover:text-foreground disabled:opacity-50"
            >
              <span className="text-2xl leading-none">+</span>
              Seleccionar PDF para previsualizar
            </button>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2 rounded-lg bg-muted px-3 py-2 text-xs">
                <span className="truncate">
                  {pendingFile.name} · <span className="text-muted-foreground">{humanSize(pendingFile.size)}</span>
                </span>
                <Badge variant="success">Listo para subir</Badge>
              </div>
              <iframe src={previewUrl} title="Previsualizacion del comprobante" className="h-64 w-full rounded-lg border border-border" />
              <div className="flex flex-wrap gap-2">
                <Button size="sm" disabled={uploading} onClick={confirmUpload}>
                  {uploading ? "Subiendo…" : replaceTargetId ? "Reemplazar comprobante" : "Confirmar y subir"}
                </Button>
                <Button size="sm" variant="outline" disabled={uploading} onClick={clearPending}>
                  Quitar
                </Button>
              </div>
            </div>
          )}

          {uploadError && <p className="text-sm text-red-600">{uploadError}</p>}

          {/* Adjuntos ya subidos */}
          {attachments.length > 0 && (
            <div className="space-y-2 border-t border-border pt-3">
              <p className="text-xs font-medium text-muted-foreground">Comprobantes subidos</p>
              {attachments.map((a) => {
                const busy = deletingId === a.id;
                return (
                  <div key={a.id} className="flex items-center justify-between gap-2 rounded-lg border border-border px-3 py-2">
                    <a href={a.fileUrl} target="_blank" rel="noreferrer" className="truncate text-sm text-blue-600 hover:underline">
                      {a.fileName || a.fileUrl}
                    </a>
                    <div className="flex shrink-0 items-center gap-1.5">
                      <button
                        type="button"
                        className="inline-flex items-center rounded-lg bg-green-600 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-green-700 disabled:opacity-50"
                        disabled={!request.whatsapp || busy}
                        onClick={() => notifyWhatsApp(a.fileUrl)}
                        title={request.whatsapp ? "Enviar por WhatsApp" : "El cliente no dejo WhatsApp"}
                      >
                        WhatsApp
                      </button>
                      <button
                        type="button"
                        className="inline-flex items-center rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium hover:bg-muted disabled:opacity-50"
                        disabled={uploading || busy}
                        onClick={() => openPicker(a.id)}
                        title="Reemplazar por otro PDF"
                      >
                        Reemplazar
                      </button>
                      <button
                        type="button"
                        className="inline-flex items-center rounded-lg border border-red-200 px-2.5 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
                        disabled={busy}
                        onClick={() => removeAttachment(a)}
                        title="Eliminar"
                      >
                        {busy ? "Eliminando…" : "Eliminar"}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function EstadoBadge({ estado }: { estado: string | null }) {
  const v: "default" | "muted" | "success" | "warning" =
    estado === "COMPLETADO" ? "success"
      : estado === "RECHAZADO_TAXISTA" || estado === "CANCELADO" || estado === "OBSERVADO" ? "warning"
      : estado === "PENDIENTE_TAXISTA" ? "muted" : "default";
  return <Badge variant={v}>{label(estado)}</Badge>;
}

function label(s: string | null): string {
  switch (s) {
    case "PENDIENTE_TAXISTA": return "Pendiente taxista";
    case "CONFIRMADO_TAXISTA": return "Confirmado";
    case "MONTO_EDITADO_CONFIRMADO": return "Monto editado";
    case "RECHAZADO_TAXISTA": return "Rechazado";
    case "PENDIENTE_BACKOFFICE": return "Pendiente backoffice";
    case "EN_PROCESO_BACKOFFICE": return "En proceso";
    case "COMPLETADO": return "Completado";
    case "OBSERVADO": return "Observado";
    case "CANCELADO": return "Cancelado";
    default: return s ?? "—";
  }
}
