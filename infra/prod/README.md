# Despliegue de producción — SUMAUP360 (VPS + Docker Compose)

Un solo VPS corre **todo** con Docker Compose, detrás de **Caddy** (reverse proxy con HTTPS
automático). El `sunat` queda interno (no expuesto); solo el backend lo llama.

```
                 Internet
                    │
             ┌──────▼──────┐   (80/443, HTTPS auto)
             │    Caddy     │
             └──┬───┬───┬───┘
   api.dominio  │   │   │  app.dominio / staff.dominio
        ┌───────▼┐ ┌▼──────┐ ┌▼─────────┐
        │backend │ │ saas  │ │backoffice │
        └──┬──┬──┘ └───────┘ └───────────┘
           │  │
     ┌─────▼┐ └────▼─────┐
     │ pg   │  │  sunat   │ (interno)
     └──────┘  └──────────┘
```

## 1. Requisitos del VPS

- Ubuntu 22.04/24.04, **mínimo 2 vCPU / 4 GB RAM** (el build de Java + 2 Next + Playwright pide RAM;
  con 2 GB usa swap o construye las imágenes en otra máquina).
- Docker Engine + plugin Compose:
  ```bash
  curl -fsSL https://get.docker.com | sh
  ```
- Puertos **80** y **443** abiertos en el firewall del proveedor.

## 2. DNS (usando tu dominio actual)

En el panel DNS de tu dominio (donde ya tienes la landing) agrega **3 registros A** apuntando a
la **IP pública del VPS**. No toques el registro de tu landing (raíz / www).

| Tipo | Nombre  | Valor           |
|------|---------|-----------------|
| A    | `api`   | IP_DEL_VPS      |
| A    | `app`   | IP_DEL_VPS      |
| A    | `staff` | IP_DEL_VPS      |

Resultado: `api.tudominio.com`, `app.tudominio.com`, `staff.tudominio.com`. Caddy saca los
certificados HTTPS solo la primera vez que se levanta (necesita que el DNS ya resuelva).

## 3. Traer el código al VPS

```bash
git clone https://github.com/sumaupcorp/Sumaup-corp.git
cd Sumaup-corp/infra/prod
```

## 4. Configurar secretos

```bash
cp .env.example .env
nano .env                 # completa DOMAIN, passwords, llaves, etc.

mkdir -p secrets
# Sube tu service account de Firebase a: secrets/firebase-service-account.json
```

> Genera secretos fuertes:
> ```bash
> openssl rand -base64 48   # para JWT_SECRET
> openssl rand -hex 24      # para DB_PASSWORD / SUNAT_API_KEY
> ```

## 5. Levantar todo

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Primera vez tarda (compila el backend con Maven, 2 builds de Next y baja Playwright).
Sigue el arranque:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

Al arrancar el backend, **Flyway** crea los schemas y tablas en la base nueva.

## 6. Verificar

```bash
curl https://api.tudominio.com/api/v1/health     # backend OK
```
- `https://app.tudominio.com`   → dashboard SaaS
- `https://staff.tudominio.com` → backoffice interno

## 7. Conectar la app móvil (Flutter → Play Store)

La app apunta al backend por su URL base. Antes de generar el bundle de Play Store:

1. Pon la URL de prod del API (`https://api.tudominio.com`) en la config del cliente Dio del
   proyecto `sumaup360-mobile` (base URL del entorno de producción).
2. Reconstruye:
   ```bash
   cd sumaup360-mobile/sumaup360_app
   flutter pub get
   dart run build_runner build --delete-conflicting-outputs
   flutter build appbundle --release
   ```
3. Asegúrate de que el dominio del backend esté permitido en CORS (`CORS_ALLOWED_ORIGINS`)
   — para apps nativas no aplica CORS, pero sí para cualquier webview.
4. Sube el `.aab` a Play Console.

## 8. Actualizar (redeploy)

```bash
cd Sumaup-corp && git pull
cd infra/prod
docker compose -f docker-compose.prod.yml up -d --build

# Si cambiaste NEXT_PUBLIC_* o la URL del API, reconstruye los frontends:
docker compose -f docker-compose.prod.yml up -d --build saas backoffice
```

## Notas / seguridad

- `SECURITY_DEV_MODE=false` en prod (sin backdoor `X-Debug-Uid`). Ya viene fijo en el compose.
- Swagger/OpenAPI desactivado en prod (fijo en el compose).
- El `sunat` corre el navegador **visible bajo Xvfb** dentro del contenedor (el WAF F5 de SUNAT
  bloquea headless desde datacenter). Si aun así bloquea, considera un proxy residencial peruano.
- Backups de la base:
  ```bash
  docker compose -f docker-compose.prod.yml exec postgres \
    pg_dump -U "$DB_USER" "$DB_NAME" > backup_$(date +%F).sql
  ```
- La base **no** se expone al exterior. Para administrarla, túnel SSH al puerto 5432 del host
  (si lo mapeas) o `docker compose exec postgres psql ...`.
- `.env` y `secrets/` están en `.gitignore`: nunca se suben.
```
