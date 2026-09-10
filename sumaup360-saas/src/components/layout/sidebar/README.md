# components/layout/sidebar — Navegación lateral

Renderiza el menú a partir de los `enabledModules` de la sesión, consultando
`src/lib/module-registry.ts` para obtener etiqueta, ruta, grupo e icono de cada módulo.
Un módulo ausente de `enabledModules` no aparece. Las acciones internas se gatean por
`permissions` (UX, no seguridad). Responsive: colapsable en móvil (360/390/768/1280).
