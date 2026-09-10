---
name: saas-rbac-gating
description: Cómo ocultar o deshabilitar UI por permiso sin confiar en el cliente. Úsalo al gatear botones, acciones, rutas o secciones según los permisos del usuario, recordando que la autorización real la decide el backend.
---

# Skill: gating de RBAC en la UI

Gatear la UI mejora la experiencia, **no** sustituye la seguridad. El backend es la autoridad.

## Principio

```
Firebase prueba identidad → idToken
Backend verifica idToken, resuelve roles/permisos/tenant → autoriza
Frontend solo refleja: oculta/deshabilita lo que el usuario no puede hacer
```

## Cómo gatear

1. Leer `permissions` (`recurso:accion`) del `SessionContext`.
2. Usar un helper `can(resource, action)` que solo consulta permisos ya entregados.
3. Ocultar o deshabilitar acciones cuando `can(...)` es falso; nunca dejar la acción rota.
4. Reflejar límites de plan/membresía (ej. techo de sucursales/usuarios alcanzado).

## Reglas de oro

1. **Nunca** confiar en el cliente para autorizar: el backend reevalúa cada request y filtra
   por `tenant_id`. Ocultar un botón no protege el endpoint.
2. Nunca usar claims del cliente como verdad para operaciones sensibles.
3. Manejar 401 (reautenticar) y 403 (sin permiso) con mensajes claros.
4. Un rol alto no habilita ver datos de otro tenant.

## Referencias

`../../docs/06-auth-security.md`, skill raíz `sumaup-auth-firebase-rbac`,
agente `saas-rbac-ui`.
