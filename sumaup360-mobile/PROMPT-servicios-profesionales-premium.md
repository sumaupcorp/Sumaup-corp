# Prompt — Módulo premium "Servicios Profesionales" (Recibos por Honorarios + Suspensión de 4ta)

## Contexto
Ecosistema SUMAUP360. Tercer rol de la Línea Personas: `SERVICIOS_PROFESIONALES` (profesionales
independientes con recibos por honorarios / 4ta categoría). Este módulo es el equivalente premium
que taxi tiene en "Comprobantes" (QR) y delivery en "Peya". **Reusar exactamente los patrones ya
construidos** en `app/taxi` (Fase 3) y `app/peya` (Fase 4): entidades + repos + service con gating
`FeatureAccessService`, adjuntos vía `attached_file`, notificaciones vía `NotificationService`,
flujo de backoffice con estados, `ApiException` para errores hacia la app.

Precio del plan: **S/ 14.90 mensual** (`serv-premium`). Español real, sin emojis, design system Suma
(móvil) / backoffice (azul/celeste/blanco, shadcn). Estados carga/error/vacío en toda UI.

## Alcance funcional (sección premium del profesional)
Dos servicios dentro del módulo, ambos premium (bloqueados si es free, con CTA a upgrade S/ 14.90):

1. **Recibos por Honorarios** — el profesional envía un **formulario** y nosotros (backoffice) le
   generamos el Recibo por Honorarios electrónico. Campos del formulario:
   - Nombre de la empresa / cliente que paga (razón social).
   - Documento del cliente (RUC o DNI) + número.
   - Descripción del servicio prestado.
   - Monto (S/). Opcional: si aplica retención de 4ta.
   - (El profesional ya tiene su RUC en el perfil; se usa como emisor.)
   El backoffice procesa la solicitud, genera el recibo y **adjunta el PDF**; la app muestra estado
   e permite descargar/copiar el enlace del recibo.

2. **Suspensión de 4ta categoría (ANUAL)** — la suspensión aplica por **año** (se solicita una vez y
   rige hasta fin del ejercicio). En su sección premium el profesional elige el **año** y **genera la
   solicitud de suspensión de cuarta**; nosotros la tramitamos ante SUNAT. El backoffice cambia el
   estado y adjunta la constancia. La app muestra el estado y la constancia. (El "calendario" es una
   selección de año, no de día/mes.)

## Backend (nuevo paquete `app/honorarios`, patrón app/taxi y app/peya)
- **Enum `ClientType`**: ya se agrega `SERVICIOS_PROFESIONALES` en el prompt de onboarding.
- **Migración Flyway (V31+)**:
  - Seed de planes en `billing.plan` (line=PERSON): `serv-free` (0) y `serv-premium` (14.90).
  - `app.honorario_request` — id, user_id, cliente_nombre, cliente_doc_type, cliente_doc_number,
    descripcion, monto, con_retencion BOOLEAN, estado VARCHAR, recibo_url, observacion, created_at,
    completed_at.
  - `app.suspension_request` — id, user_id, anio INT (año del ejercicio), estado VARCHAR,
    observacion, constancia_url, created_at, completed_at.
  - (Reutilizar `attached_file` con entity_type `HONORARIO_REQUEST` / `SUSPENSION_REQUEST`.)
- **Enums de estado**: `HonorarioStatus` (PENDIENTE / EN_PROCESO / GENERADO / OBSERVADO / CANCELADO),
  `SuspensionStatus` (SOLICITADA / EN_PROCESO / TRAMITADA / OBSERVADA / CANCELADA).
- **Feature (enum `Feature`)**: `HONORARIOS_RECIBO` y `HONORARIOS_SUSPENSION`, premium. En
  `FeatureAccessService`: para `SERVICIOS_PROFESIONALES` el precio de upgrade es 14.90 y el plan
  requerido `serv-premium`; ambas features son premium (free ve preview bloqueado).
- **Entities + repos**: `HonorarioRequest`, `SuspensionRequest` (+ sus repos). `SubscriptionService`
  ya soporta activar premium por planCode → funciona con `serv-premium`.
- **Services**:
  - `HonorarioService` (app): `create(userId, form)` con gating `HONORARIOS_RECIBO` (402 si free),
    `myRequests(userId)`, `myFiles(id)` con ownership. Notifica a backoffice/registra.
  - `SuspensionService` (app): `create(userId, periodo/fecha)` con gating `HONORARIOS_SUSPENSION`,
    `myRequests(userId)`.
  - `BackofficeHonorarioService`: list/setStatus/attach(recibo) + notifica al usuario en
    GENERADO/OBSERVADO/adjunto. `BackofficeSuspensionService`: list/setStatus/attach(constancia) +
    notifica en TRAMITADA/OBSERVADA.
- **Controllers**:
  - `/api/v1/app/honorarios` (recibos: POST/GET, GET /{id}/files) y
    `/api/v1/app/suspensiones` (POST/GET) — auth usuario app.
  - `/api/v1/backoffice/honorarios` y `/api/v1/backoffice/suspensiones` — permiso de staff
    (reusar `receipt:read`/`receipt:process` como en taxi/peya, o crear `honorarios:process` vía
    migración siguiendo el patrón V26/V30).
- Recompilar con `mvn -DskipTests compile`; la migración debe aplicar (backend hasta V30 hoy).

## Móvil (`features/honorarios`, patrón features/taxi y features/peya)
- En `home_screen` `_ModuleTab`: si `clientType == SERVICIOS_PROFESIONALES` y premium
  (`featureAccessProvider('HONORARIOS_RECIBO')`.allowed) → `HonorariosScreen`; si free → `_LockedModule`
  con CTA "Activar Premium S/ 14.90".
- `HonorariosScreen` (embedded) con dos secciones/tabs:
  - **Recibos por Honorarios**: formulario (empresa/cliente, doc, descripción, monto, retención) →
    POST create; lista de solicitudes con badge de estado y, si GENERADO, botón para copiar/abrir el
    PDF del recibo.
  - **Suspensión de 4ta**: selector de calendario (elegir periodo/fecha) → botón "Generar solicitud
    de suspensión"; lista con estado y, si TRAMITADA, la constancia (copiar/abrir enlace).
- Domain models + `HonorariosRepository`/`SuspensionRepository` (Dio) + providers Riverpod.
- Gating real con `featureAccessProvider` (bloquea por defecto si error).
- `flutter analyze` limpio.

## Backoffice (nueva sección, patrón Solicitudes/Peya)
- Nav "Honorarios" (o dos: Recibos y Suspensiones) gateado por el permiso de staff elegido.
- Tabla de solicitudes de recibo (usuario/cliente/monto/estado) + modal Gestionar: cambiar estado,
  adjuntar el PDF del recibo (por URL, consistente con lo existente), ver adjuntos → notifica al
  usuario.
- Tabla de suspensiones (usuario/periodo/estado) + modal Gestionar: cambiar estado, adjuntar
  constancia → notifica al usuario.
- Hooks TanStack en `src/features/honorarios/api.ts`. `premiumPlanFor` del backoffice: mapear
  `SERVICIOS_PROFESIONALES → serv-premium`. `tsc --noEmit` limpio.

## Criterios de aceptación
- Un profesional premium ve el módulo con las dos secciones; un free ve preview bloqueado + CTA 14.90.
- Puede enviar el formulario de Recibo por Honorarios y ver el estado; al generarse, descarga el PDF.
- Puede elegir en calendario y generar una solicitud de Suspensión de 4ta; al tramitarse, ve la
  constancia.
- El backoffice lista y procesa ambos flujos, adjunta documentos y notifica al usuario.
- Backend compila y migra; `flutter analyze` y backoffice `tsc --noEmit` limpios. Sin emojis, español.
```
