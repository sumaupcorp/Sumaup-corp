"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage } from "@/lib/firebase";
import {
  useProducts, useCreateProduct, useUpdateProduct, type Product,
} from "@/features/products/api";
import { useCatalogSearch, type CatalogProduct } from "@/features/catalog/api";
import { useCompany } from "@/features/companies/company-context";
import { useHasPermission, useSession } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { PageHeader, Spinner, Badge } from "@/components/ui/misc";
import { Paginator } from "@/components/ui/pagination";

const PRODUCTS_PAGE_SIZE = 10;

export default function ProductsPage() {
  const hasPerm = useHasPermission();
  const canManage = hasPerm("product:manage");
  const { currentCompany } = useCompany();
  const { data: products = [], isLoading } = useProducts();

  const [modalOpen, setModalOpen] = useState(false);
  const [fromCatalog, setFromCatalog] = useState<CatalogProduct | null>(null);
  const [editing, setEditing] = useState<Product | null>(null);
  const [lightbox, setLightbox] = useState<{ src: string; name: string } | null>(null);
  const [filter, setFilter] = useState("");
  const [tablePage, setTablePage] = useState(0);
  const [tab, setTab] = useState<"mine" | "catalog">("mine");

  const ownedMasterIds = useMemo(
    () => new Set(products.map((p) => p.masterProductId).filter(Boolean) as string[]),
    [products]
  );

  const visibleProducts = useMemo(() => {
    const f = filter.trim().toLowerCase();
    if (!f) return products;
    return products.filter((p) =>
      p.name.toLowerCase().includes(f) || p.sku.toLowerCase().includes(f) ||
      (p.category ?? "").toLowerCase().includes(f));
  }, [products, filter]);

  const tableTotalPages = Math.max(1, Math.ceil(visibleProducts.length / PRODUCTS_PAGE_SIZE));
  const safeTablePage = Math.min(tablePage, tableTotalPages - 1);
  const pageProducts = visibleProducts.slice(
    safeTablePage * PRODUCTS_PAGE_SIZE, (safeTablePage + 1) * PRODUCTS_PAGE_SIZE);

  const openBlank = () => { setFromCatalog(null); setModalOpen(true); };
  const openFromCatalog = (p: CatalogProduct) => { setFromCatalog(p); setModalOpen(true); };

  return (
    <div>
      <PageHeader
        title="Productos"
        subtitle="Catalogo de tu negocio"
        action={canManage ? <Button onClick={openBlank}>Agregar producto</Button> : undefined}
      />

      {/* Pestanas: tus productos vs catalogo maestro estilo tienda */}
      {canManage && (
        <div className="mb-4 flex w-fit rounded-xl bg-muted p-1">
          <ProductsTab label="Mis productos" active={tab === "mine"} onClick={() => setTab("mine")} />
          <ProductsTab label="Catalogo SUMAUP" active={tab === "catalog"} onClick={() => setTab("catalog")} />
        </div>
      )}

      {canManage && tab === "catalog" ? (
        <CatalogStore
          companyId={currentCompany?.id}
          ownedMasterIds={ownedMasterIds}
          onPick={openFromCatalog}
          onPhotoOpen={(src, name) => setLightbox({ src, name })}
        />
      ) : (
        /* MIS PRODUCTOS */
        <Card>
          <CardContent className="p-5">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-sm font-semibold">Tu inventario</h2>
              <div className="w-full max-w-56">
                <Input value={filter}
                  onChange={(e) => { setFilter(e.target.value); setTablePage(0); }}
                  placeholder="Filtrar por nombre, SKU..." className="h-9" />
              </div>
            </div>
            {isLoading ? (
              <Spinner />
            ) : visibleProducts.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                {filter ? "Nada coincide con tu filtro." : "Aun no hay productos. Añade uno desde el catalogo SUMAUP o con Agregar producto."}
              </p>
            ) : (
              <table className="w-full text-sm">
                <thead className="text-left text-muted-foreground">
                  <tr>
                    <th className="py-2">Producto</th>
                    <th className="py-2">SKU</th>
                    <th className="py-2 text-right">Precio</th>
                    <th className="py-2 text-right">Estado</th>
                    {canManage && <th className="py-2"></th>}
                  </tr>
                </thead>
                <tbody>
                  {pageProducts.map((p) => (
                    <tr key={p.id} className="border-t border-border">
                      <td className="py-2">
                        <div className="flex items-center gap-3">
                          <ProductPhoto
                            externalUrl={p.photoExternalUrl ?? null}
                            uploadedUrl={p.photoUrl ?? null}
                            name={p.name}
                            size="size-11"
                            onOpen={(src) => setLightbox({ src, name: p.name })}
                          />
                          <div className="min-w-0">
                            <p className="truncate font-medium">{p.name}</p>
                            {p.masterProductId && <Badge variant="muted">catalogo</Badge>}
                          </div>
                        </div>
                      </td>
                      <td className="py-2 font-mono text-xs">{p.sku}</td>
                      <td className="py-2 text-right">S/ {Number(p.price).toFixed(2)}</td>
                      <td className="py-2 text-right">
                        <Badge variant={p.active ? "success" : "muted"}>{p.active ? "activo" : "inactivo"}</Badge>
                      </td>
                      {canManage && (
                        <td className="py-2 text-right">
                          <Button size="sm" variant="outline" onClick={() => setEditing(p)}>Editar</Button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {!isLoading && visibleProducts.length > 0 && (
              <Paginator page={safeTablePage} totalPages={tableTotalPages}
                totalItems={visibleProducts.length} onPage={setTablePage} />
            )}
          </CardContent>
        </Card>
      )}

      {canManage && modalOpen && (
        <NewProductModal
          companyId={currentCompany?.id}
          initialCatalog={fromCatalog}
          ownedMasterIds={ownedMasterIds}
          onClose={() => setModalOpen(false)}
        />
      )}

      {canManage && editing && (
        <EditProductModal product={editing} onClose={() => setEditing(null)} />
      )}

      {lightbox && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-6"
          onClick={() => setLightbox(null)}>
          <div className="max-w-lg rounded-2xl bg-white p-4" onClick={(e) => e.stopPropagation()}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={lightbox.src} alt={lightbox.name} className="max-h-[70vh] w-full rounded-xl object-contain" />
            <div className="mt-3 flex items-center justify-between gap-3">
              <p className="text-sm font-medium">{lightbox.name}</p>
              <Button size="sm" variant="outline" onClick={() => setLightbox(null)}>Cerrar</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ProductsTab({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "min-h-11 select-none rounded-lg px-6 text-sm font-semibold transition touch-manipulation active:scale-95 " +
        (active ? "bg-white text-primary shadow-sm" : "text-muted-foreground")
      }
    >
      {label}
    </button>
  );
}

const CATALOG_PAGE_SIZE = 18;

/**
 * Catalogo SUMAUP estilo tienda mayorista: grilla de tarjetas con foto grande,
 * categorias como chips y marca como filtro. Un toque en "Añadir" jala el producto
 * con foto y nombre a tu inventario.
 */
function CatalogStore({ companyId, ownedMasterIds, onPick, onPhotoOpen }: {
  companyId?: string;
  ownedMasterIds: Set<string>;
  onPick: (p: CatalogProduct) => void;
  onPhotoOpen: (src: string, name: string) => void;
}) {
  const [q, setQ] = useState("");
  const [debounced, setDebounced] = useState("");
  const [category, setCategory] = useState("");
  const [brand, setBrand] = useState("");
  const [page, setPage] = useState(0);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(q), 300);
    return () => clearTimeout(t);
  }, [q]);

  const results = useCatalogSearch(companyId, debounced);
  const items = useMemo(() => results.data ?? [], [results.data]);
  const categories = useMemo(
    () => Array.from(new Set(items.map((p) => p.category).filter(Boolean) as string[])).sort(),
    [items]
  );
  const brands = useMemo(
    () => Array.from(new Set(items.map((p) => p.brand).filter(Boolean) as string[])).sort(),
    [items]
  );
  const visible = useMemo(
    () => items.filter((p) =>
      (!category || p.category === category) && (!brand || p.brand === brand)),
    [items, category, brand]
  );

  const totalPages = Math.max(1, Math.ceil(visible.length / CATALOG_PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageItems = visible.slice(safePage * CATALOG_PAGE_SIZE, (safePage + 1) * CATALOG_PAGE_SIZE);

  return (
    <Card>
      <CardContent className="p-4">
        {/* Buscador + marca */}
        <div className="mb-3 flex flex-wrap items-center gap-3">
          <div className="w-full max-w-md flex-1">
            <Input value={q} className="h-11 text-base"
              onChange={(e) => { setQ(e.target.value); setPage(0); }}
              placeholder="Buscar por nombre, marca o codigo de barras..." />
          </div>
          {brands.length > 0 && (
            <select
              value={brand}
              onChange={(e) => { setBrand(e.target.value); setPage(0); }}
              className="h-11 rounded-lg border border-border bg-white px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">Todas las marcas</option>
              {brands.map((b) => <option key={b} value={b}>{b}</option>)}
            </select>
          )}
          <p className="ml-auto text-xs text-muted-foreground">
            {results.isLoading ? "Buscando..." : `${visible.length} producto${visible.length === 1 ? "" : "s"}`}
          </p>
        </div>

        {/* Categorias como pasillos de tienda */}
        {categories.length > 0 && (
          <div className="mb-4 flex gap-1.5 overflow-x-auto pb-1">
            <CategoryChip label="Todos" active={!category}
              onClick={() => { setCategory(""); setPage(0); }} />
            {categories.map((c) => (
              <CategoryChip key={c} label={c} active={category === c}
                onClick={() => { setCategory(c); setPage(0); }} />
            ))}
          </div>
        )}

        {results.isLoading ? (
          <div className="flex justify-center py-14"><Spinner /></div>
        ) : results.isError ? (
          <p className="py-10 text-center text-sm text-destructive">{(results.error as Error).message}</p>
        ) : visible.length === 0 ? (
          <p className="py-14 text-center text-sm text-muted-foreground">
            {q ? "Sin resultados en el catalogo. Crealo con Agregar producto." : "El catalogo de tu rubro aun no tiene productos."}
          </p>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6">
              {pageItems.map((p) => {
                const owned = ownedMasterIds.has(p.id);
                return (
                  <div key={p.id}
                    className={
                      "relative flex flex-col rounded-xl border bg-white p-3 text-center transition " +
                      (owned ? "border-green-200 bg-green-50/30" : "border-border hover:border-primary hover:shadow-sm")
                    }>
                    {owned && (
                      <span className="absolute right-2 top-2 z-10 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-green-700">
                        Añadido
                      </span>
                    )}
                    <div className="mb-2 flex justify-center">
                      <ProductPhoto
                        externalUrl={p.photoExternalUrl}
                        uploadedUrl={p.photoUrl}
                        name={p.name}
                        size="size-24"
                        onOpen={(src) => onPhotoOpen(src, p.name)}
                      />
                    </div>
                    <p className="line-clamp-2 min-h-8 text-xs font-medium leading-tight">{p.name}</p>
                    <p className="mt-0.5 truncate text-[11px] text-muted-foreground">
                      {[p.brand, p.presentation].filter(Boolean).join(" · ") || (p.ean ?? "")}
                    </p>
                    <div className="mt-auto pt-2">
                      {owned ? (
                        <p className="text-[11px] text-muted-foreground">Ya esta en tu inventario</p>
                      ) : (
                        <Button size="sm" className="min-h-10 w-full touch-manipulation active:scale-95"
                          onClick={() => onPick(p)}>
                          Añadir
                        </Button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
            <Paginator page={safePage} totalPages={totalPages}
              totalItems={visible.length} onPage={setPage} />
          </>
        )}
      </CardContent>
    </Card>
  );
}

function CategoryChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick}
      className={
        "h-10 shrink-0 select-none rounded-full border px-4 text-sm font-medium transition touch-manipulation active:scale-95 " +
        (active ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-white text-muted-foreground hover:border-primary")
      }>
      {label}
    </button>
  );
}

/**
 * Modal de nuevo producto. Nombre y SKU funcionan como buscadores del catalogo:
 * el nombre por texto y el SKU por codigo de barras (lector incluido — el Enter del
 * lector no envia el formulario). Si el producto ya existe, se jala con un clic.
 */
function NewProductModal({ companyId, initialCatalog, ownedMasterIds, onClose }: {
  companyId?: string;
  initialCatalog: CatalogProduct | null;
  ownedMasterIds: Set<string>;
  onClose: () => void;
}) {
  const create = useCreateProduct();

  const [fromCatalog, setFromCatalog] = useState<CatalogProduct | null>(initialCatalog);
  const [sku, setSku] = useState(initialCatalog?.ean ?? "");
  const [name, setName] = useState(
    initialCatalog ? [initialCatalog.name, initialCatalog.presentation].filter(Boolean).join(" ") : ""
  );
  const [price, setPrice] = useState("");
  const [unit, setUnit] = useState("UNIDAD");
  const [category, setCategory] = useState(initialCatalog?.category ?? "");
  const [photoUrl, setPhotoUrl] = useState("");
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sugerencias: nombre (texto) y SKU (codigo de barras) alimentan el mismo panel.
  const [debouncedName, setDebouncedName] = useState("");
  const [debouncedSku, setDebouncedSku] = useState("");
  useEffect(() => {
    const t = setTimeout(() => setDebouncedName(name), 300);
    return () => clearTimeout(t);
  }, [name]);
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSku(sku), 300);
    return () => clearTimeout(t);
  }, [sku]);

  const skuLooksLikeBarcode = /^\d{8,14}$/.test(debouncedSku.trim());
  const byName = useCatalogSearch(companyId, debouncedName,
    !fromCatalog && debouncedName.trim().length >= 2);
  const bySku = useCatalogSearch(companyId, debouncedSku.trim(),
    !fromCatalog && skuLooksLikeBarcode);

  const rawResultCount = (bySku.data?.length ?? 0) + (byName.data?.length ?? 0);
  const suggested = useMemo(() => {
    const seen = new Set<string>();
    const merged: CatalogProduct[] = [];
    for (const p of [...(bySku.data ?? []), ...(byName.data ?? [])]) {
      if (!seen.has(p.id) && !ownedMasterIds.has(p.id)) {
        seen.add(p.id);
        merged.push(p);
      }
    }
    return merged.slice(0, 4);
  }, [bySku.data, byName.data, ownedMasterIds]);

  const applySuggestion = (p: CatalogProduct) => {
    setFromCatalog(p);
    setName([p.name, p.presentation].filter(Boolean).join(" "));
    if (p.ean) setSku(p.ean);
    if (p.category) setCategory(p.category);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await create.mutateAsync({
        sku, name, unit,
        price: price ? Number(price) : 0,
        category: category || undefined,
        masterProductId: fromCatalog?.id,
        photoUrl: !fromCatalog && photoUrl ? photoUrl : undefined,
        companyId,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-16">
      <div className="w-full max-w-lg rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">Agregar producto</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        {fromCatalog && (
          <div className="mb-3 flex items-center gap-3 rounded-lg border border-border bg-accent/50 p-2.5">
            <ProductPhoto externalUrl={fromCatalog.photoExternalUrl} uploadedUrl={fromCatalog.photoUrl}
              name={fromCatalog.name} size="size-10" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-medium">Del catalogo SUMAUP — foto y datos vinculados</p>
              <p className="truncate text-xs text-muted-foreground">{fromCatalog.name}</p>
            </div>
            <Button size="sm" variant="ghost" onClick={() => setFromCatalog(null)}>Quitar</Button>
          </div>
        )}

        <form onSubmit={submit} className="space-y-3">
          <div className="space-y-1">
            <Label>SKU (tu codigo interno o codigo de barras)</Label>
            <Input value={sku} onChange={(e) => setSku(e.target.value)} required autoFocus
              onKeyDown={(e) => { if (e.key === "Enter") e.preventDefault(); }}
              placeholder="Escanea el codigo de barras o escribe tu codigo"
              className="font-mono" />
            <p className="text-xs text-muted-foreground">
              Si escaneas un codigo que ya esta en el catalogo, te lo sugerimos al instante.
            </p>
          </div>
          <div className="space-y-1">
            <Label>Nombre</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} required
              placeholder="Escribe y te sugerimos si ya existe" />
          </div>

          {!fromCatalog && (byName.isFetching || bySku.isFetching) && (
            <div className="flex items-center gap-2 rounded-xl border border-border bg-muted/40 p-2.5">
              <Spinner className="size-4" />
              <span className="text-xs text-muted-foreground">Buscando en el catalogo SUMAUP...</span>
            </div>
          )}

          {!fromCatalog && !byName.isFetching && !bySku.isFetching && suggested.length > 0 && (
            <div className="rounded-xl border border-border bg-muted/40 p-2.5">
              <p className="mb-2 text-xs font-medium text-muted-foreground">
                Ya existe en el catalogo SUMAUP — usalo y ahorrate el trabajo:
              </p>
              <ul className="space-y-1.5">
                {suggested.map((p) => (
                  <li key={p.id} className="flex items-center gap-2.5 rounded-lg bg-white p-2">
                    <ProductPhoto externalUrl={p.photoExternalUrl} uploadedUrl={p.photoUrl}
                      name={p.name} size="size-9" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-xs font-medium">{p.name}</p>
                      <p className="truncate text-[11px] text-muted-foreground">
                        {[p.brand, p.presentation].filter(Boolean).join(" · ") || (p.ean ?? "")}
                      </p>
                    </div>
                    <Button size="sm" variant="outline" onClick={() => applySuggestion(p)}>Usar</Button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {!fromCatalog && !byName.isFetching && !bySku.isFetching && suggested.length === 0
            && rawResultCount > 0 && (
            <p className="rounded-xl border border-border bg-muted/40 p-2.5 text-xs text-muted-foreground">
              Ya tienes este producto en tu inventario.
            </p>
          )}

          {!fromCatalog && !byName.isFetching && !bySku.isFetching && suggested.length === 0
            && rawResultCount === 0
            && (byName.data !== undefined || bySku.data !== undefined)
            && debouncedName.trim().length >= 2 && (
            <p className="rounded-xl border border-border bg-muted/40 p-2.5 text-xs text-muted-foreground">
              No esta en el catalogo SUMAUP: se creara como producto tuyo y lo propondremos
              al catalogo para que otros negocios tambien lo encuentren.
            </p>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>Precio</Label>
              <Input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} />
            </div>
            <div className="space-y-1">
              <Label>Unidad</Label>
              <Input value={unit} onChange={(e) => setUnit(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1">
            <Label>Categoria</Label>
            <Input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="Ej. Bebidas" />
          </div>

          {/* Foto propia: solo aplica cuando el producto no viene del catalogo maestro */}
          {!fromCatalog && (
            <OwnPhotoField
              photoUrl={photoUrl}
              onChange={setPhotoUrl}
              onBusy={setUploadingPhoto}
              hint="Tomale una foto o sube una imagen: asi se vera bien en el POS desde el primer dia."
            />
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-1">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" disabled={create.isPending || uploadingPhoto}>
              {create.isPending ? "Guardando..." : "Crear producto"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

/** Subida de foto propia del producto a Firebase Storage (products/{tenantId}/...). */
function OwnPhotoField({ photoUrl, onChange, onBusy, hint }: {
  photoUrl: string;
  onChange: (url: string) => void;
  onBusy: (busy: boolean) => void;
  hint?: string;
}) {
  const { data: session } = useSession();
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const upload = async (file: File) => {
    setError(null);
    if (!file.type.startsWith("image/")) { setError("El archivo debe ser una imagen."); return; }
    if (file.size > 5 * 1024 * 1024) { setError("La imagen no debe superar los 5MB."); return; }
    setUploading(true);
    onBusy(true);
    try {
      const tenant = session?.tenantId ?? "tenant";
      const dest = ref(storage, `products/${tenant}/${Date.now()}-${file.name.replace(/[^a-zA-Z0-9.]/g, "")}`);
      await uploadBytes(dest, file, { contentType: file.type });
      onChange(await getDownloadURL(dest));
    } catch {
      setError("No pudimos subir la foto. Intenta de nuevo.");
    } finally {
      setUploading(false);
      onBusy(false);
    }
  };

  return (
    <div className="space-y-1">
      <Label>Foto del producto (opcional)</Label>
      <div className="flex items-center gap-3">
        {photoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={photoUrl} alt="Foto del producto"
            className="size-16 rounded-lg border border-border bg-white object-cover" />
        ) : (
          <div className="flex size-16 items-center justify-center rounded-lg border-2 border-dashed border-border text-[10px] text-muted-foreground">
            Sin foto
          </div>
        )}
        <div className="space-y-1">
          <input ref={fileRef} type="file" accept="image/*" className="hidden"
            onChange={(e) => { const f = e.target.files?.[0]; if (f) upload(f); e.target.value = ""; }} />
          <div className="flex gap-2">
            <Button type="button" size="sm" variant="outline" disabled={uploading}
              onClick={() => fileRef.current?.click()}>
              {uploading ? "Subiendo..." : photoUrl ? "Cambiar foto" : "Subir foto"}
            </Button>
            {photoUrl && !uploading && (
              <Button type="button" size="sm" variant="ghost" onClick={() => onChange("")}>Quitar</Button>
            )}
          </div>
          {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
        </div>
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}

/** Edicion del producto: precio, nombre, unidad, categoria, activo y foto propia. */
function EditProductModal({ product, onClose }: { product: Product; onClose: () => void }) {
  const update = useUpdateProduct();
  const [name, setName] = useState(product.name);
  const [price, setPrice] = useState(String(product.price));
  const [unit, setUnit] = useState(product.unit);
  const [category, setCategory] = useState(product.category ?? "");
  const [active, setActive] = useState(product.active);
  // null = sin cambios; "" = quitar foto propia; URL = foto nueva subida.
  const [newPhoto, setNewPhoto] = useState<string | null>(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Para productos sin vinculo al maestro, la foto mostrada es propia y editable aqui.
  const editablePhoto = newPhoto ?? (product.masterProductId ? "" : (product.photoUrl ?? ""));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await update.mutateAsync({
        id: product.id, name, unit,
        price: price ? Number(price) : 0,
        category: category || undefined,
        active,
        photoUrl: newPhoto ?? undefined,
      });
      onClose();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-16">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold">Editar producto</h3>
          <Button size="sm" variant="ghost" onClick={onClose}>Cerrar</Button>
        </div>

        <div className="mb-3 flex items-center gap-3">
          <ProductPhoto externalUrl={product.photoExternalUrl ?? null}
            uploadedUrl={product.photoUrl ?? null} name={product.name} size="size-14" />
          <div>
            <p className="font-mono text-xs text-muted-foreground">SKU: {product.sku}</p>
            {product.masterProductId && <Badge variant="muted">Del catalogo SUMAUP</Badge>}
          </div>
        </div>

        <form onSubmit={submit} className="space-y-3">
          <div className="space-y-1">
            <Label>Nombre</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>Precio</Label>
              <Input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} />
            </div>
            <div className="space-y-1">
              <Label>Unidad</Label>
              <Input value={unit} onChange={(e) => setUnit(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1">
            <Label>Categoria</Label>
            <Input value={category} onChange={(e) => setCategory(e.target.value)} />
          </div>

          <OwnPhotoField
            photoUrl={editablePhoto}
            onChange={setNewPhoto}
            onBusy={setUploadingPhoto}
            hint={product.masterProductId
              ? "Si subes una foto propia, esta manda sobre la del catalogo SUMAUP."
              : undefined}
          />

          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="accent-primary" checked={active}
              onChange={(e) => setActive(e.target.checked)} />
            Activo (visible en ventas)
          </label>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-1">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" disabled={update.isPending || uploadingPhoto}>
              {update.isPending ? "Guardando..." : "Guardar cambios"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

/**
 * Foto con cadena de respaldo: URL externa → foto propia → "Sin foto".
 * Con onOpen, el clic abre la imagen en grande (lightbox).
 */
function ProductPhoto({ externalUrl, uploadedUrl, name, size = "size-12", onOpen }: {
  externalUrl: string | null;
  uploadedUrl: string | null;
  name: string;
  size?: string;
  onOpen?: (src: string) => void;
}) {
  const [externalBroken, setExternalBroken] = useState(false);
  const src = !externalBroken && externalUrl ? externalUrl : uploadedUrl;

  if (!src) {
    return (
      <div className={`flex ${size} shrink-0 items-center justify-center rounded-lg border border-dashed border-border text-[9px] text-muted-foreground`}>
        Sin foto
      </div>
    );
  }
  const img = (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={name}
      className={`${size} shrink-0 rounded-lg border border-border bg-white object-cover`}
      onError={() => { if (!externalBroken && externalUrl && src === externalUrl) setExternalBroken(true); }} />
  );
  if (!onOpen) return img;
  return (
    <button type="button" className="shrink-0 cursor-zoom-in" onClick={() => onOpen(src)}
      title="Ver mas grande">
      {img}
    </button>
  );
}
