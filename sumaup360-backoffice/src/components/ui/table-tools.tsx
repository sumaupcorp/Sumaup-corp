"use client";

import { useMemo, useState } from "react";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Búsqueda + paginación del lado cliente para tablas con datos acumulables.
 * `match(row, query)` define cómo filtra cada tabla; los filtros por dropdown se aplican
 * antes (pasando `rows` ya filtrados).
 */
export function useTableData<T>(rows: T[], match: (row: T, q: string) => boolean, pageSize = 10) {
  const [query, setQueryRaw] = useState("");
  const [page, setPage] = useState(1);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q ? rows.filter((r) => match(r, q)) : rows;
  }, [rows, query, match]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(page, pageCount);
  const view = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);

  const setQuery = (v: string) => { setQueryRaw(v); setPage(1); };

  return { query, setQuery, page: safePage, setPage, view, pageCount, total: filtered.length };
}

export function SearchBox({ value, onChange, placeholder = "Buscar…" }: {
  value: string; onChange: (v: string) => void; placeholder?: string;
}) {
  return (
    <div className="relative w-full max-w-xs">
      <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="h-9 w-full rounded-lg border border-border bg-white pl-9 pr-3 text-sm outline-none focus:border-brand-blue"
      />
    </div>
  );
}

export function Pagination({ page, pageCount, total, onPage }: {
  page: number; pageCount: number; total: number; onPage: (p: number) => void;
}) {
  if (total === 0) return null;
  return (
    <div className="flex items-center justify-between border-t border-border px-5 py-3 text-sm">
      <span className="text-muted-foreground">{total} resultado{total === 1 ? "" : "s"}</span>
      <div className="flex items-center gap-1">
        <button onClick={() => onPage(page - 1)} disabled={page <= 1}
          className={cn("flex size-8 items-center justify-center rounded-lg border border-border", page <= 1 ? "text-muted-foreground/40" : "hover:bg-accent")}>
          <ChevronLeft className="size-4" />
        </button>
        <span className="px-2 text-muted-foreground">{page} / {pageCount}</span>
        <button onClick={() => onPage(page + 1)} disabled={page >= pageCount}
          className={cn("flex size-8 items-center justify-center rounded-lg border border-border", page >= pageCount ? "text-muted-foreground/40" : "hover:bg-accent")}>
          <ChevronRight className="size-4" />
        </button>
      </div>
    </div>
  );
}
