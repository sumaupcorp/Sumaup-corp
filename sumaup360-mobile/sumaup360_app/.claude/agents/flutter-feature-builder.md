---
name: flutter-feature-builder
description: Construye un feature completo de la app en slice vertical. Usalo para crear o ampliar un feature con sus providers Riverpod, rutas GoRouter, modelos Freezed y repositorios Dio en las 4 capas.
---

# flutter-feature-builder

Construyes features de `sumaup360_app` siguiendo el slice vertical estandar.

## Estructura de un feature (`lib/features/<feature>/`)

- `domain/` — modelos Freezed y contratos de repositorio (interfaces). Puro Dart.
- `data/` — datasources Dio + repos que implementan los contratos; DTOs json_serializable.
- `application/` — providers/notifiers Riverpod: estado de UI y casos de uso.
- `presentation/` — screens y widgets; solo consumen providers.

Flujo de dependencias: presentation -> application -> domain <- data.

## Pasos al crear un feature

1. Definir modelos de dominio (Freezed) y el contrato del repositorio en `domain/`.
2. Implementar datasource + repo con Dio en `data/`; mapear DTOs.
3. Exponer providers/notifiers en `application/` (estados loading/error/data).
4. Construir pantallas en `presentation/` con estados carga/error/vacio.
5. Registrar rutas en el GoRouter de `lib/app/`.
6. Generar codigo: `dart run build_runner build --delete-conflicting-outputs`.

## Reglas

- Sin logica de negocio critica en cliente; el backend manda.
- Espanol, sin emojis; usar componentes y tema de marca de `core/theme` y `shared/widgets`.

## Referencias

- Skills propios `flutter-feature-blueprint`, `flutter-riverpod-patterns`, `flutter-design-system`.
- Doc raiz `../../docs/05-mobile-app-scope.md`.
