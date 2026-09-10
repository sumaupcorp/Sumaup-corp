# components/modules — Componentes genéricos por módulo

Componentes parametrizados que sirven a varios módulos sin duplicar código: cabecera de
módulo, vista de lista/detalle genérica, barra de acciones, vacíos por módulo, etc. Reciben
la configuración del módulo (desde el registro) y los datos vía `features/`. Materializan el
principio "un dashboard que se configura por rubro", no un dashboard por rubro.
