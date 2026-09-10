import { ShieldCheck, BarChart3, Boxes } from "lucide-react";

export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Panel de marca */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-brand-blue p-10 text-white lg:flex">
        <div className="absolute -right-24 -top-24 size-72 rounded-full bg-white/10" />
        <div className="absolute -bottom-32 -left-16 size-96 rounded-full bg-white/5" />
        <div className="relative">
          <span className="font-heading text-2xl font-bold">SUMAUP360</span>
          <p className="mt-1 text-sm text-white/70">Panel de gestion para tu negocio</p>
        </div>
        <div className="relative space-y-5">
          <Feature icon={<Boxes className="size-5" />} title="Todo tu negocio en un lugar" desc="Productos, inventario, ventas y caja segun tu rubro." />
          <Feature icon={<BarChart3 className="size-5" />} title="Reportes claros" desc="Conoce tus ventas y tu utilidad real cada mes." />
          <Feature icon={<ShieldCheck className="size-5" />} title="Roles y permisos" desc="Crea cuentas para tus trabajadores con accesos a medida." />
        </div>
        <p className="relative text-xs text-white/60">© SUMAUP360 · Hecho en Peru</p>
      </div>

      {/* Formulario */}
      <div className="flex items-center justify-center bg-brand-soft p-6">
        <div className="w-full max-w-sm">
          <div className="mb-6 text-center lg:hidden">
            <span className="font-heading text-2xl font-bold text-brand-blue">SUMAUP360</span>
          </div>
          <h1 className="font-heading text-2xl font-bold text-foreground">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  );
}

function Feature({ icon, title, desc }: { icon: React.ReactNode; title: string; desc: string }) {
  return (
    <div className="flex gap-3">
      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-white/15">{icon}</div>
      <div>
        <p className="font-medium">{title}</p>
        <p className="text-sm text-white/70">{desc}</p>
      </div>
    </div>
  );
}
