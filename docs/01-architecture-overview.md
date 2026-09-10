# 01 — Arquitectura general

## Vista de alto nivel

```
        ┌────────────────┐   ┌────────────────────┐   ┌──────────────────┐
        │ Landing (web)  │   │ SaaS Dashboard ERP │   │ App móvil Flutter │
        │ sumaup360-reborn│  │ sumaup360-saas     │   │ sumaup360-mobile  │
        └───────┬────────┘   └─────────┬──────────┘   └────────┬─────────┘
                │  leads/contacto       │  Bearer idToken        │ Bearer idToken
                │                       ▼                        ▼
                │            ┌───────────────────────────────────────────┐
                └──────────► │   Backend Spring Boot (monolito modular)   │
                             │   sumaup360-backend  — AUTORIDAD central    │
                             │   auth · RBAC · tenant · billing · ERP ·    │
                             │   app(personas) · chatbot · audit · notif.  │
                             └───────────────┬─────────────────────────────┘
                                             │ JDBC + Flyway
                                             ▼
                             ┌───────────────────────────────────────────┐
                             │ PostgreSQL  sumaup360_db                    │
                             │ schemas: auth tenant app erp billing        │
                             │          catalog audit chatbot notification │
                             └───────────────────────────────────────────┘

        Identidad:  Firebase Auth (1 proyecto)  ←→  Firebase Admin SDK (backend verifica)
```

## Componentes

- **Backend (autoridad):** monolito modular en Spring Boot. Verifica identidad (Firebase),
  decide autorización (RBAC propio), aísla por tenant, expone APIs REST documentadas con
  OpenAPI. Migra el esquema con Flyway.
- **Landing:** marketing + captura de leads + diagnóstico web + chatbot. Hoy autónoma; se
  integrará con el backend para leads/diagnóstico cuando su fase lo requiera.
- **SaaS Dashboard:** consume el backend; UI por **módulos habilitados** según rubro/plan;
  RBAC reflejado en la UI; TanStack Query para datos; Recharts para KPIs.
- **App móvil:** consume el backend; foco Personas; Riverpod + GoRouter + Dio.
- **Backoffice (interno):** `sumaup360-backoffice` consume el mismo backend con identidad de
  **staff**; es **cross-tenant** (cima del RBAC). Procesa los comprobantes que suben los
  usuarios de la app, da soporte y gestiona licencias del SaaS ERP. Va **separado** del SaaS
  de clientes por seguridad. Ver `10-backoffice-internal.md`.

## Frontera de responsabilidades

| Capa | Decide | NO decide |
|---|---|---|
| Firebase | Identidad (quién eres) | Roles, permisos, tenant |
| Backend | Autorización, tenant, reglas de negocio, datos | Presentación |
| SaaS / Mobile / Landing / Backoffice | Presentación, UX, llamadas al backend | Reglas de negocio, autorización |

## Reglas de integración

- Frontend y mobile **nunca** implementan reglas de negocio ni autorización: solo envían el
  idToken y consumen contratos del backend.
- Contratos (DTOs/endpoints/claims) son la fuente de acoplamiento; cambiarlos exige revisar
  impacto cruzado (master architect).
- Monolito modular hoy; los módulos del backend están aislados para poder extraerse a
  servicios en el futuro **sin** hacerlo todavía.

## Decisiones clave (selladas Fase 0)

1. Un solo Firebase; backend = autoridad (ver `06-auth-security.md`).
2. Multi-tenant por `tenant_id` sobre una base con schemas de dominio.
3. Rubros por configuración (módulos habilitados), no por código duplicado.
4. Polyrepo; monolito modular; sin microservicios todavía.
