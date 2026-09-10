# features/auth — Identidad y sesión

Login con Firebase (identidad) y obtención del `idToken`. Tras autenticar, el backend resuelve
y entrega el `SessionContext` (tenant, empresa, plan, membresía, `enabledModules`, roles,
permisos). El frontend solo guarda la sesión y refleja permisos; **nunca** decide autorización.
Ver `../../../docs/06-auth-security.md` (raíz) y skill `sumaup-auth-firebase-rbac`.
