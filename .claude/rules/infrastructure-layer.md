---
paths:
  - "src/main/java/com/example/demo/*/infrastructure/**/*.java"
---

# Rules for the `infrastructure/` layer

Loaded only when working on files under any module's `infrastructure/` package. This is where
adapters and integrations live — code that translates between the outside world and the
application.

## Listeners (inbound adapters for events)

- Package-private class annotated `@org.springframework.stereotype.Component`.
- Each handler method annotated `@org.springframework.modulith.events.ApplicationModuleListener`.
- Handler invokes the module's application service — don't duplicate business logic.
- Example: `InventoryListener.on(ProductAdded event)` → `inventory.track(event.productId())`.

`@ApplicationModuleListener` is already:
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — fires only after publisher's tx commits
- `@Async` — runs on a different thread
- `@Transactional(propagation = REQUIRES_NEW)` — listener runs in its own tx

So do NOT add `@Transactional`, `@Async`, or `@TransactionalEventListener` yourself — the
composite annotation provides them.

## External-service adapters

- Package-private class annotated `@Component`.
- Implements the application-layer port interface.
- Holds HTTP clients, SDK clients, config, retry/circuit-breaker concerns — none of these
  should leak into the application or domain layers.

## Package-info

- `@org.jspecify.annotations.NullMarked`.
- Do **not** mark infrastructure as `@NamedInterface` — it's always internal. Other modules
  shouldn't import from `infrastructure/` of any module.

## Don't

- Don't use `@EventListener` without `@ApplicationModuleListener` for cross-module events.
  Plain `@EventListener` runs synchronously in the publisher's transaction, coupling the two
  modules' transactional fates.
- Don't add `@Transactional` inside a listener — it already has one.
- Don't make repository impls by hand. Spring Data generates them.
- Don't put business logic here. Listeners and adapters are translators; business rules live in
  the aggregate or domain service.
- Don't import from another module's `infrastructure/` or `web/`.
