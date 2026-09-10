// Cliente del modulo de plantillas de documentos. Usa el idToken de Firebase (igual que el
// resto del SaaS). El PDF lo genera el backend; aqui solo se configura/previsualiza.
import { auth } from "@/lib/firebase";
import type {
  CreateSeriesRequest,
  DocumentPreviewRequest,
  DocumentSeries,
  DocumentTemplate,
  DocumentTemplateRequest,
  DocumentType,
} from "../types/document-template.types";

const ERP = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080") + "/api/v1/erp";

async function headers(json = true): Promise<Record<string, string>> {
  const h: Record<string, string> = {};
  if (json) h["Content-Type"] = "application/json";
  const u = auth.currentUser;
  if (u) h.Authorization = `Bearer ${await u.getIdToken()}`;
  return h;
}

async function asJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const b = await res.json().catch(() => ({}));
    throw new Error((b as { message?: string }).message ?? `Error ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export const documentTemplateService = {
  async list(companyId: string, documentType?: DocumentType): Promise<DocumentTemplate[]> {
    const qs = new URLSearchParams({ companyId });
    if (documentType) qs.set("documentType", documentType);
    return asJson(await fetch(`${ERP}/document-templates?${qs}`, { headers: await headers(false) }));
  },
  async get(id: string, companyId: string): Promise<DocumentTemplate> {
    return asJson(await fetch(`${ERP}/document-templates/${id}?companyId=${companyId}`, { headers: await headers(false) }));
  },
  async create(req: DocumentTemplateRequest): Promise<DocumentTemplate> {
    return asJson(await fetch(`${ERP}/document-templates`, { method: "POST", headers: await headers(), body: JSON.stringify(req) }));
  },
  async update(id: string, req: DocumentTemplateRequest): Promise<DocumentTemplate> {
    return asJson(await fetch(`${ERP}/document-templates/${id}`, { method: "PUT", headers: await headers(), body: JSON.stringify(req) }));
  },
  async setDefault(id: string, companyId: string): Promise<DocumentTemplate> {
    return asJson(await fetch(`${ERP}/document-templates/${id}/set-default?companyId=${companyId}`, { method: "POST", headers: await headers(false) }));
  },
  async previewHtml(req: DocumentPreviewRequest): Promise<string> {
    const res = await fetch(`${ERP}/document-templates/preview`, { method: "POST", headers: await headers(), body: JSON.stringify(req) });
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.text();
  },
  async renderPdf(body: unknown): Promise<Blob> {
    const res = await fetch(`${ERP}/documents/render-pdf`, { method: "POST", headers: await headers(), body: JSON.stringify(body) });
    if (!res.ok) throw new Error(`Error ${res.status}`);
    return res.blob();
  },
  async listSeries(companyId: string): Promise<DocumentSeries[]> {
    return asJson(await fetch(`${ERP}/document-series?companyId=${companyId}`, { headers: await headers(false) }));
  },
  async createSeries(req: CreateSeriesRequest): Promise<DocumentSeries> {
    return asJson(await fetch(`${ERP}/document-series`, { method: "POST", headers: await headers(), body: JSON.stringify(req) }));
  },
};
