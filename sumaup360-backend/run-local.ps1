# Arranca el backend SUMAUP360 conectado a Firebase (dev).
# Uso:  .\run-local.ps1      (desde la carpeta sumaup360-backend)
#
# Requiere el service account en firebase-service-account.json (ya en .gitignore).

$env:FIREBASE_SERVICE_ACCOUNT = "$PSScriptRoot\firebase-service-account.json"
$env:SECURITY_DEV_MODE        = "false"   # tokens reales; sin backdoor X-Debug-Uid
# IA: OpenAI (GPT) como proveedor principal; Gemini queda de respaldo.
$env:AI_PROVIDER              = "openai"
$env:OPENAI_MODEL             = "gpt-4o-mini"

# Llaves de IA (secretas): viven en run-local.secrets.ps1 (ignorado por git).
# Copia run-local.secrets.example.ps1 -> run-local.secrets.ps1 y pon tus llaves.
$secrets = "$PSScriptRoot\run-local.secrets.ps1"
if (Test-Path $secrets) {
    . $secrets
} else {
    Write-Warning "Falta run-local.secrets.ps1 (OPENAI_API_KEY / GEMINI_API_KEY). La IA caera al generador de reglas."
}
# Super-admin de bootstrap (este uid queda STAFF admin al iniciar sesion):
$env:BOOTSTRAP_ADMIN_UID      = "nM7QwIy19ZUlMJJi0buTiSMEJeG3"   # prueba.saas@sumaup.dev

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
