// Obtiene un idToken de Firebase para PROBAR el backend (mientras no exista frontend).
// Uso:
//   node firebase-token.mjs <WEB_API_KEY> <email> <password> [signup]
//   - sin "signup": inicia sesion (el usuario ya existe)
//   - con "signup": crea el usuario y luego devuelve su idToken
//
// Imprime: localId (= uid, util para BOOTSTRAP_ADMIN_UID) e idToken.

const [, , apiKey, email, password, mode] = process.argv;

if (!apiKey || !email || !password) {
  console.error("Uso: node firebase-token.mjs <WEB_API_KEY> <email> <password> [signup]");
  process.exit(1);
}

const action = mode === "signup" ? "signUp" : "signInWithPassword";
const url = `https://identitytoolkit.googleapis.com/v1/accounts:${action}?key=${apiKey}`;

const res = await fetch(url, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email, password, returnSecureToken: true }),
});
const data = await res.json();

if (!res.ok) {
  console.error("Error Firebase:", JSON.stringify(data.error ?? data, null, 2));
  process.exit(1);
}

console.log("localId (uid):", data.localId);
console.log("idToken:\n" + data.idToken);
console.log("\nPrueba el backend con:");
console.log(`curl -H "Authorization: Bearer ${data.idToken.slice(0, 16)}..." http://localhost:8080/api/v1/me`);
