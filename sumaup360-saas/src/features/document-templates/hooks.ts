"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { documentTemplateService } from "./services/document-template.service";
import type {
  DocumentPreviewRequest,
  DocumentTemplate,
  DocumentTemplateRequest,
} from "./types/document-template.types";

/**
 * Hooks React Query del modulo de plantillas: la lista, el guardado y el preview
 * comparten cache e invalidaciones, asi la UI se actualiza sola tras cada cambio.
 */

export function useDocumentTemplates(companyId?: string) {
  return useQuery({
    queryKey: ["doc-templates", companyId],
    queryFn: () => documentTemplateService.list(companyId!),
    enabled: !!companyId,
  });
}

/** Crea o actualiza segun venga id. Invalida lista y previews al terminar. */
export function useSaveTemplate(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { id?: string } & DocumentTemplateRequest): Promise<DocumentTemplate> =>
      input.id
        ? documentTemplateService.update(input.id, { ...input, id: undefined } as DocumentTemplateRequest)
        : documentTemplateService.create(input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["doc-templates", companyId] });
      qc.invalidateQueries({ queryKey: ["doc-preview"] });
    },
  });
}

export function useSetDefaultTemplate(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => documentTemplateService.setDefault(id, companyId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["doc-templates", companyId] }),
  });
}

/**
 * Preview HTML con datos de ejemplo. Mantiene el HTML anterior mientras llega el nuevo
 * (sin parpadeo) y expone isFetching para el indicador "actualizando".
 */
export function useTemplatePreview(request: DocumentPreviewRequest, enabled = true) {
  return useQuery({
    queryKey: ["doc-preview", JSON.stringify(request)],
    queryFn: () => documentTemplateService.previewHtml(request),
    enabled,
    placeholderData: (prev) => prev,
    staleTime: 30_000,
  });
}

/** Valor con retraso: evita pedir un preview por cada tecla. */
export function useDebounced<T>(value: T, delayMs = 450): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(t);
  }, [value, delayMs]);
  return debounced;
}
