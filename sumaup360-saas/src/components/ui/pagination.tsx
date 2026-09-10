"use client";

import { cn } from "@/lib/utils";

/**
 * Paginador estandar de tablas y listas. Botones amplios (tactil) y contador
 * de registros. Se oculta solo cuando hay una unica pagina.
 */
export function Paginator({ page, totalPages, totalItems, onPage, className }: {
  /** Pagina actual (base 0, igual que la API). */
  page: number;
  totalPages: number;
  totalItems?: number;
  onPage: (page: number) => void;
  className?: string;
}) {
  if (totalPages <= 1) return null;

  return (
    <div className={cn("flex items-center justify-between gap-2 pt-3", className)}>
      <button
        type="button"
        disabled={page <= 0}
        onClick={() => onPage(page - 1)}
        className="min-h-10 select-none rounded-lg border border-border px-3.5 text-sm font-medium text-muted-foreground transition touch-manipulation hover:border-primary hover:text-primary active:scale-95 disabled:pointer-events-none disabled:opacity-40"
      >
        Anterior
      </button>
      <p className="text-xs text-muted-foreground">
        Pagina {page + 1} de {totalPages}
        {totalItems != null && <span> · {totalItems} registro{totalItems === 1 ? "" : "s"}</span>}
      </p>
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPage(page + 1)}
        className="min-h-10 select-none rounded-lg border border-border px-3.5 text-sm font-medium text-muted-foreground transition touch-manipulation hover:border-primary hover:text-primary active:scale-95 disabled:pointer-events-none disabled:opacity-40"
      >
        Siguiente
      </button>
    </div>
  );
}
