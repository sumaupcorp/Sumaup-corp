"use client";

import { Button } from "@/components/ui/button";

/**
 * Dialogo de confirmacion estandar: toda accion que cambia estado con un solo click
 * (confirmar, cancelar, eliminar) debe pasar por aqui para evitar toques por error,
 * sobre todo en pantallas tactiles.
 */
export function ConfirmDialog({ title, message, confirmLabel, destructive, loading, onConfirm, onCancel }: {
  title: string;
  message: string;
  confirmLabel: string;
  /** true para acciones que anulan o eliminan (boton rojo). */
  destructive?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="fixed inset-0 z-[60] flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-28"
      onClick={onCancel}>
      <div className="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}>
        <h3 className="text-sm font-semibold">{title}</h3>
        <p className="mt-1.5 text-sm text-muted-foreground">{message}</p>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="min-h-11" disabled={loading} onClick={onCancel}>
            Volver
          </Button>
          <Button variant={destructive ? "destructive" : "default"} className="min-h-11"
            disabled={loading} onClick={onConfirm}>
            {loading ? "Aplicando..." : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
