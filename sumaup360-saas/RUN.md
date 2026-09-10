# SUMAUP360 — SaaS panel (cliente / ERP)

App Next.js 16 + Firebase (login) conectada al backend `sumaup360-backend`.

## Requisitos para que funcione
1. Backend corriendo en `http://localhost:8080` con Firebase activo
   (`sumaup360-backend/run-local.ps1`).
2. `.env.local` con la config de Firebase web y `NEXT_PUBLIC_API_BASE_URL` (ya configurado).

## Correr
```bash
cd sumaup360-saas
npm install      # solo la primera vez
npm run dev      # http://localhost:3000 (o 3001 si el 3000 esta ocupado)
npm run build    # build de produccion
```

## Credenciales de prueba (super-admin)
- Correo: **prueba.saas@sumaup.dev**
- Contrasena: **Clave12345**
- Tiene tenant "Mi Negocio Demo" con datos demo (empresa minimarket + empresa restaurante).

## Que puedes hacer
- **Login** real con Firebase (correo o Google).
- El **menu lateral se arma desde los modulos habilitados** de la empresa seleccionada
  (cambia la empresa arriba a la izquierda) y respeta tus **permisos (RBAC)**.
- Empresas, Sucursales, Productos, Inventario (stock/ajustes), Ventas/Caja (POS),
  Clientes, Proveedores, Reportes, Documentos, Equipo (usuarios+roles), Modulos.
- Cambia a la empresa **Cevicheria Demo** para ver **Restaurante** (mesas/comandas) y **Cocina**.

## Arquitectura
- `src/lib/firebase.ts` · `src/lib/api.ts` — auth + cliente HTTP (Bearer idToken).
- `src/features/*` — auth/session, companies, products, inventory, sales, customers,
  suppliers, branches, team (RBAC), restaurant, document-templates, catalog, reports.
- `src/lib/navigation.ts` — mapeo modulo->menu (gating por enabledModules + permiso).
- `src/app/dashboard/*` — paginas; layout con guard de auth + shell.
