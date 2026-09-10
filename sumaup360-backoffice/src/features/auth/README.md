# features/auth — Autenticación de staff

Login de staff por Firebase (mismo proyecto del ecosistema) → `idToken` → el backend verifica y
resuelve el contexto **staff** (≠ cliente) con su rol. Gestiona sesión y exposición del
`StaffSessionContext`. La autorización la decide el backend; aquí solo se refleja.
