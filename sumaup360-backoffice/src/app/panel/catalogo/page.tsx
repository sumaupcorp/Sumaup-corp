"use client";

import { useMemo, useRef, useState } from "react";
import { ref as storageRef, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage } from "@/lib/firebase";
import {
  useBusinessTypes, useMasterProducts, useCreateMasterProduct, useUpdateMasterProduct,
  useCatalogProposals, useApproveProposal, useRejectProposal,
  PHOTO_SOURCES, type MasterProduct, type SaveMasterProduct,
} from "@/features/catalog/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";

/**
 * Catalogo maestro de productos: el staff registra productos por rubro (una foto canonica
 * por producto) y los negocios del SaaS los buscan para añadirlos a su inventario.
 */
export default function CatalogoPage() {
  const [q, setQ] = useState("");
  const [rubro, setRubro] = useState("");
  const [onlyNoPhoto, setOnlyNoPhoto] = useState(false);
  const { data: rubros = [] } = useBusinessTypes();
  const products = useMasterProducts(q, rubro);
  const [editing, setEditing] = useState<MasterProduct | null>(null);
  const [creating, setCreating] = useState(false);

  const [brand, setBrand] = useState("");

  const rubroName = (code: string) => rubros.find((r) => r.code === code)?.name ?? code;
  const hasPhoto = (p: MasterProduct) => !!(p.photoExternalUrl || p.photoUrl);
  const brands = useMemo(
    () => Array.from(new Set((products.data ?? []).map((p) => p.brand).filter(Boolean) as string[])).sort(),
    [products.data]
  );
  const rows = (products.data ?? []).filter((p) =>
    (!onlyNoPhoto || !hasPhoto(p)) && (!brand || p.brand === brand));

  return (
    <div>
      <PageHeader
        title="Catalogo maestro"
        subtitle="Productos por rubro que los negocios del SaaS añaden a su inventario"
        action={<Button onClick={() => { setCreating(true); setEditing(null); }}>Nuevo producto</Button>}
      />

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="w-full max-w-xs space-y-1">
          <Label>Buscar</Label>
          <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Nombre, marca o codigo de barras" />
        </div>
        <div className="w-48 space-y-1">
          <Label>Rubro</Label>
          <Select value={rubro} onChange={(e) => setRubro(e.target.value)}>
            <option value="">Todos</option>
            {rubros.map((r) => <option key={r.code} value={r.code}>{r.name}</option>)}
          </Select>
        </div>
        <div className="w-44 space-y-1">
          <Label>Marca</Label>
          <Select value={brand} onChange={(e) => setBrand(e.target.value)}>
            <option value="">Todas</option>
            {brands.map((b) => <option key={b} value={b}>{b}</option>)}
          </Select>
        </div>
        <label className="flex h-10 items-center gap-2 text-sm">
          <input type="checkbox" className="accent-brand-blue" checked={onlyNoPhoto}
            onChange={(e) => setOnlyNoPhoto(e.target.checked)} />
          Solo sin foto
        </label>
      </div>

      <ProposalsQueue rubroName={rubroName} />

      {(creating || editing) && (
        <ProductForm
          key={editing?.id ?? "new"}
          product={editing}
          rubros={rubros}
          onClose={() => { setCreating(false); setEditing(null); }}
        />
      )}

      <Card>
        <CardContent className="p-0">
          {products.isLoading ? (
            <div className="p-6"><Spinner /></div>
          ) : rows.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">
              {onlyNoPhoto ? "Todos los productos del filtro tienen foto." : "Sin productos. Registra el primero con el boton Nuevo producto."}
            </p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
                  <th className="px-5 py-3">Producto</th>
                  <th className="px-3 py-3">Codigo de barras</th>
                  <th className="px-3 py-3">Rubros</th>
                  <th className="px-3 py-3">Estado</th>
                  <th className="px-3 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {rows.map((p) => (
                  <tr key={p.id} className={p.active ? "" : "opacity-50"}>
                    <td className="px-5 py-2.5">
                      <div className="flex items-center gap-3">
                        <ProductPhoto externalUrl={p.photoExternalUrl} uploadedUrl={p.photoUrl} name={p.name} />
                        <div>
                          <p className="font-medium">
                            {p.name}
                            {!hasPhoto(p) && <Badge variant="warning" className="ml-2">Sin foto</Badge>}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {[p.brand, p.presentation, p.category].filter(Boolean).join(" · ")}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-3 py-2.5 font-mono text-xs">{p.ean ?? "—"}</td>
                    <td className="px-3 py-2.5">
                      <div className="flex max-w-56 flex-wrap gap-1">
                        {p.rubros.map((r) => <Badge key={r} variant="muted">{rubroName(r)}</Badge>)}
                      </div>
                    </td>
                    <td className="px-3 py-2.5">
                      <Badge variant={p.verified ? "success" : "warning"}>
                        {p.verified ? "Verificado" : "Pendiente"}
                      </Badge>
                    </td>
                    <td className="px-3 py-2.5 text-right">
                      <Button size="sm" variant="outline" onClick={() => { setEditing(p); setCreating(false); }}>
                        Editar
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * Cola de propuestas de los negocios: productos propios de tenants que no estaban en el
 * catalogo. Aprobar crea el producto maestro (sin foto → completarla luego) y vincula el
 * producto del negocio; rechazar la descarta.
 */
function ProposalsQueue({ rubroName }: { rubroName: (code: string) => string }) {
  const proposals = useCatalogProposals();
  const approve = useApproveProposal();
  const reject = useRejectProposal();
  const items = proposals.data ?? [];

  if (proposals.isLoading || items.length === 0) return null;

  return (
    <Card className="mb-4 border-amber-200">
      <CardContent className="p-5">
        <div className="mb-3 flex items-center gap-2">
        <h2 className="text-sm font-semibold">Propuestas de negocios</h2>
          <Badge variant="warning">{items.length} pendiente{items.length === 1 ? "" : "s"}</Badge>
        </div>
        <p className="mb-3 text-sm text-muted-foreground">
          Productos que los negocios crearon y no estaban en el catalogo. Al aprobar se crean
          en el catalogo maestro (sin foto: usa el filtro Solo sin foto para completarlas).
        </p>
        <ul className="divide-y divide-border text-sm">
          {items.map((p) => (
            <li key={p.id} className="flex flex-wrap items-center justify-between gap-2 py-2.5">
              <div className="min-w-0">
                <p className="font-medium">{p.name}</p>
                <p className="text-xs text-muted-foreground">
                  {[p.ean, p.category, p.businessType ? rubroName(p.businessType) : null]
                    .filter(Boolean).join(" · ")}
                  {" · "}{new Date(p.createdAt).toLocaleDateString("es-PE")}
                </p>
              </div>
              <span className="flex shrink-0 gap-2">
                <Button size="sm" disabled={approve.isPending}
                  onClick={() => approve.mutate(p.id)}>Aprobar</Button>
                <Button size="sm" variant="ghost" disabled={reject.isPending}
                  onClick={() => reject.mutate(p.id)}>Rechazar</Button>
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}

/**
 * Foto del producto con cadena de respaldo: URL externa → foto subida → "Sin foto".
 * Si el hotlink externo se cae (404, anti-hotlink, cambio de URL), el onError pasa
 * automaticamente a la foto propia sin romper la vista.
 */
function ProductPhoto({ externalUrl, uploadedUrl, name, size = "size-11" }: {
  externalUrl: string | null;
  uploadedUrl: string | null;
  name: string;
  size?: string;
}) {
  const [externalBroken, setExternalBroken] = useState(false);
  const src = !externalBroken && externalUrl ? externalUrl : uploadedUrl;

  if (!src) {
    return (
      <div className={`flex ${size} shrink-0 items-center justify-center rounded-lg border border-dashed border-border text-[10px] text-muted-foreground`}>
        Sin foto
      </div>
    );
  }
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={src}
      alt={name}
      className={`${size} shrink-0 rounded-lg border border-border bg-white object-cover`}
      onError={() => { if (!externalBroken && externalUrl && src === externalUrl) setExternalBroken(true); }}
    />
  );
}

function ProductForm({ product, rubros, onClose }: {
  product: MasterProduct | null;
  rubros: { code: string; name: string }[];
  onClose: () => void;
}) {
  const create = useCreateMasterProduct();
  const update = useUpdateMasterProduct();
  const fileRef = useRef<HTMLInputElement>(null);

  const [form, setForm] = useState<SaveMasterProduct>({
    ean: product?.ean ?? "",
    name: product?.name ?? "",
    brand: product?.brand ?? "",
    category: product?.category ?? "",
    presentation: product?.presentation ?? "",
    photoUrl: product?.photoUrl ?? "",
    photoExternalUrl: product?.photoExternalUrl ?? "",
    photoSource: product?.photoSource ?? "fabricante",
    photoSourceUrl: product?.photoSourceUrl ?? "",
    verified: product?.verified ?? true,
    active: product?.active ?? true,
    rubros: product?.rubros ?? [],
  });
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = <K extends keyof SaveMasterProduct>(k: K, v: SaveMasterProduct[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const toggleRubro = (code: string) =>
    set("rubros", form.rubros.includes(code)
      ? form.rubros.filter((r) => r !== code)
      : [...form.rubros, code]);

  const uploadPhoto = async (file: File) => {
    if (!file.type.startsWith("image/")) { setError("El archivo debe ser una imagen."); return; }
    if (file.size > 5 * 1024 * 1024) { setError("La imagen no debe superar los 5MB."); return; }
    setUploading(true);
    setError(null);
    try {
      const dest = storageRef(storage, `catalog/${Date.now()}_${file.name.replace(/[^\w.\-]+/g, "_")}`);
      await uploadBytes(dest, file, { contentType: file.type });
      set("photoUrl", await getDownloadURL(dest));
    } catch {
      setError("No pudimos subir la imagen. Revisa las reglas de Storage (carpeta catalog/).");
    } finally {
      setUploading(false);
    }
  };

  const save = async () => {
    setError(null);
    if (!form.name.trim()) { setError("Ingresa el nombre del producto."); return; }
    if (form.rubros.length === 0) { setError("Elige al menos un rubro."); return; }
    const payload: SaveMasterProduct = {
      ...form,
      ean: form.ean?.trim() || null,
      name: form.name.trim(),
    };
    try {
      if (product) await update.mutateAsync({ id: product.id, ...payload });
      else await create.mutateAsync(payload);
      onClose();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const pending = create.isPending || update.isPending;

  return (
    <Card className="mb-4">
      <CardContent className="space-y-4 p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold">{product ? "Editar producto" : "Nuevo producto"}</h2>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div className="space-y-1">
            <Label>Nombre</Label>
            <Input value={form.name} onChange={(e) => set("name", e.target.value)}
              placeholder="Ej. Gaseosa Coca-Cola 500ml" />
          </div>
          <div className="space-y-1">
            <Label>Marca</Label>
            <Input value={form.brand ?? ""} onChange={(e) => set("brand", e.target.value)}
              placeholder="Ej. Coca-Cola" />
          </div>
          <div className="space-y-1">
            <Label>Codigo de barras (EAN)</Label>
            <Input value={form.ean ?? ""} onChange={(e) => set("ean", e.target.value.replace(/\D/g, ""))}
              placeholder="Ej. 7751580000000" maxLength={14} className="font-mono" />
          </div>
          <div className="space-y-1">
            <Label>Categoria</Label>
            <Input value={form.category ?? ""} onChange={(e) => set("category", e.target.value)}
              placeholder="Ej. Bebidas" />
          </div>
          <div className="space-y-1">
            <Label>Presentacion</Label>
            <Input value={form.presentation ?? ""} onChange={(e) => set("presentation", e.target.value)}
              placeholder="Ej. Botella 500ml" />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label>Rubros donde se ofrece</Label>
          <div className="flex flex-wrap gap-2">
            {rubros.map((r) => {
              const on = form.rubros.includes(r.code);
              return (
                <button key={r.code} type="button" onClick={() => toggleRubro(r.code)}
                  className={
                    "rounded-full border px-3 py-1 text-xs font-medium transition " +
                    (on ? "border-brand-blue bg-accent text-accent-foreground"
                        : "border-border bg-white text-muted-foreground hover:border-muted-foreground/40")
                  }>
                  {r.name}
                </button>
              );
            })}
          </div>
        </div>

        <div className="space-y-3 rounded-xl border border-border bg-muted/30 p-4">
          <div className="flex items-start gap-4">
            <div className="shrink-0 space-y-1 text-center">
              <ProductPhoto
                key={`${form.photoExternalUrl}|${form.photoUrl}`}
                externalUrl={form.photoExternalUrl?.trim() || null}
                uploadedUrl={form.photoUrl?.trim() || null}
                name={form.name || "Producto"}
                size="size-24"
              />
              <p className="text-[10px] text-muted-foreground">Como se vera</p>
            </div>
            <div className="min-w-0 flex-1 space-y-3">
              <div className="space-y-1">
                <Label>Foto por URL (externa, opcional)</Label>
                <Input value={form.photoExternalUrl ?? ""} onChange={(e) => set("photoExternalUrl", e.target.value)}
                  placeholder="https://... imagen del fabricante o tienda" />
                <p className="text-xs text-muted-foreground">
                  Se muestra primero. Si el enlace se cae, pasa automaticamente a la foto subida.
                </p>
              </div>
              <div className="space-y-1">
                <Label>Foto subida (respaldo permanente)</Label>
                <div className="flex items-center gap-3">
                  <input ref={fileRef} type="file" accept="image/*" className="hidden"
                    onChange={(e) => { const f = e.target.files?.[0]; if (f) void uploadPhoto(f); e.target.value = ""; }} />
                  <Button size="sm" variant="outline" disabled={uploading} onClick={() => fileRef.current?.click()}>
                    {uploading ? "Subiendo..." : form.photoUrl ? "Cambiar" : "Subir imagen"}
                  </Button>
                  {form.photoUrl && !uploading && (
                    <Button size="sm" variant="ghost" onClick={() => set("photoUrl", "")}>Quitar</Button>
                  )}
                </div>
              </div>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1">
              <Label>Procedencia de la foto</Label>
              <Select value={form.photoSource ?? ""} onChange={(e) => set("photoSource", e.target.value)}>
                {PHOTO_SOURCES.map((s) => <option key={s.code} value={s.code}>{s.label}</option>)}
              </Select>
            </div>
            <div className="space-y-1">
              <Label>URL de origen (opcional)</Label>
              <Input value={form.photoSourceUrl ?? ""} onChange={(e) => set("photoSourceUrl", e.target.value)}
                placeholder="https://... de donde salio la foto" />
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-6">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="accent-brand-blue" checked={form.verified ?? false}
              onChange={(e) => set("verified", e.target.checked)} />
            Verificado (visible con sello de confianza)
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="accent-brand-blue" checked={form.active ?? true}
              onChange={(e) => set("active", e.target.checked)} />
            Activo (los negocios lo encuentran al buscar)
          </label>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <Button disabled={pending || uploading} onClick={save}>
          {pending ? "Guardando..." : product ? "Guardar cambios" : "Registrar producto"}
        </Button>
      </CardContent>
    </Card>
  );
}
