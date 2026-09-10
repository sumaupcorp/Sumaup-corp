"use client";

import { useEffect, useState } from "react";
import { documentTemplateService } from "../services/document-template.service";
import {
  DOCUMENT_TYPE_LABELS,
  type DocumentSeries,
  type DocumentType,
} from "../types/document-template.types";

const DOC_TYPES = Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[];

/** Tipos tributarios: solo con el modulo de facturacion electronica activo. */
const FISCAL_TYPES: DocumentType[] = [
  "SALE_RECEIPT", "INVOICE", "CREDIT_NOTE", "DEBIT_NOTE", "DELIVERY_GUIDE",
];

/** Crear series por tipo de documento y ver el correlativo actual. */
export function DocumentSeriesForm({ companyId, fiscalEnabled = true }: {
  companyId: string;
  fiscalEnabled?: boolean;
}) {
  const [series, setSeries] = useState<DocumentSeries[]>([]);
  const [documentType, setDocumentType] = useState<DocumentType>(
    fiscalEnabled ? "INVOICE" : "POS_TICKET");
  const [code, setCode] = useState(fiscalEnabled ? "F001" : "T001");
  const [error, setError] = useState<string | null>(null);
  const visibleTypes = DOC_TYPES.filter((t) => fiscalEnabled || !FISCAL_TYPES.includes(t));

  const load = () =>
    documentTemplateService
      .listSeries(companyId)
      .then(setSeries)
      .catch((e) => setError(String(e.message ?? e)));

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [companyId]);

  const create = async () => {
    setError(null);
    try {
      await documentTemplateService.createSeries({ companyId, documentType, series: code });
      load();
    } catch (e) {
      setError(String((e as Error).message));
    }
  };

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-slate-800">Series de documentos</h2>
      <div className="flex items-end gap-3">
        <label className="text-sm">
          <span className="text-slate-600">Tipo</span>
          <select
            className="mt-1 rounded-lg border border-slate-200 px-3 py-2"
            value={documentType}
            onChange={(e) => setDocumentType(e.target.value as DocumentType)}
          >
            {visibleTypes.map((t) => (
              <option key={t} value={t}>
                {DOCUMENT_TYPE_LABELS[t]}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          <span className="text-slate-600">Serie</span>
          <input
            className="mt-1 rounded-lg border border-slate-200 px-3 py-2"
            value={code}
            onChange={(e) => setCode(e.target.value)}
          />
        </label>
        <button
          type="button"
          onClick={create}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white"
        >
          Crear serie
        </button>
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
        {series.map((s) => (
          <li key={s.id} className="flex justify-between px-4 py-2 text-sm">
            <span>
              {DOCUMENT_TYPE_LABELS[s.documentType]} - <strong>{s.series}</strong>
            </span>
            <span className="text-slate-500">
              correlativo: {s.currentNumber} {s.active ? "" : "(inactiva)"}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
