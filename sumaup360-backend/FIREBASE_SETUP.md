# Conectar Firebase (dev) al backend SUMAUP360

El backend ya está preparado: verifica el `idToken` con Firebase Admin SDK y decide la
autorización (RBAC) por su cuenta. Solo falta conectar tu proyecto Firebase.

## Qué necesitas (checklist)

1. **Un proyecto Firebase** (tu proyecto de dev). 1 solo proyecto para todo el ecosistema.
2. **Service Account JSON** (clave privada del Admin SDK) → para el BACKEND.
   - Firebase Console → ⚙ Configuración del proyecto → **Cuentas de servicio** →
     *Generar nueva clave privada* → descarga el `.json`.
3. **Un proveedor de inicio de sesión habilitado** → Console → Authentication → *Sign-in method*.
   - Para probar rápido: habilita **Correo electrónico/contraseña**.
   - (Google/Apple/Teléfono se habilitan igual; los usan la web y la app.)
4. **Web API Key** (solo para PROBAR obteniendo un idToken) →
   Console → ⚙ Configuración del proyecto → General → *Tu app web* → `apiKey`.

> El backend NO usa la Web API Key ni la config de cliente; solo usa el **service account**.
> La Web API Key y la config (`apiKey`, `authDomain`, `projectId`...) son para el frontend y
> la app móvil cuando se construyan.

## Paso 1 — Coloca el service account

Guarda el JSON descargado como (ya está en `.gitignore`, no se versiona):

```
sumaup360-backend/firebase-service-account.json
```

(O en otra ruta y apunta con la variable `FIREBASE_SERVICE_ACCOUNT`.)

## Paso 2 — Arranca el backend apuntando a Firebase

Variables (PowerShell, desde `sumaup360-backend/`):

```powershell
$env:FIREBASE_SERVICE_ACCOUNT = "$PWD\firebase-service-account.json"
$env:FIREBASE_PROJECT_ID      = "tu-project-id"   # opcional; normalmente se infiere del JSON
$env:SECURITY_DEV_MODE        = "false"           # desactiva el backdoor X-Debug-Uid
.\mvnw.cmd spring-boot:run
```

En el arranque debe decir: `Firebase Admin inicializado. Verificacion de idToken ACTIVA.`
(Si dice "INACTIVA", la ruta del service account es incorrecta.)

## Paso 3 — Haz que TU usuario sea admin (bootstrap)

La primera vez que un `uid` inicia sesión, el backend lo provisiona. Para que TU usuario quede
como **STAFF admin** automáticamente, define su `uid` antes de su primer login:

```powershell
$env:BOOTSTRAP_ADMIN_UID = "EL_UID_DE_TU_USUARIO_FIREBASE"
```

(El `uid` lo obtienes al crear/loguear el usuario — ver Paso 4, campo `localId`.)

## Paso 4 — Obtener un idToken de prueba (sin frontend aún)

Como la web y la app todavía no existen, usa el script incluido para crear/loguear un usuario
y obtener su `idToken` vía la API REST de Firebase:

```bash
# crear usuario de prueba y obtener idToken (la primera vez usa "signup")
node scripts/firebase-token.mjs <WEB_API_KEY> test@correo.com MiClave123 signup
# siguientes veces:
node scripts/firebase-token.mjs <WEB_API_KEY> test@correo.com MiClave123
```

Te imprime el `localId` (= el **uid**) y el `idToken`.

## Paso 5 — Llamar al backend con el idToken real

```bash
curl -H "Authorization: Bearer <ID_TOKEN>" http://localhost:8080/api/v1/me
```

Si configuraste `BOOTSTRAP_ADMIN_UID` con ese `uid`, `/me` mostrará `userType: STAFF` y los
permisos de admin. Si no, será un usuario `PERSON` sin roles (correcto para la app).

## Notas de seguridad

- `SECURITY_DEV_MODE=false` en cualquier entorno con Firebase real (desactiva `X-Debug-Uid`).
- El service account es secreto: nunca lo subas a git (ya está en `.gitignore`).
- En producción, inyecta `FIREBASE_SERVICE_ACCOUNT` por variable/secret manager, no por archivo.
