---
name: flutter-feature-blueprint
description: Estructura estandar de un feature en slice vertical para sumaup360_app y como se conecta con router, providers y red. Usalo al crear o revisar cualquier feature en lib/features/.
---

# flutter-feature-blueprint

Plano de un feature de `sumaup360_app`. Cada feature vive en `lib/features/<feature>/`
con cuatro capas y un flujo de dependencias unidireccional.

## Capas

```
lib/features/<feature>/
  domain/        Modelos Freezed + contratos (interfaces) de repositorio. Puro Dart.
  data/          Datasources Dio + repos que implementan los contratos. DTOs json_serializable.
  application/   Providers/notifiers Riverpod: estado de UI y casos de uso.
  presentation/  Screens y widgets. Solo consumen providers.
```

Dependencias: `presentation -> application -> domain` y `data -> domain` (data implementa
los contratos de domain). presentation y application **no** conocen Dio.

## Como se conecta

- **Router**: las screens del feature se registran en el GoRouter de `lib/app/`.
- **Providers**: el notifier del feature se expone como provider; la UI lo observa con `ref.watch`.
- **Red**: el repo del feature usa el cliente Dio de `core/network/` (ya inyecta el idToken).
- **Errores**: el repo mapea fallos a `Failure`/`AppException` de `core/errors/`.
- **UI compartida**: estados carga/error/vacio con widgets de `shared/widgets/` y tema de `core/theme/`.

## Checklist

1. Modelos + contrato en `domain/`.
2. Datasource + repo en `data/`.
3. Providers/notifiers en `application/`.
4. Screens con 3 estados en `presentation/`.
5. Rutas en `lib/app/`.
6. `dart run build_runner build --delete-conflicting-outputs`.

Features actuales: onboarding, auth, diagnosis, home, income, expenses, receipts, alerts,
plans, profile, chatbot, subscription.
