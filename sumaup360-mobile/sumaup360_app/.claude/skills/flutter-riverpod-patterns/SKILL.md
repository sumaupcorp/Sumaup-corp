---
name: flutter-riverpod-patterns
description: Convenciones de Riverpod para sumaup360_app — providers, notifiers, modelado de estado y consumo desde la UI. Usalo al escribir o revisar la capa application de cualquier feature.
---

# flutter-riverpod-patterns

Patrones de estado con Riverpod en `sumaup360_app`. La capa `application/` de cada feature
expone el estado; la UI (`presentation/`) lo consume.

## Tipos de provider

- `Provider` — dependencias inmutables (cliente Dio, repos, config).
- `FutureProvider` — lecturas asincronas simples (one-shot).
- `NotifierProvider` / `AsyncNotifierProvider` — estado mutable con logica (formularios,
  listas con acciones, sesion). Preferir estos para casos de uso.

## Estado

- Modelar estado de pantalla como dato inmutable (Freezed) o usar `AsyncValue<T>`.
- Cubrir siempre **loading / error / data(vacio o con datos)**.
- En la UI: `final estado = ref.watch(miProvider);` y `estado.when(data:, loading:, error:)`.
- Acciones: `ref.read(miProvider.notifier).hacerAlgo()`.

## Convenciones

- Un provider por responsabilidad; nombres claros (`incomeListProvider`, `authNotifierProvider`).
- Los providers viven en `application/`; no exponer Dio ni repos directamente a la UI.
- Inyectar dependencias via `ref.watch(otroProvider)`, no instanciar a mano.
- Limpiar/invalidar estado en logout (`ref.invalidate`).
- Evitar logica de negocio critica: orquestar repos, no decidir permisos (eso es del backend).

## Anti-patrones

- No usar `setState` para estado de negocio.
- No llamar a Dio desde widgets.
- No guardar tokens en providers en memoria como fuente unica: la fuente es secure storage.
