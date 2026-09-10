# Despliegue — Frontend (landing + SaaS dashboard)

Notas de despliegue de los proyectos Next.js. Detalle final en Fase 9.

## Proyectos
- `sumaup360-reborn` — landing comercial.
- `sumaup360-saas` — dashboard SaaS/ERP.

## Build
```
npm install
npm run build
npm run start   # o despliegue en plataforma (Vercel u otra)
```

## Variables de entorno
- URL base del backend (API).
- Configuración de Firebase web (apiKey, authDomain, projectId, etc.) — del MISMO proyecto
  Firebase del ecosistema.
- Sin secretos del lado servidor en el cliente.

## Checklist
- `npm run build` y `npm run lint` sin errores.
- Responsive 360/390/768/1280, sin overflow horizontal.
- Solo assets locales; sin imágenes externas; sin emojis; español real.
- El SaaS construye su navegación desde `enabledModules` del backend.
