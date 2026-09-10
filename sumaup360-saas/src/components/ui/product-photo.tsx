"use client";

import { useState } from "react";

/**
 * Foto de producto con cadena de respaldo: URL externa → foto propia → "Sin foto".
 * Si el hotlink externo se cae, pasa automaticamente a la foto subida sin romper la vista.
 */
export function ProductPhoto({ externalUrl, uploadedUrl, name, className = "size-12" }: {
  externalUrl?: string | null;
  uploadedUrl?: string | null;
  name: string;
  className?: string;
}) {
  const [externalBroken, setExternalBroken] = useState(false);
  const src = !externalBroken && externalUrl ? externalUrl : uploadedUrl;

  if (!src) {
    return (
      <div className={`flex ${className} shrink-0 items-center justify-center rounded-lg border border-dashed border-border bg-muted/40 text-[9px] text-muted-foreground`}>
        Sin foto
      </div>
    );
  }
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={name}
      className={`${className} shrink-0 rounded-lg border border-border bg-white object-cover`}
      onError={() => { if (!externalBroken && externalUrl && src === externalUrl) setExternalBroken(true); }} />
  );
}
