# PRP: Notifications module

<!-- Example PRP. Generated to illustrate the template filled out for this repo. -->

## 1. Context

Customer-facing comms are scattered — the only notifier today is `customers/infrastructure/LoggingWelcomeNotifier`, which logs a fake welcome email on `CustomerService.register`. As we add more async customer comms (order confirmation, low-stock warnings), we need a dedicated bounded context that owns the contract between "something happened in a domain module" and "a message goes out."

This PRP creates a `notifications` module that listens to domain events from other modules and dispatches notifications via a pluggable port. It does not implement a real email adapter — that's a follow-up PRP once we pick a provider.

## 2. Success criteria

- A `notifications` top-level package exists with `domain/`, `application/`, `infrastructure/` subpackages following the conventions in `DDD.md` and `MODULITH.md`.
- `@ApplicationModuleListener` in `notifications` consumes `CustomerRegistered` and `OrderCompleted` events and invokes the notifications application service.
- `NotificationService.sendWelcome(CustomerIdentifier, EmailAddress)` and `NotificationService.sendOrderConfirmation(OrderIdentifier)` exist.
- Default adapter is `LoggingNotificationSender` (writes to SLF4J at INFO) — same pattern as today's `LoggingWelcomeNotifier`.
- `CustomerService.register` no longer directly invokes `WelcomeNotifier`; the behavior moves to the new listener. `WelcomeNotifier` + `LoggingWelcomeNotifier` are deleted.
- `EndToEndOrderFlowTests` still passes.
- `ModulithStructureTests` and `JMoleculesRulesTests` still pass.
- New `NotificationsModuleTests` (`@ApplicationModuleTest`) asserts that consuming `CustomerRegistered` publishes a `NotificationSent` event.
- `./mvnw verify` green.

## 3. Relevant existing code

- `src/main/java/com/example/demo/customers/infrastructure/LoggingWelcomeNotifier.java:15-26` — the port+adapter pattern to mirror for the new `NotificationSender` abstraction.
- `src/main/java/com/example/demo/inventory/infrastructure/InventoryListener.java:18-33` — how `@ApplicationModuleListener` methods are shaped in this codebase (async + tx + after-commit semantics come with the annotation).
- `src/main/java/com/example/demo/customers/application/CustomerService.java:20-26` — current usage site of `WelcomeNotifier`. Must be updated to stop calling it directly.
- `src/test/java/com/example/demo/customers/CustomersModuleTests.java:17-29` — shape of `@ApplicationModuleTest` + `PublishedEvents` assertion, to clone for `NotificationsModuleTests`.
- `DDD.md` — summary table for where to place each new class (section "Summary: where each concern lives").

## 4. Architecture notes

### Placement
- Module: **new** (`com.example.demo.notifications`)
- New files:
  - `notifications/package-info.java`
  - `notifications/domain/package-info.java` (`@NamedInterface`)
  - `notifications/domain/NotificationSent.java` (domain event, published externally)
  - `notifications/application/package-info.java`
  - `notifications/application/NotificationService.java`
  - `notifications/application/NotificationSender.java` (port interface)
  - `notifications/infrastructure/package-info.java`
  - `notifications/infrastructure/LoggingNotificationSender.java` (default adapter)
  - `notifications/infrastructure/CustomerEventsListener.java` (`@ApplicationModuleListener` on `CustomerRegistered`)
  - `notifications/infrastructure/OrderEventsListener.java` (`@ApplicationModuleListener` on `OrderCompleted`)

### Cross-module impact
- Events published: `NotificationSent` (from the listener, after the adapter returns)
- Events consumed: `CustomerRegistered` (from `customers.domain`), `OrderCompleted` (from `orders.domain`)
- Application services called cross-module: none needed — the event payloads carry everything

### Data model
- No new aggregates, no new persisted state. Notifications is purely a reactor module.
- No Flyway migration required.

### External dependencies
- Port: `NotificationSender` interface in `notifications/application/` with methods `sendWelcome(EmailAddress to)` and `sendOrderConfirmation(OrderIdentifier orderId, EmailAddress to)`.
- Adapter: `LoggingNotificationSender` in `notifications/infrastructure/`.

### Test strategy
- `NotificationsModuleTests` (`@ApplicationModuleTest` slicing just notifications).
- No changes needed to `EndToEndOrderFlowTests` — it should pass unchanged (it didn't assert on welcome behavior anyway).
- `CustomersModuleTests` loses the welcome assertion (it was incidental).

### Deletions
- `customers/application/WelcomeNotifier.java`
- `customers/infrastructure/LoggingWelcomeNotifier.java`
- `customers/infrastructure/package-info.java` if it becomes empty

## 5. Task list

- [ ] 1. Create empty `notifications` module skeleton: the four `package-info.java` files with `@NamedInterface` on `notifications/domain/` and `notifications/application/`.
      Verify: `./mvnw test -Dtest=ModulithStructureTests`

- [ ] 2. Add `notifications/domain/NotificationSent.java` as a `@DomainEvent` record.
      Verify: `./mvnw test -Dtest=JMoleculesRulesTests`

- [ ] 3. Add `notifications/application/NotificationSender.java` (interface) and `NotificationService.java` (service that calls it and registers the `NotificationSent` event).
      Verify: `./mvnw test`

- [ ] 4. Add `notifications/infrastructure/LoggingNotificationSender.java` implementing `NotificationSender`. Mirror `LoggingWelcomeNotifier` style.
      Verify: `./mvnw test`

- [ ] 5. Add `notifications/infrastructure/CustomerEventsListener.java` with `@ApplicationModuleListener` on `CustomerRegistered`, calling `NotificationService.sendWelcome(...)`.
      Verify: `./mvnw test`

- [ ] 6. Add `notifications/infrastructure/OrderEventsListener.java` with `@ApplicationModuleListener` on `OrderCompleted`, calling `NotificationService.sendOrderConfirmation(...)`.
      Verify: `./mvnw test`

- [ ] 7. Remove `customers/application/WelcomeNotifier.java` and its adapter. Remove the call from `CustomerService.register`.
      Verify: `./mvnw test -Dtest=CustomersModuleTests`

- [ ] 8. Add `NotificationsModuleTests` (`@ApplicationModuleTest`) that publishes a `CustomerRegistered` and asserts a `NotificationSent` was published in response.
      Verify: `./mvnw test -Dtest=NotificationsModuleTests`

- [ ] 9. Run full quality gate.
      Verify: `./mvnw verify`

## 6. Validation gates

- `./mvnw verify` (full pipeline, final gate)
- `./mvnw test -Dtest=ModulithStructureTests` (module boundaries still clean)
- `./mvnw test -Dtest=JMoleculesRulesTests` (DDD tactical rules still hold)
- `./mvnw test -Dtest=EndToEndOrderFlowTests` (existing scenario unchanged)
- `./mvnw test -Dtest=NotificationsModuleTests` (new module works in isolation)

## 7. Error handling / risks

- **Risk:** the `notifications.domain` package isn't `@NamedInterface`, so nothing else can import `NotificationSent`. Guard: step 1 adds the annotation.
- **Risk:** deleting `WelcomeNotifier` breaks `CustomersModuleTests` if it asserted on logs. Check before step 7; if it does, relax the assertion.
- **Risk:** `OrderCompleted` doesn't currently carry an `EmailAddress`, so `sendOrderConfirmation` can't resolve the customer's email. **Resolve during task 6** by calling `CustomerService.get(customerId)` cross-module (customers' `application` is already `@NamedInterface`).
- **Rollback:** if `./mvnw verify` fails and the cause is unclear, `git reset --hard <task-1-commit>` and re-plan the broken step.

## 8. Out of scope

- Real email delivery (SendGrid/SES/etc.) — follow-up PRP.
- Templated message bodies — v2.
- Retry on adapter failure — v2.
- Per-customer notification preferences (opt-out) — v2.
- Persistence of notification history — v2.
