# Despliegue — Backend (sumaup360-backend)

Notas de despliegue del backend Spring Boot. Detalle final en Fase 9.

## Build
```
cd sumaup360-backend
mvn clean package
java -jar target/sumaup360-backend-*.jar --spring.profiles.active=prod
```

## Requisitos de entorno
- Java 21.
- PostgreSQL accesible; base `sumaup360_db` creada. Flyway aplica el esquema al arrancar.
- Variables/secretos:
  - Credenciales de PostgreSQL (host, puerto, usuario, contraseña).
  - `FIREBASE_SERVICE_ACCOUNT` — ruta o contenido del service account de Firebase Admin
    (NUNCA en el repositorio).
  - Secretos de JWT interno si se habilita.

## Perfiles
- `local` — desarrollo (definido en `application.yml`).
- `prod` — pendiente: pool de conexiones, logging, CORS restringido, OpenAPI según política.

## Checklist previo a producción
- Flyway aplica en limpio sobre una base nueva.
- Verificación de idToken Firebase activa y probada.
- Enforcement de RBAC + `tenant_id` verificado.
- CORS limitado a los orígenes de landing y SaaS.
